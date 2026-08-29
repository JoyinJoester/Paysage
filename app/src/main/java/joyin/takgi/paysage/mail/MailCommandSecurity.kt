package joyin.takgi.paysage.mail

import android.content.Context
import joyin.takgi.paysage.R
import java.time.Instant
import java.time.OffsetDateTime
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.mail.internet.InternetAddress

enum class MailCommandAction(val wireName: String) {
    Status("status"),
    DeviceStatus("device-status"),
    NetworkSpeed("network-speed"),
    RetryCache("retry-cache"),
    PauseForwarding("pause-forwarding"),
    ResumeForwarding("resume-forwarding"),
    ToggleForwarding("shareswitch");

    companion object {
        fun fromWireName(value: String): MailCommandAction? {
            val normalized = value.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.wireName == normalized } ?: when (normalized) {
                "sw" -> ToggleForwarding
                "sta" -> DeviceStatus
                "net", "network" -> NetworkSpeed
                else -> null
            }
        }
    }
}

data class ParsedMailCommand(
    val action: MailCommandAction,
    val key: String?,
    val signature: String?,
    val expiresRaw: String,
    val expiresAtEpochMillis: Long,
    val nonce: String,
    val argument: String? = null,
    val requiresAuthenticator: Boolean = true
)

data class TrustedMailSender(
    val email: String,
    val allowedActions: Set<MailCommandAction>,
    val secret: String,
    val enabled: Boolean = true
)

enum class MailCommandDecisionCode {
    Allowed,
    NoCommand,
    InvalidCommand,
    InvalidSender,
    SenderNotWhitelisted,
    SenderDisabled,
    ActionNotAllowed,
    MissingAuthenticator,
    InvalidAuthenticator,
    Expired,
    ExpiresTooFar,
    NonceReused
}

data class MailCommandDecision(
    val allowed: Boolean,
    val code: MailCommandDecisionCode,
    val message: String
)

object MailAddressNormalizer {
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        val parsedAddresses = runCatching {
            InternetAddress.parseHeader(trimmed, false)
                .mapNotNull { address -> address.address?.trim()?.takeIf { it.isNotBlank() } }
        }.getOrNull()

        val candidate = when {
            parsedAddresses != null && parsedAddresses.size == 1 -> parsedAddresses.single()
            parsedAddresses != null && parsedAddresses.size > 1 -> return null
            else -> trimmed
                .substringAfterLast("<")
                .substringBefore(">")
                .trim()
        }

        val normalized = candidate.lowercase(Locale.ROOT)
        return normalized.takeIf { value ->
            value.count { it == '@' } == 1 &&
                value.substringBefore('@').isNotBlank() &&
                value.substringAfter('@').contains('.') &&
                !value.any(Char::isWhitespace)
        }
    }
}

object MailCommandParser {
    fun parseMessage(
        subject: String,
        body: String?,
        context: Context? = null
    ): Result<ParsedMailCommand> {
        val bodyResult = parse(body.orEmpty(), context)
        val bodyError = bodyResult.exceptionOrNull() as? MailCommandParseException
        if (bodyResult.isSuccess || bodyError?.code != MailCommandDecisionCode.NoCommand) {
            return bodyResult
        }

        val subjectResult = parse(subject, context)
        val subjectError = subjectResult.exceptionOrNull() as? MailCommandParseException
        return if (subjectResult.isSuccess || subjectError?.code != MailCommandDecisionCode.NoCommand) {
            subjectResult
        } else {
            bodyResult
        }
    }

    fun parse(body: String, context: Context? = null): Result<ParsedMailCommand> = runCatching {
        val rawLines = body.lineSequence().toList()
        val trimmedNonBlankLines = rawLines
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val simpleCommandLines = trimmedNonBlankLines.filter { isSimpleCommandLine(it) }
        val commandLines = trimmedNonBlankLines.filter { isCommandLine(it) }

        if (simpleCommandLines.isNotEmpty()) {
            val distinctSimpleCommandLines = simpleCommandLines.distinctBy { normalizeSimpleCommandLine(it) }
            if (distinctSimpleCommandLines.size > 1 || commandLines.isNotEmpty()) {
                throw MailCommandParseException(
                    MailCommandDecisionCode.InvalidCommand,
                    mailCommandText(context, R.string.message_mail_only_one_command)
                )
            }
            return@runCatching parseSimpleCommandLine(distinctSimpleCommandLines.single(), context)
        }

        if (commandLines.size > 1) {
            throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_only_one_command)
            )
        }
        val commandLine = commandLines.firstOrNull()
            ?: throw MailCommandParseException(
                MailCommandDecisionCode.NoCommand,
                mailCommandText(context, R.string.message_mail_no_paysage_command)
            )
        val commandLineIndex = rawLines.indexOfFirst { it.trim() == commandLine }

        val actionTokens = commandLine
            .substring(COMMAND_PREFIX.length)
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (actionTokens.size != 1) {
            throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_command_line_invalid)
            )
        }
        val actionName = actionTokens.single()
        val action = MailCommandAction.fromWireName(actionName)
            ?: throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_unknown_command)
            )

        val fieldLines = rawLines
            .drop(commandLineIndex + 1)
            .takeWhile { it.trim().isNotBlank() }
            .map { it.trim() }
        val fields = parseFields(fieldLines, context = context)

        val expiresRaw = fields["expires"]
            ?: throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_missing_expires)
            )
        val nonce = fields["nonce"]?.takeIf { it.isNotBlank() }
            ?: throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_missing_nonce)
            )
        val key = fields["key"]?.takeIf { it.isNotBlank() }
        val signature = fields["sig"]?.takeIf { it.isNotBlank() }
        if (signature != null && !signaturePattern.matches(signature)) {
            throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_signature_invalid)
            )
        }

        ParsedMailCommand(
            action = action,
            key = key,
            signature = signature,
            expiresRaw = expiresRaw,
            expiresAtEpochMillis = parseExpires(expiresRaw, context),
            nonce = nonce
        )
    }

    private fun parseFields(lines: List<String>, context: Context?): Map<String, String> {
        val allowedFields = setOf("key", "sig", "expires", "nonce")
        val fields = linkedMapOf<String, String>()
        lines.forEach { line ->
            val separatorIndex = line.indexOf(':')
            if (separatorIndex <= 0) {
                return@forEach
            }
            val name = line.substring(0, separatorIndex).trim().lowercase(Locale.ROOT)
            if (name !in allowedFields) {
                throw MailCommandParseException(
                    MailCommandDecisionCode.InvalidCommand,
                    mailCommandText(context, R.string.message_mail_unsupported_field)
                )
            }
            if (fields.containsKey(name)) {
                throw MailCommandParseException(
                    MailCommandDecisionCode.InvalidCommand,
                    mailCommandText(context, R.string.message_mail_duplicate_field)
                )
            }
            fields[name] = validateFieldValue(
                name = name,
                value = line.substring(separatorIndex + 1).trim(),
                context = context
            )
        }
        return fields
    }

    private fun validateFieldValue(name: String, value: String, context: Context?): String {
        val maxLength = fieldMaxLengths.getValue(name)
        if (value.isBlank()) {
            throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_field_empty)
            )
        }
        if (value.length > maxLength) {
            throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_field_too_long)
            )
        }
        if (value.any { Character.isISOControl(it) }) {
            throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_field_control_chars)
            )
        }
        return value
    }

    private fun parseExpires(raw: String, context: Context?): Long =
        runCatching { Instant.parse(raw).toEpochMilli() }
            .getOrElse {
                runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
                    .getOrElse {
                        throw MailCommandParseException(
                            MailCommandDecisionCode.InvalidCommand,
                            mailCommandText(context, R.string.message_mail_expires_invalid)
                        )
                    }
            }

    private val fieldMaxLengths = mapOf(
        "key" to 256,
        "sig" to 96,
        "expires" to 64,
        "nonce" to 128
    )

    private val signaturePattern = Regex("(?i)^sha256=[0-9a-f]{64}$")

    private const val COMMAND_PREFIX = "#paysage"
    private const val SIMPLE_COMMAND_PREFIX = "/paysage"

    private fun parseSimpleCommandLine(line: String, context: Context?): ParsedMailCommand {
        val tokens = line.substring(SIMPLE_COMMAND_PREFIX.length).trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_command_line_invalid)
            )
        }
        val normalized = tokens.map { it.lowercase(Locale.ROOT) }
        val parsed = when {
            normalized.size == 1 && normalized[0] in setOf("shareswitch", "sw") ->
                MailCommandAction.ToggleForwarding to null
            normalized.size == 1 && normalized[0] in setOf("status", "sta") ->
                MailCommandAction.DeviceStatus to null
            normalized.size == 1 && normalized[0] in setOf("network", "net") ->
                MailCommandAction.NetworkSpeed to null
            else -> throw MailCommandParseException(
                MailCommandDecisionCode.InvalidCommand,
                mailCommandText(context, R.string.message_mail_unknown_command)
            )
        }
        return ParsedMailCommand(
            action = parsed.first,
            key = null,
            signature = null,
            expiresRaw = "",
            expiresAtEpochMillis = Long.MAX_VALUE,
            nonce = "",
            argument = parsed.second,
            requiresAuthenticator = false
        )
    }

    private fun isSimpleCommandLine(line: String): Boolean =
        line.equals(SIMPLE_COMMAND_PREFIX, ignoreCase = true) ||
            (
                line.length > SIMPLE_COMMAND_PREFIX.length &&
                    line.regionMatches(
                        thisOffset = 0,
                        other = SIMPLE_COMMAND_PREFIX,
                        otherOffset = 0,
                        length = SIMPLE_COMMAND_PREFIX.length,
                        ignoreCase = true
                    ) &&
                    line[SIMPLE_COMMAND_PREFIX.length].isWhitespace()
                )

    private fun normalizeSimpleCommandLine(line: String): String =
        line.trim()
            .split(Regex("\\s+"))
            .joinToString(" ")
            .lowercase(Locale.ROOT)

    private fun isCommandLine(line: String): Boolean =
        line.equals(COMMAND_PREFIX, ignoreCase = true) ||
            (
                line.length > COMMAND_PREFIX.length &&
                    line.regionMatches(
                        thisOffset = 0,
                        other = COMMAND_PREFIX,
                        otherOffset = 0,
                        length = COMMAND_PREFIX.length,
                        ignoreCase = true
                    ) &&
                    line[COMMAND_PREFIX.length].isWhitespace()
                )
}

object MailCommandSignature {
    fun canonicalPayload(command: ParsedMailCommand): String =
        listOf(command.action.wireName, command.expiresRaw, command.nonce).joinToString("\n")

    fun hmacSha256Hex(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    fun sign(command: ParsedMailCommand, secret: String): String =
        "sha256=${hmacSha256Hex(secret, canonicalPayload(command))}"
}

object MailCommandSecurityPolicy {
    fun evaluate(
        rawSender: String,
        command: ParsedMailCommand?,
        trustedSenders: List<TrustedMailSender>,
        usedNonces: Set<String>,
        nowEpochMillis: Long,
        context: Context? = null
    ): MailCommandDecision {
        val normalizedSender = MailAddressNormalizer.normalize(rawSender)
            ?: return deny(context, MailCommandDecisionCode.InvalidSender, R.string.message_mail_sender_invalid)

        val trusted = trustedSenders.firstOrNull {
            MailAddressNormalizer.normalize(it.email) == normalizedSender
        } ?: return deny(context, MailCommandDecisionCode.SenderNotWhitelisted, R.string.message_mail_sender_not_whitelisted)

        if (!trusted.enabled) {
            return deny(context, MailCommandDecisionCode.SenderDisabled, R.string.message_mail_sender_disabled)
        }

        if (trusted.secret.isBlank() && command?.requiresAuthenticator != false) {
            return deny(context, MailCommandDecisionCode.InvalidAuthenticator, R.string.message_mail_sender_missing_secret)
        }

        val parsedCommand = command
            ?: return deny(context, MailCommandDecisionCode.NoCommand, R.string.message_mail_no_paysage_command)

        if (!parsedCommand.requiresAuthenticator) {
            return MailCommandDecision(
                allowed = true,
                code = MailCommandDecisionCode.Allowed,
                message = mailCommandText(context, R.string.message_mail_security_passed)
            )
        }

        if (parsedCommand.action !in trusted.allowedActions) {
            return deny(context, MailCommandDecisionCode.ActionNotAllowed, R.string.message_mail_action_not_allowed)
        }

        if (parsedCommand.key != null && parsedCommand.signature != null) {
            return deny(context, MailCommandDecisionCode.InvalidAuthenticator, R.string.message_mail_authenticator_exclusive)
        }

        if (parsedCommand.expiresAtEpochMillis <= nowEpochMillis) {
            return deny(context, MailCommandDecisionCode.Expired, R.string.message_mail_command_expired)
        }

        if (parsedCommand.expiresAtEpochMillis > nowEpochMillis + MAX_COMMAND_VALIDITY_MS) {
            return deny(context, MailCommandDecisionCode.ExpiresTooFar, R.string.message_mail_command_validity_too_long)
        }

        if (nonceKeys(normalizedSender, parsedCommand.nonce).any { it in usedNonces }) {
            return deny(context, MailCommandDecisionCode.NonceReused, R.string.message_mail_nonce_reused)
        }

        if (!hasValidAuthenticator(parsedCommand, trusted.secret)) {
            val code = if (parsedCommand.key == null && parsedCommand.signature == null) {
                MailCommandDecisionCode.MissingAuthenticator
            } else {
                MailCommandDecisionCode.InvalidAuthenticator
            }
            return deny(
                context,
                code,
                if (code == MailCommandDecisionCode.MissingAuthenticator) {
                    R.string.message_mail_missing_authenticator
                } else {
                    R.string.message_mail_authenticator_invalid
                }
            )
        }

        return MailCommandDecision(
            allowed = true,
            code = MailCommandDecisionCode.Allowed,
            message = mailCommandText(context, R.string.message_mail_security_passed)
        )
    }

    fun nonceKey(sender: String, nonce: String): String =
        "sha256:${sha256Hex("${MailAddressNormalizer.normalize(sender).orEmpty()}\n$nonce")}"

    fun nonceForStorage(nonce: String): String =
        "sha256:${sha256Hex(nonce)}"

    private fun nonceKeys(sender: String, nonce: String): Set<String> =
        setOf(
            nonceKey(sender, nonce),
            legacyNonceKey(sender, nonce)
        )

    private fun legacyNonceKey(sender: String, nonce: String): String =
        "${MailAddressNormalizer.normalize(sender).orEmpty()}::$nonce"

    private fun hasValidAuthenticator(command: ParsedMailCommand, secret: String): Boolean {
        return when {
            command.key != null -> constantTimeEquals(command.key, secret)
            command.signature != null -> constantTimeEquals(
                command.signature.lowercase(Locale.ROOT),
                MailCommandSignature.sign(command, secret)
            )
            else -> false
        }
    }

    private fun deny(context: Context?, code: MailCommandDecisionCode, messageRes: Int): MailCommandDecision =
        MailCommandDecision(allowed = false, code = code, message = mailCommandText(context, messageRes))

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val leftBytes = left.toByteArray(Charsets.UTF_8)
        val rightBytes = right.toByteArray(Charsets.UTF_8)
        var diff = leftBytes.size xor rightBytes.size
        val maxSize = maxOf(leftBytes.size, rightBytes.size)
        for (index in 0 until maxSize) {
            val l = leftBytes.getOrElse(index) { 0 }
            val r = rightBytes.getOrElse(index) { 0 }
            diff = diff or (l.toInt() xor r.toInt())
        }
        return diff == 0
    }

    private const val MAX_COMMAND_VALIDITY_MS = 30L * 60L * 1000L
}

class MailCommandParseException(
    val code: MailCommandDecisionCode,
    override val message: String
) : IllegalArgumentException(message)

private fun mailCommandText(context: Context?, resId: Int): String =
    context?.getString(resId) ?: when (resId) {
        // 无 Context 时(纯 JVM 测试/解析早期)回退中文文案,与 values-zh 保持一致
        R.string.message_mail_only_one_command -> "一封邮件只能包含一条 Paysage 指令。"
        R.string.message_mail_no_paysage_command -> "未找到 Paysage 指令。"
        R.string.message_mail_command_line_invalid -> "指令行格式无效。"
        R.string.message_mail_unknown_command -> "未知邮件指令。"
        R.string.message_mail_missing_expires -> "指令缺少 expires。"
        R.string.message_mail_missing_nonce -> "指令缺少 nonce。"
        R.string.message_mail_signature_invalid -> "指令签名格式无效。"
        R.string.message_mail_unsupported_field -> "指令包含不支持的字段。"
        R.string.message_mail_duplicate_field -> "指令字段重复。"
        R.string.message_mail_field_empty -> "指令字段不能为空。"
        R.string.message_mail_field_too_long -> "指令字段过长。"
        R.string.message_mail_field_control_chars -> "指令字段包含不可见控制字符。"
        R.string.message_mail_expires_invalid -> "expires 格式无效。"
        R.string.message_mail_sender_invalid -> "发件人地址无效。"
        R.string.message_mail_sender_not_whitelisted -> "发件人不在白名单中。"
        R.string.message_mail_sender_disabled -> "白名单邮箱已禁用。"
        R.string.message_mail_sender_missing_secret -> "授权邮箱缺少指令密钥,请刷新密钥。"
        R.string.message_mail_action_not_allowed -> "该邮箱没有执行此指令的权限。"
        R.string.message_mail_authenticator_exclusive -> "指令只能选择密钥或签名其中一种。"
        R.string.message_mail_command_expired -> "指令已过期。"
        R.string.message_mail_command_validity_too_long -> "指令有效期过长。"
        R.string.message_mail_nonce_reused -> "指令 nonce 已使用。"
        R.string.message_mail_missing_authenticator -> "指令缺少密钥或签名。"
        R.string.message_mail_authenticator_invalid -> "指令密钥或签名错误。"
        R.string.message_mail_security_passed -> "指令通过安全校验。"
        else -> ""
    }
