package joyin.takgi.paysage.mail

import joyin.takgi.paysage.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MailInboxRecoveryAdvisorTest {
    // adviceFor(context, kind) 需要 Context 解析文案,纯 JVM 测试改为
    // 断言 adviceResFor 的资源映射
    @Test
    fun noAdviceWhenThereIsNoFailure() {
        assertNull(MailInboxRecoveryAdvisor.adviceResFor(MailInboxFailureKind.None))
    }

    @Test
    fun everyFailureKindHasAdvice() {
        MailInboxFailureKind.entries
            .filter { it != MailInboxFailureKind.None }
            .forEach { kind ->
                assertNotNull("missing advice for $kind", MailInboxRecoveryAdvisor.adviceResFor(kind))
            }
    }

    @Test
    fun authenticationFailurePointsToFixAction() {
        val advice = MailInboxRecoveryAdvisor.adviceResFor(MailInboxFailureKind.AuthenticationFailed)

        assertNotNull(advice)
        assertEquals(R.string.title_mail_login_failed, advice!!.titleRes)
        assertEquals(R.string.message_mail_login_failed, advice.messageRes)
        assertEquals(R.string.action_fix, advice.actionLabelRes)
    }

    @Test
    fun backgroundRestrictionAdviceMentionsOptimizeAction() {
        val advice = MailInboxRecoveryAdvisor.adviceResFor(MailInboxFailureKind.BackgroundRestricted)

        assertNotNull(advice)
        assertEquals(R.string.title_mail_background_restricted, advice!!.titleRes)
        assertEquals(R.string.action_optimize, advice.actionLabelRes)
    }

    @Test
    fun rejectedCommandAdvicePointsToLogs() {
        val advice = MailInboxRecoveryAdvisor.adviceResFor(MailInboxFailureKind.CommandRejected)

        assertNotNull(advice)
        assertEquals(R.string.title_mail_commands_rejected, advice!!.titleRes)
        assertEquals(R.string.action_view, advice.actionLabelRes)
    }
}
