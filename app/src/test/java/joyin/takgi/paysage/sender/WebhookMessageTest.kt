package joyin.takgi.paysage.sender

import joyin.takgi.paysage.data.AccountType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookMessageTest {

    @Test
    fun genericWebhookUsesUrlAndTokenHeader() {
        val request = WebhookMessage.build(
            AccountType.WEBHOOK,
            webhookUrl = "https://example.com/hook",
            webhookSecret = "token123",
            title = "10086",
            content = "hello"
        )
        assertEquals("https://example.com/hook", request.url)
        assertEquals("token123", request.headers["X-Paysage-Token"])
        val body = JSONObject(request.body)
        assertEquals("10086", body.getString("title"))
        assertEquals("hello", body.getString("message"))
    }

    @Test
    fun genericWebhookWithoutSecretHasNoTokenHeader() {
        val request = WebhookMessage.build(
            AccountType.WEBHOOK, "https://example.com/hook", "", "t", "c"
        )
        assertNull(request.headers["X-Paysage-Token"])
    }

    @Test
    fun barkUsesDefaultServerWithDeviceKeyWhenUrlBlank() {
        val request = WebhookMessage.build(
            AccountType.BARK, "", "mykey", "title", "body text"
        )
        assertEquals("https://api.day.app/mykey", request.url)
        val body = JSONObject(request.body)
        assertEquals("title", body.getString("title"))
        assertEquals("body text", body.getString("body"))
        assertEquals("mykey", body.getString("device_key"))
    }

    @Test
    fun barkPrefersCustomUrlAndTrimsTrailingSlash() {
        val request = WebhookMessage.build(
            AccountType.BARK, "https://bark.self.host/abc/", "", "t", "c"
        )
        assertEquals("https://bark.self.host/abc", request.url)
    }

    @Test
    fun serverChanAppendsSendKeyAndFormEncodes() {
        val request = WebhookMessage.build(
            AccountType.SERVERCHAN, "", "SCT123", "标题 here", "正文 a&b"
        )
        assertEquals("https://sctapi.ftqq.com/SCT123.send", request.url)
        assertEquals(WebhookMessage.CONTENT_TYPE_FORM, request.contentType)
        val params = request.body.split("&").associate {
            val (k, v) = it.split("=", limit = 2)
            k to URLDecoder.decode(v, Charsets.UTF_8)
        }
        assertEquals("标题 here", params["title"])
        assertEquals("正文 a&b", params["desp"])
    }

    @Test
    fun dingTalkAppendsTimestampAndSignWhenSecretPresent() {
        val now = 1_700_000_000_000L
        val secret = "SEC123"
        val request = WebhookMessage.build(
            AccountType.DINGTALK, "https://oapi.dingtalk.com/robot/send?access_token=x", secret, "t", "c", now
        )
        assertTrue(request.url.startsWith("https://oapi.dingtalk.com/robot/send?access_token=x&timestamp=$now&sign="))

        val expectedSign = Base64.getEncoder().encodeToString(
            Mac.getInstance("HmacSHA256").run {
                init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
                doFinal("$now\n$secret".toByteArray())
            }
        )
        assertTrue(request.url.endsWith("&sign=${java.net.URLEncoder.encode(expectedSign, "UTF-8")}"))
        assertEquals("c", JSONObject(request.body).getJSONObject("text").getString("content"))
    }

    @Test
    fun dingTalkWithoutSecretKeepsUrlUntouched() {
        val request = WebhookMessage.build(
            AccountType.DINGTALK, "https://oapi.dingtalk.com/robot/send?access_token=x", "", "t", "c"
        )
        assertEquals("https://oapi.dingtalk.com/robot/send?access_token=x", request.url)
    }

    @Test
    fun feishuAndWecomBuildTextMessages() {
        val feishu = WebhookMessage.build(AccountType.FEISHU, "https://open.feishu.cn/hook/x", "", "t", "c")
        val feishuBody = JSONObject(feishu.body)
        assertEquals("text", feishuBody.getString("msg_type"))
        assertEquals("c", feishuBody.getJSONObject("content").getString("text"))

        val wecom = WebhookMessage.build(AccountType.WECOM, "https://qyapi.weixin.qq.com/hook/x", "", "t", "c")
        val wecomBody = JSONObject(wecom.body)
        assertEquals("text", wecomBody.getString("msgtype"))
        assertEquals("c", wecomBody.getJSONObject("text").getString("content"))
    }

    @Test
    fun readinessFollowsTypeSpecificRules() {
        // 通用/钉钉/飞书/企业微信只要求 URL
        assertFalse(WebhookMessage.isReady(AccountType.WEBHOOK, "", ""))
        assertTrue(WebhookMessage.isReady(AccountType.WEBHOOK, "https://a.b", ""))
        assertTrue(WebhookMessage.isReady(AccountType.FEISHU, "https://a.b", ""))
        // Bark / Server酱 至少要有 Key,URL 可留空走官方服务器
        assertFalse(WebhookMessage.isReady(AccountType.BARK, "", ""))
        assertTrue(WebhookMessage.isReady(AccountType.BARK, "", "key"))
        assertTrue(WebhookMessage.isReady(AccountType.SERVERCHAN, "", "key"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun genericWebhookRequiresUrl() {
        WebhookMessage.build(AccountType.WEBHOOK, "", "", "t", "c")
    }
}
