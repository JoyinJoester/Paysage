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
            val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")

                val message = appContext?.getString(
                    R.string.format_sms_telegram_message,
                    from,
                    DateFormatter.format(timestamp),
                    content
                ) ?: "New SMS\n\nSender: $from\nTime: ${DateFormatter.format(timestamp)}\n\nContent:\n$content"

                val json = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", message)
                }

                outputStream.write(json.toString().toByteArray())

                if (responseCode !in 200..299) {
                    throw Exception("Telegram API error: $responseCode")
                }
            }
        }
    }
}
