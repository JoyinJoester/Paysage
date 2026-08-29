package joyin.takgi.paysage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import joyin.takgi.paysage.reliability.SmsForwardRequest
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import joyin.takgi.paysage.reliability.SmsSegmentBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SmsReliabilityManager.ensureScheduled(context)
                    messages.toForwardRequests().forEach { request ->
                        SmsSegmentBuffer.enqueue(context, request)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun Array<SmsMessage>.toForwardRequests(): List<SmsForwardRequest> =
        filter { message ->
            message.displayOriginatingAddress?.isNotBlank() == true &&
                message.displayMessageBody?.isNotBlank() == true
        }
            .groupBy { message -> message.displayOriginatingAddress.orEmpty() }
            .mapNotNull { (sender, parts) ->
                val content = parts.joinToString(separator = "") { part ->
                    part.displayMessageBody.orEmpty()
                }.trim()
                if (content.isBlank()) {
                    null
                } else {
                    SmsForwardRequest(
                        sender = sender,
                        content = content,
                        timestamp = parts.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis(),
                        source = "broadcast"
                    )
                }
            }
}
