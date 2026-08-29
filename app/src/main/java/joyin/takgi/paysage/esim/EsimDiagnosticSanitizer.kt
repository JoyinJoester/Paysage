package joyin.takgi.paysage.esim

object EsimDiagnosticSanitizer {
    fun sanitize(raw: String, maxLength: Int? = null): String {
        val sanitized = raw
            .replace(Regex("(?i)LPA:1\\$[^\\s]+"), "LPA:1$[REDACTED]")
            .replace(Regex("(?i)(eid|iccid|imsi|activation.?code|matching.?id|confirmation.?code)\\s*[:=]\\s*[^\\s,;]+")) {
                "${it.groupValues[1]}=[REDACTED]"
            }
            .replace(Regex("(?<!\\d)\\d{18,32}(?!\\d)"), "[REDACTED_ID]")
            .replace(Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE), "[REDACTED_EMAIL]")
            .replace(Regex("(?<!\\d)\\+?\\d[\\d\\s-]{6,}\\d(?!\\d)"), "[REDACTED_NUMBER]")
        return maxLength?.let { sanitized.take(it) } ?: sanitized
    }
}
