package takagi.ru.paysage.ui.book.read.provider

import android.util.Log

private const val TAG = "ContentParser"

/**
 * 内容块基类
 */
sealed class ContentBlock

/**
 * 文本块
 */
data class TextBlock(val text: String) : ContentBlock()

/**
 * 图片块
 */
data class ImageBlock(val src: String) : ContentBlock()

/**
 * HTML 内容解析器
 * 将 HTML 拆分为文本块和图片块
 */
object ContentParser {
    
    /**
     * 解析 HTML 内容为块列表
     */
    fun parseHtmlContent(html: String): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        
        // 图片标签正则
        val imgPattern = Regex("""<img[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        
        var lastIndex = 0
        for (match in imgPattern.findAll(html)) {
            // 添加图片前的文本
            if (match.range.first > lastIndex) {
                val textBefore = html.substring(lastIndex, match.range.first)
                val cleanedText = cleanHtml(textBefore)
                if (cleanedText.isNotBlank()) {
                    blocks.add(TextBlock(cleanedText))
                }
            }
            
            // 添加图片
            val src = match.groupValues[1]
            blocks.add(ImageBlock(src))
            
            lastIndex = match.range.last + 1
        }
        
        // 添加剩余文本
        if (lastIndex < html.length) {
            val remaining = html.substring(lastIndex)
            val cleanedText = cleanHtml(remaining)
            if (cleanedText.isNotBlank()) {
                blocks.add(TextBlock(cleanedText))
            }
        }
        
        // 如果没有找到任何图片，整个 HTML 作为文本
        if (blocks.isEmpty()) {
            val cleanedText = cleanHtml(html)
            if (cleanedText.isNotBlank()) {
                blocks.add(TextBlock(cleanedText))
            }
        }
        
        Log.d(TAG, "解析完成: ${blocks.size} 个块 (${blocks.count { it is TextBlock }} 文本, ${blocks.count { it is ImageBlock }} 图片)")
        return blocks
    }
    
    /**
     * 清理 HTML 标签，提取纯文本
     */
    private fun cleanHtml(html: String): String {
        var text = html
        
        // 移除不可见标签
        text = text.replace(Regex("<head[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        
        // 将 HTML 中的换行符和制表符转换为空格
        text = text.replace(Regex("[\\r\\n\\t]+"), " ")
        
        // 处理块级元素和换行
        text = text.replace(Regex("</(?:p|div|h\\d)[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "\n• ")
        
        // 移除所有剩余 HTML 标签
        text = text.replace(Regex("<[^>]+>"), "")
        
        // 解码 HTML 实体
        text = text.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&ldquo;", """)
            .replace("&rdquo;", """)
            .replace("&lsquo;", "'")
            .replace("&rsquo;", "'")
            .replace("&mdash;", "—")
            .replace("&middot;", "·")
        
        // 移除多余空白行
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        
        // 每段添加缩进
        val lines = text.split("\n")
        val sb = StringBuilder()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                // 如果不是特殊符号开头，添加缩进
                if (!trimmed.startsWith("•")) {
                    sb.append("\u3000\u3000")
                }
                sb.append(trimmed).append("\n")
            }
        }
        
        return sb.toString().trim()
    }
}
