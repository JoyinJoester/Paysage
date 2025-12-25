package takagi.ru.paysage.ui.book.read.entities.column

/**
 * 基础列类（参考 Legado）
 * 表示一行中的一个元素（文本或图片）
 */
sealed class BaseColumn {
    abstract val start: Float  // X 轴起始位置
    abstract val end: Float    // X 轴结束位置
}
