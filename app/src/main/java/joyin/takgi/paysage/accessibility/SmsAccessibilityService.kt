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
            if (isSmsNotification(event.packageName?.toString(), title, text)) {
                val sender = extractSender(title, text)
                val content = extractContent(text)

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

    private fun isSmsNotification(pkg: String?, title: String, text: String): Boolean {
        val packageName = pkg.orEmpty()
        if (packageName.isBlank() || packageName in ignoredPackages) return false
        if (packageName.startsWith(BuildConfig.APPLICATION_ID)) return false

        if (packageName in smsPackages) return true

        val combinedText = "$title\n$text"
        return smsPackageHints.any { packageName.contains(it, ignoreCase = true) } &&
            smsTextHints.any { combinedText.contains(it, ignoreCase = true) }
    }

    private fun extractSender(title: String, text: String): String {
        // 尝试从标题提取号码/联系人
        val senderPattern = Regex("^([+\\d\\s\\-()]+|[\\u4e00-\\u9fa5]+)[:：]")
        val match = senderPattern.find(title) ?: senderPattern.find(text)
        return match?.groupValues?.get(1)?.trim() ?: title.split(":").firstOrNull()?.trim() ?: ""
    }

    private fun extractContent(text: String): String {
        // 移除发件人前缀
        return text.substringAfter(":", text).trim()
    }

    companion object {
        private val ignoredPackages = setOf(
            BuildConfig.APPLICATION_ID,
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.thunderdog.challegram",
            "com.whatsapp",
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.discord",
            "com.slack",
            "com.microsoft.teams"
        )

        private val smsPackages = setOf(
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.messaging",
            "com.miui.mms",
            "com.coloros.mms",
            "com.vivo.messaging",
            "com.huawei.message",
            "com.hihonor.mms",
            "com.sonyericsson.conversations"
        )

        private val smsPackageHints = listOf("mms", "sms", "messaging", "message")
        private val smsTextHints = listOf("短信", "SMS", "验证码", "verification code", "code")
    }
}
