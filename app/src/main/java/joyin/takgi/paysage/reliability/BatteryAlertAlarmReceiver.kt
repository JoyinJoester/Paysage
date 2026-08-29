package joyin.takgi.paysage.reliability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BatteryAlertAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BatteryAlertReliabilityManager.enqueueImmediateCheck(context)
        BatteryAlertReliabilityManager.scheduleAlarm(context)
    }
}
