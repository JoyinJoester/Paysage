package takagi.ru.paysage.ui.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.ui.components.ExpressiveIconButton

/**
 * 阅读器底部工具栏
 * 
 * M3E 风格的底部控制栏，包含：
 * - 上一页按钮
 * - 进度滑块
 * - 页码显示
 * - 下一页按钮
 * - 半透明渐变背景
 * 
 * 设计要点：
 * 1. 使用 ExpressiveIconButton 提供触觉反馈
 * 2. 进度滑块支持快速跳转
 * 3. 实时显示当前页码和总页数
 * 4. 半透明背景不遮挡内容
 */
@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    enabled: Boolean = true
) {
    // 创建渐变背景（从完全透明到半透明）
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            backgroundColor.copy(alpha = 0.7f),
            backgroundColor
        )
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = gradientBrush)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 进度滑块
            Slider(
                value = currentPage.toFloat(),
                onValueChange = { value ->
                    onPageChange(value.toInt())
                },
                valueRange = 1f..totalPages.toFloat(),
                enabled = enabled && totalPages > 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 控制按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上一页按钮
                ExpressiveIconButton(
                    onClick = onPreviousPage,
                    enabled = enabled && currentPage > 1,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "上一页",
                        tint = if (enabled && currentPage > 1) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
                
                // 页码显示
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 下一页按钮
                ExpressiveIconButton(
                    onClick = onNextPage,
                    enabled = enabled && currentPage < totalPages,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "下一页",
                        tint = if (enabled && currentPage < totalPages) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 简化版阅读器底部工具栏
 * 
 * 只显示页码，不显示控制按钮
 */
@Composable
fun SimpleReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$currentPage / $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
