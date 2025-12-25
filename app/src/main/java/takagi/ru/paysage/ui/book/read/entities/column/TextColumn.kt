package takagi.ru.paysage.ui.book.read.entities.column

/**
 * 文本列（参考 Legado）
 * 表示一行中的文本内容
 */
data class TextColumn(
    override val start: Float,
    override val end: Float,
    val charData: String  // 这一列的文本内容
) : BaseColumn()
