package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException

class MailInboxFailureClassifierTest {
    @Test
    fun classifiesDirectAuthenticationFailure() {
        val kind = MailInboxFailureClassifier.classify(
            AuthenticationFailedException("bad credentials")
        )

        assertEquals(MailInboxFailureKind.AuthenticationFailed, kind)
    }

    @Test
    fun classifiesNestedAuthenticationFailure() {
        val kind = MailInboxFailureClassifier.classify(
            MessagingException("login failed", AuthenticationFailedException("bad credentials"))
        )

        assertEquals(MailInboxFailureKind.AuthenticationFailed, kind)
    }

    @Test
    fun keepsRealtimeEnabledForTransientNetworkFailures() {
        assertFalse(
            MailInboxIdleFailurePolicy.shouldDisableRealtime(
                MailInboxFailureKind.NetworkOrServerFailed
            )
        )
    }

    @Test
    fun disablesRealtimeForUserFixableCredentialFailures() {
        assertTrue(
            MailInboxIdleFailurePolicy.shouldDisableRealtime(
                MailInboxFailureKind.AuthenticationFailed
            )
        )
    }
}
