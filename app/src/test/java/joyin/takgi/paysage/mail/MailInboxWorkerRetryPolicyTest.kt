package joyin.takgi.paysage.mail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailInboxWorkerRetryPolicyTest {
    @Test
    fun retriesTransientNetworkOrServerFailures() {
        val result = MailInboxRefreshResult(
            success = false,
            message = "邮箱服务器暂时不可达。",
            failureKind = MailInboxFailureKind.NetworkOrServerFailed
        )

        assertTrue(MailInboxWorkerRetryPolicy.shouldRetry(result))
    }

    @Test
    fun doesNotRetryAuthenticationFailuresUntilUserFixesCredentials() {
        val result = MailInboxRefreshResult(
            success = false,
            message = "邮箱登录失败，请检查用户名和授权码。",
            failureKind = MailInboxFailureKind.AuthenticationFailed
        )

        assertFalse(MailInboxWorkerRetryPolicy.shouldRetry(result))
    }

    @Test
    fun doesNotRetryInvalidConfigurationUntilUserFixesSettings() {
        val result = MailInboxRefreshResult(
            success = false,
            message = "邮箱配置不完整。",
            failureKind = MailInboxFailureKind.InvalidConfig
        )

        assertFalse(MailInboxWorkerRetryPolicy.shouldRetry(result))
    }

    @Test
    fun doesNotRetrySuccessfulRefreshWithRejectedCommands() {
        val result = MailInboxRefreshResult(
            success = true,
            message = "已检查 1 封邮件，执行 0 条指令，忽略 1 封。",
            failureKind = MailInboxFailureKind.None,
            fetched = 1,
            ignored = 1,
            rejected = 1
        )

        assertFalse(MailInboxWorkerRetryPolicy.shouldRetry(result))
    }
}
