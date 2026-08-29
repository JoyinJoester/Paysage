package joyin.takgi.paysage.reliability

import android.content.Context
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardAccount
import joyin.takgi.paysage.security.ForwardAccountSecretStore
import joyin.takgi.paysage.sender.EmailSender
import joyin.takgi.paysage.sender.TelegramSender
import kotlinx.coroutines.flow.first

class ForwardAccountAlertSender(context: Context) {
    private val appContext = context.applicationContext
    private val accountDao = AppDatabase.getDatabase(appContext).forwardAccountDao()
    private val secretStore = ForwardAccountSecretStore(appContext)

    suspend fun send(subject: String, body: String) {
        if (!SmsNetworkMonitor.isConnected(appContext)) return
        accountDao.getEnabled().first().forEach { account ->
            when (account.type) {
                AccountType.EMAIL -> sendEmail(account, subject, body)
                AccountType.TELEGRAM -> sendTelegram(account, subject, body)
            }
        }
    }

    private suspend fun sendEmail(account: ForwardAccount, subject: String, body: String) {
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
            return
        }

        EmailSender(
            smtpHost = securedAccount.smtpHost,
            smtpPort = securedAccount.smtpPort,
            username = securedAccount.smtpUsername,
            credential = credential,
            toEmail = securedAccount.toEmail,
            context = appContext,
            authType = securedAccount.smtpAuthType
        ).sendPlain(subject, body)
    }

    private suspend fun sendTelegram(account: ForwardAccount, subject: String, body: String) {
        if (account.botToken.isBlank() || account.chatId.isBlank()) return
        TelegramSender(account.botToken, account.chatId, appContext)
            .sendPlain("$subject\n\n$body")
    }
}
