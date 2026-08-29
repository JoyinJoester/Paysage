package joyin.takgi.paysage.reliability

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.util.DateFormatter

class BatteryThresholdAlertForwarder(context: Context) {
    private val appContext = context.applicationContext
    private val alertSender = ForwardAccountAlertSender(appContext)

    suspend fun forward(alerts: List<BatteryThresholdAlert>) {
        alerts.forEach { alert ->
            val subject = appContext.getString(R.string.format_battery_alert_forward_subject, alert.title)
            val body = buildBody(alert)
            alertSender.send(subject, body)
        }
    }

    private fun buildBody(alert: BatteryThresholdAlert): String =
        appContext.getString(
            R.string.format_battery_alert_forward_body,
            alert.message,
            DateFormatter.format(alert.timestamp),
            alert.deviceStatusReport
        )
}
