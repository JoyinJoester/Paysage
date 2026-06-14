package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MailInboxRecoveryAdvisorTest {
    @Test
    fun noAdviceWhenThereIsNoFailure() {
        assertNull(MailInboxRecoveryAdvisor.adviceFor(MailInboxFailureKind.None))
    }

    @Test
    fun authenticationFailurePointsToAccountAndImapSetup() {
        val advice = MailInboxRecoveryAdvisor.adviceFor(MailInboxFailureKind.AuthenticationFailed)

        assertNotNull(advice)
        assertEquals("邮箱登录失败", advice?.title)
        assertTrue(advice?.message.orEmpty().contains("授权码"))
        assertTrue(advice?.message.orEmpty().contains("IMAP"))
    }

    @Test
    fun rejectedCommandAdviceExplainsSecurityChecks() {
        val advice = MailInboxRecoveryAdvisor.adviceFor(MailInboxFailureKind.CommandRejected)

        assertNotNull(advice)
        assertTrue(advice?.message.orEmpty().contains("密钥"))
        assertTrue(advice?.message.orEmpty().contains("nonce"))
    }

    @Test
    fun backgroundRestrictionAdviceMentionsWorkManagerAndBatteryOptimization() {
        val advice = MailInboxRecoveryAdvisor.adviceFor(MailInboxFailureKind.BackgroundRestricted)

        assertNotNull(advice)
        assertTrue(advice?.message.orEmpty().contains("电池优化"))
        assertTrue(advice?.message.orEmpty().contains("WorkManager"))
    }
}
