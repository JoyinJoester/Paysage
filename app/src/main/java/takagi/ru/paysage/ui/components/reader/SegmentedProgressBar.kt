package takagi.ru.paysage.ui.components.reader

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 章节分段信息
 * @param startPage 段起始页（全书页码）
 * @param endPage 段结束页（全书页码，不含）
 * @param chapterIndices 包含的章节索引列表
 */
data class ChapterSegment(
    val startPage: Int,
    val endPage: Int,
    val chapterIndices: List<Int>
)

/**
 * M3E 分段式进度条
 * 
 * 特点：
 * - 显示整本书进度
 * - 章节之间有视觉间隔
 * - 连续的短章节（<5页）自动合并
 * - 点击/拖动可跳转
 * - 平滑动画效果
 */
@Composable
fun SegmentedChapterProgressBar(
    currentPage: Int,
    totalPages: Int,
    chapterPageCounts: List<Int>,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 6.dp,
    gapWidth: Dp = 3.dp,
    minPagesForSeparateSegment: Int = 5
) {
    // 计算分段信息
    val segments = remember(chapterPageCounts, minPagesForSeparateSegment) {
        calculateChapterSegments(chapterPageCounts, minPagesForSeparateSegment)
    }
    
    // 动画进度
    val animatedProgress by animateFloatAsState(
        targetValue = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progressAnimation"
    )
    
    val density = LocalDensity.current
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val gapWidthPx = with(density) { gapWidth.toPx() }
    val cornerRadius = with(density) { (trackHeight / 2).toPx() }
    
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val thumbColor = MaterialTheme.colorScheme.primary
    
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 预计算段的视觉位置信息
        val segmentPositions = remember(segments, totalPages) {
            if (segments.isEmpty() || totalPages <= 0) emptyList()
            else {
                val result = mutableListOf<Triple<Float, Float, ChapterSegment>>() // startRatio, endRatio, segment
                var cumulativePages = 0
                segments.forEach { segment ->
                    val segmentPages = segment.endPage - segment.startPage
                    val startRatio = cumulativePages.toFloat() / totalPages
                    cumulativePages += segmentPages
                    val endRatio = cumulativePages.toFloat() / totalPages
                    result.add(Triple(startRatio, endRatio, segment))
                }
                result
            }
        }
        
        // 将屏幕位置转换为页码
        fun xPositionToPage(xRatio: Float, width: Float, gapPx: Float): Int {
            if (segments.isEmpty() || totalPages <= 0) return 0
            
            val totalGap = (segments.size - 1) * gapPx
            val availableWidth = width - totalGap
            if (availableWidth <= 0) return 0
            
            var accumulatedX = 0f
            for ((index, segment) in segments.withIndex()) {
                val segmentPages = segment.endPage - segment.startPage
                val segmentWidth = (segmentPages.toFloat() / totalPages) * availableWidth
                val segmentEndX = accumulatedX + segmentWidth
                val xPosition = xRatio * width
                
                if (xPosition <= segmentEndX || index == segments.size - 1) {
                    // 点击在这个段内
                    val relativeX = (xPosition - accumulatedX).coerceIn(0f, segmentWidth)
                    val progressInSegment = if (segmentWidth > 0) relativeX / segmentWidth else 0f
                    val pageInSegment = (progressInSegment * segmentPages).toInt()
                    return (segment.startPage + pageInSegment).coerceIn(0, totalPages - 1)
                }
                
                accumulatedX = segmentEndX + gapPx
            }
            return 0
        }
        
        // 将页码转换为屏幕X位置比例
        fun pageToXRatio(page: Int, width: Float, gapPx: Float): Float {
            if (segments.isEmpty() || totalPages <= 0 || width <= 0) return 0f
            
            val totalGap = (segments.size - 1) * gapPx
            val availableWidth = width - totalGap
            if (availableWidth <= 0) return 0f
            
            var accumulatedX = 0f
            for ((index, segment) in segments.withIndex()) {
                val segmentPages = segment.endPage - segment.startPage
                val segmentWidth = (segmentPages.toFloat() / totalPages) * availableWidth
                
                if (page >= segment.startPage && page < segment.endPage) {
                    // 页面在这个段内
                    val pageInSegment = page - segment.startPage
                    val progressInSegment = if (segmentPages > 0) pageInSegment.toFloat() / segmentPages else 0f
                    return (accumulatedX + progressInSegment * segmentWidth) / width
                }
                
                accumulatedX += segmentWidth + gapPx
            }
            return 1f
        }
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .pointerInput(segments, totalPages) {
                    detectTapGestures { offset ->
                        if (totalPages > 0) {
                            val targetPage = xPositionToPage(offset.x / size.width, size.width.toFloat(), gapWidthPx)
                            onPageSelected(targetPage)
                        }
                    }
                }
                .pointerInput(segments, totalPages) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragPosition = offset.x / size.width
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragPosition = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (totalPages > 0) {
                                val targetPage = xPositionToPage(dragPosition, size.width.toFloat(), gapWidthPx)
                                onPageSelected(targetPage)
                            }
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val totalGapWidth = (segments.size - 1) * gapWidthPx
            val availableWidth = size.width - totalGapWidth
            
            if (availableWidth <= 0 || totalPages <= 0) return@Canvas
            
            var xOffset = 0f
            
            // 计算当前页或拖动位置对应的页码
            val displayPage = if (isDragging) {
                xPositionToPage(dragPosition, size.width, gapWidthPx)
            } else {
                currentPage
            }
            
            // 计算当前页对应的视觉位置
            val currentXRatio = if (isDragging) {
                dragPosition
            } else {
                pageToXRatio(currentPage, size.width, gapWidthPx)
            }
            val thumbX = (currentXRatio * size.width).coerceIn(trackHeightPx, size.width - trackHeightPx)
            
            segments.forEachIndexed { index, segment ->
                val segmentPages = segment.endPage - segment.startPage
                val segmentWidth = (segmentPages.toFloat() / totalPages) * availableWidth
                
                if (segmentWidth > 0) {
                    val segmentStartX = xOffset
                    
                    // 绘制非激活轨道
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(segmentStartX, 0f),
                        size = Size(segmentWidth, trackHeightPx),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    // 计算此段内的激活进度（使用 displayPage 支持拖动预览）
                    if (displayPage >= segment.startPage) {
                        val pagesFilledInSegment = if (displayPage >= segment.endPage) {
                            segmentPages // 整段填满
                        } else {
                            displayPage - segment.startPage + 1 // 部分填充
                        }
                        val fillRatio = pagesFilledInSegment.toFloat() / segmentPages
                        val activeWidth = fillRatio * segmentWidth
                        
                        if (activeWidth > 0) {
                            drawRoundRect(
                                color = activeColor,
                                topLeft = Offset(segmentStartX, 0f),
                                size = Size(activeWidth.coerceAtMost(segmentWidth), trackHeightPx),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    }
                }
                
                xOffset += segmentWidth + gapWidthPx
            }
            
            // 绘制拖动指示器（圆点）
            val thumbRadius = trackHeightPx * 1.2f
            
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(thumbX, trackHeightPx / 2)
            )
            
            // 拖动时显示更大的指示器
            if (isDragging) {
                drawCircle(
                    color = thumbColor.copy(alpha = 0.3f),
                    radius = thumbRadius * 1.8f,
                    center = Offset(thumbX, trackHeightPx / 2)
                )
            }
        }
    }
}

/**
 * 计算章节分段
 * 连续的短章节（页数 < minPages）合并为一个段
 */
private fun calculateChapterSegments(
    chapterPageCounts: List<Int>,
    minPages: Int
): List<ChapterSegment> {
    if (chapterPageCounts.isEmpty()) return emptyList()
    
    val segments = mutableListOf<ChapterSegment>()
    var currentGroupChapters = mutableListOf<Int>()
    var currentGroupStartPage = 0
    var currentPage = 0
    
    chapterPageCounts.forEachIndexed { index, pageCount ->
        val isShort = pageCount < minPages
        
        if (isShort) {
            // 短章节，加入当前分组
            if (currentGroupChapters.isEmpty()) {
                currentGroupStartPage = currentPage
            }
            currentGroupChapters.add(index)
        } else {
            // 长章节，先结束之前的分组（如果有）
            if (currentGroupChapters.isNotEmpty()) {
                segments.add(ChapterSegment(
                    startPage = currentGroupStartPage,
                    endPage = currentPage,
                    chapterIndices = currentGroupChapters.toList()
                ))
                currentGroupChapters.clear()
            }
            
            // 单独作为一个段
            segments.add(ChapterSegment(
                startPage = currentPage,
                endPage = currentPage + pageCount,
                chapterIndices = listOf(index)
            ))
        }
        
        currentPage += pageCount
    }
    
    // 处理末尾的短章节分组
    if (currentGroupChapters.isNotEmpty()) {
        segments.add(ChapterSegment(
            startPage = currentGroupStartPage,
            endPage = currentPage,
            chapterIndices = currentGroupChapters.toList()
        ))
    }
    
    return segments
}

/**
 * 带进度文字的分段进度条
 */
@Composable
fun SegmentedProgressBarWithLabel(
    currentPage: Int,
    totalPages: Int,
    chapterPageCounts: List<Int>,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 当前页码
        Text(
            text = "${currentPage + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 分段进度条
        SegmentedChapterProgressBar(
            currentPage = currentPage,
            totalPages = totalPages,
            chapterPageCounts = chapterPageCounts,
            onPageSelected = onPageSelected,
            modifier = Modifier.weight(1f)
        )
        
        // 总页数
        Text(
            text = "$totalPages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
