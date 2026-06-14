package joyin.takgi.paysage.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mail_trusted_senders",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class MailTrustedSenderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val allowedActions: String,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "mail_command_nonces")
data class MailCommandNonceEntity(
    @PrimaryKey
    val nonceKey: String,
    val sender: String,
    val nonce: String,
    val action: String,
    val usedAt: Long,
    val expiresAt: Long
)

@Entity(
    tableName = "mail_command_records",
    indices = [
        Index(value = ["messageKey"], unique = false),
        Index(value = ["processedAtMillis"])
    ]
)
data class MailCommandRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val messageKey: String,
    val messageNumber: Int,
    val sender: String,
    val normalizedSender: String,
    val subject: String,
    val receivedAtMillis: Long,
    val action: String,
    val decisionCode: String,
    val allowed: Boolean,
    val executed: Boolean,
    val resultMessage: String,
    val processedAtMillis: Long
)
