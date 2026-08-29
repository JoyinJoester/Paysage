package joyin.takgi.paysage.esim

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EsimIsolatedLpaService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messenger = Messenger(IncomingHandler(this))

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun readExternalProfiles(replyTo: Messenger?) {
        if (replyTo == null) return
        readExternalProfiles(replyTo, null)
    }

    private fun readExternalProfiles(replyTo: Messenger?, source: EsimExternalProfileSource?) {
        if (replyTo == null) return
        scope.launch {
            val reply = Message.obtain(null, MSG_EXTERNAL_PROFILE_RESULT)
            val data = Bundle()
            runCatching {
                val gateway = EsimSystemGateway(applicationContext)
                val result = if (source == null) {
                    Log.i(
                        TAG,
                        "Isolated external eUICC read start: all sources, paysageAraM=${gateway.araMAccessRuleSha1()}, easyEuiccAraM=$EASY_EUICC_ARA_M_SHA1"
                    )
                    EsimPersistentDiagnostics.append(
                        applicationContext,
                        "isolated_read_start",
                        "source=all paysageAraM=${gateway.araMAccessRuleSha1()} easyEuiccAraM=$EASY_EUICC_ARA_M_SHA1"
                    )
                    gateway.externalProfileSummariesInCurrentProcess()
                } else {
                    Log.i(
                        TAG,
                        "Isolated external eUICC read start: source=${source.label}, identity=${source.identity}, kind=${source.kind}, aid=${source.isdrAidLabel}, logicalSlot=${source.logicalSlotIndex}, paysageAraM=${gateway.araMAccessRuleSha1()}, easyEuiccAraM=$EASY_EUICC_ARA_M_SHA1"
                    )
                    EsimPersistentDiagnostics.append(
                        applicationContext,
                        "isolated_read_start",
                        "source=${source.label} identity=${source.identity} kind=${source.kind} aid=${source.isdrAidLabel} logicalSlot=${source.logicalSlotIndex} paysageAraM=${gateway.araMAccessRuleSha1()} easyEuiccAraM=$EASY_EUICC_ARA_M_SHA1"
                    )
                    gateway.externalProfileSummariesForSourceInCurrentProcess(source)
                }
                EsimPersistentDiagnostics.append(
                    applicationContext,
                    "isolated_read_result",
                    "profiles=${result.profiles.size} sources=${result.availableSources.size} message=${result.summaryMessage}"
                )
                data.putString(KEY_RESULT_JSON, EsimExternalProfileJson.encodeReadResult(result))
                reply.arg1 = RESULT_OK
            }.onFailure { error ->
                Log.w(TAG, "Isolated external eUICC read failed", error)
                EsimPersistentDiagnostics.append(
                    applicationContext,
                    "isolated_read_error",
                    "error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                )
                data.putString(KEY_ERROR_MESSAGE, error.message ?: error.javaClass.simpleName)
                reply.arg1 = RESULT_ERROR
            }
            reply.data = data
            try {
                replyTo.send(reply)
            } catch (error: RemoteException) {
                Log.w(TAG, "Unable to deliver isolated external eUICC read result", error)
            }
        }
    }

    private class IncomingHandler(
        private val service: EsimIsolatedLpaService
    ) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_EXTERNAL_PROFILE_READ -> {
                    val source = msg.data.getString(KEY_SOURCE_JSON)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { raw -> runCatching { EsimExternalProfileJson.decodeSourceString(raw) }.getOrNull() }
                    service.readExternalProfiles(msg.replyTo, source)
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    companion object {
        private const val TAG = "EsimIsolatedLpaService"

        const val MSG_EXTERNAL_PROFILE_READ = 1
        const val MSG_EXTERNAL_PROFILE_RESULT = 2

        const val RESULT_OK = 1
        const val RESULT_ERROR = 2

        const val KEY_RESULT_JSON = "result_json"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_SOURCE_JSON = "source_json"

        private const val EASY_EUICC_ARA_M_SHA1 = "2A2FA878BC7C3354C2CF82935A5945A3EDAE4AFA"
    }
}
