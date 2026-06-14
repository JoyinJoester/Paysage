package joyin.takgi.paysage.reliability

import android.content.Context

class SmsForwardingControlStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isPaused(): Boolean = preferences.getBoolean(KEY_PAUSED, false)

    fun setPaused(paused: Boolean) {
        preferences.edit()
            .putBoolean(KEY_PAUSED, paused)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun updatedAt(): Long = preferences.getLong(KEY_UPDATED_AT, 0L)

    companion object {
        private const val PREFS_NAME = "paysage_forwarding_control"
        private const val KEY_PAUSED = "paused"
        private const val KEY_UPDATED_AT = "updated_at"
    }
}
