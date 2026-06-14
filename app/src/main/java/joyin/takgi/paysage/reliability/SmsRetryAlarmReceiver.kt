package joyin.takgi.paysage.reliability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsRetryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        SmsReliabilityManager.enqueueImmediateRetry(context)
        SmsReliabilityManager.scheduleAlarm(context)
    }
}
