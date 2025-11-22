package takagi.ru.paysage.ui.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.ui.components.ExpressiveIconButton

/**
 * 阅读器顶部工具栏
 * 
 * M3E 风格的顶部工具栏，包含：
 * - 返回按钮
 * - 书籍标题
 * - 章节信息
 * - 更多菜单按钮
 * - 半透明渐变背景
 * 
 * 设计要点：
 * 1. 使用 ExpressiveIconButton 提供触觉反馈
 * 2. 半透明背景不遮挡阅读内容
 * 3. 渐变效果增强层次感
 * 4. 支持长标题的省略显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    bookTitle: String,
    chapterTitle: String,
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
) {
    // 创建渐变背景（从半透明到完全透明）
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            backgroundColor,
            backgroundColor.copy(alpha = 0.7f),
            Color.Transparent
        )
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = gradientBrush)
    ) {
        TopAppBar(
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 书籍标题
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 章节标题
                    if (chapterTitle.isNotEmpty()) {
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                ExpressiveIconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                ExpressiveIconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}

/**
 * 简化版阅读器顶部工具栏
 * 
 * 只显示书籍标题和返回按钮
 */
@Composable
fun SimpleReaderTopBar(
    bookTitle: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ReaderTopBar(
        bookTitle = bookTitle,
        chapterTitle = "",
        onNavigateBack = onNavigateBack,
        onMoreClick = {},
        modifier = modifier
    )
}
