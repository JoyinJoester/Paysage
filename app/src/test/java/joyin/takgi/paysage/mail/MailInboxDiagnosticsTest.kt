package joyin.takgi.paysage.mail

import joyin.takgi.paysage.data.MailTrustedSenderEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailInboxDiagnosticsTest {
    @Test
    fun reportRedactsCredentialsAndAddresses() {
        val report = MailInboxDiagnostics.buildReport(
            account = MailInboxAccountConfig(
                host = "imap.mail.example.com",
                port = 993,
                username = "owner@example.com",
                password = "app-password-secret",
                useSsl = true,
                enabled = true
            ),
            runtimeStatus = MailInboxRuntimeStatus(
                lastCheckAt = 1_789_000_000_000L,
                lastSuccessAt = 1_789_000_000_000L,
                lastMessage = "key: leaked nonce: leaked",
                lastFetched = 3,
                lastExecuted = 1,
                lastIgnored = 2,
                lastRejected = 0
            ),
            realtimeSettings = MailInboxRealtimeSettings(
                enabled = true,
                idleReconnectMinutes = 15
            ),
            trustedSenders = listOf(
                MailTrustedSenderEntity(
                    email = "owner@example.com",
                    allowedActions = "status,retry-cache",
                    enabled = true,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            ),
            generatedAtMillis = 1_789_000_000_000L
        )

        assertTrue(report.contains("account.host=***.example.com"))
        assertTrue(report.contains("account.username=ow***@example.com"))
        assertTrue(report.contains("trustedSenders.enabled=1"))
        assertFalse(report.contains("app-password-secret"))
        assertFalse(report.contains("owner@example.com"))
        assertFalse(report.contains("key: leaked"))
        assertFalse(report.contains("nonce: leaked"))
    }

    @Test
    fun readinessReportsMissingSetup() {
        val issues = MailInboxDiagnostics.readinessIssues(
            account = MailInboxAccountConfig(),
            trustedSenders = emptyList()
        )

        assertTrue(issues.any { it.contains("not fully configured") })
        assertTrue(issues.any { it.contains("disabled") })
        assertTrue(issues.any { it.contains("No enabled trusted sender") })
    }
}
