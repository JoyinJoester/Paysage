package joyin.takgi.paysage.reliability

import android.content.Context
import java.util.Locale

class SmsDedupeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "paysage_sms_dedupe",
        Context.MODE_PRIVATE
    )

    fun markIfNew(request: SmsForwardRequest): Boolean {
        val now = System.currentTimeMillis()
        cleanup(now)
        val key = request.stableKey()
        if (preferences.contains(key)) {
            return false
        }
        preferences.edit()
            .putLong(key, now)
            .apply()
        return true
    }

    private fun cleanup(now: Long) {
        val expiredKeys = preferences.all.mapNotNull { (key, value) ->
            val timestamp = value as? Long ?: return@mapNotNull key
            if (now - timestamp > DEDUPE_WINDOW_MS) key else null
        }
        if (expiredKeys.isEmpty()) return

        val editor = preferences.edit()
        expiredKeys.forEach(editor::remove)
        editor.apply()
    }

    private fun SmsForwardRequest.stableKey(): String {
        val minuteBucket = timestamp / DEDUPE_BUCKET_MS
        val normalizedSender = sender.trim().lowercase(Locale.US)
        val normalizedContent = content.trim()
        return listOf(normalizedSender, normalizedContent, minuteBucket)
            .joinToString(separator = "|")
            .hashCode()
            .toString()
    }

    companion object {
        private const val DEDUPE_WINDOW_MS = 60L * 60L * 1000L
        private const val DEDUPE_BUCKET_MS = 60L * 1000L
    }
}
