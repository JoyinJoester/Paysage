package joyin.takgi.paysage.reliability

import joyin.takgi.paysage.mail.MailBodyTextExtractor

/**
 * MMS 文本提取,纯逻辑便于单元测试。
 * 只转发电信文本:优先 text/plain,没有才用其他文本类型;纯媒体彩信
 * (图片/音视频)无文本可转发,由调用方跳过。
 */
data class MmsPart(
    val contentType: String?,
    val text: String?
)

object MmsTextExtractor {

    const val CONTENT_TYPE_PLAIN = "text/plain"

    fun hasText(parts: List<MmsPart>): Boolean = parts.any {
        it.contentType?.startsWith("text/") == true && !it.text.isNullOrBlank()
    }

    fun extractText(parts: List<MmsPart>): String {
        val textParts = parts.filter {
            it.contentType?.startsWith("text/") == true && !it.text.isNullOrBlank()
        }
        if (textParts.isEmpty()) return ""
        val preferred = textParts.filter { it.contentType.equals(CONTENT_TYPE_PLAIN, ignoreCase = true) }
            .ifEmpty { textParts }
        val combined = preferred.joinToString(separator = "\n") { it.text!!.trim() }
        val cleaned = if (combined.contains('<') && combined.contains('>')) {
            MailBodyTextExtractor.htmlToText(combined)
        } else {
            combined
        }
        return cleaned.trim()
    }
}
