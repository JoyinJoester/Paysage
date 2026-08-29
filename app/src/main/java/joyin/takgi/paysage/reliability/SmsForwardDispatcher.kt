package joyin.takgi.paysage.reliability

import android.content.Context
import android.content.Intent
import android.os.Build
import joyin.takgi.paysage.R
import joyin.takgi.paysage.service.ForwardService

object SmsForwardDispatcher {
    const val EXTRA_SENDER = "sender"
    const val EXTRA_CONTENT = "content"
    const val EXTRA_TIMESTAMP = "timestamp"
    const val EXTRA_SOURCE = "source"

    suspend fun dispatch(context: Context, request: SmsForwardRequest): Boolean {
        val appContext = context.applicationContext
        // 只做认领;永久去重记录由 ForwardService 在转发完成后写入,
        // 避免认领后进程死亡导致短信被去重挡住却从未发出。
        if (!SmsDedupeStore(appContext).claimIfNew(request)) {
            return false
        }

        return try {
            val intent = Intent(appContext, ForwardService::class.java)
                .putExtra(EXTRA_SENDER, request.sender)
                .putExtra(EXTRA_CONTENT, request.content)
                .putExtra(EXTRA_TIMESTAMP, request.timestamp)
                .putExtra(EXTRA_SOURCE, request.source)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
            true
        } catch (error: RuntimeException) {
            val message = appContext.getString(R.string.message_foreground_service_blocked_sms_cached)
            SmsForwarder(appContext).queue(request, message)
            // 已进入离线缓存,属安全终态,升级为永久去重记录
            SmsDedupeStore(appContext).record(request)
            SmsReliabilityManager.enqueueImmediateRetry(appContext)
            SmsReliabilityNotifier.notifyForwardOutcome(
                appContext,
                SmsForwardOutcome(
                    request = request,
                    forwarded = false,
                    queued = true,
                    filtered = false,
                    emailSuccess = false,
                    telegramSuccess = false,
                    message = message
                )
            )
            false
        }
    }
}
