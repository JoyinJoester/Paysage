package joyin.takgi.paysage.mail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailInboxPrivacySanitizerTest {
    @Test
    fun redactsCredentialsAndRemoteCommandFromFailureMessage() {
        val redacted = MailInboxPrivacySanitizer.redact(
            "AUTH failed for owner@example.com password=app-pass token: api-token " +
                "key: command-key sig=sha256abcdef nonce: replay-1 #paysage status"
        )

        assertFalse(redacted.contains("owner@example.com"))
        assertFalse(redacted.contains("app-pass"))
        assertFalse(redacted.contains("api-token"))
        assertFalse(redacted.contains("command-key"))
        assertFalse(redacted.contains("sha256abcdef"))
        assertFalse(redacted.contains("replay-1"))
        assertFalse(redacted.contains("#paysage"))
        assertTrue(redacted.contains("<email>"))
        assertTrue(redacted.contains("<paysage-command-redacted>"))
    }

    @Test
    fun redactsBearerAndActivationCode() {
        val redacted = MailInboxPrivacySanitizer.redact(
            "server rejected Authorization: Bearer abc.def.ghi and LPA:1\$example.com\$matching-id"
        )

        assertFalse(redacted.contains("abc.def.ghi"))
        assertFalse(redacted.contains("example.com\$matching-id"))
        assertTrue(redacted.contains("Authorization: <redacted>"))
        assertTrue(redacted.contains("LPA:1\$<redacted>"))
    }

    @Test
    fun redactsLocalizedCredentialFields() {
        val redacted = MailInboxPrivacySanitizer.redact(
            "登录失败 授权码：mail-auth-code 指令密钥：command-key 签名：sha256secret nonce：raw-nonce"
        )

        assertFalse(redacted.contains("mail-auth-code"))
        assertFalse(redacted.contains("command-key"))
        assertFalse(redacted.contains("sha256secret"))
        assertFalse(redacted.contains("raw-nonce"))
        assertTrue(redacted.contains("授权码: <redacted>"))
        assertTrue(redacted.contains("指令密钥: <redacted>"))
        assertTrue(redacted.contains("签名: <redacted>"))
        assertTrue(redacted.contains("nonce: <redacted>"))
    }

    @Test
    fun truncatesVeryLongFailureMessage() {
        val redacted = MailInboxPrivacySanitizer.redact("x".repeat(400), maxLength = 80)

        assertTrue(redacted.length <= 83)
        assertTrue(redacted.endsWith("..."))
    }
}
