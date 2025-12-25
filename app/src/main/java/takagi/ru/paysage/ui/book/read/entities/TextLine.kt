package takagi.ru.paysage.ui.book.read.entities

import takagi.ru.paysage.ui.book.read.entities.column.BaseColumn

/**
 * 文本行（参考 Legado TextLine）
 * 表示页面中的一行，可以包含文本或图片
 */
data class TextLine(
    var text: String = "",
    var lineTop: Float = 0f,      // 行顶部 Y 坐标
    var lineBottom: Float = 0f,   // 行底部 Y 坐标  
    var lineBase: Float = 0f,     // 文本基线 Y 坐标
    var lineStart: Float = 0f,    // 行起始 X 坐标
    var lineEnd: Float = 0f,      // 行结束 X 坐标
    val columns: MutableList<BaseColumn> = mutableListOf(),
    var isTitle: Boolean = false,
    var isImage: Boolean = false,
    var isParagraphEnd: Boolean = false,
    var charOffset: Int = 0       // 本行在章节中的字符偏移
) {
    fun addColumn(column: BaseColumn) {
        columns.add(column)
    }
    
    val height: Float
        get() = lineBottom - lineTop
}
