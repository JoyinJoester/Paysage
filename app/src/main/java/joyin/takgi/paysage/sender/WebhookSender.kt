package joyin.takgi.paysage.sender

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class WebhookSender(
    private val type: AccountType,
    private val webhookUrl: String,
    private val webhookSecret: String,
    context: Context? = null
) {
    private val appContext = context?.applicationContext

    suspend fun send(from: String, content: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val formatted = appContext?.getString(
                R.string.format_sms_telegram_message,
                from,
                DateFormatter.format(timestamp),
                content
            ) ?: "New SMS\n\nSender: $from\nTime: ${DateFormatter.format(timestamp)}\n\nContent:\n$content"
            execute(WebhookMessage.build(type, webhookUrl, webhookSecret, from, formatted))
        }
    }

    suspend fun sendPlain(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            execute(WebhookMessage.build(type, webhookUrl, webhookSecret, "Paysage", text))
        }
    }

    private fun execute(request: WebhookMessage.Request) {
        require(request.url.startsWith("http://") || request.url.startsWith("https://")) {
            "Webhook URL must start with http(s)://"
        }
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", request.contentType)
                request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
            }
            connection.outputStream.use { output ->
                output.write(request.body.toByteArray(Charsets.UTF_8))
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                runCatching {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty()
            }
            if (connection.responseCode !in 200..299) {
                throw Exception("Webhook error: HTTP ${connection.responseCode} ${responseText.take(160)}")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
    }
}
