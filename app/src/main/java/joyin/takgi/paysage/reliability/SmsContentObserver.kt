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
import kotlinx.coroutines.launch

class SmsContentObserver(
    private val context: Context,
    handler: Handler,
    private val scope: CoroutineScope
) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        scope.launch {
            readRecentInboxMessages().forEach { request ->
                SmsForwardDispatcher.dispatch(context, request)
            }
        }
    }

    private fun readRecentInboxMessages(): List<SmsForwardRequest> {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoff.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT 5"

        return runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                buildList {
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
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val RECENT_WINDOW_MS = 10L * 60L * 1000L
    }
}
