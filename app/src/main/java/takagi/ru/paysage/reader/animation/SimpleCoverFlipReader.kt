package takagi.ru.paysage.reader.animation

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import takagi.ru.paysage.reader.touch.TouchZone
import takagi.ru.paysage.reader.touch.detectTouchZone
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 优化版覆盖翻页阅读器
 *
 * 交互逻辑：
 * 1. 翻下一页 (Next): 当前页 (Current) 向左滑动移出，露出下一页 (Next)。
 *    - 视觉层级: Current (Top) > Next (Bottom)
 * 2. 翻上一页 (Prev): 上一页 (Prev) 从左侧滑入覆盖，盖住当前页 (Current)。
 *    - 视觉层级: Prev (Top) > Current (Bottom)
 *
 * 这种逻辑模拟了纸质书页的层叠关系：Page 0 在 Page 1 上面，Page 1 在 Page 2 上面...
 */
@Composable
fun SimpleCoverFlipReader(
    totalPages: Int,
    currentPage: Int,
    currentBitmap: Bitmap?, // 新增参数：直接传入当前页的 Bitmap
    onLoadPage: (Int) -> Bitmap?,
    onPreload: (Int) -> Unit = {}, // 新增参数：预加载回调
    onTap: (TouchZone) -> Unit = {},
    onPageChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    
    // 当前显示的页面索引
    var activePage by remember { mutableIntStateOf(currentPage.coerceIn(0, maxOf(0, totalPages - 1))) }

    // 当外部 currentPage 变化时，同步更新 activePage
    // 但仅当不在动画中且确实有变化时才更新，避免覆盖本地的翻页状态
    LaunchedEffect(currentPage) {
        if (activePage != currentPage && !isAnimating && !isDragging) {
            activePage = currentPage
        }
    }

    var screenWidth by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val animatable = remember { Animatable(0f) }

    // 加载页面位图
    // 优先使用传入的 currentBitmap (当 activePage == currentPage 时)
    // 这样可以避免 onLoadPage 缓存未命中导致的闪烁或空白
    val pageBitmap = if (activePage == currentPage) currentBitmap else onLoadPage(activePage)
    
    // 预加载下一页和上一页
    // 注意：这里我们不依赖 currentBitmap，因为 currentBitmap 总是指向 currentPage
    // 当 activePage 变化时（例如动画完成后），我们需要确保 nextBitmap 和 prevBitmap 能正确获取
    val nextBitmap = onLoadPage(activePage + 1)
    val prevBitmap = onLoadPage(activePage - 1)

    // 触发预加载
    LaunchedEffect(activePage) {
        onPreload(activePage + 1)
        onPreload(activePage - 1)
    }

    // ...


    // 翻页动画辅助函数
    fun turnPage(forward: Boolean) {
        if (isAnimating || isDragging) return
        
        // 立即触发预加载
        if (forward) onPreload(activePage + 1) else onPreload(activePage - 1)

        isAnimating = true
        coroutineScope.launch {
            if (forward) {
                if (activePage < totalPages - 1) {
                    // 翻下一页：当前页向左移出
                    animatable.animateTo(
                        targetValue = -screenWidth.toFloat(),
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                    // 动画结束后，立即更新 activePage 并重置偏移量
                    activePage++
                    onPageChange(activePage)
                    animatable.snapTo(0f)
                }
            } else {
                if (activePage > 0) {
                    // 翻上一页：上一页从左侧移入
                    animatable.animateTo(
                        targetValue = screenWidth.toFloat(),
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                    // 动画结束后，立即更新 activePage 并重置偏移量
                    activePage--
                    onPageChange(activePage)
                    animatable.snapTo(0f)
                }
            }
            isAnimating = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val zone = detectTouchZone(offset, IntSize(screenWidth, size.height))
                    when (zone) {
                        TouchZone.MIDDLE_LEFT -> turnPage(forward = false)
                        TouchZone.MIDDLE_RIGHT -> turnPage(forward = true)
                        else -> onTap(zone)
                    }
                }
            }
            .pointerInput(Unit) {
                screenWidth = size.width
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        // 拖拽开始时，预加载前后页
                        onPreload(activePage + 1)
                        onPreload(activePage - 1)
                        coroutineScope.launch {
                            animatable.stop()
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val currentOffset = animatable.value
                        val newOffset = currentOffset + dragAmount

                        // 限制滑动范围
                        val targetOffset = when {
                            // 向左滑 (翻下一页)
                            newOffset < 0 -> {
                                if (activePage < totalPages - 1) {
                                    newOffset.coerceAtLeast(-screenWidth.toFloat())
                                } else {
                                    newOffset * 0.3f
                                }
                            }
                            // 向右滑 (翻上一页)
                            newOffset > 0 -> {
                                if (activePage > 0) {
                                    newOffset.coerceAtMost(screenWidth.toFloat())
                                } else {
                                    newOffset * 0.3f
                                }
                            }
                            else -> 0f
                        }

                        coroutineScope.launch {
                            animatable.snapTo(targetOffset)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        val currentOffset = animatable.value
                        val threshold = screenWidth * 0.3f

                        coroutineScope.launch {
                            if (currentOffset < 0) {
                                // 翻下一页逻辑
                                if (activePage < totalPages - 1 && (currentOffset < -threshold)) {
                                    animatable.animateTo(
                                        targetValue = -screenWidth.toFloat(),
                                        animationSpec = tween(durationMillis = 250)
                                    )
                                    activePage++
                                    onPageChange(activePage)
                                    animatable.snapTo(0f)
                                } else {
                                    animatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            } else if (currentOffset > 0) {
                                // 翻上一页逻辑
                                if (activePage > 0 && (currentOffset > threshold)) {
                                    animatable.animateTo(
                                        targetValue = screenWidth.toFloat(),
                                        animationSpec = tween(durationMillis = 250)
                                    )
                                    activePage--
                                    onPageChange(activePage)
                                    animatable.snapTo(0f)
                                } else {
                                    animatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    }
                )
            }
    ) {
        val offset = animatable.value

        // 渲染逻辑
        if (offset <= 0) {
            // Case 1: 静止 或 向左滑 (翻下一页)
            // 绘制顺序: 下一页 (Bottom) -> 当前页 (Top, Moving)

            // 1. 绘制下一页 (Bottom Layer)
            // 始终绘制下一页，即使没有滑动，这样当当前页滑走时，下一页已经准备好了
            if (activePage < totalPages - 1) {
                Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
                    if (nextBitmap != null) {
                        PageContent(bitmap = nextBitmap)
                    } else if (activePage + 1 == currentPage && currentBitmap != null) {
                        PageContent(bitmap = currentBitmap)
                    }
                    // 变暗效果已移除
                }
            }

            // 2. 绘制当前页 (Top Layer)
            // 当前页随着 offset 移动
            if (activePage < totalPages) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(offset.roundToInt(), 0) }
                        .zIndex(1f)
                ) {
                    if (pageBitmap != null) {
                        PageContent(bitmap = pageBitmap)
                    }
                }

                // 绘制右侧边缘阴影
                if (activePage < totalPages - 1) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight()
                            .offset { IntOffset(offset.roundToInt() + screenWidth, 0) }
                            .zIndex(1.5f)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

        } else {
            // Case 2: 向右滑 (翻上一页)
            // 绘制顺序: 当前页 (Bottom) -> 上一页 (Top, Moving)

            // 1. 绘制当前页 (Bottom Layer)
            if (activePage < totalPages) {
                Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
                    if (pageBitmap != null) {
                        PageContent(bitmap = pageBitmap)
                    }
                    // 变暗效果已移除
                }
            }

            // 2. 绘制上一页 (Top Layer)
            // 上一页从左侧滑入
            if (activePage > 0) {
                val prevPageOffset = -screenWidth + offset

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(prevPageOffset.roundToInt(), 0) }
                        .zIndex(1f)
                ) {
                    if (prevBitmap != null) {
                        PageContent(bitmap = prevBitmap)
                    } else if (activePage - 1 == currentPage && currentBitmap != null) {
                        PageContent(bitmap = currentBitmap)
                    }
                }

                // 绘制上一页的右侧阴影
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .offset { IntOffset(prevPageOffset.roundToInt() + screenWidth, 0) }
                        .zIndex(1.5f)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}
