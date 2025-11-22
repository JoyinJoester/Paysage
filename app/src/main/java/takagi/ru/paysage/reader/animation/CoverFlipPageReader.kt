package takagi.ru.paysage.reader.animation

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import takagi.ru.paysage.reader.touch.TouchZone
import takagi.ru.paysage.reader.touch.detectTouchZone
import kotlin.math.absoluteValue

/**
 * 覆盖翻页阅读器 - 基于 Legado 实现（懒加载版本）
 * 
 * 核心原理：
 * 1. 使用 HorizontalPager 实现横向滑动
 * 2. 通过 graphicsLayer 控制页面的 translationX，实现覆盖效果
 * 3. 支持懒加载，按需加载页面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverFlipPageReader(
    totalPages: Int,
    currentPage: Int,
    onLoadPage: (Int) -> Bitmap?,
    onTap: (TouchZone) -> Unit = {},
    onPageChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (totalPages == 0) return
    
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, totalPages - 1),
        pageCount = { totalPages }
    )
    
    // 监听页面变化
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != currentPage) {
                onPageChange(page)
            }
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondBoundsPageCount = 1 // 预加载前后各1页
    ) { page ->
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val bitmap = onLoadPage(page)
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 覆盖翻页效果的核心实现
                    // 当前页之后的页面（page > currentPage）需要跟随屏幕移动
                    if (page > pagerState.currentPage) {
                        // 计算需要平移的距离
                        // pageOffset 为负数时，表示页面在屏幕右侧
                        // 我们需要让它跟随屏幕，所以要抵消默认的滑动
                        translationX = -pageOffset.absoluteValue * size.width
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val zone = detectTouchZone(offset, size)
                        onTap(zone)
                    }
                }
        ) {
            if (bitmap != null) {
                AsyncImage(
                    model = bitmap,
                    contentDescription = "Page ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 显示加载中
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
