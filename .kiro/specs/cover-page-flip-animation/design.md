# 覆盖翻页动画设计文档

## 概述

本设计文档描述了如何在 Paysage 阅读器中实现覆盖翻页动画效果。该动画模式下,上层页面完全覆盖下层页面,翻页时上层页面整体移开,下层页面逐渐显露。

根据开发指南,我们将基于 Compose 的 LazyRow 实现完整的覆盖翻页方案。LazyRow 在 Compose 中相当于 RecyclerView,我们将实现:

1. 跨页吸附(Snap to Page)
2. 覆盖效果(通过 graphicsLayer 控制子 View 层级)
3. 页面滑动回调(类似 ViewPager 的 onPageScrolled)
4. 子 View 绘制顺序控制

## 架构设计

### 核心组件

```
ReaderContent (Compose LazyRow)
├── CoverFlipPagerState (分页状态管理)
├── SnapFlingBehavior (吸附行为)
│   ├── SnapHelper (吸附辅助类)
│   └── SmoothScroller (平滑滚动器)
├── PageScrollListener (页面滚动监听)
├── CoverFlipTransformer (覆盖效果变换器)
└── ChildDrawingOrderController (子View绘制顺序控制)
```

### 技术选型

- **UI 框架**: Jetpack Compose LazyRow
- **吸附机制**: Custom FlingBehavior + Animatable
- **页面变换**: GraphicsLayer + translationX
- **绘制顺序**: zIndex modifier
- **状态管理**: Compose State + ViewModel

### 与 RecyclerView 方案的对应关系

| RecyclerView 概念 | Compose 对应实现 |
|------------------|-----------------|
| RecyclerView | LazyRow |
| LayoutManager | LazyListState |
| OnScrollListener | LazyListState.firstVisibleItemScrollOffset |
| OnFlingListener | FlingBehavior |
| ChildDrawingOrderCallback | zIndex modifier |
| PageTransformer | graphicsLayer modifier |
| OrientationHelper | LazyListLayoutInfo |

## 组件设计

### 1. CoverFlipPagerState

分页状态管理器,类似 RecyclerView 的状态管理。

```kotlin
@Stable
class CoverFlipPagerState(
    initialPage: Int = 0,
    val pageCount: Int
) {
    // LazyListState 用于控制 LazyRow
    val lazyListState = LazyListState(initialPage)
    
    // 当前页索引
    var currentPage by mutableStateOf(initialPage)
        private set
    
    // 滚动偏移量(像素)
    var scrollOffset by mutableStateOf(0f)
        private set
    
    // 滚动进度(0f 到 1f)
    val scrollProgress: Float
        get() = scrollOffset / pageWidth
    
    // 页面宽度(从 LazyListLayoutInfo 获取)
    var pageWidth by mutableStateOf(0)
        private set
    
    // 是否正在滚动
    val isScrollInProgress: Boolean
        get() = lazyListState.isScrollInProgress
    
    // 滚动到指定页面
    suspend fun scrollToPage(page: Int) {
        lazyListState.animateScrollToItem(page)
        currentPage = page
    }
    
    // 更新状态(在 LaunchedEffect 中调用)
    fun updateState(layoutInfo: LazyListLayoutInfo) {
        val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
        if (firstVisibleItem != null) {
            currentPage = firstVisibleItem.index
            scrollOffset = -firstVisibleItem.offset.toFloat()
            pageWidth = firstVisibleItem.size
        }
    }
}
```

### 2. SnapFlingBehavior

自定义 Fling 行为,实现跨页吸附。

```kotlin
class SnapFlingBehavior(
    private val pagerState: CoverFlipPagerState,
    private val snapThreshold: Float = 0.3f,  // 30% 阈值
    private val velocityThreshold: Float = 1000f  // dp/s
) : FlingBehavior {
    
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // 1. 计算目标页面
        val targetPage = calculateTargetPage(
            currentPage = pagerState.currentPage,
            scrollProgress = pagerState.scrollProgress,
            velocity = initialVelocity
        )
        
        // 2. 执行吸附动画
        return snapToPage(targetPage, initialVelocity)
    }
    
    private fun calculateTargetPage(
        currentPage: Int,
        scrollProgress: Float,
        velocity: Float
    ): Int {
        // 根据滚动进度和速度判断目标页面
        val absProgress = abs(scrollProgress)
        val absVelocity = abs(velocity)
        
        return when {
            // 速度足够大,直接翻页
            absVelocity > velocityThreshold -> {
                if (velocity > 0) currentPage + 1 else currentPage - 1
            }
            // 滚动超过阈值,翻页
            absProgress > snapThreshold -> {
                if (scrollProgress > 0) currentPage + 1 else currentPage - 1
            }
            // 否则回到当前页
            else -> currentPage
        }.coerceIn(0, pagerState.pageCount - 1)
    }
    
    private suspend fun ScrollScope.snapToPage(
        targetPage: Int,
        initialVelocity: Float
    ): Float {
        // 使用 Animatable 实现平滑滚动
        val animatable = Animatable(pagerState.scrollOffset)
        val targetOffset = targetPage * pagerState.pageWidth.toFloat()
        
        animatable.animateTo(
            targetValue = targetOffset,
            initialVelocity = initialVelocity,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) {
            // 更新滚动位置
            val delta = value - pagerState.scrollOffset
            scrollBy(delta)
        }
        
        return animatable.velocity
    }
}
```

### 3. PageScrollListener

页面滚动监听器,类似 RecyclerView.OnScrollListener。

```kotlin
class PageScrollListener(
    private val onPageScrolled: (
        position: Int,      // 当前页索引
        positionOffset: Float,  // 偏移量(0f 到 1f)
        positionOffsetPixels: Int  // 偏移量(像素)
    ) -> Unit
) {
    fun onScroll(pagerState: CoverFlipPagerState) {
        val position = pagerState.currentPage
        val offsetPixels = pagerState.scrollOffset.toInt()
        val offset = if (pagerState.pageWidth > 0) {
            offsetPixels.toFloat() / pagerState.pageWidth
        } else 0f
        
        onPageScrolled(position, offset, offsetPixels)
    }
}
```

### 4. CoverFlipTransformer

覆盖效果变换器,类似 ViewPager 的 PageTransformer。

```kotlin
class CoverFlipTransformer {
    /**
     * 计算页面变换
     * @param page 页面索引
     * @param position 页面位置(-1f 到 1f, 0f 表示完全可见)
     * @param pageWidth 页面宽度
     * @return 变换参数(translationX, alpha, zIndex)
     */
    fun transformPage(
        page: Int,
        position: Float,
        pageWidth: Int
    ): PageTransform {
        return when {
            // 左侧页面(已经翻过的页面)
            position < -1f -> {
                PageTransform(
                    translationX = 0f,
                    alpha = 0f,
                    zIndex = 0f
                )
            }
            // 当前可见的两个页面
            position <= 0f -> {
                // 第一个页面(底层,静止)
                PageTransform(
                    translationX = 0f,
                    alpha = 1f,
                    zIndex = 0f
                )
            }
            position <= 1f -> {
                // 第二个页面(上层,跟随滚动)
                // 关键:translationX 需要抵消 LazyRow 的滚动
                val offsetX = -position * pageWidth
                PageTransform(
                    translationX = offsetX,
                    alpha = 1f,
                    zIndex = 1f  // 确保在上层
                )
            }
            // 右侧页面(还未显示的页面)
            else -> {
                PageTransform(
                    translationX = 0f,
                    alpha = 0f,
                    zIndex = 0f
                )
            }
        }
    }
    
    /**
     * 计算阴影透明度
     */
    fun calculateShadowAlpha(position: Float, maxAlpha: Float = 0.4f): Float {
        return when {
            position in 0f..1f -> position * maxAlpha
            else -> 0f
        }
    }
}

data class PageTransform(
    val translationX: Float,
    val alpha: Float,
    val zIndex: Float
)
```

### 5. ChildDrawingOrderController

子 View 绘制顺序控制器,确保前面的页面在后面的页面之上。

在 Compose 中,我们使用 `zIndex` modifier 来控制绘制顺序:

```kotlin
// 在 LazyRow 的 items 中使用
item(key = pageIndex) {
    val transform = coverFlipTransformer.transformPage(
        page = pageIndex,
        position = calculatePosition(pageIndex),
        pageWidth = pageWidth
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = transform.translationX
                alpha = transform.alpha
                // zIndex 控制绘制顺序,值越大越在上层
                this.zIndex = transform.zIndex
            }
    ) {
        // 页面内容
    }
}
```

## 数据模型

### CoverFlipConfig

覆盖翻页配置数据类。

```kotlin
data class CoverFlipConfig(
    // 动画时长
    val animationDuration: Int = 300,  // 毫秒
    
    // 阈值配置
    val swipeThreshold: Float = 0.3f,  // 30% 屏幕宽度
    val velocityThreshold: Float = 1000f,  // dp/s
    
    // 阴影配置
    val shadowEnabled: Boolean = true,
    val shadowMaxAlpha: Float = 0.4f,
    val shadowBlurRadius: Float = 8f,  // dp
    
    // 边界反弹配置
    val bounceEnabled: Boolean = true,
    val bounceMaxDisplacement: Float = 100f,  // dp
    val bounceDuration: Int = 200  // 毫秒
)
```

## UI 实现

### ReaderContent 组件改造

完整的覆盖翻页实现:

```kotlin
@Composable
fun ReaderContentWithCoverFlip(
    pages: List<Bitmap>,  // 所有页面的位图列表
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
    
    // 6. 获取屏幕宽度
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageWidth = constraints.maxWidth
        
        // 7. LazyRow 实现分页滚动
        LazyRow(
            state = pagerState.lazyListState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = SnapFlingBehavior(
                pagerState = pagerState,
                snapThreshold = config.swipeThreshold,
                velocityThreshold = config.velocityThreshold
            ),
            userScrollEnabled = true
        ) {
            itemsIndexed(
                items = pages,
                key = { index, _ -> index }
            ) { index, bitmap ->
                // 8. 计算页面位置
                val position = calculatePagePosition(
                    pageIndex = index,
                    currentPage = pagerState.currentPage,
                    scrollProgress = pagerState.scrollProgress
                )
                
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
                        .width(pageWidth.dp)
                        .fillMaxHeight()
                        .graphicsLayer {
                            // 关键:translationX 实现覆盖效果
                            translationX = transform.translationX
                            alpha = transform.alpha
                            // zIndex 控制绘制顺序
                            this.zIndex = transform.zIndex
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val zone = detectTouchZone(
                                    offset,
                                    IntSize(pageWidth, constraints.maxHeight)
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
 * 计算页面位置
 * @return -1f 到 1f 之间的值,0f 表示完全可见
 */
private fun calculatePagePosition(
    pageIndex: Int,
    currentPage: Int,
    scrollProgress: Float
): Float {
    return (pageIndex - currentPage) - scrollProgress
}
```

### PageContent 组件

单个页面的内容渲染:

```kotlin
@Composable
fun PageContent(
    bitmap: Bitmap,
    shadowAlpha: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 绘制页面图片
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val imageBitmap = bitmap.asImageBitmap()
                
                // 计算缩放比例(保持宽高比)
                val scale = minOf(
                    size.width / imageBitmap.width,
                    size.height / imageBitmap.height
                )
                
                val scaledWidth = imageBitmap.width * scale
                val scaledHeight = imageBitmap.height * scale
                
                // 居中对齐
                val dx = (size.width - scaledWidth) / 2f
                val dy = (size.height - scaledHeight) / 2f
                
                // 绘制位图
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(dx, dy)
                canvas.nativeCanvas.scale(scale, scale)
                canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
                canvas.nativeCanvas.restore()
            }
        }
        
        // 绘制阴影效果
        if (shadowAlpha > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(0f, 0f),
                    size = size
                )
            }
        }
    }
}
```

## 核心动画逻辑详解

### 1. 跨页吸附(Snap to Page)

参考开发指南中的实现,分为两种情况:

#### (1) Scroll Idle - 拖拽结束后的吸附

```kotlin
// 在 SnapFlingBehavior 中实现
private suspend fun ScrollScope.snapToTargetExistingView(): Pair<Int, Int>? {
    val layoutInfo = pagerState.lazyListState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    
    if (visibleItems.isEmpty()) return null
    
    // 找到距离中心最近的 View
    var closestChild: LazyListItemInfo? = null
    var absClosest = Int.MAX_VALUE
    var scrollDistance = 0
    
    // LazyRow 的中心点
    val containerCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2
    
    for (item in visibleItems) {
        val childCenter = item.offset + item.size / 2
        val absDistance = abs(childCenter - containerCenter)
        
        if (absDistance < absClosest) {
            absClosest = absDistance
            closestChild = item
            scrollDistance = childCenter - containerCenter
        }
    }
    
    closestChild ?: return null
    
    // 平滑滚动到目标位置
    pagerState.lazyListState.animateScrollBy(scrollDistance.toFloat())
    
    return Pair(scrollDistance, closestChild.index)
}
```

#### (2) Fling - 快速滑动的吸附

```kotlin
// 在 SnapFlingBehavior.performFling 中实现
override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
    // 1. 找到吸附目标位置
    val targetPosition = findTargetSnapPosition(
        layoutInfo = pagerState.lazyListState.layoutInfo,
        velocity = initialVelocity
    )
    
    if (targetPosition == -1) return initialVelocity
    
    // 2. 使用 LinearSmoothScroller 的思路实现平滑滚动
    return snapFromFling(targetPosition, initialVelocity)
}

private fun findTargetSnapPosition(
    layoutInfo: LazyListLayoutInfo,
    velocity: Float
): Int {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return -1
    
    // 中心点以前和以后距离最近的 View
    var closestChildBeforeCenter: LazyListItemInfo? = null
    var distanceBefore = Int.MIN_VALUE
    var closestChildAfterCenter: LazyListItemInfo? = null
    var distanceAfter = Int.MAX_VALUE
    
    val containerCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2
    
    for (item in visibleItems) {
        val childCenter = item.offset + item.size / 2
        val distance = childCenter - containerCenter
        
        // 找到两个方向最近的 View
        if (distance in (distanceBefore + 1)..0) {
            distanceBefore = distance
            closestChildBeforeCenter = item
        }
        if (distance in 0 until distanceAfter) {
            distanceAfter = distance
            closestChildAfterCenter = item
        }
    }
    
    // 根据方向选择 Fling 到哪个 View
    val forwardDirection = velocity > 0
    return when {
        forwardDirection && closestChildAfterCenter != null -> {
            closestChildAfterCenter.index
        }
        !forwardDirection && closestChildBeforeCenter != null -> {
            closestChildBeforeCenter.index
        }
        else -> -1
    }
}

private suspend fun ScrollScope.snapFromFling(
    targetPosition: Int,
    initialVelocity: Float
): Float {
    // 使用 Animatable 实现平滑滚动
    val animatable = Animatable(0f, Float.VectorConverter)
    
    // 计算目标偏移量
    val currentOffset = pagerState.lazyListState.firstVisibleItemScrollOffset
    val targetOffset = targetPosition * pagerState.pageWidth
    val distance = targetOffset - currentOffset
    
    // 执行动画
    animatable.animateTo(
        targetValue = distance.toFloat(),
        initialVelocity = initialVelocity,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) {
        val delta = value - animatable.value
        scrollBy(delta)
    }
    
    return animatable.velocity
}
```

### 2. 覆盖效果实现

参考开发指南,关键是让第二个可见 View 跟随屏幕滑动:

```kotlin
// 在 CoverFlipTransformer.transformPage 中实现
fun transformPage(page: Int, position: Float, pageWidth: Int): PageTransform {
    return when {
        // 第一个页面(底层,静止)
        position <= 0f -> {
            PageTransform(
                translationX = 0f,  // 不移动
                alpha = 1f,
                zIndex = 0f  // 在下层
            )
        }
        // 第二个页面(上层,跟随滚动)
        position <= 1f -> {
            // 关键:translationX 需要抵消 LazyRow 的滚动
            // position 表示页面相对于当前页的位置
            // position * pageWidth 是 LazyRow 自动滚动的距离
            // 我们需要反向移动相同的距离,让页面看起来"跟随屏幕"
            val offsetX = -position * pageWidth
            
            PageTransform(
                translationX = offsetX,
                alpha = 1f,
                zIndex = 1f  // 在上层
            )
        }
        // 其他页面不可见
        else -> {
            PageTransform(
                translationX = 0f,
                alpha = 0f,
                zIndex = 0f
            )
        }
    }
}
```

### 3. 子 View 绘制顺序控制

在 Compose 中使用 `zIndex` modifier:

```kotlin
// 在 LazyRow 的 items 中
Box(
    modifier = Modifier
        .graphicsLayer {
            translationX = transform.translationX
            alpha = transform.alpha
            // zIndex 控制绘制顺序
            // 值越大越在上层,确保前面的页面在后面的页面之上
            this.zIndex = transform.zIndex
        }
) {
    // 页面内容
}
```

这相当于 RecyclerView 中的:

```kotlin
// RecyclerView 方式
override fun onGetChildDrawingOrder(childCount: Int, i: Int) = childCount - i - 1
```

### 4. 阴影效果

```kotlin
fun calculateShadowAlpha(position: Float, maxAlpha: Float = 0.4f): Float {
    // 只在上层页面(position > 0)显示阴影
    return when {
        position in 0f..1f -> {
            // 阴影透明度随位置线性增加
            position * maxAlpha
        }
        else -> 0f
    }
}
```

## 性能优化

### 1. 硬件加速

使用 `graphicsLayer` 确保所有变换都在 GPU 上执行:

```kotlin
Modifier.graphicsLayer {
    translationX = offsetX
    alpha = alpha
}
```

### 2. 位图预渲染

在动画开始前预先加载和缓存位图:

```kotlin
class BitmapPreloader {
    private val cache = LruCache<Int, Bitmap>(maxSize = 3)
    
    fun preload(pageIndex: Int, bitmap: Bitmap) {
        cache.put(pageIndex, bitmap)
    }
    
    fun get(pageIndex: Int): Bitmap? = cache.get(pageIndex)
}
```

### 3. 避免重组

使用 `remember` 和 `derivedStateOf` 减少不必要的重组:

```kotlin
val layers = remember(currentPageBitmap, nextPageBitmap, previousPageBitmap, direction) {
    layerManager.getPageLayers(...)
}
```

## 边界处理

### 第一页和最后一页

```kotlin
fun handleBoundary(isFirstPage: Boolean, isLastPage: Boolean, direction: SwipeDirection) {
    when {
        isFirstPage && direction == SwipeDirection.RIGHT -> {
            // 显示回弹动画
            showBounceAnimation(direction)
        }
        isLastPage && direction == SwipeDirection.LEFT -> {
            // 显示回弹动画
            showBounceAnimation(direction)
        }
        else -> {
            // 正常翻页
            performPageFlip(direction)
        }
    }
}
```

### 回弹动画

```kotlin
suspend fun showBounceAnimation(direction: SwipeDirection) {
    val bounceOffset = if (direction == SwipeDirection.RIGHT) 100f else -100f
    
    // 弹出
    animate(
        initialValue = 0f,
        targetValue = bounceOffset,
        animationSpec = tween(durationMillis = 100)
    ) { value, _ ->
        offsetProgress = value / screenWidth
    }
    
    // 回弹
    animate(
        initialValue = bounceOffset,
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) { value, _ ->
        offsetProgress = value / screenWidth
    }
}
```

## 多点触控处理

```kotlin
fun handleMultiTouch(pointerCount: Int) {
    if (pointerCount > 1) {
        // 取消当前动画
        cancelAnimation()
        // 回到原位
        animateToRest()
    }
}
```

## 测试策略

### 单元测试

- 动画状态转换逻辑
- 阈值判断逻辑
- 边界条件处理

### UI 测试

- 拖动手势响应
- 动画流畅度
- 边界回弹效果

### 性能测试

- 帧率监控 (目标 60fps)
- 内存使用
- CPU/GPU 负载

## 集成方案

### 与现有 ReaderScreen 集成

```kotlin
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    // ...
) {
    val config by viewModel.readerConfig.collectAsState()
    
    // 根据配置选择翻页模式
    when (config.pageFlipMode) {
        PageFlipMode.COVER -> {
            ReaderContentWithCoverFlip(
                currentPageBitmap = viewModel.currentPage,
                nextPageBitmap = viewModel.nextPage,
                previousPageBitmap = viewModel.previousPage,
                config = config,
                onPageChange = viewModel::onPageChange
            )
        }
        else -> {
            // 其他翻页模式
        }
    }
}
```

## 配置选项

在 ReaderConfig 中添加覆盖翻页相关配置:

```kotlin
data class ReaderConfig(
    // ... 现有配置
    
    // 覆盖翻页配置
    val coverFlipConfig: CoverFlipConfig = CoverFlipConfig(),
    val pageFlipMode: PageFlipMode = PageFlipMode.SLIDE
)

enum class PageFlipMode {
    SLIDE,      // 滑动模式
    COVER,      // 覆盖模式
    SIMULATION  // 仿真模式
}
```

## 总结

本设计采用 Compose 原生方案实现覆盖翻页动画,相比 RecyclerView 方案更加简洁,与现有架构集成更好。核心思路是:

1. 使用双层页面结构(上层+下层)
2. 上层页面跟随手势移动,下层页面保持静止
3. 使用 GraphicsLayer 实现硬件加速
4. 通过阈值和速度判断翻页意图
5. 添加阴影效果增强视觉深度

该方案满足所有需求文档中的验收标准,同时保持了良好的性能和可维护性。
