package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.mail.internet.InternetAddress

class MailInboxAccountTest {
    @Test
    fun validatesCompleteImapConfig() {
        val config = MailInboxAccountConfig(
            host = "imap.example.com",
            port = 993,
            username = "owner@example.com",
            password = "app-password",
            useSsl = true,
            enabled = true
        )

        assertTrue(config.isConfigured)
        assertEquals(emptyList<MailAccountIssue>(), MailInboxAccountValidator.validate(config))
    }

    @Test
    fun reportsMissingFieldsWithoutLeakingPassword() {
        val config = MailInboxAccountConfig(
            host = "",
            port = 70000,
            username = "",
            password = ""
        )

        val issues = MailInboxAccountValidator.validate(config)

        assertFalse(config.isConfigured)
        assertEquals(
            listOf(
                MailAccountIssue.MissingHost,
                MailAccountIssue.InvalidPort,
                MailAccountIssue.MissingUsername,
                MailAccountIssue.MissingPassword
            ),
            issues
        )
    }

    @Test
    fun preservesMultipleFromAddressesForSecurityNormalization() {
        val from = MailMessageAddressFormatter.fromHeader(
            arrayOf(
                InternetAddress("owner@example.com"),
                InternetAddress("intruder@example.com")
            )
        )

        assertEquals("owner@example.com, intruder@example.com", from)
        assertEquals(null, MailAddressNormalizer.normalize(from))
    }

    @Test
    fun preservesSingleDisplayNameFromAddress() {
        val from = MailMessageAddressFormatter.fromHeader(
            arrayOf(InternetAddress("OWNER@example.com", "Owner, Team"))
        )

        assertEquals("owner@example.com", MailAddressNormalizer.normalize(from))
    }
}
