package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailMessageIdentityTest {
    @Test
    fun messageIdKeyIsStableWhenImapMessageNumberChanges() {
        val first = summary(messageNumber = 7)
        val moved = summary(messageNumber = 19)

        assertEquals(
            MailMessageIdentity.primaryKey(first),
            MailMessageIdentity.primaryKey(moved)
        )
    }

    @Test
    fun lookupKeysIncludeLegacyMessageNumberKeyForUpgradeCompatibility() {
        val withMessageId = summary(messageNumber = 7, messageId = "<abc@example.com>")
        val legacyOnly = summary(messageNumber = 7, messageId = "")

        val lookupKeys = MailMessageIdentity.lookupKeys(withMessageId)

        assertTrue(lookupKeys.contains(MailMessageIdentity.primaryKey(withMessageId)))
        assertTrue(lookupKeys.contains(MailMessageIdentity.primaryKey(legacyOnly)))
    }

    @Test
    fun messageIdentityKeysDoNotExposeRawMailMetadata() {
        val keys = MailMessageIdentity.lookupKeys(
            summary(
                messageId = "<secret-message-id@example.com>",
                from = "Owner <owner@example.com>",
                subject = "key: command-secret"
            )
        )

        keys.forEach { key ->
            assertTrue(key.startsWith("sha256:"))
            assertFalse(key.contains("secret-message-id"))
            assertFalse(key.contains("owner@example.com"))
            assertFalse(key.contains("command-secret"))
        }
    }

    private fun summary(
        messageNumber: Int = 7,
        messageId: String = "<abc@example.com>",
        from: String = "Owner <owner@example.com>",
        subject: String = "Status command"
    ): MailMessageSummary =
        MailMessageSummary(
            messageNumber = messageNumber,
            messageId = messageId,
            from = from,
            normalizedFrom = MailAddressNormalizer.normalize(from),
            subject = subject,
            receivedAtMillis = 1_789_000_000_000L,
            seen = false
        )
}
