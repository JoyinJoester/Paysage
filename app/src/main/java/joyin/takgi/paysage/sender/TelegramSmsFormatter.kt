package joyin.takgi.paysage.sender

internal object TelegramSmsFormatter {
    private const val PARSE_MODE_MARKDOWN_V2 = "MarkdownV2"

    data class FormattedMessage(
        val text: String,
        val parseMode: String
    )

    fun formatWithCodeSpoilers(rawMessage: String): FormattedMessage {
        val ranges = verificationCodeRanges(rawMessage)
        return FormattedMessage(
            text = rawMessage.escapeMarkdownV2WithSpoilers(ranges),
            parseMode = PARSE_MODE_MARKDOWN_V2
        )
    }

    private fun verificationCodeRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        keywordBeforeCode.findAll(text).forEach { match ->
            match.groups[2]?.range
                ?.takeIf { text.substring(it).isLikelyVerificationCode() }
                ?.let(ranges::add)
        }
        codeBeforeKeyword.findAll(text).forEach { match ->
            match.groups[2]?.range
                ?.takeIf { text.substring(it).isLikelyVerificationCode() }
                ?.let(ranges::add)
        }
        return ranges
            .sortedBy { it.first }
            .fold(mutableListOf()) { merged, range ->
                if (merged.none { existing -> range.first <= existing.last && range.last >= existing.first }) {
                    merged += range
                }
                merged
            }
    }

    private fun String.escapeMarkdownV2WithSpoilers(spoilerRanges: List<IntRange>): String {
        if (spoilerRanges.isEmpty()) return escapeMarkdownV2()
        val builder = StringBuilder(length + spoilerRanges.size * 4)
        var cursor = 0
        spoilerRanges.forEach { range ->
            if (range.first > cursor) {
                builder.append(substring(cursor, range.first).escapeMarkdownV2())
            }
            builder.append("||")
            builder.append(substring(range).escapeMarkdownV2())
            builder.append("||")
            cursor = range.last + 1
        }
        if (cursor < length) {
            builder.append(substring(cursor).escapeMarkdownV2())
        }
        return builder.toString()
    }

    private fun String.escapeMarkdownV2(): String {
        val builder = StringBuilder(length)
        forEach { char ->
            if (char in markdownV2ReservedChars) builder.append('\\')
            builder.append(char)
        }
        return builder.toString()
    }

    private fun String.isLikelyVerificationCode(): Boolean {
        val normalized = trim().replace("-", "")
        if (normalized.length !in 4..10) return false
        if (normalized.none(Char::isDigit)) return false
        if (normalized.all { it == normalized.first() }) return false
        return normalized.all { it.isLetterOrDigit() }
    }

    private val keywordPattern =
        "(验证码|校验码|动态码|短信码|登录码|确认码|一次性密码|verification\\s*code|security\\s*code|one[-\\s]?time\\s*password|passcode|otp|code)"

    private val keywordBeforeCode = Regex(
        pattern = "$keywordPattern[^\\p{L}\\p{N}]{0,16}([A-Z0-9][A-Z0-9-]{2,10}[A-Z0-9])",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val codeBeforeKeyword = Regex(
        pattern = "(^|[^\\p{L}\\p{N}])([A-Z0-9][A-Z0-9-]{2,10}[A-Z0-9])([^\\p{L}\\p{N}]{0,16}(是|为|is|as)?[^\\p{L}\\p{N}]{0,8})$keywordPattern(?=$|[^\\p{L}\\p{N}])",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val markdownV2ReservedChars = setOf(
        '_',
        '*',
        '[',
        ']',
        '(',
        ')',
        '~',
        '`',
        '>',
        '#',
        '+',
        '-',
        '=',
        '|',
        '{',
        '}',
        '.',
        '!',
        '\\'
    )
}
