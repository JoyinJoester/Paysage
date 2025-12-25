package takagi.ru.paysage.ui.book.read.entities.column

/**
 * 图片列（参考 Legado）
 * 表示一行中的图片内容
 */
data class ImageColumn(
    override val start: Float,
    override val end: Float,
    val src: String  // 图片源（base64 或 URL）
) : BaseColumn()
