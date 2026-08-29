package joyin.takgi.paysage.reliability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.CallLog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CallLogObserver(
    private val context: Context,
    handler: Handler,
    private val scope: CoroutineScope
) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        scope.launch {
            val dispatcher = CallAlertDispatcher(context)
            readRecentCalls().forEach { request ->
                dispatcher.dispatch(request)
            }
        }
    }

    private fun readRecentCalls(): List<CallAlertRequest> {
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE
        )
        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        val selection = "${CallLog.Calls.DATE} >= ?"
        val selectionArgs = arrayOf(cutoff.toString())
        val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 8"

        return runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                buildList {
                    while (cursor.moveToNext()) {
                        val kind = cursor.getInt(typeIndex).toAlertKind() ?: continue
                        add(
                            CallAlertRequest(
                                number = cursor.getString(numberIndex).orEmpty(),
                                kind = kind,
                                timestamp = cursor.getLong(dateIndex),
                                source = "call_log"
                            )
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun Int.toAlertKind(): CallAlertKind? =
        when (this) {
            CallLog.Calls.INCOMING_TYPE -> CallAlertKind.Incoming
            CallLog.Calls.MISSED_TYPE -> CallAlertKind.Missed
            CallLog.Calls.REJECTED_TYPE -> CallAlertKind.Rejected
            CallLog.Calls.BLOCKED_TYPE -> CallAlertKind.Blocked
            else -> null
        }

    companion object {
        private const val RECENT_WINDOW_MS = 10L * 60L * 1000L
    }
}
