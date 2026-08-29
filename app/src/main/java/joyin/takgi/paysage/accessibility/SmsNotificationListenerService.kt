package joyin.takgi.paysage.accessibility

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import joyin.takgi.paysage.reliability.SmsForwardDispatcher
import joyin.takgi.paysage.reliability.SmsForwardRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 通知监听采集:相比无障碍服务,这是系统标准的通知读取方式,
 * 覆盖面更稳、不需要无障碍开关。捕获到短信通知后仍走统一的
 * 认领式去重,与广播/观察器/无障碍链路互不重复转发。
 */
class SmsNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (!SmsNotificationParser.isSmsNotification(sbn.packageName, title, text)) return

        val sender = SmsNotificationParser.extractSender(title, text)
        val content = SmsNotificationParser.extractContent(text)
        if (sender.isBlank() || content.isBlank()) return

        val request = SmsForwardRequest(
            sender = sender,
            content = content,
            timestamp = System.currentTimeMillis(),
            source = "notification_listener"
        )
        val appContext = applicationContext
        scope.launch {
            SmsForwardDispatcher.dispatch(appContext, request)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
