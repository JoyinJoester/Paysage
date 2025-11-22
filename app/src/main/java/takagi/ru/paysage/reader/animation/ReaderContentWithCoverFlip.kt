package takagi.ru.paysage.reader.animation

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import takagi.ru.paysage.reader.touch.TouchZone
import takagi.ru.paysage.reader.touch.detectTouchZone

/**
 * 覆盖翻页阅读器内容组件
 * 
 * 使用 LazyRow 实现覆盖翻页动画效果。
 * 
 * @param pages 所有页面的位图列表
 * @param initialPage 初始页面索引
 * @param config 覆盖翻页配置
 * @param onTap 点击回调
 * @param onPageChange 页面切换回调
 * @param modifier Modifier
 */
@Composable
fun ReaderContentWithCoverFlip(
    pages: List<Bitmap>,
    initialPage: Int = 0,
    config: CoverFlipConfig = CoverFlipConfig(),
    onTap: (zone: TouchZone) -> Unit,
    onPageChange: (page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. 创建 PagerState
    val pagerState = remember(pages.size) {
        CoverFlipPagerState(
            initialPage = initialPage,
            pageCount = pages.size
        )
    }
    
    // 2. 创建变换器
    val transformer = remember { CoverFlipTransformer() }
    
    // 3. 创建滚动监听器
    val scrollListener = remember {
        PageScrollListener { position, offset, offsetPixels ->
            // 可以在这里处理滚动回调
            // 例如:更新进度条、触发其他 UI 更新等
        }
    }
    
    // 4. 监听滚动状态
    LaunchedEffect(pagerState.lazyListState) {
        snapshotFlow {
            pagerState.lazyListState.layoutInfo
        }.collect { layoutInfo ->
            pagerState.updateState(layoutInfo)
            scrollListener.onScroll(pagerState)
        }
    }
    
    // 5. 监听页面变化
    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }
    
    // 6. 多点触控状态
    var isMultiTouch by remember { mutableStateOf(false) }
    
    // 7. 获取屏幕宽度
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageWidth = constraints.maxWidth
        val pageHeight = constraints.maxHeight
        
        // 8. LazyRow 实现分页滚动
        LazyRow(
            state = pagerState.lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // 监听多点触控
                    awaitEachGesture {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.size
                            
                            // 检测多点触控
                            if (pointerCount > 1 && !isMultiTouch) {
                                isMultiTouch = true
                            } else if (pointerCount == 1 && isMultiTouch) {
                                isMultiTouch = false
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            flingBehavior = SnapFlingBehavior(
                pagerState = pagerState,
                snapThreshold = config.swipeThreshold,
                velocityThreshold = config.velocityThreshold
            ),
            userScrollEnabled = !isMultiTouch  // 多点触控时禁用滚动
        ) {
            itemsIndexed(
                items = pages,
                key = { index, _ -> index }
            ) { index, bitmap ->
                // 8. 计算页面位置
                val position = pagerState.getPagePosition(index)
                
                // 9. 应用变换
                val transform = transformer.transformPage(
                    page = index,
                    position = position,
                    pageWidth = pageWidth
                )
                
                // 10. 计算阴影
                val shadowAlpha = if (config.shadowEnabled) {
                    transformer.calculateShadowAlpha(
                        position = position,
                        maxAlpha = config.shadowMaxAlpha
                    )
                } else 0f
                
                // 11. 渲染页面
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(transform.zIndex)  // 设置 zIndex 确保正确的绘制顺序
                        .graphicsLayer {
                            // 关键:translationX 实现覆盖效果
                            translationX = transform.translationX
                            alpha = transform.alpha
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val zone = detectTouchZone(
                                    offset,
                                    IntSize(pageWidth, pageHeight)
                                )
                                onTap(zone)
                            }
                        }
                ) {
                    // 绘制页面内容
                    PageContent(
                        bitmap = bitmap,
                        shadowAlpha = shadowAlpha
                    )
                }
            }
        }
    }
}

/**
 * 覆盖翻页配置
 * 
 * @param animationDuration 动画时长(毫秒)
 * @param swipeThreshold 吸附阈值(0f 到 1f)
 * @param velocityThreshold 速度阈值(dp/s)
 * @param shadowEnabled 是否启用阴影
 * @param shadowMaxAlpha 阴影最大透明度
 * @param shadowBlurRadius 阴影模糊半径(dp)
 * @param bounceEnabled 是否启用边界回弹
 * @param bounceMaxDisplacement 回弹最大位移(dp)
 * @param bounceDuration 回弹动画时长(毫秒)
 */
data class CoverFlipConfig(
    val animationDuration: Int = 300,
    val swipeThreshold: Float = 0.3f,
    val velocityThreshold: Float = 1000f,
    val shadowEnabled: Boolean = true,
    val shadowMaxAlpha: Float = 0.4f,
    val shadowBlurRadius: Float = 8f,
    val bounceEnabled: Boolean = true,
    val bounceMaxDisplacement: Float = 100f,
    val bounceDuration: Int = 200
)
