package joyin.takgi.paysage.mail

object MailInboxPrivacySanitizer {
    private val emailPattern = Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")
    private val commandLinePattern = Regex("(?i)#paysage[^\\r\\n]*")
    private val lpaPattern = Regex("(?i)LPA:1\\$[^\\s]+")
    private val bearerPattern = Regex("(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=\\-]+")
    private val sensitiveFieldPattern = Regex(
        "(?i)(password|passwd|authorization|auth|token|access_token|refresh_token|secret|key|sig|signature|nonce|密码|口令|授权码|认证码|验证码|令牌|密钥|指令密钥|签名)\\s*[:=：]\\s*[^\\s;；，,]+"
    )

    fun redact(text: String, maxLength: Int = 240): String {
        val redacted = text
            .replace(emailPattern, "<email>")
            .replace(commandLinePattern, "<paysage-command-redacted>")
            .replace(lpaPattern) { "LPA:1\$<redacted>" }
            .replace(bearerPattern) { match -> "${match.groupValues[1]} <redacted>" }
            .replace(sensitiveFieldPattern) { match -> "${match.groupValues[1]}: <redacted>" }
            .trim()

        return if (redacted.length <= maxLength) {
            redacted
        } else {
            redacted.take(maxLength).trimEnd() + "..."
        }
    }
}
