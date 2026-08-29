package joyin.takgi.paysage.telegram

import android.content.Context
import android.content.SharedPreferences
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardAccount
import joyin.takgi.paysage.mail.MailCommandDecisionCode
import joyin.takgi.paysage.mail.MailCommandParseException
import joyin.takgi.paysage.mail.MailCommandParser
import joyin.takgi.paysage.mail.PaysageCommandExecutor
import joyin.takgi.paysage.mail.displayName
import joyin.takgi.paysage.reliability.SmsForwardingControlStore
import joyin.takgi.paysage.sender.TelegramSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class TelegramCommandRefreshResult(
    val success: Boolean,
    val checked: Int = 0,
    val executed: Int = 0,
    val ignored: Int = 0,
    val message: String = "",
    val hasAccounts: Boolean = false
)

class TelegramCommandRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val stateStore = TelegramCommandStateStore(appContext)
    private val commandExecutor = PaysageCommandExecutor(appContext)
    private val forwardingControlStore = SmsForwardingControlStore(appContext)

    suspend fun refreshCommands(
        pollTimeoutSeconds: Int = 0
    ): TelegramCommandRefreshResult = refreshMutex.withLock {
        refreshCommandsLocked(pollTimeoutSeconds)
    }

    private suspend fun refreshCommandsLocked(
        pollTimeoutSeconds: Int
    ): TelegramCommandRefreshResult {
        val accounts = database.forwardAccountDao().getEnabled().first()
            .filter { it.type == AccountType.TELEGRAM && it.botToken.isNotBlank() && it.chatId.isNotBlank() }

        if (accounts.isEmpty()) {
            return TelegramCommandRefreshResult(
                success = true,
                message = "No Telegram command account.",
                hasAccounts = false
            )
        }

        var checked = 0
        var executed = 0
        var ignored = 0
        var lastError: String? = null

        accounts.groupBy { it.botToken.trim() }.forEach { (botToken, tokenAccounts) ->
            runCatching {
                val allowedByChatId = tokenAccounts.associateBy { it.chatId.trim() }
                val client = TelegramBotApi(botToken)
                client.deleteWebhook()
                val updates = client.getUpdates(
                    offset = stateStore.readOffset(botToken),
                    timeoutSeconds = pollTimeoutSeconds
                )
                updates.forEach { update ->
                    val updateId = update.optLong("update_id", -1L)
                    if (updateId >= 0L) {
                        stateStore.writeOffset(botToken, updateId + 1L)
                    }
                    checked += 1

                    val message = update.optJSONObject("message") ?: run {
                        ignored += 1
                        return@forEach
                    }
                    val chat = message.optJSONObject("chat") ?: run {
                        ignored += 1
                        return@forEach
                    }
                    val chatId = chat.opt("id")?.toString().orEmpty()
                    val account = allowedByChatId[chatId] ?: run {
                        ignored += 1
                        return@forEach
                    }
                    val text = message.optString("text").trim()
                    if (text.isBlank()) {
                        ignored += 1
                        return@forEach
                    }

                    val normalizedText = normalizeTelegramCommandText(text)
                    val parseResult = MailCommandParser.parseMessage(
                        subject = "",
                        body = normalizedText,
                        context = appContext
                    )
                    val command = parseResult.getOrNull()
                    if (command == null) {
                        val parseError = parseResult.exceptionOrNull() as? MailCommandParseException
                        if (parseError?.code != MailCommandDecisionCode.NoCommand && normalizedText.startsWith("/paysage", ignoreCase = true)) {
                            sendTelegramReply(
                                account,
                                appContext.getString(
                                    R.string.format_telegram_command_rejected,
                                    parseError?.message.orEmpty()
                                )
                            )
                            ignored += 1
                        } else {
                            ignored += 1
                        }
                        return@forEach
                    }

                    val resultMessage = commandExecutor.execute(command)
                    val reply = buildExecutionReply(command.action.displayName(appContext), command.argument, resultMessage)
                    sendTelegramReply(account, reply)
                    executed += 1
                }
            }.onFailure { error ->
                val raw = error.message.orEmpty()
                lastError = if (raw.contains("Conflict", ignoreCase = true) || raw.contains("409")) {
                    // 409:同一 Token 被多处 getUpdates 或 webhook 占用
                    appContext.getString(
                        R.string.format_telegram_command_conflict_hint,
                        raw.take(120)
                    )
                } else {
                    raw.ifBlank { error::class.java.simpleName }
                }
            }
        }

        return TelegramCommandRefreshResult(
            success = lastError == null,
            checked = checked,
            executed = executed,
            ignored = ignored,
            hasAccounts = true,
            message = lastError?.let {
                appContext.getString(R.string.format_telegram_command_refresh_failed, it.take(180))
            } ?: appContext.getString(R.string.format_telegram_command_refresh_success, checked, executed, ignored)
        )
    }

    private suspend fun sendTelegramReply(account: ForwardAccount, text: String) {
        TelegramSender(
            botToken = account.botToken,
            chatId = account.chatId,
            context = appContext
        ).sendPlain(text)
    }

    private suspend fun buildExecutionReply(
        actionName: String,
        argument: String?,
        resultMessage: String
    ): String {
        val pendingCount = database.pendingForwardDao().pendingCount()
        val forwardingState = if (forwardingControlStore.isPaused()) {
            appContext.getString(R.string.status_paused)
        } else {
            appContext.getString(R.string.title_forward_running)
        }
        return buildString {
            appendLine(appContext.getString(R.string.telegram_command_completed))
            appendLine(appContext.getString(R.string.format_telegram_command_action, actionName))
            argument?.takeIf { it.isNotBlank() }?.let {
                appendLine(appContext.getString(R.string.format_telegram_command_argument, it))
            }
            appendLine()
            appendLine(appContext.getString(R.string.mail_receipt_result_title))
            appendLine(resultMessage)
            appendLine()
            appendLine(appContext.getString(R.string.mail_receipt_current_status_title))
            appendLine(appContext.getString(R.string.format_mail_receipt_forwarding_state, forwardingState))
            appendLine(appContext.getString(R.string.format_mail_receipt_pending_cache, pendingCount))
        }
    }

    private fun normalizeTelegramCommandText(text: String): String {
        val lines = text.lines()
        if (lines.isEmpty()) return text
        val firstLine = lines.first().trim()
        val normalizedFirstLine = botMentionCommandRegex.replaceFirst(firstLine, "/paysage")
        if (normalizedFirstLine == firstLine) return text
        return listOf(normalizedFirstLine)
            .plus(lines.drop(1))
            .joinToString("\n")
    }

    companion object {
        private val refreshMutex = Mutex()
        private val botMentionCommandRegex = Regex("^/paysage@[A-Za-z0-9_]+(?=\\s|$)", RegexOption.IGNORE_CASE)
    }
}

private class TelegramBotApi(
    private val botToken: String
) {
    suspend fun deleteWebhook(): Unit = withContext(Dispatchers.IO) {
        runCatching {
            request(
                method = "deleteWebhook",
                body = JSONObject().apply {
                    put("drop_pending_updates", false)
                }
            )
        }.getOrThrow()
    }

    suspend fun getUpdates(
        offset: Long,
        timeoutSeconds: Int = 0
    ): List<JSONObject> = withContext(Dispatchers.IO) {
        val response = request(
            method = "getUpdates",
            readTimeoutMs = maxOf(READ_TIMEOUT_MS, (timeoutSeconds + 10) * 1000),
            body = JSONObject().apply {
                if (offset > 0L) put("offset", offset)
                put("limit", 30)
                put("timeout", timeoutSeconds.coerceIn(0, 50))
                put("allowed_updates", JSONArray().put("message"))
            }
        )
        val updates = response.optJSONArray("result") ?: JSONArray()
        buildList {
            for (index in 0 until updates.length()) {
                updates.optJSONObject(index)?.let(::add)
            }
        }
    }

    private fun request(
        method: String,
        body: JSONObject,
        readTimeoutMs: Int = READ_TIMEOUT_MS
    ): JSONObject {
        val connection = URL("https://api.telegram.org/bot$botToken/$method")
            .openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = readTimeoutMs
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
                val description = response.optString("description").ifBlank { "HTTP ${connection.responseCode}" }
                throw IllegalStateException("Telegram $method failed: ${description.take(180)}")
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

private class TelegramCommandStateStore(context: Context) {
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readOffset(botToken: String): Long =
        preferences.getLong(offsetKey(botToken), 0L)

    fun writeOffset(botToken: String, nextOffset: Long) {
        preferences.edit()
            .putLong(offsetKey(botToken), nextOffset)
            .apply()
    }

    private fun offsetKey(botToken: String): String =
        "offset_${sha256(botToken)}"

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "paysage_telegram_command_state"
    }
}
