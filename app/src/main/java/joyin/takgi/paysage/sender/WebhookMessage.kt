package joyin.takgi.paysage.sender

import joyin.takgi.paysage.data.AccountType
import org.json.JSONObject
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 按 Webhook 类账号类型构造推送请求。纯逻辑无 Android 依赖,便于单元测试。
 *
 * 各通道字段约定:
 * - WEBHOOK: url 为完整地址,secret 可选,作为 X-Paysage-Token 请求头
 * - BARK: url 可留空(用官方服务器),填了则优先;secret 为设备 Key
 * - SERVERCHAN: url 可留空(用官方服务器);secret 为 SendKey
 * - DINGTALK: url 为机器人 Webhook 地址;secret 为加签密钥(可选)
 * - FEISHU / WECOM: url 为机器人 Webhook 地址,secret 不使用
 */
object WebhookMessage {

    data class Request(
        val url: String,
        val contentType: String,
        val body: String,
        val headers: Map<String, String> = emptyMap()
    )

    fun isReady(type: AccountType, webhookUrl: String, webhookSecret: String): Boolean =
        when (type) {
            AccountType.BARK, AccountType.SERVERCHAN -> webhookSecret.isNotBlank()
            else -> webhookUrl.isNotBlank()
        }

    fun build(
        type: AccountType,
        webhookUrl: String,
        webhookSecret: String,
        title: String,
        content: String,
        now: Long = System.currentTimeMillis()
    ): Request = when (type) {
        AccountType.BARK -> bark(webhookUrl, webhookSecret, title, content)
        AccountType.SERVERCHAN -> serverChan(webhookUrl, webhookSecret, title, content)
        AccountType.DINGTALK -> dingTalk(webhookUrl, webhookSecret, content, now)
        AccountType.FEISHU -> Request(
            url = requireUrl(webhookUrl),
            contentType = CONTENT_TYPE_JSON,
            body = JSONObject().apply {
                put("msg_type", "text")
                put("content", JSONObject().put("text", content))
            }.toString()
        )
        AccountType.WECOM -> Request(
            url = requireUrl(webhookUrl),
            contentType = CONTENT_TYPE_JSON,
            body = JSONObject().apply {
                put("msgtype", "text")
                put("text", JSONObject().put("content", content.take(WECOM_MAX_LENGTH)))
            }.toString()
        )
        else -> genericWebhook(webhookUrl, webhookSecret, title, content)
    }

    private fun bark(webhookUrl: String, secret: String, title: String, content: String): Request {
        val url = if (webhookUrl.isBlank()) {
            "$BARK_DEFAULT_SERVER/${secret.trim()}"
        } else {
            webhookUrl.trim().trimEnd('/')
        }
        return Request(
            url = url,
            contentType = CONTENT_TYPE_JSON,
            body = JSONObject().apply {
                put("title", title.take(BARK_TITLE_MAX_LENGTH))
                put("body", content)
                if (secret.isNotBlank()) put("device_key", secret.trim())
            }.toString()
        )
    }

    private fun serverChan(webhookUrl: String, secret: String, title: String, content: String): Request {
        val base = webhookUrl.ifBlank { SERVERCHAN_DEFAULT_SERVER }.trim().trimEnd('/')
        return Request(
            url = "$base/${secret.trim()}.send",
            contentType = CONTENT_TYPE_FORM,
            body = listOf(
                "title" to title.take(SERVERCHAN_TITLE_MAX_LENGTH),
                "desp" to content
            ).joinToString("&") { (key, value) ->
                "$key=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
            }
        )
    }

    private fun dingTalk(url: String, secret: String, content: String, now: Long): Request {
        var targetUrl = requireUrl(url).trim()
        if (secret.isNotBlank()) {
            val sign = URLEncoder.encode(hmacSha256Base64("$now\n${secret.trim()}", secret.trim()), "UTF-8")
            val separator = if (targetUrl.contains("?")) "&" else "?"
            targetUrl += "${separator}timestamp=$now&sign=$sign"
        }
        return Request(
            url = targetUrl,
            contentType = CONTENT_TYPE_JSON,
            body = JSONObject().apply {
                put("msgtype", "text")
                put("text", JSONObject().put("content", content.take(DINGTALK_MAX_LENGTH)))
            }.toString()
        )
    }

    private fun genericWebhook(url: String, secret: String, title: String, content: String): Request {
        val headers = if (secret.isBlank()) {
            emptyMap()
        } else {
            mapOf("X-Paysage-Token" to secret.trim())
        }
        return Request(
            url = requireUrl(url),
            contentType = CONTENT_TYPE_JSON,
            body = JSONObject().apply {
                put("title", title)
                put("message", content)
            }.toString(),
            headers = headers
        )
    }

    private fun requireUrl(url: String): String =
        url.trim().also { require(it.isNotBlank()) { "Webhook URL is required." } }

    private fun hmacSha256Base64(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return java.util.Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }

    const val BARK_DEFAULT_SERVER = "https://api.day.app"
    const val SERVERCHAN_DEFAULT_SERVER = "https://sctapi.ftqq.com"
    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"

    private const val BARK_TITLE_MAX_LENGTH = 200
    private const val SERVERCHAN_TITLE_MAX_LENGTH = 100
    private const val WECOM_MAX_LENGTH = 2000
    private const val DINGTALK_MAX_LENGTH = 5000
}
