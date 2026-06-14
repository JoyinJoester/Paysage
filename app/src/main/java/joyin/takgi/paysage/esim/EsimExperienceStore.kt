package joyin.takgi.paysage.esim

import android.content.Context

class EsimExperienceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "paysage_esim_experience",
        Context.MODE_PRIVATE
    )

    fun hasCompletedCompatibilityCheck(): Boolean =
        preferences.getLong(KEY_COMPATIBILITY_CHECKED_AT, 0L) > 0L

    fun markCompatibilityChecked() {
        preferences.edit()
            .putLong(KEY_COMPATIBILITY_CHECKED_AT, System.currentTimeMillis())
            .apply()
    }

    fun resetCompatibilityCheck() {
        preferences.edit()
            .remove(KEY_COMPATIBILITY_CHECKED_AT)
            .apply()
    }

    companion object {
        private const val KEY_COMPATIBILITY_CHECKED_AT = "compatibility_checked_at"
    }
}
