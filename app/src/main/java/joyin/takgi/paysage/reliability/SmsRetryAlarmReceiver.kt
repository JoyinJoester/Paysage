package joyin.takgi.paysage.reliability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import joyin.takgi.paysage.reliability.root.RootSmsFallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsRetryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        SmsReliabilityManager.enqueueImmediateRetry(context)
        SmsReliabilityManager.scheduleAlarm(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RootSmsFallback.check(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
