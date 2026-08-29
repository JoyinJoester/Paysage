package joyin.takgi.paysage.reliability

import android.content.Context

data class BatteryAlertSettings(
    val enabled: Boolean = true,
    val lowBatteryPercent: Int = 15,
    val highTemperatureCelsius: Int = 45,
    val continuousTemperatureAlertsEnabled: Boolean = false,
    val continuousTemperatureIntervalMinutes: Int = 30
)

class BatteryAlertSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun read(): BatteryAlertSettings =
        BatteryAlertSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, true),
            lowBatteryPercent = preferences.getInt(KEY_LOW_BATTERY_PERCENT, 15)
                .coerceIn(MIN_LOW_BATTERY_PERCENT, MAX_LOW_BATTERY_PERCENT),
            highTemperatureCelsius = preferences.getInt(KEY_HIGH_TEMPERATURE_CELSIUS, 45)
                .coerceIn(MIN_HIGH_TEMPERATURE_CELSIUS, MAX_HIGH_TEMPERATURE_CELSIUS),
            continuousTemperatureAlertsEnabled = preferences.getBoolean(
                KEY_CONTINUOUS_TEMPERATURE_ALERTS_ENABLED,
                false
            ),
            continuousTemperatureIntervalMinutes = preferences.getInt(
                KEY_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES,
                30
            ).coerceIn(
                MIN_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES,
                MAX_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES
            )
        )

    fun write(settings: BatteryAlertSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(
                KEY_LOW_BATTERY_PERCENT,
                settings.lowBatteryPercent.coerceIn(MIN_LOW_BATTERY_PERCENT, MAX_LOW_BATTERY_PERCENT)
            )
            .putInt(
                KEY_HIGH_TEMPERATURE_CELSIUS,
                settings.highTemperatureCelsius.coerceIn(
                    MIN_HIGH_TEMPERATURE_CELSIUS,
                    MAX_HIGH_TEMPERATURE_CELSIUS
                )
            )
            .putBoolean(
                KEY_CONTINUOUS_TEMPERATURE_ALERTS_ENABLED,
                settings.continuousTemperatureAlertsEnabled
            )
            .putInt(
                KEY_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES,
                settings.continuousTemperatureIntervalMinutes.coerceIn(
                    MIN_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES,
                    MAX_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES
                )
            )
            .apply()
    }

    fun lastAlertAt(kind: String): Long = preferences.getLong(KEY_LAST_ALERT_PREFIX + kind, 0L)

    fun markAlerted(kind: String, timestamp: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_LAST_ALERT_PREFIX + kind, timestamp)
            .apply()
    }

    fun isConditionActive(kind: String): Boolean =
        preferences.getBoolean(KEY_ACTIVE_CONDITION_PREFIX + kind, false)

    fun markConditionActive(kind: String) {
        preferences.edit()
            .putBoolean(KEY_ACTIVE_CONDITION_PREFIX + kind, true)
            .apply()
    }

    fun clearConditionActive(kind: String) {
        preferences.edit()
            .putBoolean(KEY_ACTIVE_CONDITION_PREFIX + kind, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "battery_alert_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LOW_BATTERY_PERCENT = "low_battery_percent"
        private const val KEY_HIGH_TEMPERATURE_CELSIUS = "high_temperature_celsius"
        private const val KEY_CONTINUOUS_TEMPERATURE_ALERTS_ENABLED =
            "continuous_temperature_alerts_enabled"
        private const val KEY_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES =
            "continuous_temperature_interval_minutes"
        private const val KEY_LAST_ALERT_PREFIX = "last_alert_"
        private const val KEY_ACTIVE_CONDITION_PREFIX = "active_condition_"

        const val MIN_LOW_BATTERY_PERCENT = 1
        const val MAX_LOW_BATTERY_PERCENT = 100
        const val MIN_HIGH_TEMPERATURE_CELSIUS = 30
        const val MAX_HIGH_TEMPERATURE_CELSIUS = 90
        const val MIN_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES = 15
        const val MAX_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES = 360
    }
}
