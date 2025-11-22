package takagi.ru.paysage.reader.text

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import takagi.ru.paysage.reader.ReaderConfig

/**
 * 文本阅读器视图 - Compose 组件
 * 使用 Canvas 渲染文本，然后转换为 Bitmap 显示
 */
@Composable
fun TextReaderView(
    content: TextContent,
    config: ReaderConfig,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onTap: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var pages by remember { mutableStateOf<List<TextPage>>(emptyList()) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // 创建排版引擎和渲染器
    val layoutEngine = remember(config) { TextLayoutEngine(config) }
    val renderer = remember(config) { TextPageRenderer(config) }
    
    // 当内容或配置变化时重新排版
    LaunchedEffect(content, config, viewSize) {
        if (viewSize.width > 0 && viewSize.height > 0) {
            withContext(Dispatchers.Default) {
                pages = layoutEngine.layoutText(
                    content,
                    viewSize.width,
                    viewSize.height
                )
            }
        }
    }
    
    // 当页面变化时渲染当前页
    LaunchedEffect(pages, currentPage) {
        if (pages.isNotEmpty() && currentPage in pages.indices) {
            withContext(Dispatchers.Default) {
                val page = pages[currentPage]
                val bitmap = Bitmap.createBitmap(
                    viewSize.width,
                    viewSize.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                
                // 填充背景
                canvas.drawColor(config.bgColor)
                
                // 渲染文本
                renderer.renderPage(canvas, page)
                
                currentPageBitmap = bitmap
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pages) {
                detectTapGestures(
                    onTap = { offset ->
                        val x = offset.x
                        val y = offset.y
                        
                        // 简单的翻页逻辑：左侧上一页，右侧下一页
                        if (x < size.width / 3) {
                            // 上一页
                            if (currentPage > 0) {
                                onPageChange(currentPage - 1)
                            }
                        } else if (x > size.width * 2 / 3) {
                            // 下一页
                            if (currentPage < pages.size - 1) {
                                onPageChange(currentPage + 1)
                            }
                        } else {
                            // 中间区域 - 触发菜单
                            onTap(x, y)
                        }
                    }
                )
            }
    ) {
        // 使用 onSizeChanged 获取视图大小
        androidx.compose.foundation.layout.BoxWithConstraints {
            LaunchedEffect(constraints) {
                viewSize = IntSize(
                    constraints.maxWidth,
                    constraints.maxHeight
                )
            }
            
            // 显示渲染的页面
            currentPageBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Text Page",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 文本阅读器视图的简化版本 - 用于快速集成
 */
@Composable
fun SimpleTextReaderView(
    text: String,
    chapterTitle: String = "",
    config: ReaderConfig,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }
    
    val content = remember(text) {
        TextContent(
            text = text,
            chapterIndex = 0,
            chapterTitle = chapterTitle,
            bookId = 0
        )
    }
    
    TextReaderView(
        content = content,
        config = config,
        currentPage = currentPage,
        onPageChange = { currentPage = it },
        modifier = modifier
    )
}
