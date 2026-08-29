package joyin.takgi.paysage.telegram

import android.content.Context
import joyin.takgi.paysage.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TelegramBotSetupResult(
    val botName: String,
    val botUsername: String,
    val chatId: String,
    val chatTitle: String
)

class TelegramBotSetupProbe(context: Context) {
    private val appContext = context.applicationContext

    suspend fun fetchLatestChat(botToken: String): Result<TelegramBotSetupResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(botToken.isNotBlank()) {
                appContext.getString(R.string.message_telegram_token_required)
            }

            val bot = request(botToken.trim(), "getMe", JSONObject())
                .getJSONObject("result")
            val updates = request(
                botToken.trim(),
                "getUpdates",
                JSONObject().apply {
                    put("limit", 50)
                    put("timeout", 0)
                    put("allowed_updates", JSONArray().put("message"))
                }
            ).optJSONArray("result") ?: JSONArray()

            val chat = latestMessageChat(updates)
                ?: error(appContext.getString(R.string.message_telegram_send_bot_message_first))

            TelegramBotSetupResult(
                botName = bot.optString("first_name").ifBlank { appContext.getString(R.string.type_telegram) },
                botUsername = bot.optString("username"),
                chatId = chat.opt("id")?.toString().orEmpty(),
                chatTitle = chatTitle(chat)
            )
        }
    }

    private fun latestMessageChat(updates: JSONArray): JSONObject? {
        for (index in updates.length() - 1 downTo 0) {
            val message = updates.optJSONObject(index)?.optJSONObject("message") ?: continue
            val chat = message.optJSONObject("chat") ?: continue
            if (chat.opt("id") != null) return chat
        }
        return null
    }

    private fun chatTitle(chat: JSONObject): String {
        val explicitTitle = chat.optString("title").ifBlank { chat.optString("username") }
        if (explicitTitle.isNotBlank()) return explicitTitle
        return listOf(chat.optString("first_name"), chat.optString("last_name"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { appContext.getString(R.string.value_unknown) }
    }

    private fun request(botToken: String, method: String, body: JSONObject): JSONObject {
        val connection = URL("https://api.telegram.org/bot$botToken/$method")
            .openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray())
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            val response = JSONObject(responseText.ifBlank { "{}" })
            if (connection.responseCode !in 200..299 || !response.optBoolean("ok", false)) {
                val description = response.optString("description").ifBlank {
                    "HTTP ${connection.responseCode}"
                }
                throw IllegalStateException(description)
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
    }
}
