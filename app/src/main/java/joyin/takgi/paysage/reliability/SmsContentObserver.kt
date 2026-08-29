package joyin.takgi.paysage.reliability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SmsContentObserver(
    private val context: Context,
    handler: Handler,
    private val scope: CoroutineScope
) : ContentObserver(handler) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val processMutex = Mutex()

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        scope.launch {
            delay(DEBOUNCE_MS)
            processMutex.withLock {
                dispatchNewInboxMessages()
            }
        }
    }

    /**
     * 只处理上次之后新写入收件箱的行。旧实现每次都重读最近 10 分钟
     * 再全部交去重,一旦去重窗口错过就会重复转发;这里改为按 _ID 增量。
     */
    private suspend fun dispatchNewInboxMessages() {
        val lastSeenId = preferences.getLong(KEY_LAST_SEEN_ID, NO_LAST_SEEN_ID)
        val query = queryNewInboxMessages(lastSeenId) ?: return

        // 首次运行也会检查最近窗口,兜住广播漏收的消息;
        // 是否真的转发由入口去重与已转发的广播副本比对决定。
        query.requests.forEach { request ->
            SmsSegmentBuffer.enqueue(context, request)
        }
        if (query.maxSeenId > lastSeenId) {
            preferences.edit().putLong(KEY_LAST_SEEN_ID, query.maxSeenId).apply()
        }
    }

    private fun queryNewInboxMessages(lastSeenId: Long): InboxQueryResult? {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        val selection = if (lastSeenId == NO_LAST_SEEN_ID) {
            "${Telephony.Sms.DATE} >= ?"
        } else {
            "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms._ID} > ?"
        }
        val selectionArgs = if (lastSeenId == NO_LAST_SEEN_ID) {
            arrayOf(cutoff.toString())
        } else {
            arrayOf(cutoff.toString(), lastSeenId.toString())
        }
        val sortOrder = "${Telephony.Sms._ID} ASC"

        return runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                var maxSeenId = lastSeenId.coerceAtLeast(NO_LAST_SEEN_ID)
                val requests = buildList {
                    while (cursor.moveToNext()) {
                        val type = cursor.getInt(typeIndex)
                        if (type != Telephony.Sms.MESSAGE_TYPE_INBOX) continue
                        val sender = cursor.getString(addressIndex).orEmpty()
                        val content = cursor.getString(bodyIndex).orEmpty()
                        val timestamp = cursor.getLong(dateIndex)
                        if (sender.isNotBlank() && content.isNotBlank()) {
                            add(
                                SmsForwardRequest(
                                    sender = sender,
                                    content = content,
                                    timestamp = timestamp,
                                    source = "content_observer"
                                )
                            )
                        }
                        val rowId = cursor.getLong(idIndex)
                        if (rowId > maxSeenId) {
                            maxSeenId = rowId
                        }
                    }
                }
                InboxQueryResult(requests, maxSeenId)
            }
        }.getOrNull()
    }

    private data class InboxQueryResult(
        val requests: List<SmsForwardRequest>,
        val maxSeenId: Long
    )

    companion object {
        private const val PREFERENCES_NAME = "paysage_sms_content_observer"
        private const val KEY_LAST_SEEN_ID = "last_seen_id"
        private const val NO_LAST_SEEN_ID = -1L
        private const val RECENT_WINDOW_MS = 10L * 60L * 1000L
        private const val DEBOUNCE_MS = 300L
    }
}
