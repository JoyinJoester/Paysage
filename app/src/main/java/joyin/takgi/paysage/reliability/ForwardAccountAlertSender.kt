package joyin.takgi.paysage.reliability

import android.content.Context
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardAccount
import joyin.takgi.paysage.security.ForwardAccountSecretStore
import joyin.takgi.paysage.sender.EmailSender
import joyin.takgi.paysage.sender.TelegramSender
import joyin.takgi.paysage.sender.WebhookMessage
import joyin.takgi.paysage.sender.WebhookSender
import kotlinx.coroutines.flow.first

class ForwardAccountAlertSender(context: Context) {
    private val appContext = context.applicationContext
    private val accountDao = AppDatabase.getDatabase(appContext).forwardAccountDao()
    private val secretStore = ForwardAccountSecretStore(appContext)

    suspend fun send(subject: String, body: String) {
        // 告警类推送保持原行为:全部通道都发
        sendTo(subject, body, useEmail = true, useTelegram = true, useWebhook = true)
    }

    /** 已配置且可用的通道;两者都有时由调用方弹窗让用户选择 */
    suspend fun configuredTargets(): Pair<Boolean, Boolean> {
        val accounts = accountDao.getEnabled().first()
        val email = accounts.any {
            it.type == AccountType.EMAIL && it.smtpHost.isNotBlank() && it.toEmail.isNotBlank()
        }
        val telegram = accounts.any {
            it.type == AccountType.TELEGRAM && it.botToken.isNotBlank() && it.chatId.isNotBlank()
        }
        return email to telegram
    }

    /** 定向推送到指定通道,任一通道成功即返回 true */
    suspend fun sendTo(
        subject: String,
        body: String,
        useEmail: Boolean,
        useTelegram: Boolean,
        useWebhook: Boolean = false
    ): Boolean {
        if (!SmsNetworkMonitor.isConnected(appContext)) return false
        var anySuccess = false
        val accounts = accountDao.getEnabled().first()
        if (useEmail) {
            accounts.filter { it.type == AccountType.EMAIL }.forEach { account ->
                if (sendEmail(account, subject, body)) anySuccess = true
            }
        }
        if (useTelegram) {
            accounts.filter { it.type == AccountType.TELEGRAM }.forEach { account ->
                if (sendTelegram(account, subject, body)) anySuccess = true
            }
        }
        if (useWebhook) {
            accounts.filter { it.type.isWebhookType }.forEach { account ->
                if (sendWebhook(account, subject, body)) anySuccess = true
            }
        }
        return anySuccess
    }

    private suspend fun sendEmail(account: ForwardAccount, subject: String, body: String): Boolean {
        val securedAccount = secretStore.migratePlaintextCredential(account)
        val credentialRef = secretStore.accountCredentialRef(securedAccount)
        val credential = secretStore.readCredential(credentialRef).ifBlank {
            securedAccount.smtpPassword
        }
        if (
            securedAccount.smtpHost.isBlank() ||
            securedAccount.smtpUsername.isBlank() ||
            securedAccount.toEmail.isBlank() ||
            credential.isBlank()
        ) {
            return false
        }

        return EmailSender(
            smtpHost = securedAccount.smtpHost,
            smtpPort = securedAccount.smtpPort,
            username = securedAccount.smtpUsername,
            credential = credential,
            toEmail = securedAccount.toEmail,
            context = appContext,
            authType = securedAccount.smtpAuthType
        ).sendPlain(subject, body).isSuccess
    }

    private suspend fun sendTelegram(account: ForwardAccount, subject: String, body: String): Boolean {
        if (account.botToken.isBlank() || account.chatId.isBlank()) return false
        return TelegramSender(account.botToken, account.chatId, appContext)
            .sendPlain("$subject\n\n$body")
            .isSuccess
    }

    private suspend fun sendWebhook(account: ForwardAccount, subject: String, body: String): Boolean {
        if (!WebhookMessage.isReady(account.type, account.webhookUrl, account.webhookSecret)) return false
        return WebhookSender(account.type, account.webhookUrl, account.webhookSecret, appContext)
            .sendPlain("$subject\n\n$body")
            .isSuccess
    }
}
