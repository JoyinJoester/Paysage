package joyin.takgi.paysage.sender

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.SmtpAuthType
import joyin.takgi.paysage.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailSender(
    private val smtpHost: String,
    private val smtpPort: Int,
    private val username: String,
    private val credential: String,
    private val toEmail: String,
    context: Context? = null,
    private val authType: SmtpAuthType = SmtpAuthType.PASSWORD,
    private val encryptionKeyBase64: String = ""
) {
    private val appContext = context?.applicationContext

    suspend fun send(from: String, content: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(smtpHost.isNotBlank()) { "SMTP host is required." }
            require(username.isNotBlank()) { "SMTP username is required." }
            require(credential.isNotBlank()) { "SMTP credential is required." }
            require(toEmail.isNotBlank()) { "Recipient email is required." }

            val props = Properties().apply {
                put("mail.smtp.host", smtpHost)
                put("mail.smtp.port", smtpPort.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.connectiontimeout", CONNECTION_TIMEOUT_MS.toString())
                put("mail.smtp.timeout", READ_TIMEOUT_MS.toString())
                put("mail.smtp.writetimeout", WRITE_TIMEOUT_MS.toString())
                put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
                if (smtpPort == SSL_SMTP_PORT) {
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
                if (authType == SmtpAuthType.XOAUTH2) {
                    put("mail.smtp.auth.mechanisms", "XOAUTH2")
                    put("mail.smtp.sasl.enable", "true")
                    put("mail.smtp.sasl.mechanisms", "XOAUTH2")
                    put("mail.smtp.auth.login.disable", "true")
                    put("mail.smtp.auth.plain.disable", "true")
                }
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(username, credential)
            })
            val plainBody = buildPlainBody(from, content, timestamp)
            val encryptedPayload = encryptionKeyBase64
                .takeIf { it.isNotBlank() }
                ?.let { EmailPayloadEncryption.encrypt(plainBody, it) }

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = if (encryptedPayload == null) {
                    appContext?.getString(R.string.format_sms_email_subject, from)
                        ?: "[SMS] From $from"
                } else {
                    setHeader("X-Paysage-Encryption", encryptedPayload.version)
                    appContext?.getString(R.string.format_sms_encrypted_email_subject)
                        ?: "[Paysage] Encrypted SMS"
                }
                setText(encryptedPayload?.let(::buildEncryptedBody) ?: plainBody)
            }

            Transport.send(message)
        }
    }

    private fun buildPlainBody(from: String, content: String, timestamp: Long): String =
        appContext?.getString(
            R.string.format_sms_forward_body,
            from,
            DateFormatter.format(timestamp),
            content
        ) ?: "Sender: $from\nTime: ${DateFormatter.format(timestamp)}\n\nContent:\n$content"

    private fun buildEncryptedBody(payload: EncryptedEmailPayload): String =
        appContext?.getString(
            R.string.format_sms_encrypted_forward_body,
            payload.version,
            payload.ivBase64,
            payload.ciphertextBase64
        ) ?: "Paysage encrypted SMS\nFormat: ${payload.version}\nIV: ${payload.ivBase64}\nCiphertext: ${payload.ciphertextBase64}"

    companion object {
        private const val SSL_SMTP_PORT = 465
        private const val CONNECTION_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
        private const val WRITE_TIMEOUT_MS = 20_000
    }
}
