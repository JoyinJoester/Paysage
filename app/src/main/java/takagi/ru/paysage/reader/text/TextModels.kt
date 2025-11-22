package takagi.ru.paysage.reader.text

/**
 * 文本内容数据模型
 * 参考 Legado 设计，但使用独立实现
 */

/**
 * 文本内容 - 代表一个章节的完整文本
 */
data class TextContent(
    val text: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val bookId: Long
)

/**
 * 文本页面 - 代表排版后的一页内容
 */
data class TextPage(
    val pageIndex: Int,
    val lines: List<TextLine>,
    val chapterIndex: Int,
    val chapterTitle: String,
    val totalPages: Int = 0
) {
    val charCount: Int
        get() = lines.sumOf { it.text.length }
    
    val isEmpty: Boolean
        get() = lines.isEmpty()
}

/**
 * 文本行 - 代表页面中的一行文本
 */
data class TextLine(
    val text: String,
    val y: Float,              // 行顶部 Y 坐标
    val baseline: Float,       // 基线 Y 坐标
    val height: Float,         // 行高
    val lineIndex: Int = 0,    // 在页面中的行索引
    val isTitle: Boolean = false,  // 是否为标题
    val isParagraphStart: Boolean = false,  // 是否为段落开始
    val isParagraphEnd: Boolean = false     // 是否为段落结束
) {
    val bottom: Float
        get() = y + height
}

/**
 * 文本位置 - 用于定位文本中的特定位置
 */
data class TextPosition(
    val pageIndex: Int,
    val lineIndex: Int,
    val charIndex: Int
) {
    companion object {
        val ZERO = TextPosition(0, 0, 0)
    }
    
    fun isValid(): Boolean = pageIndex >= 0 && lineIndex >= 0 && charIndex >= 0
}

/**
 * 文本选择 - 代表选中的文本范围
 */
data class TextSelection(
    val start: TextPosition,
    val end: TextPosition,
    val selectedText: String = ""
) {
    val isEmpty: Boolean
        get() = start == end
    
    fun contains(position: TextPosition): Boolean {
        return when {
            position.pageIndex < start.pageIndex || position.pageIndex > end.pageIndex -> false
            position.pageIndex == start.pageIndex && position.pageIndex == end.pageIndex -> {
                position.lineIndex > start.lineIndex && position.lineIndex < end.lineIndex ||
                position.lineIndex == start.lineIndex && position.charIndex >= start.charIndex ||
                position.lineIndex == end.lineIndex && position.charIndex <= end.charIndex
            }
            position.pageIndex == start.pageIndex -> {
                position.lineIndex > start.lineIndex ||
                position.lineIndex == start.lineIndex && position.charIndex >= start.charIndex
            }
            position.pageIndex == end.pageIndex -> {
                position.lineIndex < end.lineIndex ||
                position.lineIndex == end.lineIndex && position.charIndex <= end.charIndex
            }
            else -> true
        }
    }
}

/**
 * 搜索结果 - 代表搜索到的文本位置
 */
data class SearchResult(
    val position: TextPosition,
    val matchText: String,
    val context: String,  // 上下文文本
    val pageIndex: Int
)

/**
 * 文本章节信息
 */
data class TextChapter(
    val index: Int,
    val title: String,
    val content: String,
    val pageCount: Int = 0
)
