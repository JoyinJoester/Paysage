package joyin.takgi.paysage.debug

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import joyin.takgi.paysage.BuildConfig
import joyin.takgi.paysage.R
import joyin.takgi.paysage.esim.EsimDiagnosticSanitizer
import joyin.takgi.paysage.esim.EsimPersistentDiagnostics
import joyin.takgi.paysage.esim.EsimSystemGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeveloperLogExport(
    val report: String,
    val lineCount: Int,
    val errorCount: Int,
    val warningCount: Int
)

data class DeveloperLogShare(
    val intent: Intent,
    val fileName: String,
    val filePath: String,
    val attached: Boolean,
    val externalCopyPath: String? = null,
    val warning: String? = null
)

object DeveloperLogExporter {
    private const val LOG_LINE_LIMIT = 1200
    private const val SHARE_DIR = "developer_logs"
    private const val SHARE_PREFIX = "paysage_logs_"
    private const val EASY_EUICC_ARA_M_SHA1 = "2A2FA878BC7C3354C2CF82935A5945A3EDAE4AFA"

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    suspend fun collect(context: Context): DeveloperLogExport = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val appProcessLogs = readLogcat(
            arrayOf(
                "logcat",
                "-d",
                "-v",
                "threadtime",
                "--pid",
                android.os.Process.myPid().toString(),
                "-t",
                LOG_LINE_LIMIT.toString(),
                "*:V"
            )
        )
        val crashLogs = readLogcat(
            arrayOf(
                "logcat",
                "-d",
                "-v",
                "threadtime",
                "-t",
                "300",
                "AndroidRuntime:E",
                "System.err:W",
                "libc:E",
                "*:S"
            )
        )
        val packageLogs = readLogcat(
            arrayOf(
                "logcat",
                "-d",
                "-v",
                "threadtime",
                "-t",
                "500",
                "${appContext.packageName}:V",
                "Paysage:V",
                "*:S"
            )
        )
        val relatedLogs = readPaysageRelatedLogcat(appContext.packageName)
        val euiccDiagnostics = EsimPersistentDiagnostics.read(appContext)
        val combinedLogs = buildString {
            appendSection("Persistent eUICC Diagnostics", euiccDiagnostics)
            appendSection("App Process Logcat", appProcessLogs)
            appendSection("Crash And System Errors", crashLogs)
            appendSection("Package Tagged Logs", packageLogs)
            appendSection("Paysage Related Logcat", relatedLogs)
        }.trim()
        val sanitizedLogs = sanitize(combinedLogs)
        val lines = sanitizedLogs.lineSequence().filter { it.isNotBlank() }.toList()

        val packageInfo = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        }.getOrNull()
        val araMSha1 = runCatching { EsimSystemGateway(appContext).araMAccessRuleSha1() }
            .getOrDefault("")

        val report = buildString {
            appendLine("=== Paysage Developer Log Report ===")
            appendLine("exportedAt=${timeFormatter.format(Date())}")
            appendLine("package=${appContext.packageName}")
            appendLine("versionName=${packageInfo?.versionName.orEmpty().ifBlank { "unknown" }}")
            appendLine("versionCode=${packageInfo.versionCodeCompat()}")
            appendLine("fullVersion=${BuildConfig.FULL_VERSION_NAME}")
            appendLine("buildDetail=${BuildConfig.BUILD_DETAIL_TAG}")
            appendLine("buildTime=${BuildConfig.BUILD_TIME}")
            appendLine("gitSha=${BuildConfig.GIT_SHA}")
            appendLine("apkArch=${BuildConfig.APK_ARCH}")
            appendLine("deviceAbis=${Build.SUPPORTED_ABIS.joinToString(",")}")
            appendLine("android=${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("paysageAraMSha1=${araMSha1.ifBlank { "unknown" }}")
            appendLine("easyEuiccAraMSha1=$EASY_EUICC_ARA_M_SHA1")
            appendLine("simSlotAraMNote=SIM-slot OMAPI removable eUICC access depends on card-side ARA-M authorization for this signing certificate.")
            appendLine()
            appendLine("=== Notes ===")
            appendLine(appContext.getString(R.string.developer_log_privacy_note))
            appendLine()
            appendLine("=== Logcat ===")
            if (sanitizedLogs.isBlank()) {
                appendLine(appContext.getString(R.string.developer_no_logs))
            } else {
                appendLine(sanitizedLogs)
            }
        }

        DeveloperLogExport(
            report = report,
            lineCount = lines.size,
            errorCount = lines.count { it.isErrorLine() },
            warningCount = lines.count { it.isWarningLine() }
        )
    }

    suspend fun clearLogcat(context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        context?.applicationContext?.let { appContext ->
            EsimPersistentDiagnostics.clear(appContext)
        }
        val process = runCatching {
            ProcessBuilder("logcat", "-c")
                .redirectErrorStream(true)
                .start()
        }.getOrNull() ?: return@withContext false
        runCatching { process.inputStream.bufferedReader().use { it.readText() } }
        runCatching { process.waitFor() }.getOrDefault(-1) == 0
    }

    suspend fun createShareIntent(context: Context, report: String): DeveloperLogShare = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val shareDir = File(appContext.cacheDir, SHARE_DIR).apply {
            if (!exists()) mkdirs()
        }
        cleanupOldFiles(shareDir)
        val file = File(shareDir, "$SHARE_PREFIX${fileFormatter.format(Date())}.txt")
        file.writeText(report)
        val externalCopy = writeExternalCopy(appContext, file.name, report)
        val uri = runCatching {
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
        }.getOrElse { error ->
            return@withContext DeveloperLogShare(
                intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, appContext.getString(R.string.developer_log_share_subject))
                    putExtra(Intent.EXTRA_TEXT, report)
                },
                fileName = file.name,
                filePath = file.absolutePath,
                attached = false,
                externalCopyPath = externalCopy?.absolutePath,
                warning = error.safeDescription()
            )
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appContext.getString(R.string.developer_log_share_subject))
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, appContext.getString(R.string.developer_log_share_hint))
            clipData = ClipData.newUri(appContext.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        DeveloperLogShare(
            intent = shareIntent,
            fileName = file.name,
            filePath = file.absolutePath,
            attached = true,
            externalCopyPath = externalCopy?.absolutePath
        )
    }

    private fun writeExternalCopy(context: Context, fileName: String, report: String): File? =
        runCatching {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
            val dir = File(baseDir, SHARE_DIR).apply {
                if (!exists()) mkdirs()
            }
            cleanupOldFiles(dir)
            File(dir, fileName).also { copy -> copy.writeText(report) }
        }.getOrNull()

    private fun StringBuilder.appendSection(title: String, body: String) {
        if (body.isBlank()) return
        if (isNotBlank()) appendLine()
        appendLine("---- $title ----")
        appendLine(body.trim())
    }

    private fun readLogcat(command: Array<String>): String {
        val process = runCatching {
            ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
        }.getOrNull() ?: return ""
        val output = runCatching {
            process.inputStream.bufferedReader().use { it.readText() }
        }.getOrDefault("")
        runCatching { process.waitFor() }
        return output.trim()
    }

    private fun readPaysageRelatedLogcat(packageName: String): String {
        val raw = readLogcat(
            arrayOf(
                "logcat",
                "-d",
                "-v",
                "threadtime",
                "-t",
                "1800",
                "*:V"
            )
        )
        if (raw.isBlank()) return ""
        val needles = listOf(
            packageName,
            packageName.substringAfterLast('.'),
            "takgi.paysage",
            "ysage:euicc_lpa",
            "Esim",
            "eSIM",
            "Euicc",
            "eUICC",
            "OMAPI",
            "ISD-R",
            "ARA-M",
            "lpac",
            "Lpac",
            "LocalProfileAssistant",
            "Fatal signal",
            "DEBUG",
            "libc"
        )
        return raw.lineSequence()
            .filter { line -> needles.any { needle -> line.contains(needle, ignoreCase = true) } }
            .take(LOG_LINE_LIMIT)
            .joinToString(separator = "\n")
    }

    private fun sanitize(raw: String): String =
        EsimDiagnosticSanitizer.sanitize(raw)

    private fun String.isErrorLine(): Boolean =
        contains("FATAL EXCEPTION", ignoreCase = true) ||
            contains(Regex("""\s[EF]\s[^:]+:""")) ||
            contains("[ERROR]", ignoreCase = true)

    private fun String.isWarningLine(): Boolean =
        contains(Regex("""\sW\s[^:]+:""")) ||
            contains("[WARN]", ignoreCase = true)

    private fun cleanupOldFiles(dir: File) {
        val files = dir.listFiles { file ->
            file.isFile && file.name.startsWith(SHARE_PREFIX) && file.name.endsWith(".txt")
        } ?: return
        files.sortedByDescending { it.lastModified() }
            .drop(10)
            .forEach { runCatching { it.delete() } }
    }

    private fun android.content.pm.PackageInfo?.versionCodeCompat(): Long =
        if (this == null) {
            -1L
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }

    private fun Throwable.safeDescription(): String {
        val message = message?.takeIf { it.isNotBlank() }
        return if (message == null) {
            javaClass.simpleName
        } else {
            "${javaClass.simpleName}: ${message.take(180)}"
        }
    }
}
