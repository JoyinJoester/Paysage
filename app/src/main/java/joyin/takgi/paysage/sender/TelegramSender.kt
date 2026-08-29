package joyin.takgi.paysage.sender

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class TelegramSender(
    private val botToken: String,
    private val chatId: String,
    context: Context? = null
) {
    private val appContext = context?.applicationContext

    suspend fun send(from: String, content: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            val rawMessage = appContext?.getString(
                R.string.format_sms_telegram_message,
                from,
                DateFormatter.format(timestamp),
                content
            ) ?: "New SMS\n\nSender: $from\nTime: ${DateFormatter.format(timestamp)}\n\nContent:\n$content"
            val message = TelegramSmsFormatter.formatWithCodeSpoilers(rawMessage)
            sendMessage(message.text, message.parseMode)
        }
    }

    suspend fun sendPlain(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            sendMessage(text)
        }
    }

    private fun sendMessage(text: String, parseMode: String? = null) {
        require(botToken.isNotBlank()) { "Telegram bot token is required." }
        require(chatId.isNotBlank()) { "Telegram chat ID is required." }
        require(text.isNotBlank()) { "Telegram message is required." }

        val connection = URL("https://api.telegram.org/bot$botToken/sendMessage")
            .openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }

            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text.take(MAX_MESSAGE_LENGTH))
                parseMode?.takeIf { it.isNotBlank() }?.let { put("parse_mode", it) }
                put("disable_web_page_preview", true)
            }

            connection.outputStream.use { output ->
                output.write(json.toString().toByteArray())
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                runCatching {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty()
            }
            val response = runCatching { JSONObject(responseText.ifBlank { "{}" }) }.getOrNull()
            if (connection.responseCode !in 200..299 || response?.optBoolean("ok", false) != true) {
                val description = response?.optString("description")
                    ?.takeIf { it.isNotBlank() }
                    ?: responseText.take(160).ifBlank { "HTTP ${connection.responseCode}" }
                throw Exception("Telegram API error: $description")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
        private const val MAX_MESSAGE_LENGTH = 3900
    }
}
