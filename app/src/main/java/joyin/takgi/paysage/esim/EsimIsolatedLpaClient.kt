package joyin.takgi.paysage.esim

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal object EsimIsolatedLpaClient {
    suspend fun readExternalProfileSummaries(context: Context): EsimExternalProfileReadResult =
        withTimeoutOrNull(READ_TIMEOUT_MS) {
            val appContext = context.applicationContext
            val sources = EsimSystemGateway(appContext).externalProfileSourceCandidates()
            if (sources.isEmpty()) {
                return@withTimeoutOrNull bindAndRead(appContext, null)
            }

            val profiles = mutableListOf<EsimExternalProfileSummary>()
            val messages = mutableListOf<String>()
            val availableSources = mutableListOf<EsimExternalProfileSource>()
            sources.groupBy { source -> source.channelGroupKey }.values.forEach { group ->
                val openedAids = mutableListOf<ByteArray>()
                for (source in group) {
                    if (!shouldAttemptSource(openedAids, source)) {
                        Log.i(
                            TAG,
                            "Skipping external eUICC source after valid channel: source=${source.label}, opened=${openedAids.joinToString { EsimApdu.aidLabel(it) }}"
                        )
                        break
                    }
                    val result = bindAndRead(appContext, source)
                    profiles += result.profiles
                    messages += result.messages.ifEmpty { listOf(result.summaryMessage) }
                    availableSources += result.availableSources
                    if (source.kind == EsimExternalProfileSourceKind.Omapi && result.shouldStopOmapiGroup()) {
                        EsimPersistentDiagnostics.append(
                            appContext,
                            "isolated_client_group_stop",
                            "source=${source.diagnosticLabel()} reason=${result.summaryMessage}"
                        )
                        Log.w(
                            TAG,
                            "Stopping OMAPI attempts for reader after blocked source=${source.label}, reason=${result.summaryMessage}"
                        )
                        break
                    }
                    if (result.availableSources.isNotEmpty()) {
                        openedAids += source.isdrAid
                    }
                }
            }
            EsimExternalProfileReadResult(
                profiles = profiles.distinctBy { "${it.source.identity}:${it.iccid}" },
                messages = messages,
                availableSources = availableSources.distinctBy { it.identity },
                summaryMessageOverride = when {
                    profiles.isNotEmpty() -> "Read ${profiles.size} external eUICC profile(s)."
                    messages.isNotEmpty() -> compactExternalProfileReadMessages(appContext, messages)
                    else -> "No external eUICC profiles were read."
                }
            )
        } ?: interruptedResult("外置 eUICC 读取进程超时，已保护主界面不闪退。")

    private suspend fun bindAndRead(
        appContext: Context,
        source: EsimExternalProfileSource?
    ): EsimExternalProfileReadResult =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean(false)
                var bound = false

                fun finish(result: EsimExternalProfileReadResult, connection: ServiceConnection? = null) {
                    if (!completed.compareAndSet(false, true)) return
                    EsimPersistentDiagnostics.append(
                        appContext,
                        "isolated_client_finish",
                        "source=${source.diagnosticLabel()} profiles=${result.profiles.size} sources=${result.availableSources.size} message=${result.summaryMessage}"
                    )
                    if (bound && connection != null) {
                        runCatching { appContext.unbindService(connection) }
                    }
                    continuation.resume(result)
                }

                lateinit var connection: ServiceConnection
                val replyMessenger = Messenger(
                    Handler(Looper.getMainLooper()) { message ->
                        if (message.what == EsimIsolatedLpaService.MSG_EXTERNAL_PROFILE_RESULT) {
                            val result = when (message.arg1) {
                                EsimIsolatedLpaService.RESULT_OK -> runCatching {
                                    EsimExternalProfileJson.decodeReadResult(
                                        message.data.getString(EsimIsolatedLpaService.KEY_RESULT_JSON).orEmpty()
                                    )
                                }.getOrElse { error ->
                                    interruptedResult("外置 eUICC 读取结果解析失败：${error.message ?: error.javaClass.simpleName}")
                                }
                                else -> interruptedResult(
                                    "外置 eUICC 读取失败：${
                                        message.data.getString(EsimIsolatedLpaService.KEY_ERROR_MESSAGE)
                                            ?: "未知错误"
                                    }"
                                )
                            }
                            finish(result, connection)
                            true
                        } else {
                            false
                        }
                    }
                )

                connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        EsimPersistentDiagnostics.append(
                            appContext,
                            "isolated_client_connected",
                            "source=${source.diagnosticLabel()} component=${name?.flattenToShortString().orEmpty()}"
                        )
                        if (service == null) {
                            finish(interruptedResult("外置 eUICC 读取进程没有返回 Binder。"), this)
                            return
                        }
                        val serviceMessenger = Messenger(service)
                        val request = Message.obtain(null, EsimIsolatedLpaService.MSG_EXTERNAL_PROFILE_READ)
                        request.replyTo = replyMessenger
                        source?.let {
                            request.data.putString(
                                EsimIsolatedLpaService.KEY_SOURCE_JSON,
                                EsimExternalProfileJson.encodeSourceString(it)
                            )
                        }
                        try {
                            serviceMessenger.send(request)
                        } catch (error: RemoteException) {
                            Log.w(TAG, "Unable to request isolated external eUICC read", error)
                            finish(
                                interruptedResult("无法连接外置 eUICC 读取进程：${error.message ?: error.javaClass.simpleName}"),
                                this
                            )
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        EsimPersistentDiagnostics.append(
                            appContext,
                            "isolated_client_disconnected",
                            "source=${source.diagnosticLabel()} component=${name?.flattenToShortString().orEmpty()}"
                        )
                        finish(interruptedResult(source.interruptedMessage("读取进程已中断")), this)
                    }

                    override fun onBindingDied(name: ComponentName?) {
                        EsimPersistentDiagnostics.append(
                            appContext,
                            "isolated_client_binding_died",
                            "source=${source.diagnosticLabel()} component=${name?.flattenToShortString().orEmpty()}"
                        )
                        finish(interruptedResult(source.interruptedMessage("读取进程绑定已失效，请重新读取")), this)
                    }

                    override fun onNullBinding(name: ComponentName?) {
                        EsimPersistentDiagnostics.append(
                            appContext,
                            "isolated_client_null_binding",
                            "source=${source.diagnosticLabel()} component=${name?.flattenToShortString().orEmpty()}"
                        )
                        finish(interruptedResult("外置 eUICC 读取进程无法绑定。"), this)
                    }
                }

                continuation.invokeOnCancellation {
                    if (completed.compareAndSet(false, true) && bound) {
                        runCatching { appContext.unbindService(connection) }
                    }
                }

                bound = appContext.bindService(
                    Intent(appContext, EsimIsolatedLpaService::class.java),
                    connection,
                    Context.BIND_AUTO_CREATE
                )
                EsimPersistentDiagnostics.append(
                    appContext,
                    "isolated_client_bind",
                    "source=${source.diagnosticLabel()} bound=$bound"
                )
                if (!bound) {
                    finish(interruptedResult("外置 eUICC 读取进程启动失败。"), connection)
                }
            }
        }

    private fun interruptedResult(message: String): EsimExternalProfileReadResult =
        EsimExternalProfileReadResult(
            profiles = emptyList(),
            messages = listOf(message),
            availableSources = emptyList(),
            summaryMessageOverride = message
        )

    private fun EsimExternalProfileSource?.interruptedMessage(reason: String): String =
        if (this == null) {
            "外置 eUICC $reason，主界面已保持运行。"
        } else {
            "${label} $reason，已继续尝试其他读卡通道。"
        }

    private fun EsimExternalProfileSource?.diagnosticLabel(): String =
        if (this == null) {
            "all"
        } else {
            "identity=$identity kind=$kind aid=$isdrAidLabel label=$label"
        }

    private val EsimExternalProfileSource.channelGroupKey: String
        get() = buildString {
            append(kind.name)
            append(':')
            append(key)
            append(':')
            when (kind) {
                EsimExternalProfileSourceKind.UsbCcid -> {
                    append("if")
                    append(usbInterfaceId ?: 0)
                }
                EsimExternalProfileSourceKind.Omapi -> {
                    append("slot")
                    append(logicalSlotIndex ?: -1)
                }
            }
        }

    private fun shouldAttemptSource(
        openedAids: List<ByteArray>,
        source: EsimExternalProfileSource
    ): Boolean {
        if (openedAids.isEmpty()) return true
        if (openedAids.any { !EsimApdu.isEstkSpecificIsdRAid(it) }) return false
        if (source.isdrAid.contentEquals(EsimApdu.ISD_R_AID)) return false
        return EsimApdu.isEstkSpecificIsdRAid(source.isdrAid)
    }

    private fun EsimExternalProfileReadResult.shouldStopOmapiGroup(): Boolean =
        profiles.isEmpty() && messages.any { message ->
            message.indicatesOmapiHardBlock()
        }

    private fun String.indicatesOmapiHardBlock(): Boolean =
        contains("访问规则拒绝") ||
            contains("ARA-M", ignoreCase = true) ||
            contains("access rule", ignoreCase = true) ||
            contains("access rules", ignoreCase = true) ||
            contains("rejected Paysage", ignoreCase = true) ||
            contains("OMAPI 服务连接超时") ||
            contains("OMAPI service", ignoreCase = true) && contains("timeout", ignoreCase = true) ||
            contains("OMAPI 与安全元素通信失败") ||
            contains("secure element communication", ignoreCase = true)

    private const val TAG = "EsimIsolatedLpaClient"
    private const val READ_TIMEOUT_MS = 45_000L
}
