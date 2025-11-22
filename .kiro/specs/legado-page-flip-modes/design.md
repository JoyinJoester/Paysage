# 设计文档

## 概述

本设计文档详细说明如何在 Paysage 阅读器中实现多种翻页模式，借鉴 Legado 阅读器的优秀实现。我们将创建一个灵活的翻页系统，支持仿真翻页、滑动翻页、覆盖翻页、滚动翻页等多种模式，并提供流畅的动画效果和良好的性能。

## 架构

### 核心组件层次结构

```
ReaderScreen (UI层)
    ↓
PageFlipManager (管理层)
    ↓
PageDelegate (抽象层)
    ↓
├── SimulationPageDelegate (仿真翻页)
├── SlidePageDelegate (滑动翻页)
├── CoverPageDelegate (覆盖翻页)
├── ScrollPageDelegate (滚动翻页)
└── NoAnimPageDelegate (无动画翻页)
```

### 设计原则

1. **委托模式**: 使用 PageDelegate 抽象类定义翻页行为接口
2. **策略模式**: 不同的翻页模式作为不同的策略实现
3. **状态管理**: 使用 Compose State 管理翻页状态和动画
4. **性能优化**: 使用位图缓存和异步渲染
5. **可扩展性**: 易于添加新的翻页模式

## 组件和接口

### 1. PageDelegate 抽象类

```kotlin
abstract class PageDelegate(
    protected val context: Context
) {
    // 视图尺寸
    protected var viewWidth: Int = 0
    protected var viewHeight: Int = 0
    
    // 触摸点
    protected var startX: Float = 0f
    protected var startY: Float = 0f
    protected var touchX: Float = 0f
    protected var touchY: Float = 0f
    
    // 状态
    var isMoved = false
    var isRunning = false
    var isCancel = false
    var direction = PageDirection.NONE
    
    // 抽象方法
    abstract fun onTouch(event: MotionEvent)
    abstract fun onDraw(canvas: Canvas)
    abstract fun onAnimStart(animationSpeed: Int)
    abstract fun onAnimStop()
    abstract fun nextPageByAnim(animationSpeed: Int)
    abstract fun prevPageByAnim(animationSpeed: Int)
    abstract fun abortAnim()
    
    // 通用方法
    open fun setViewSize(width: Int, height: Int)
    open fun computeScroll()
}
```


### 2. PageFlipManager

管理翻页模式的切换和状态。

```kotlin
class PageFlipManager(
    private val context: Context,
    private val onPageChange: (Int) -> Unit
) {
    private var currentDelegate: PageDelegate? = null
    private var currentMode: PageFlipMode = PageFlipMode.SLIDE
    
    // 页面缓存
    private var currentPageBitmap: Bitmap? = null
    private var nextPageBitmap: Bitmap? = null
    private var prevPageBitmap: Bitmap? = null
    
    fun setFlipMode(mode: PageFlipMode) {
        currentDelegate = when (mode) {
            PageFlipMode.SIMULATION -> SimulationPageDelegate(context)
            PageFlipMode.SLIDE -> SlidePageDelegate(context)
            PageFlipMode.COVER -> CoverPageDelegate(context)
            PageFlipMode.SCROLL -> ScrollPageDelegate(context)
            PageFlipMode.NONE -> NoAnimPageDelegate(context)
        }
        currentMode = mode
    }
    
    fun handleTouch(event: MotionEvent) {
        currentDelegate?.onTouch(event)
    }
    
    fun draw(canvas: Canvas) {
        currentDelegate?.onDraw(canvas)
    }
}
```

### 3. SimulationPageDelegate (仿真翻页)

实现贝塞尔曲线的页面卷曲效果。

**核心算法**:
- 使用贝塞尔曲线计算页面卷曲路径
- 计算阴影和高光效果
- 使用 Matrix 变换实现页面翻转

**关键点**:
1. 计算拖拽点对应的页脚位置
2. 计算两条贝塞尔曲线的控制点
3. 绘制当前页、下一页和卷曲效果
4. 添加阴影增强立体感

```kotlin
class SimulationPageDelegate(context: Context) : PageDelegate(context) {
    // 贝塞尔曲线点
    private val bezierStart1 = PointF()
    private val bezierControl1 = PointF()
    private val bezierVertex1 = PointF()
    private val bezierEnd1 = PointF()
    
    // 路径
    private val path0 = Path()
    private val path1 = Path()
    
    // 阴影
    private val backShadowDrawable: GradientDrawable
    private val frontShadowDrawable: GradientDrawable
    
    override fun onDraw(canvas: Canvas) {
        calcPoints()
        drawCurrentPageArea(canvas)
        drawNextPageAreaAndShadow(canvas)
        drawCurrentPageShadow(canvas)
        drawCurrentBackArea(canvas)
    }
    
    private fun calcPoints() {
        // 计算贝塞尔曲线控制点
        // 实现页面卷曲效果
    }
}
```

### 4. SlidePageDelegate (滑动翻页)

简单的滑动效果，页面跟随手指移动。

```kotlin
class SlidePageDelegate(context: Context) : PageDelegate(context) {
    override fun onDraw(canvas: Canvas) {
        val offsetX = touchX - startX
        val distanceX = if (offsetX > 0) {
            offsetX - viewWidth
        } else {
            offsetX + viewWidth
        }
        
        when (direction) {
            PageDirection.PREV -> {
                canvas.withTranslation(distanceX + viewWidth) {
                    drawBitmap(currentPageBitmap)
                }
                canvas.withTranslation(distanceX) {
                    drawBitmap(prevPageBitmap)
                }
            }
            PageDirection.NEXT -> {
                canvas.withTranslation(distanceX) {
                    drawBitmap(nextPageBitmap)
                }
                canvas.withTranslation(distanceX - viewWidth) {
                    drawBitmap(currentPageBitmap)
                }
            }
        }
    }
}
```


### 5. CoverPageDelegate (覆盖翻页)

下一页覆盖当前页的效果。

```kotlin
class CoverPageDelegate(context: Context) : PageDelegate(context) {
    private val shadowDrawable: GradientDrawable
    
    override fun onDraw(canvas: Canvas) {
        val offsetX = touchX - startX
        val distanceX = if (offsetX > 0) {
            offsetX - viewWidth
        } else {
            offsetX + viewWidth
        }
        
        when (direction) {
            PageDirection.PREV -> {
                canvas.withTranslation(distanceX) {
                    drawBitmap(prevPageBitmap)
                }
                addShadow(distanceX, canvas)
            }
            PageDirection.NEXT -> {
                canvas.withClip(viewWidth + offsetX, 0f, viewWidth, viewHeight) {
                    drawBitmap(nextPageBitmap)
                }
                canvas.withTranslation(distanceX - viewWidth) {
                    drawBitmap(currentPageBitmap)
                }
                addShadow(distanceX, canvas)
            }
        }
    }
}
```

### 6. ScrollPageDelegate (滚动翻页)

垂直滚动效果，支持惯性滚动。

```kotlin
class ScrollPageDelegate(context: Context) : PageDelegate(context) {
    private val velocityTracker = VelocityTracker.obtain()
    private val scroller = Scroller(context, LinearInterpolator())
    
    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abortAnim()
                velocityTracker.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker.addMovement(event)
                onScroll()
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.computeCurrentVelocity(1000)
                startFling(velocityTracker.yVelocity.toInt())
            }
        }
    }
    
    private fun startFling(velocityY: Int) {
        scroller.fling(
            0, touchY.toInt(),
            0, velocityY,
            0, 0,
            -10 * viewHeight, 10 * viewHeight
        )
    }
}
```

## 数据模型

### PageFlipMode 枚举

```kotlin
enum class PageFlipMode {
    SIMULATION,  // 仿真翻页
    SLIDE,       // 滑动翻页
    COVER,       // 覆盖翻页
    SCROLL,      // 滚动翻页
    NONE         // 无动画
}
```

### PageDirection 枚举

```kotlin
enum class PageDirection {
    NONE,   // 无方向
    PREV,   // 上一页
    NEXT    // 下一页
}
```

### PageFlipState 状态类

```kotlin
data class PageFlipState(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val flipMode: PageFlipMode = PageFlipMode.SLIDE,
    val isAnimating: Boolean = false,
    val direction: PageDirection = PageDirection.NONE,
    val animationProgress: Float = 0f
)
```

## Compose 集成

### PageFlipContainer Composable

```kotlin
@Composable
fun PageFlipContainer(
    currentBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    prevBitmap: Bitmap?,
    flipMode: PageFlipMode,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val flipManager = remember { PageFlipManager(context, onPageChange) }
    
    LaunchedEffect(flipMode) {
        flipManager.setFlipMode(flipMode)
    }
    
    AndroidView(
        factory = { context ->
            PageFlipView(context).apply {
                setFlipManager(flipManager)
            }
        },
        update = { view ->
            view.updateBitmaps(currentBitmap, nextBitmap, prevBitmap)
        },
        modifier = modifier
    )
}
```


### PageFlipView 自定义 View

```kotlin
class PageFlipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var flipManager: PageFlipManager? = null
    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null
    private var prevBitmap: Bitmap? = null
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        flipManager?.setViewSize(w, h)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        flipManager?.handleTouch(event)
        return true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        flipManager?.draw(canvas)
    }
    
    override fun computeScroll() {
        super.computeScroll()
        flipManager?.computeScroll()
        if (flipManager?.isAnimating == true) {
            invalidate()
        }
    }
}
```

## 错误处理

### 1. 位图加载失败

```kotlin
fun loadPageBitmap(page: Int): Bitmap? {
    return try {
        // 加载位图逻辑
        loadBitmapFromPage(page)
    } catch (e: OutOfMemoryError) {
        // 内存不足，降低图片质量
        loadBitmapWithLowerQuality(page)
    } catch (e: Exception) {
        // 其他错误，显示占位图
        createPlaceholderBitmap()
    }
}
```

### 2. 动画中断处理

```kotlin
fun abortAnimation() {
    if (isAnimating) {
        scroller.abortAnimation()
        isAnimating = false
        // 恢复到最近的有效页面
        snapToNearestPage()
    }
}
```

### 3. 触摸冲突处理

```kotlin
override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
    return when {
        isAnimating -> true  // 动画中拦截所有触摸
        isZoomed -> false    // 缩放时不拦截
        else -> super.onInterceptTouchEvent(event)
    }
}
```

## 测试策略

### 1. 单元测试

- 测试贝塞尔曲线计算的正确性
- 测试页面方向判断逻辑
- 测试动画状态转换

```kotlin
@Test
fun testBezierCalculation() {
    val delegate = SimulationPageDelegate(context)
    delegate.setTouchPoint(100f, 100f)
    val points = delegate.calculateBezierPoints()
    assertNotNull(points)
    assertTrue(points.isValid())
}
```

### 2. UI 测试

- 测试不同翻页模式的切换
- 测试手势识别的准确性
- 测试动画流畅度

```kotlin
@Test
fun testPageFlipGesture() {
    composeTestRule.setContent {
        PageFlipContainer(...)
    }
    
    composeTestRule.onNodeWithTag("pageView")
        .performTouchInput {
            swipeLeft()
        }
    
    // 验证页面已切换
    verify { onPageChange(1) }
}
```

### 3. 性能测试

- 测试动画帧率
- 测试内存使用
- 测试位图缓存效率

```kotlin
@Test
fun testAnimationPerformance() {
    val frameRates = mutableListOf<Float>()
    
    repeat(100) {
        val startTime = System.nanoTime()
        delegate.onDraw(canvas)
        val endTime = System.nanoTime()
        val fps = 1_000_000_000f / (endTime - startTime)
        frameRates.add(fps)
    }
    
    val avgFps = frameRates.average()
    assertTrue(avgFps >= 30f)  // 至少 30 FPS
}
```


## 性能优化

### 1. 位图缓存策略

```kotlin
class BitmapCache(private val maxSize: Int = 3) {
    private val cache = LruCache<Int, Bitmap>(maxSize)
    
    fun get(page: Int): Bitmap? = cache.get(page)
    
    fun put(page: Int, bitmap: Bitmap) {
        cache.put(page, bitmap)
    }
    
    fun preload(pages: List<Int>) {
        pages.forEach { page ->
            if (cache.get(page) == null) {
                loadBitmapAsync(page) { bitmap ->
                    cache.put(page, bitmap)
                }
            }
        }
    }
}
```

### 2. 异步渲染

```kotlin
class AsyncPageRenderer(
    private val scope: CoroutineScope
) {
    fun renderPageAsync(
        page: Int,
        onComplete: (Bitmap) -> Unit
    ) {
        scope.launch(Dispatchers.Default) {
            val bitmap = loadAndProcessBitmap(page)
            withContext(Dispatchers.Main) {
                onComplete(bitmap)
            }
        }
    }
}
```

### 3. 硬件加速

```kotlin
class PageFlipView : View {
    init {
        // 启用硬件加速
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    override fun onDraw(canvas: Canvas) {
        // 使用硬件加速的 Canvas
        if (canvas.isHardwareAccelerated) {
            drawWithHardwareAcceleration(canvas)
        } else {
            drawWithSoftwareRendering(canvas)
        }
    }
}
```

### 4. 降级策略

```kotlin
class PerformanceMonitor {
    private val frameRates = mutableListOf<Float>()
    
    fun shouldDowngrade(): Boolean {
        val avgFps = frameRates.takeLast(30).average()
        return avgFps < 30f
    }
    
    fun applyDowngrade(delegate: PageDelegate) {
        when (delegate) {
            is SimulationPageDelegate -> {
                // 禁用阴影效果
                delegate.disableShadows()
            }
            is ScrollPageDelegate -> {
                // 减少滚动平滑度
                delegate.reduceSmoothing()
            }
        }
    }
}
```

## 配置和设置

### 设置界面集成

```kotlin
@Composable
fun PageFlipSettings(
    currentMode: PageFlipMode,
    onModeChange: (PageFlipMode) -> Unit
) {
    Column {
        Text("翻页模式", style = MaterialTheme.typography.titleMedium)
        
        PageFlipMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeChange(mode) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(mode.displayName)
                    Text(
                        mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

### 持久化配置

```kotlin
class PageFlipPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("page_flip", Context.MODE_PRIVATE)
    
    var flipMode: PageFlipMode
        get() = PageFlipMode.valueOf(
            prefs.getString("flip_mode", PageFlipMode.SLIDE.name) ?: PageFlipMode.SLIDE.name
        )
        set(value) {
            prefs.edit().putString("flip_mode", value.name).apply()
        }
    
    var animationSpeed: Int
        get() = prefs.getInt("animation_speed", 300)
        set(value) {
            prefs.edit().putInt("animation_speed", value).apply()
        }
}
```

## 可访问性

### 1. 触觉反馈

```kotlin
fun provideTactileFeedback(view: View, type: Int = HapticFeedbackConstants.VIRTUAL_KEY) {
    if (Settings.System.getInt(
        view.context.contentResolver,
        Settings.System.HAPTIC_FEEDBACK_ENABLED,
        0
    ) != 0) {
        view.performHapticFeedback(type)
    }
}
```

### 2. 语音提示

```kotlin
fun announcePageChange(view: View, page: Int, total: Int) {
    view.announceForAccessibility("第 ${page + 1} 页，共 $total 页")
}
```

### 3. 大字体支持

```kotlin
fun adjustForLargeText(context: Context): Float {
    val fontScale = context.resources.configuration.fontScale
    return when {
        fontScale > 1.3f -> 1.5f  // 大字体模式
        fontScale > 1.15f -> 1.2f // 中等字体
        else -> 1.0f              // 正常字体
    }
}
```

## 总结

本设计文档详细说明了如何在 Paysage 中实现多种翻页模式。核心设计包括：

1. **灵活的架构**: 使用委托模式和策略模式，易于扩展
2. **丰富的动画**: 支持仿真、滑动、覆盖、滚动等多种效果
3. **性能优化**: 位图缓存、异步渲染、硬件加速
4. **用户体验**: 流畅的动画、准确的手势识别、可配置的选项
5. **可访问性**: 触觉反馈、语音提示、大字体支持

通过学习 Legado 的优秀实现，我们可以为 Paysage 用户提供一流的阅读体验。
