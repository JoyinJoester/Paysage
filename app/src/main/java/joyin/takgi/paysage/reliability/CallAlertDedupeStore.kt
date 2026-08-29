package joyin.takgi.paysage.reliability

import android.content.Context
import java.util.Locale

class CallAlertDedupeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "paysage_call_alert_dedupe",
        Context.MODE_PRIVATE
    )

    fun markIfNew(request: CallAlertRequest): Boolean {
        val now = System.currentTimeMillis()
        cleanup(now)
        val keys = request.stableKeys()
        if (keys.any(preferences::contains)) return false

        preferences.edit()
            .putLong(keys.first(), now)
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

    private fun CallAlertRequest.stableKeys(): List<String> {
        val bucket = timestamp / DEDUPE_BUCKET_MS
        return (bucket - 2..bucket + 2).map { stableKey(it) }
    }

    private fun CallAlertRequest.stableKey(bucket: Long): String {
        val normalizedNumber = number.trim().lowercase(Locale.US).ifBlank { "unknown" }
        return listOf(normalizedNumber, bucket)
            .joinToString(separator = "|")
            .hashCode()
            .toString()
    }

    companion object {
        private const val DEDUPE_WINDOW_MS = 2L * 60L * 60L * 1000L
        private const val DEDUPE_BUCKET_MS = 60L * 1000L
    }
}
