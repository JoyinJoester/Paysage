package joyin.takgi.paysage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forward_accounts")
data class ForwardAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: AccountType, // EMAIL, TELEGRAM 或 Webhook 类通道
    val isEnabled: Boolean = true,

    // 邮箱配置
    val smtpProvider: SmtpProvider = SmtpProvider.CUSTOM,
    val smtpHost: String = "",
    val smtpPort: Int = 587,
    val smtpUsername: String = "",
    val smtpPassword: String = "",
    val smtpAuthType: SmtpAuthType = SmtpAuthType.PASSWORD,
    val smtpCredentialRef: String = "",
    val toEmail: String = "",
    val emailEncryptionEnabled: Boolean = false,
    val emailEncryptionKeyRef: String = "",

    // Telegram 配置
    val botToken: String = "",
    val chatId: String = "",

    // Webhook 类通道配置(通用 Webhook / Bark / Server酱 / 钉钉 / 飞书 / 企业微信)
    val webhookUrl: String = "",
    val webhookSecret: String = "",

    // 过滤规则（号码白名单，逗号分隔）
    val phoneWhitelist: String = "" // 空=全部转发
)

enum class AccountType {
    EMAIL,
    TELEGRAM,
    WEBHOOK,
    BARK,
    SERVERCHAN,
    DINGTALK,
    FEISHU,
    WECOM;

    val isWebhookType: Boolean
        get() = this in WEBHOOK..WECOM
}

enum class SmtpProvider {
    GMAIL, CUSTOM
}

enum class SmtpAuthType {
    PASSWORD, XOAUTH2
}
