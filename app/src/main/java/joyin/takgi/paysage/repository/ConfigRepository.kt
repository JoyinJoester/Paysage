package joyin.takgi.paysage.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class EmailConfig(
    val host: String = "",
    val port: Int = 587,
    val username: String = "",
    val password: String = "",
    val toEmail: String = "",
    val enabled: Boolean = false
)

data class TelegramConfig(
    val botToken: String = "",
    val chatId: String = "",
    val enabled: Boolean = false
)

class ConfigRepository(private val context: Context) {

    private object Keys {
        val SMTP_HOST = stringPreferencesKey("smtp_host")
        val SMTP_PORT = intPreferencesKey("smtp_port")
        val SMTP_USERNAME = stringPreferencesKey("smtp_username")
        val SMTP_PASSWORD = stringPreferencesKey("smtp_password")
        val TO_EMAIL = stringPreferencesKey("to_email")
        val EMAIL_ENABLED = booleanPreferencesKey("email_enabled")

        val BOT_TOKEN = stringPreferencesKey("bot_token")
        val CHAT_ID = stringPreferencesKey("chat_id")
        val TELEGRAM_ENABLED = booleanPreferencesKey("telegram_enabled")
    }

    val emailConfig: Flow<EmailConfig> = context.dataStore.data.map { prefs ->
        EmailConfig(
            host = prefs[Keys.SMTP_HOST] ?: "",
            port = prefs[Keys.SMTP_PORT] ?: 587,
            username = prefs[Keys.SMTP_USERNAME] ?: "",
            password = prefs[Keys.SMTP_PASSWORD] ?: "",
            toEmail = prefs[Keys.TO_EMAIL] ?: "",
            enabled = prefs[Keys.EMAIL_ENABLED] ?: false
        )
    }

    val telegramConfig: Flow<TelegramConfig> = context.dataStore.data.map { prefs ->
        TelegramConfig(
            botToken = prefs[Keys.BOT_TOKEN] ?: "",
            chatId = prefs[Keys.CHAT_ID] ?: "",
            enabled = prefs[Keys.TELEGRAM_ENABLED] ?: false
        )
    }

    suspend fun saveEmailConfig(config: EmailConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SMTP_HOST] = config.host
            prefs[Keys.SMTP_PORT] = config.port
            prefs[Keys.SMTP_USERNAME] = config.username
            prefs[Keys.SMTP_PASSWORD] = config.password
            prefs[Keys.TO_EMAIL] = config.toEmail
            prefs[Keys.EMAIL_ENABLED] = config.enabled
        }
    }

    suspend fun saveTelegramConfig(config: TelegramConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BOT_TOKEN] = config.botToken
            prefs[Keys.CHAT_ID] = config.chatId
            prefs[Keys.TELEGRAM_ENABLED] = config.enabled
        }
    }
}
