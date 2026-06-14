package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailCommandRecordPrivacyTest {
    @Test
    fun redactsSensitiveCommandFieldsFromSubject() {
        val redacted = MailCommandRecordPrivacy.redactSubject(
            "#paysage status key: secret-123 sig=sha256abcdef nonce: replay-1"
        )

        assertFalse(redacted.contains("secret-123"))
        assertFalse(redacted.contains("sha256abcdef"))
        assertFalse(redacted.contains("replay-1"))
        assertTrue(redacted.contains("[已隐藏]"))
    }

    @Test
    fun redactsCredentialFieldsFromSubject() {
        val redacted = MailCommandRecordPrivacy.redactSubject(
            "debug password: app-password authorization=BearerToken token: api-token secret：mail-secret"
        )

        assertFalse(redacted.contains("app-password"))
        assertFalse(redacted.contains("BearerToken"))
        assertFalse(redacted.contains("api-token"))
        assertFalse(redacted.contains("mail-secret"))
        assertTrue(redacted.contains("password: [已隐藏]"))
        assertTrue(redacted.contains("authorization: [已隐藏]"))
        assertTrue(redacted.contains("token: [已隐藏]"))
        assertTrue(redacted.contains("secret: [已隐藏]"))
    }

    @Test
    fun redactsLocalizedCredentialFieldsFromSubject() {
        val redacted = MailCommandRecordPrivacy.redactSubject(
            "登录失败 授权码：mail-auth-code 指令密钥：command-key 签名：sha256secret nonce：raw-nonce"
        )

        assertFalse(redacted.contains("mail-auth-code"))
        assertFalse(redacted.contains("command-key"))
        assertFalse(redacted.contains("sha256secret"))
        assertFalse(redacted.contains("raw-nonce"))
        assertTrue(redacted.contains("授权码: [已隐藏]"))
        assertTrue(redacted.contains("指令密钥: [已隐藏]"))
        assertTrue(redacted.contains("签名: [已隐藏]"))
        assertTrue(redacted.contains("nonce: [已隐藏]"))
    }

    @Test
    fun redactsSenderAddressWithoutFullDomain() {
        assertEquals(
            "ow***@ex***",
            MailCommandRecordPrivacy.redactAddress("\"Owner\" <owner@example.com>")
        )
        assertFalse(
            MailCommandRecordPrivacy.redactAddress("\"Owner\" <owner@example.com>")
                .contains("example.com")
        )
    }

    @Test
    fun senderFingerprintDoesNotExposeAddress() {
        val fingerprint = MailCommandRecordPrivacy.senderFingerprint("\"Owner\" <owner@example.com>")

        assertTrue(fingerprint.startsWith("sha256:"))
        assertFalse(fingerprint.contains("owner"))
        assertFalse(fingerprint.contains("example.com"))
        assertEquals(
            fingerprint,
            MailCommandRecordPrivacy.senderFingerprint("OWNER@example.com")
        )
    }

    @Test
    fun stableHashDoesNotExposeRawMessageKeyInput() {
        val hash = MailCommandRecordPrivacy.stableHash("owner@example.com::12::123456::42")

        assertTrue(hash.startsWith("sha256:"))
        assertFalse(hash.contains("owner@example.com"))
        assertFalse(hash.contains("123456"))
    }
}
