package takagi.ru.paysage.ui.book.read.entities

/**
 * 文本页面（参考 Legado TextPage）
 * 使用 TextLine 列表存储页面内容
 */
data class TextPage(
    val index: Int = 0,
    val chapterIndex: Int = 0,
    val chapterTitle: String = "",
    val lines: MutableList<TextLine> = mutableListOf(),
    var height: Float = 0f,
    var charOffset: Int = 0  // 本页起始字符偏移
) {
    fun addLine(line: TextLine) {
        lines.add(line)
    }
    
    val isEmpty: Boolean
        get() = lines.isEmpty()
        
    companion object {
        val EMPTY = TextPage(
            index = 0,
            chapterIndex = 0,
            chapterTitle = "",
            lines = mutableListOf()
        )
    }
}

/**
 * 章节页面集合
 */
data class TextChapterPages(
    val chapterIndex: Int,
    val chapterTitle: String,
    val pages: List<TextPage>,
    val totalChars: Int = 0
) {
    val pageCount: Int get() = pages.size
    
    fun getPage(index: Int): TextPage? = pages.getOrNull(index)
    
    companion object {
        val EMPTY = TextChapterPages(
            chapterIndex = 0,
            chapterTitle = "",
            pages = emptyList(),
            totalChars = 0
        )
    }
}

/**
 * 全局页面位置
 */
data class GlobalPagePosition(
    val chapterIndex: Int,
    val pageIndexInChapter: Int
)
