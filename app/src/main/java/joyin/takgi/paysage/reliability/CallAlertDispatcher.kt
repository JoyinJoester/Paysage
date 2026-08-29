package joyin.takgi.paysage.reliability

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.util.DateFormatter

enum class CallAlertKind {
    Incoming,
    Missed,
    Rejected,
    Blocked
}

data class CallAlertRequest(
    val number: String,
    val kind: CallAlertKind,
    val timestamp: Long,
    val source: String
)

class CallAlertDispatcher(context: Context) {
    private val appContext = context.applicationContext
    private val dedupeStore = CallAlertDedupeStore(appContext)
    private val alertSender = ForwardAccountAlertSender(appContext)

    suspend fun dispatch(request: CallAlertRequest): Boolean {
        if (!dedupeStore.markIfNew(request)) return false

        val title = request.kind.title(appContext)
        val number = request.number.ifBlank {
            appContext.getString(R.string.label_unknown_number)
        }
        val source = request.source.sourceLabel(appContext)
        val subject = appContext.getString(R.string.format_call_alert_forward_subject, title)
        val body = appContext.getString(
            R.string.format_call_alert_forward_body,
            title,
            number,
            DateFormatter.format(request.timestamp),
            source
        )
        alertSender.send(subject, body)
        return true
    }
}

private fun CallAlertKind.title(context: Context): String =
    context.getString(
        when (this) {
            CallAlertKind.Incoming -> R.string.title_call_alert_incoming
            CallAlertKind.Missed -> R.string.title_call_alert_missed
            CallAlertKind.Rejected -> R.string.title_call_alert_rejected
            CallAlertKind.Blocked -> R.string.title_call_alert_blocked
        }
    )

private fun String.sourceLabel(context: Context): String =
    context.getString(
        when (this) {
            "phone_state" -> R.string.source_call_phone_state
            "call_log" -> R.string.source_call_log
            else -> R.string.source_call_unknown
        }
    )
