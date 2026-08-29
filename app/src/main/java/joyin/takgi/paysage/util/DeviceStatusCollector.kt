package joyin.takgi.paysage.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import joyin.takgi.paysage.R

data class UptimeParts(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long
)

data class DeviceStatus(
    val batteryLevel: Int,
    val batteryTemperature: Float,
    val isCharging: Boolean,
    val batteryHealth: String,
    val batteryVoltage: Float,
    val chargeSource: String,
    val timestamp: Long = System.currentTimeMillis(),
    val uptimeMillis: Long = 0L
)

object DeviceStatusCollector {
    /**
     * Splits elapsed time since boot into display-friendly units. Android's
     * elapsedRealtime() is monotonic and includes time spent in deep sleep.
     */
    fun uptimeParts(uptimeMillis: Long): UptimeParts {
        val totalSeconds = uptimeMillis.coerceAtLeast(0L) / 1_000L
        val days = totalSeconds / (24L * 60L * 60L)
        val hours = (totalSeconds % (24L * 60L * 60L)) / (60L * 60L)
        val minutes = (totalSeconds % (60L * 60L)) / 60L
        val seconds = totalSeconds % 60L
        return UptimeParts(days, hours, minutes, seconds)
    }

    fun collectStatus(context: Context): DeviceStatus {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            -1
        }

        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val batteryTemperature = if (temperature >= 0) temperature / 10f else -1f

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val batteryHealth = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.battery_health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.battery_health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.battery_health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.battery_health_over_voltage)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.battery_health_cold)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.battery_health_unspecified)
            else -> context.getString(R.string.battery_health_unknown)
        }

        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val batteryVoltage = if (voltage >= 0) voltage / 1000f else -1f

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargeSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.charge_source_ac)
            BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.charge_source_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.charge_source_wireless)
            0 -> context.getString(R.string.charge_source_none)
            else -> context.getString(R.string.charge_source_unknown)
        }

        return DeviceStatus(
            batteryLevel = batteryLevel,
            batteryTemperature = batteryTemperature,
            isCharging = isCharging,
            batteryHealth = batteryHealth,
            batteryVoltage = batteryVoltage,
            chargeSource = chargeSource,
            uptimeMillis = SystemClock.elapsedRealtime()
        )
    }

    fun formatStatusReport(context: Context, status: DeviceStatus): String {
        return buildString {
            appendLine(context.getString(R.string.device_status_title))
            appendLine("━━━━━━━━━━━━")

            val batteryEmoji = when {
                status.batteryLevel < 0 -> "❓"
                status.batteryLevel >= 90 -> "🔋"
                status.batteryLevel >= 60 -> "🔋"
                status.batteryLevel >= 30 -> "🔋"
                status.batteryLevel >= 15 -> "🪫"
                else -> "🪫"
            }

            val chargingStatus = if (status.isCharging) {
                context.getString(R.string.status_charging)
            } else {
                context.getString(R.string.status_not_charging)
            }

            if (status.batteryLevel >= 0) {
                appendLine("$batteryEmoji ${context.getString(R.string.battery_level)}: ${status.batteryLevel}% ($chargingStatus)")
            } else {
                appendLine("$batteryEmoji ${context.getString(R.string.battery_level)}: ${context.getString(R.string.status_unavailable)}")
            }

            val tempEmoji = when {
                status.batteryTemperature < 0 -> "❓"
                status.batteryTemperature >= 45f -> "🔥"
                status.batteryTemperature >= 40f -> "🌡️"
                else -> "🌡️"
            }

            if (status.batteryTemperature >= 0) {
                val tempWarning = if (status.batteryTemperature >= 45f) {
                    " ⚠️ ${context.getString(R.string.temperature_warning)}"
                } else ""
                appendLine("$tempEmoji ${context.getString(R.string.battery_temperature)}: ${String.format("%.1f", status.batteryTemperature)}°C$tempWarning")
            } else {
                appendLine("$tempEmoji ${context.getString(R.string.battery_temperature)}: ${context.getString(R.string.status_unavailable)}")
            }

            if (status.batteryVoltage >= 0) {
                appendLine("⚡ ${context.getString(R.string.battery_voltage)}: ${String.format("%.2f", status.batteryVoltage)}V")
            }

            appendLine("🏥 ${context.getString(R.string.battery_health)}: ${status.batteryHealth}")

            if (status.isCharging) {
                appendLine("🔌 ${context.getString(R.string.charge_source)}: ${status.chargeSource}")
            }

            val uptime = uptimeParts(status.uptimeMillis)
            val uptimeText = buildList {
                if (uptime.days > 0) {
                    add(context.getString(R.string.uptime_days, uptime.days))
                }
                if (uptime.hours > 0) {
                    add(context.getString(R.string.uptime_hours, uptime.hours))
                }
                if (uptime.minutes > 0) {
                    add(context.getString(R.string.uptime_minutes, uptime.minutes))
                }
                if (uptime.seconds > 0 || isEmpty()) {
                    add(context.getString(R.string.uptime_seconds, uptime.seconds))
                }
            }.joinToString(" ")
            appendLine("⏱️ ${context.getString(R.string.device_uptime)}: $uptimeText")

            appendLine()
            val formattedTime = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date(status.timestamp))
            appendLine("🕐 ${context.getString(R.string.last_updated)}: $formattedTime")
        }
    }
}
