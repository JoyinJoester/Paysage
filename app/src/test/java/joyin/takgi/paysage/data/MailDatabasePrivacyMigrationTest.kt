package joyin.takgi.paysage.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailDatabasePrivacyMigrationTest {
    @Test
    fun migratesLegacyNonceRowWithoutExposingSenderOrNonce() {
        val nonceKey = MailDatabasePrivacyMigration.migrateNonceKey(
            oldNonceKey = "owner@example.com::nonce-123",
            sender = "OWNER@example.com",
            nonce = "nonce-123"
        )
        val sender = MailDatabasePrivacyMigration.migrateNonceSender("OWNER@example.com")
        val nonce = MailDatabasePrivacyMigration.migrateNonceValue("nonce-123")

        assertSha256(nonceKey)
        assertSha256(sender)
        assertSha256(nonce)
        assertFalse(nonceKey.contains("owner@example.com"))
        assertFalse(nonceKey.contains("nonce-123"))
        assertFalse(sender.contains("owner@example.com"))
        assertFalse(nonce.contains("nonce-123"))
    }

    @Test
    fun preservesAlreadyHashedNonceIdentifiers() {
        val hashedKey = MailDatabasePrivacyMigration.stableHash("sender\nnonce")
        val hashedNonce = MailDatabasePrivacyMigration.stableHash("nonce")

        assertEquals(
            hashedKey,
            MailDatabasePrivacyMigration.migrateNonceKey(
                oldNonceKey = hashedKey,
                sender = "owner@example.com",
                nonce = "nonce-123"
            )
        )
        assertEquals(hashedNonce, MailDatabasePrivacyMigration.migrateNonceValue(hashedNonce))
    }

    @Test
    fun migratesLegacyRecordKeyAndSenderWithoutExposingEmail() {
        val messageKey = MailDatabasePrivacyMigration.migrateRecordMessageKey(
            "owner@example.com::42::2099"
        )
        val sender = MailDatabasePrivacyMigration.migrateRecordSender("OWNER@example.com")

        assertSha256(messageKey)
        assertSha256(sender)
        assertFalse(messageKey.contains("owner@example.com"))
        assertFalse(sender.contains("owner@example.com"))
    }

    @Test
    fun senderFingerprintNormalizesCaseAndWhitespace() {
        assertEquals(
            MailDatabasePrivacyMigration.senderFingerprint(" owner@example.com "),
            MailDatabasePrivacyMigration.senderFingerprint("OWNER@example.com")
        )
    }

    private fun assertSha256(value: String) {
        assertTrue(value.matches(Regex("^sha256:[0-9a-f]{64}$")))
    }
}
