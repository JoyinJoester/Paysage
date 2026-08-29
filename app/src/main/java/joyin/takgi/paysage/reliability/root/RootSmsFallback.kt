package joyin.takgi.paysage.reliability.root

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.Telephony
import joyin.takgi.paysage.reliability.SmsForwardRequest
import joyin.takgi.paysage.reliability.SmsSegmentBuffer
import java.io.File

/**
 * Root 短信库兜底:广播/观察器都被 ROM 拦掉时,经 root 拷贝
 * mmssms.db 后按 _ID 增量读取收件箱,仍走分段合并与入口去重。
 */
object RootSmsFallback {

    private const val SMS_DB = "/data/data/com.android.providers.telephony/databases/mmssms.db"

    suspend fun check(context: Context): Int {
        val appContext = context.applicationContext
        val store = RootSettingsStore(appContext)
        if (!store.smsFallbackEnabled) return 0
        if (!RootShell.isAvailable()) return 0

        val staged = copyDatabase(appContext) ?: return 0
        try {
            return readAndDispatch(appContext, staged, store)
        } finally {
            staged.forEach { file -> file.delete() }
        }
    }

    private suspend fun copyDatabase(context: Context): List<File>? {
        val target = File(context.cacheDir, "root_sms_fallback").apply { mkdirs() }
        val result = RootShell.exec(
            "cp $SMS_DB $SMS_DB-wal $SMS_DB-shm $target/ 2>/dev/null; " +
                // 600:短信库是全量明文,副本期间只允许本应用 uid 读取
                "chmod 600 ${target.absolutePath}/* 2>/dev/null; echo done"
        )
        if (!result.success) return null
        val db = File(target, "mmssms.db")
        if (!db.exists() || db.length() == 0L) return null
        return buildList {
            add(db)
            add(File(target, "mmssms.db-wal"))
            add(File(target, "mmssms.db-shm"))
        }.filter { it.exists() }
    }

    private suspend fun readAndDispatch(
        context: Context,
        stagedFiles: List<File>,
        store: RootSettingsStore
    ): Int {
        val lastSeenId = store.lastFallbackSmsId
        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        var dispatched = 0
        var maxSeenId = lastSeenId

        runCatching {
            SQLiteDatabase.openDatabase(
                stagedFiles.first().absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                val selection = if (lastSeenId == RootSettingsStore.NO_LAST_SEEN_ID) {
                    "${Telephony.Sms.DATE} >= ?"
                } else {
                    "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms._ID} > ?"
                }
                val selectionArgs = if (lastSeenId == RootSettingsStore.NO_LAST_SEEN_ID) {
                    arrayOf(cutoff.toString())
                } else {
                    arrayOf(cutoff.toString(), lastSeenId.toString())
                }
                db.query(
                    "sms",
                    arrayOf("_id", "address", "body", "date", "type"),
                    selection,
                    selectionArgs,
                    null,
                    null,
                    "_id ASC"
                ).use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("_id")
                    val addressIndex = cursor.getColumnIndexOrThrow("address")
                    val bodyIndex = cursor.getColumnIndexOrThrow("body")
                    val dateIndex = cursor.getColumnIndexOrThrow("date")
                    val typeIndex = cursor.getColumnIndexOrThrow("type")
                    while (cursor.moveToNext()) {
                        val rowId = cursor.getLong(idIndex)
                        if (rowId > maxSeenId) maxSeenId = rowId
                        if (cursor.getInt(typeIndex) != Telephony.Sms.MESSAGE_TYPE_INBOX) continue
                        val sender = cursor.getString(addressIndex).orEmpty()
                        val body = cursor.getString(bodyIndex).orEmpty()
                        if (sender.isBlank() || body.isBlank()) continue
                        SmsSegmentBuffer.enqueue(
                            context,
                            SmsForwardRequest(
                                sender = sender,
                                content = body,
                                timestamp = cursor.getLong(dateIndex),
                                source = "root_fallback"
                            )
                        )
                        dispatched += 1
                    }
                }
            }
        }

        if (maxSeenId > lastSeenId) {
            store.lastFallbackSmsId = maxSeenId
        }
        return dispatched
    }

    private const val RECENT_WINDOW_MS = 10L * 60L * 1000L
}
