package takagi.ru.paysage.ui.components.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.ui.book.read.entities.TextPage

/**
 * 文本页面内容渲染组件
 * 使用 TextPageCanvas 渲染（Legado 风格）
 */
@Composable
fun TextPageContent(
    page: TextPage,
    config: ReaderConfig,
    modifier: Modifier = Modifier
) {
    // 使用 Canvas 渲染
    TextPageCanvas(
        page = page,
        config = config,
        modifier = modifier
    )
}
