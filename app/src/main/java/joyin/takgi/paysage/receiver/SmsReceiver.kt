package joyin.takgi.paysage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import joyin.takgi.paysage.reliability.SmsForwardDispatcher
import joyin.takgi.paysage.reliability.SmsForwardRequest
import joyin.takgi.paysage.reliability.SmsReliabilityManager
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
                    messages.forEach { smsMessage ->
                        val request = SmsForwardRequest(
                            sender = smsMessage.displayOriginatingAddress.orEmpty(),
                            content = smsMessage.displayMessageBody.orEmpty(),
                            timestamp = smsMessage.timestampMillis,
                            source = "broadcast"
                        )
                        if (request.sender.isNotBlank() && request.content.isNotBlank()) {
                            SmsForwardDispatcher.dispatch(context, request)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
