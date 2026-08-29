package joyin.takgi.paysage.esim

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EsimPersistentDiagnostics {
    private const val FILE_NAME = "euicc_diagnostics.log"
    private const val MAX_LINES = 240
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun append(context: Context, stage: String, message: String) {
        val file = file(context)
        val line = "${formatter.format(Date())} stage=${stage.sanitizeField()} ${message.sanitizeDiagnosticLine()}"
        val existing = runCatching {
            if (file.exists()) file.readLines() else emptyList()
        }.getOrDefault(emptyList())
        val next = (existing + line).takeLast(MAX_LINES)
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(next.joinToString(separator = "\n", postfix = "\n"))
        }
    }

    @Synchronized
    fun read(context: Context): String =
        runCatching {
            val file = file(context)
            if (file.exists()) EsimDiagnosticSanitizer.sanitize(file.readText().trim()) else ""
        }.getOrDefault("")

    @Synchronized
    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    private fun String.sanitizeField(): String =
        replace(Regex("\\s+"), "_")
            .take(48)

    private fun String.sanitizeDiagnosticLine(): String =
        EsimDiagnosticSanitizer.sanitize(this, maxLength = 900)
}
