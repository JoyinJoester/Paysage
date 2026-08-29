package joyin.takgi.paysage.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import joyin.takgi.paysage.BuildConfig
import joyin.takgi.paysage.reliability.SmsForwardDispatcher
import joyin.takgi.paysage.reliability.SmsForwardRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SmsAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val notification = event.parcelableData as? android.app.Notification ?: return

            val extras = notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            // 检测短信通知特征
            if (SmsNotificationParser.isSmsNotification(event.packageName?.toString(), title, text)) {
                val sender = SmsNotificationParser.extractSender(title, text)
                val content = SmsNotificationParser.extractContent(text)

                if (sender.isNotEmpty() && content.isNotEmpty()) {
                    val request = SmsForwardRequest(
                        sender = sender,
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        source = "accessibility_notification"
                    )
                    scope.launch {
                        SmsForwardDispatcher.dispatch(this@SmsAccessibilityService, request)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
