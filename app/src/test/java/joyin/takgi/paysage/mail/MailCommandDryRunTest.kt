package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MailCommandDryRunTest {
    private val now = Instant.parse("2026-06-13T12:30:00Z").toEpochMilli()
    private val future = "2026-06-13T12:40:00Z"

    @Test
    fun allowsValidCommandWithoutExecutingIt() {
        val result = MailCommandDryRun.evaluate(
            rawSender = "owner@example.com",
            body = MailCommandTemplate.keyCommand(
                action = MailCommandAction.Status,
                key = "secret-123",
                expiresRaw = future,
                nonce = "n1"
            ),
            trustedSenders = ownerSender(),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertTrue(result.allowed)
        assertTrue(result.wouldExecute)
        assertEquals(MailCommandAction.Status, result.action)
    }

    @Test
    fun rejectsNonWhitelistedSenderBeforeTrustingCommand() {
        val result = MailCommandDryRun.evaluate(
            rawSender = "intruder@example.com",
            body = MailCommandTemplate.keyCommand(
                action = MailCommandAction.Status,
                key = "secret-123",
                expiresRaw = future,
                nonce = "n1"
            ),
            trustedSenders = ownerSender(),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertFalse(result.allowed)
        assertEquals(MailCommandDecisionCode.SenderNotWhitelisted, result.code)
    }

    @Test
    fun rejectsUnauthorizedAction() {
        val result = MailCommandDryRun.evaluate(
            rawSender = "owner@example.com",
            body = MailCommandTemplate.keyCommand(
                action = MailCommandAction.RetryCache,
                key = "secret-123",
                expiresRaw = future,
                nonce = "n1"
            ),
            trustedSenders = ownerSender(),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertFalse(result.allowed)
        assertEquals(MailCommandDecisionCode.ActionNotAllowed, result.code)
    }

    @Test
    fun rejectsReusedNonce() {
        val result = MailCommandDryRun.evaluate(
            rawSender = "owner@example.com",
            body = MailCommandTemplate.keyCommand(
                action = MailCommandAction.Status,
                key = "secret-123",
                expiresRaw = future,
                nonce = "n1"
            ),
            trustedSenders = ownerSender(),
            usedNonces = setOf(MailCommandSecurityPolicy.nonceKey("owner@example.com", "n1")),
            nowEpochMillis = now
        )

        assertFalse(result.allowed)
        assertEquals(MailCommandDecisionCode.NonceReused, result.code)
    }

    @Test
    fun allowsTrailingMailTextAfterCommandBlockSeparator() {
        val result = MailCommandDryRun.evaluate(
            rawSender = "owner@example.com",
            body = """
                #paysage status
                key: secret-123
                expires: $future
                nonce: n1

                From: previous@example.com
                quoted reply text
            """.trimIndent(),
            trustedSenders = ownerSender(),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertTrue(result.allowed)
        assertTrue(result.wouldExecute)
        assertEquals(MailCommandAction.Status, result.action)
    }

    @Test
    fun rejectsTrustedSenderWithMissingSecretBeforeParsingCommandTrust() {
        val result = MailCommandDryRun.evaluate(
            rawSender = "owner@example.com",
            body = """
                #paysage status
                sig: sha256=${"0".repeat(64)}
                expires: $future
                nonce: n1
            """.trimIndent(),
            trustedSenders = listOf(
                TrustedMailSender(
                    email = "owner@example.com",
                    allowedActions = setOf(MailCommandAction.Status),
                    secret = ""
                )
            ),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertFalse(result.allowed)
        assertEquals(MailCommandDecisionCode.InvalidAuthenticator, result.code)
        assertEquals(null, result.action)
    }

    private fun ownerSender(): List<TrustedMailSender> =
        listOf(
            TrustedMailSender(
                email = "owner@example.com",
                allowedActions = setOf(MailCommandAction.Status),
                secret = "secret-123"
            )
        )
}
