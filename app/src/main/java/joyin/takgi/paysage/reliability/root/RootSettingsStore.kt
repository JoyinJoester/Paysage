package joyin.takgi.paysage.reliability.root

import android.content.Context

class RootSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    var keepAliveScriptEnabled: Boolean
        get() = preferences.getBoolean(KEY_KEEP_ALIVE, false)
        set(value) = preferences.edit().putBoolean(KEY_KEEP_ALIVE, value).apply()

    var smsFallbackEnabled: Boolean
        get() = preferences.getBoolean(KEY_SMS_FALLBACK, false)
        set(value) = preferences.edit().putBoolean(KEY_SMS_FALLBACK, value).apply()

    var lastFallbackSmsId: Long
        get() = preferences.getLong(KEY_FALLBACK_SMS_ID, NO_LAST_SEEN_ID)
        set(value) = preferences.edit().putLong(KEY_FALLBACK_SMS_ID, value).apply()

    var lastGrantTimestamp: Long
        get() = preferences.getLong(KEY_LAST_GRANT, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_GRANT, value).apply()

    companion object {
        private const val PREFERENCES_NAME = "paysage_root_settings"
        private const val KEY_KEEP_ALIVE = "keep_alive_script_enabled"
        private const val KEY_SMS_FALLBACK = "sms_fallback_enabled"
        private const val KEY_FALLBACK_SMS_ID = "last_fallback_sms_id"
        private const val KEY_LAST_GRANT = "last_grant_timestamp"
        const val NO_LAST_SEEN_ID = -1L
    }
}
