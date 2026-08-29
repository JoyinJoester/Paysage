package joyin.takgi.paysage.reliability

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import joyin.takgi.paysage.MainActivity
import joyin.takgi.paysage.R
import joyin.takgi.paysage.util.DeviceStatus
import joyin.takgi.paysage.util.DeviceStatusCollector

data class BatteryThresholdAlert(
    val kind: String,
    val title: String,
    val message: String,
    val deviceStatusReport: String,
    val timestamp: Long = System.currentTimeMillis()
)

object BatteryThresholdNotifier {
    fun checkAndNotify(context: Context, bypassCooldown: Boolean = false): List<BatteryThresholdAlert> {
        val appContext = context.applicationContext
        val settingsStore = BatteryAlertSettingsStore(appContext)
        val settings = settingsStore.read()
        if (!settings.enabled) return emptyList()

        val status = DeviceStatusCollector.collectStatus(appContext)
        val statusReport = DeviceStatusCollector.formatStatusReport(appContext, status)
        val alerts = mutableListOf<BatteryThresholdAlert>()
        if (
            status.batteryLevel in 0..settings.lowBatteryPercent &&
            !status.isCharging &&
            shouldNotify(settingsStore, KIND_LOW_BATTERY, bypassCooldown)
        ) {
            val title = appContext.getString(R.string.title_battery_low_alert)
            val message = appContext.getString(
                R.string.format_battery_low_alert,
                status.batteryLevel,
                settings.lowBatteryPercent
            )
            notifyThreshold(
                context = appContext,
                kind = KIND_LOW_BATTERY,
                title = title,
                message = message,
                notificationId = LOW_BATTERY_NOTIFICATION_ID,
                settingsStore = settingsStore,
                status = status
            )
            alerts += BatteryThresholdAlert(
                kind = KIND_LOW_BATTERY,
                title = title,
                message = message,
                deviceStatusReport = statusReport
            )
        }

        if (status.batteryTemperature >= settings.highTemperatureCelsius) {
            if (shouldNotifyHighTemperature(settingsStore, settings, bypassCooldown)) {
                val title = appContext.getString(R.string.title_battery_temperature_alert)
                val message = appContext.getString(
                    R.string.format_battery_temperature_alert,
                    status.batteryTemperature,
                    settings.highTemperatureCelsius
                )
                notifyThreshold(
                    context = appContext,
                    kind = KIND_HIGH_TEMPERATURE,
                    title = title,
                    message = message,
                    notificationId = HIGH_TEMPERATURE_NOTIFICATION_ID,
                    settingsStore = settingsStore,
                    status = status
                )
                alerts += BatteryThresholdAlert(
                    kind = KIND_HIGH_TEMPERATURE,
                    title = title,
                    message = message,
                    deviceStatusReport = statusReport
                )
            }
            settingsStore.markConditionActive(KIND_HIGH_TEMPERATURE)
        } else {
            settingsStore.clearConditionActive(KIND_HIGH_TEMPERATURE)
        }
        return alerts
    }

    private fun notifyThreshold(
        context: Context,
        kind: String,
        title: String,
        message: String,
        notificationId: Int,
        settingsStore: BatteryAlertSettingsStore,
        status: DeviceStatus? = null
    ) {
        ensureChannel(context)
        val detail = status?.let { DeviceStatusCollector.formatStatusReport(context, it) } ?: message
        if (canNotify(context)) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
                .setContentIntent(mainActivityIntent(context))
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
        settingsStore.markAlerted(kind)
    }

    private fun shouldNotify(
        store: BatteryAlertSettingsStore,
        kind: String,
        bypassCooldown: Boolean
    ): Boolean {
        if (bypassCooldown) return true
        val lastAlertAt = store.lastAlertAt(kind)
        return System.currentTimeMillis() - lastAlertAt >= ALERT_COOLDOWN_MS
    }

    private fun shouldNotifyHighTemperature(
        store: BatteryAlertSettingsStore,
        settings: BatteryAlertSettings,
        bypassCooldown: Boolean
    ): Boolean {
        if (bypassCooldown) return true
        if (!store.isConditionActive(KIND_HIGH_TEMPERATURE)) return true
        if (!settings.continuousTemperatureAlertsEnabled) return false
        val lastAlertAt = store.lastAlertAt(KIND_HIGH_TEMPERATURE)
        val repeatIntervalMs = settings.continuousTemperatureIntervalMinutes * 60L * 1000L
        return System.currentTimeMillis() - lastAlertAt >= repeatIntervalMs
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_battery_threshold_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_battery_threshold_channel_desc)
            }
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun mainActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private const val CHANNEL_ID = "paysage_battery_threshold"
    private const val KIND_LOW_BATTERY = "low_battery"
    private const val KIND_HIGH_TEMPERATURE = "high_temperature"
    private const val LOW_BATTERY_NOTIFICATION_ID = 3401
    private const val HIGH_TEMPERATURE_NOTIFICATION_ID = 3402
    private const val ALERT_COOLDOWN_MS = 2L * 60L * 60L * 1000L
}
