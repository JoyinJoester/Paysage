package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MailCommandTemplateTest {
    private val now = Instant.parse("2026-06-13T12:30:00Z").toEpochMilli()
    private val future = "2026-06-13T12:40:00Z"

    @Test
    fun keyTemplateCanBeParsed() {
        val command = MailCommandParser.parse(
            MailCommandTemplate.keyCommand(
                action = MailCommandAction.Status,
                key = "secret-123",
                expiresRaw = future,
                nonce = "n1"
            )
        ).getOrThrow()

        assertEquals(MailCommandAction.Status, command.action)
        assertEquals("secret-123", command.key)
        assertEquals("n1", command.nonce)
    }

    @Test
    fun hmacTemplatePassesSecurityPolicy() {
        val command = MailCommandParser.parse(
            MailCommandTemplate.hmacCommand(
                action = MailCommandAction.RetryCache,
                secret = "secret-123",
                expiresRaw = future,
                nonce = "n1"
            )
        ).getOrThrow()
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "owner@example.com",
            command = command,
            trustedSenders = listOf(
                TrustedMailSender(
                    email = "owner@example.com",
                    allowedActions = setOf(MailCommandAction.RetryCache),
                    secret = "secret-123"
                )
            ),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertTrue(decision.allowed)
    }

    @Test
    fun wrongKeyTemplateFailsSecurityPolicy() {
        val command = MailCommandParser.parse(
            MailCommandTemplate.wrongKeyCommand(
                action = MailCommandAction.Status,
                expiresRaw = future,
                nonce = "n1"
            )
        ).getOrThrow()
        val decision = MailCommandSecurityPolicy.evaluate(
            rawSender = "owner@example.com",
            command = command,
            trustedSenders = listOf(
                TrustedMailSender(
                    email = "owner@example.com",
                    allowedActions = setOf(MailCommandAction.Status),
                    secret = "secret-123"
                )
            ),
            usedNonces = emptySet(),
            nowEpochMillis = now
        )

        assertFalse(decision.allowed)
        assertEquals(MailCommandDecisionCode.InvalidAuthenticator, decision.code)
    }

    @Test
    fun defaultNonceIncludesUniqueSuffix() {
        val first = MailCommandTemplate.defaultNonce(
            action = MailCommandAction.Status,
            nowMillis = 1234L,
            uniqueSuffix = "first"
        )
        val second = MailCommandTemplate.defaultNonce(
            action = MailCommandAction.Status,
            nowMillis = 1234L,
            uniqueSuffix = "second"
        )

        assertEquals("smoke-status-1234-first", first)
        assertEquals("smoke-status-1234-second", second)
        assertFalse(first == second)
    }

    @Test
    fun replayPairUsesTheSameNonceForBothMessages() {
        val pair = MailCommandTemplate.replayPair(
            action = MailCommandAction.Status,
            key = "secret-123",
            nonce = "replay-nonce"
        )

        val first = MailCommandParser.parse(pair.first).getOrThrow()
        val second = MailCommandParser.parse(pair.second).getOrThrow()

        assertEquals("replay-nonce", first.nonce)
        assertEquals(first.nonce, second.nonce)
    }
}
