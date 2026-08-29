package joyin.takgi.paysage.accessibility

import joyin.takgi.paysage.BuildConfig

/**
 * 无障碍与通知监听两条链路共用的短信通知识别/解析。
 * 纯逻辑便于单元测试。
 */
object SmsNotificationParser {

    fun isSmsNotification(pkg: String?, title: String, text: String): Boolean {
        val packageName = pkg.orEmpty()
        if (packageName.isBlank() || packageName in ignoredPackages) return false
        if (packageName.startsWith(BuildConfig.APPLICATION_ID)) return false

        if (packageName in smsPackages) return true

        val combinedText = "$title\n$text"
        return smsPackageHints.any { packageName.contains(it, ignoreCase = true) } &&
            smsTextHints.any { combinedText.contains(it, ignoreCase = true) }
    }

    fun extractSender(title: String, text: String): String {
        // 尝试从标题提取号码/联系人
        val senderPattern = Regex("^([+\\d\\s\\-()]+|[\\u4e00-\\u9fa5]+)[:：]")
        val match = senderPattern.find(title) ?: senderPattern.find(text)
        return match?.groupValues?.get(1)?.trim() ?: title.split(":").firstOrNull()?.trim() ?: ""
    }

    fun extractContent(text: String): String {
        // 移除发件人前缀
        return text.substringAfter(":", text).trim()
    }

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
