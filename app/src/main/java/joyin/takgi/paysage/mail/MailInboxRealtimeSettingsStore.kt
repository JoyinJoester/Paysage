package joyin.takgi.paysage.mail

import android.content.Context

data class MailInboxRealtimeSettings(
    val enabled: Boolean = false,
    val idleReconnectMinutes: Int = 15
)

class MailInboxRealtimeSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun read(): MailInboxRealtimeSettings =
        MailInboxRealtimeSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            idleReconnectMinutes = preferences.getInt(KEY_IDLE_RECONNECT_MINUTES, 15)
                .coerceIn(MIN_RECONNECT_MINUTES, MAX_RECONNECT_MINUTES)
        )

    fun write(settings: MailInboxRealtimeSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(
                KEY_IDLE_RECONNECT_MINUTES,
                settings.idleReconnectMinutes.coerceIn(MIN_RECONNECT_MINUTES, MAX_RECONNECT_MINUTES)
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "paysage_mail_inbox_realtime"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_IDLE_RECONNECT_MINUTES = "idle_reconnect_minutes"
        const val MIN_RECONNECT_MINUTES = 5
        const val MAX_RECONNECT_MINUTES = 60
    }
}
