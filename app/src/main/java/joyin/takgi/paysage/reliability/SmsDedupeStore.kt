package joyin.takgi.paysage.reliability

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SmsDedupeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun markIfNew(request: SmsForwardRequest): Boolean {
        val now = System.currentTimeMillis()
        val decisionEntry = SmsDedupeEntry(
            sender = request.sender,
            content = request.content,
            timestamp = now
        )
        val entries = loadEntries()
        if (!SmsDedupePolicy.isNew(entries, decisionEntry, now)) {
            return false
        }
        saveEntries(SmsDedupePolicy.record(entries, decisionEntry, now))
        return true
    }

    private fun loadEntries(): List<SmsDedupeEntry> {
        cleanupLegacyBucketKeys()
        val raw = preferences.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                SmsDedupeEntry(
                    sender = item.optString(KEY_SENDER),
                    content = item.optString(KEY_CONTENT),
                    timestamp = item.optLong(KEY_TIMESTAMP)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveEntries(entries: List<SmsDedupeEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put(KEY_SENDER, entry.sender)
                    .put(KEY_CONTENT, entry.content)
                    .put(KEY_TIMESTAMP, entry.timestamp)
            )
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    // 旧版去重按分钟桶哈希存 key,清理掉避免无限累积
    private fun cleanupLegacyBucketKeys() {
        val legacyKeys = preferences.all.keys.filter { it != KEY_ENTRIES }
        if (legacyKeys.isEmpty()) return
        val editor = preferences.edit()
        legacyKeys.forEach(editor::remove)
        editor.apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "paysage_sms_dedupe"
        private const val KEY_ENTRIES = "entries"
        private const val KEY_SENDER = "sender"
        private const val KEY_CONTENT = "content"
        private const val KEY_TIMESTAMP = "timestamp"
    }
}
