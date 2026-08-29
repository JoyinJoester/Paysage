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

/**
 * MMS 采集:观察 content://mms,按 _ID 增量读取新收到的彩信,
 * 提取发件人与文本后走统一的分段合并 + 入口去重。
 * 纯媒体彩信(无文本)跳过;读文本不读附件,避免大附件内存与流量开销。
 */
class MmsContentObserver(
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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_MMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!hasReadSmsPermission()) {
            return
        }

        scope.launch {
            delay(DEBOUNCE_MS)
            processMutex.withLock {
                dispatchNewMms()
            }
        }
    }

    private fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private suspend fun dispatchNewMms() {
        val lastSeenId = preferences.getLong(KEY_LAST_SEEN_ID, NO_LAST_SEEN_ID)
        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        val selection =
            "${Telephony.Mms.MESSAGE_BOX} = ? AND ${Telephony.Mms.DATE} >= ? AND ${Telephony.Mms._ID} > ?"
        val selectionArgs = arrayOf(
            Telephony.Mms.MESSAGE_BOX_INBOX.toString(),
            (cutoff / 1000).toString(),
            lastSeenId.toString()
        )

        val result = runCatching {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE),
                selection,
                selectionArgs,
                "${Telephony.Mms._ID} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)
                var maxSeenId = lastSeenId
                val requests = buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        if (id > maxSeenId) maxSeenId = id
                        val request = readMmsRequest(id, cursor.getLong(dateIndex) * 1000)
                        if (request != null) add(request)
                    }
                }
                MmsQueryResult(requests, maxSeenId)
            }
        }.getOrNull()

        val query = result ?: return
        query.requests.forEach { request ->
            SmsSegmentBuffer.enqueue(context, request)
        }
        if (query.maxSeenId > lastSeenId) {
            preferences.edit().putLong(KEY_LAST_SEEN_ID, query.maxSeenId).apply()
        }
    }

    private fun readMmsRequest(mmsId: Long, timestampMs: Long): SmsForwardRequest? {
        val sender = readSender(mmsId)
        if (sender.isNullOrBlank()) return null
        val text = MmsTextExtractor.extractText(readParts(mmsId))
        if (text.isBlank()) return null
        return SmsForwardRequest(
            sender = sender,
            content = text,
            timestamp = timestampMs,
            source = "mms"
        )
    }

    private fun readSender(mmsId: Long): String? =
        runCatching {
            context.contentResolver.query(
                Uri.parse("${Telephony.Mms.CONTENT_URI}/$mmsId/addr"),
                arrayOf(Telephony.Mms.Addr.ADDRESS),
                "${Telephony.Mms.Addr.TYPE} = ?",
                arrayOf(ADDRESS_TYPE_FROM.toString()),
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS)
                if (cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()

    private fun readParts(mmsId: Long): List<MmsPart> =
        runCatching {
            context.contentResolver.query(
                Uri.parse("${Telephony.Mms.CONTENT_URI}/$mmsId/part"),
                arrayOf(Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.TEXT),
                null,
                null,
                null
            )?.use { cursor ->
                val ctIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE)
                val textIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT)
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            MmsPart(
                                contentType = cursor.getString(ctIndex),
                                text = cursor.getString(textIndex)
                            )
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())

    private data class MmsQueryResult(
        val requests: List<SmsForwardRequest>,
        val maxSeenId: Long
    )

    companion object {
        private const val PREFERENCES_NAME = "paysage_mms_observer"
        private const val KEY_LAST_SEEN_ID = "last_seen_id"
        private const val NO_LAST_SEEN_ID = 0L
        private const val RECENT_WINDOW_MS = 10L * 60L * 1000L
        private const val DEBOUNCE_MS = 500L
        private const val ADDRESS_TYPE_FROM = 137
    }
}
