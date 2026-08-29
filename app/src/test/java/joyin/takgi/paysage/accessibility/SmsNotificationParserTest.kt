package joyin.takgi.paysage.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsNotificationParserTest {

    @Test
    fun knownSmsPackageIsAccepted() {
        assertTrue(
            SmsNotificationParser.isSmsNotification(
                "com.google.android.apps.messaging",
                "10086",
                "您的验证码是123456"
            )
        )
    }

    @Test
    fun ownPackageAndIgnoredAppsAreRejected() {
        assertFalse(
            SmsNotificationParser.isSmsNotification("joyin.takgi.paysage", "title", "text")
        )
        assertFalse(
            SmsNotificationParser.isSmsNotification("org.telegram.messenger", "TG", "message")
        )
    }

    @Test
    fun unknownPackageNeedsTextHint() {
        assertFalse(
            SmsNotificationParser.isSmsNotification("com.random.app", "标题", "正文")
        )
        assertTrue(
            SmsNotificationParser.isSmsNotification("com.random.app.messaging", "标题", "短信内容")
        )
    }

    @Test
    fun extractsSenderFromColonTitle() {
        assertEquals("10086", SmsNotificationParser.extractSender("10086:验证码", "正文"))
        assertEquals("招商银行", SmsNotificationParser.extractSender("招商银行：消费提醒", "正文"))
    }

    @Test
    fun extractsContentStrippingSenderPrefix() {
        assertEquals("验证码是9527", SmsNotificationParser.extractContent("10086:验证码是9527"))
        assertEquals("无冒号正文", SmsNotificationParser.extractContent("无冒号正文"))
    }
}
