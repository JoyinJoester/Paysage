# 自然翻页交互设计文档

## 概述

本设计文档描述如何实现符合自然阅读习惯的堆叠式翻页交互效果。核心设计原则是确保滑动方向与页面移动方向完全一致，提供实时跟随手指的流畅体验，并正确处理所有边界情况。

### 设计目标

1. **方向一致性**: 向右滑动页面向右移，向左滑动页面向左移
2. **实时响应**: 页面位置精确跟随手指，延迟 < 16ms
3. **堆叠效果**: 当前页在上，新页在下，保持清晰的层级关系
4. **流畅动画**: 使用 ease-out 曲线，完成/取消动画自然流畅
5. **边界处理**: 第一页和最后一页正确响应，不产生混淆

### 当前问题分析

现有实现存在以下问题：

1. **方向映射错误**: 
   - 当前代码在 `handleDrag` 中使用 `dragStartX - offset.x` (向前) 和 `offset.x - dragStartX` (向后)
   - 这导致向右滑动时计算出负值，页面实际向左移动
   
2. **缺少实时跟随**:
   - 手势处理器只在 `onDragEnd` 时判断是否翻页
   - 拖动过程中没有实时更新页面位置

3. **层级关系不清晰**:
   - 没有明确的 z-index 管理
   - 阴影效果不够明显

## 架构设计

### 核心修改点

```
┌─────────────────────────────────────────────────────────┐
│              PageViewWithTransition                      │
│  ┌────────────────────────────────────────────────────┐ │
│  │  detectDragGestures                                │ │
│  │    ├─ onDragStart: 初始化手势状态                  │ │
│  │    ├─ onDrag: 实时更新页面位置                     │ │
│  │    ├─ onDragEnd: 判断完成或取消                    │ │
│  │    └─ onDragCancel: 重置状态                       │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│           PageGestureHandler (修改)                      │
│  ┌────────────────────────────────────────────────────┐ │
│  │  handleDragStart: 判断方向并初始化                 │ │
│  │  handleDragUpdate: 计算实时进度                    │ │
│  │  handleDragEnd: 判断阈值并触发动画                 │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│         PageTransitionContainer (修改)                   │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Box (下层 - 新页面)                               │ │
│  │    └─ 固定位置，不移动                             │ │
│  │  Box (上层 - 当前页面)                             │ │
│  │    └─ translationX = dragDistance                  │ │
│  │    └─ shadow = progress * 0.3                      │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## 组件和接口

### 1. PageGestureHandler 修改

**新增方法**: `handleDragUpdate` - 处理拖动过程中的实时更新

**修改逻辑**:

```kotlin
class PageGestureHandler(
    private val config: TransitionConfig,
    private val onTransitionStart: (direction: TransitionDirection) -> Unit,
    private val onTransitionUpdate: (progress: Float, dragDistance: Float) -> Unit,  // 新增 dragDistance
    private val onTransitionEnd: (complete: Boolean) -> Unit
) {
    private var dragStartX = 0f
    private var isDragging = false
    private var currentDirection: TransitionDirection = TransitionDirection.FORWARD
    private var screenWidth = 0f
    
    /**
     * 处理拖动开始
     */
    fun handleDragStart(offset: Offset, screenSize: Size, canGoBack: Boolean, canGoForward: Boolean): Boolean {
        screenWidth = screenSize.width
        
        // 判断初始方向
        val isLeftHalf = offset.x < screenSize.width / 2
        
        // 检查边界条件
        if (isLeftHalf && !canGoBack) return false  // 左侧但不能后退
        if (!isLeftHalf && !canGoForward) return false  // 右侧但不能前进
        
        dragStartX = offset.x
        isDragging = true
        currentDirection = if (isLeftHalf) {
            TransitionDirection.BACKWARD
        } else {
            TransitionDirection.FORWARD
        }
        
        onTransitionStart(currentDirection)
        return true
    }
    
    /**
     * 处理拖动更新 - 实时跟随手指
     */
    fun handleDragUpdate(currentOffset: Offset) {
        if (!isDragging) return
        
        // 计算实际拖动距离（像素）
        val dragDistance = currentOffset.x - dragStartX
        
        // 根据方向判断是否有效拖动
        val effectiveDrag = when (currentDirection) {
            TransitionDirection.BACKWARD -> {
                // 向右滑动（上一页）：dragDistance 应该为正
                if (dragDistance > 0) dragDistance else 0f
            }
            TransitionDirection.FORWARD -> {
                // 向左滑动（下一页）：dragDistance 应该为负，取绝对值
                if (dragDistance < 0) -dragDistance else 0f
            }
        }
        
        // 计算进度 (0.0 到 1.0)
        val progress = (effectiveDrag / screenWidth).coerceIn(0f, 1f)
        
        // 传递进度和实际拖动距离
        onTransitionUpdate(progress, effectiveDrag)
    }
    
    /**
     * 处理拖动结束
     */
    fun handleDragEnd(velocity: Velocity, currentProgress: Float) {
        if (!isDragging) return
        
        // 判断是否完成翻页
        val velocityThreshold = 1000f
        val shouldComplete = currentProgress > config.threshold || 
                            kotlin.math.abs(velocity.x) > velocityThreshold
        
        onTransitionEnd(shouldComplete)
        isDragging = false
    }
    
    /**
     * 取消拖动（多点触控）
     */
    fun cancelDrag() {
        if (isDragging) {
            onTransitionEnd(false)
            isDragging = false
        }
    }
}
```

### 2. PageTransitionContainer 修改

**新增参数**: `dragDistance` - 实际拖动距离（像素）

**修改布局逻辑**:

```kotlin
@Composable
fun PageTransitionContainer(
    currentPageBitmap: Bitmap,
    nextPageBitmap: Bitmap,
    transitionState: TransitionState,
    animator: TransitionAnimator,
    dragDistance: Float = 0f,  // 新增：实际拖动距离
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 下层：新页面（固定位置）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 新页面完全不动
                    alpha = 1f
                }
        ) {
            Image(
                bitmap = when (transitionState.direction) {
                    TransitionDirection.FORWARD -> nextPageBitmap.asImageBitmap()
                    TransitionDirection.BACKWARD -> nextPageBitmap.asImageBitmap()
                },
                contentDescription = "Next Page",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        // 上层：当前页面（跟随手指移动）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 关键修改：直接使用拖动距离
                    translationX = when (transitionState.direction) {
                        TransitionDirection.FORWARD -> -dragDistance  // 向左移动
                        TransitionDirection.BACKWARD -> dragDistance   // 向右移动
                    }
                    
                    // 阴影效果
                    shadowElevation = 8.dp.toPx()
                    
                    // 如果动画器提供了额外的变换，应用它们
                    val transform = animator.calculateTransform(transitionState.progress)
                    alpha = transform.alpha
                }
                .drawBehind {
                    // 绘制边缘阴影
                    val shadowAlpha = (transitionState.progress * 0.3f).coerceIn(0f, 0.3f)
                    drawRect(
                        color = Color.Black.copy(alpha = shadowAlpha),
                        topLeft = when (transitionState.direction) {
                            TransitionDirection.FORWARD -> Offset(size.width - 20f, 0f)
                            TransitionDirection.BACKWARD -> Offset(0f, 0f)
                        },
                        size = Size(20f, size.height)
                    )
                }
        ) {
            Image(
                bitmap = currentPageBitmap.asImageBitmap(),
                contentDescription = "Current Page",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
```

### 3. PageViewWithTransition 修改

**集成实时拖动更新**:

```kotlin
@Composable
fun PageViewWithTransition(
    currentBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    previousBitmap: Bitmap?,  // 新增：上一页位图
    transitionState: ReaderTransitionState,
    currentPage: Int,
    totalPages: Int,
    scale: Float,
    offset: Offset,
    touchZoneConfig: TouchZoneConfig,
    readingDirection: ReadingDirection,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onTouchZone: (TouchZone) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 手势状态
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var currentDragOffset by remember { mutableStateOf(Offset.Zero) }
    var currentDragDistance by remember { mutableStateOf(0f) }
    var isMultiTouch by remember { mutableStateOf(false) }
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentPage, totalPages, scale) {
                    // 只在未缩放时处理翻页手势
                    if (scale <= 1f) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                // 检查是否可以翻页
                                val canGoBack = currentPage > 0
                                val canGoForward = currentPage < totalPages - 1
                                
                                // 初始化手势
                                val started = transitionState.gestureHandler.handleDragStart(
                                    startOffset,
                                    screenSize,
                                    canGoBack,
                                    canGoForward
                                )
                                
                                if (started) {
                                    dragStartOffset = startOffset
                                    currentDragOffset = startOffset
                                    currentDragDistance = 0f
                                    isMultiTouch = false
                                }
                            },
                            onDrag = { change, _ ->
                                // 检测多点触控
                                if (change.pressed && change.previousPressed) {
                                    // 更新当前位置
                                    currentDragOffset = change.position
                                    
                                    // 计算拖动距离
                                    currentDragDistance = currentDragOffset.x - dragStartOffset.x
                                    
                                    // 实时更新手势处理器
                                    transitionState.gestureHandler.handleDragUpdate(currentDragOffset)
                                }
                            },
                            onDragEnd = {
                                // 获取当前进度
                                val progress = transitionState.transitionState.progress
                                
                                // 判断是否完成翻页
                                transitionState.gestureHandler.handleDragEnd(
                                    Velocity.Zero,
                                    progress
                                )
                                
                                // 重置状态
                                dragStartOffset = Offset.Zero
                                currentDragOffset = Offset.Zero
                                currentDragDistance = 0f
                            },
                            onDragCancel = {
                                // 取消手势
                                transitionState.gestureHandler.cancelDrag()
                                
                                // 重置状态
                                dragStartOffset = Offset.Zero
                                currentDragOffset = Offset.Zero
                                currentDragDistance = 0f
                            }
                        )
                    }
                }
                // 多点触控检测
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            // 检测第二根手指
                            if (event.changes.size > 1 && !isMultiTouch) {
                                isMultiTouch = true
                                transitionState.gestureHandler.cancelDrag()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 渲染页面
            if (transitionState.transitionState.isActive) {
                // 正在过渡中
                val displayBitmap = when (transitionState.transitionState.direction) {
                    TransitionDirection.FORWARD -> nextBitmap
                    TransitionDirection.BACKWARD -> previousBitmap
                }
                
                if (currentBitmap != null && displayBitmap != null) {
                    PageTransitionContainer(
                        currentPageBitmap = currentBitmap,
                        nextPageBitmap = displayBitmap,
                        transitionState = transitionState.transitionState,
                        animator = transitionState.animator,
                        dragDistance = currentDragDistance,  // 传递实际拖动距离
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // 静止状态，显示当前页
                currentBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Current Page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
```

### 4. TransitionState 扩展

**新增字段**:

```kotlin
data class TransitionState(
    val isActive: Boolean = false,
    val fromPage: Int = 0,
    val toPage: Int = 0,
    val progress: Float = 0f,
    val direction: TransitionDirection = TransitionDirection.FORWARD,
    val dragDistance: Float = 0f  // 新增：实际拖动距离（像素）
) {
    companion object {
        val Idle = TransitionState()
    }
}
```

## 数据模型

### DragGestureState (新增)

```kotlin
/**
 * 拖动手势状态
 */
data class DragGestureState(
    val isActive: Boolean = false,
    val startOffset: Offset = Offset.Zero,
    val currentOffset: Offset = Offset.Zero,
    val direction: TransitionDirection? = null,
    val isMultiTouch: Boolean = false
) {
    /**
     * 总拖动距离（X轴）
     */
    val totalDragX: Float
        get() = currentOffset.x - startOffset.x
    
    /**
     * 总拖动距离（Y轴）
     */
    val totalDragY: Float
        get() = currentOffset.y - startOffset.y
    
    /**
     * 有效拖动距离（根据方向）
     */
    fun getEffectiveDrag(): Float {
        return when (direction) {
            TransitionDirection.BACKWARD -> if (totalDragX > 0) totalDragX else 0f
            TransitionDirection.FORWARD -> if (totalDragX < 0) -totalDragX else 0f
            null -> 0f
        }
    }
}
```

## 动画曲线

### Ease-Out 曲线实现

使用 Compose 的 `FastOutSlowInEasing`:

```kotlin
// 完成翻页动画
val completeAnimationSpec = tween<Float>(
    durationMillis = 300,
    easing = FastOutSlowInEasing  // ease-out 效果
)

// 回弹动画
val bounceBackAnimationSpec = tween<Float>(
    durationMillis = 250,
    easing = FastOutSlowInEasing
)

// 快速滑动动画
val fastSwipeAnimationSpec = tween<Float>(
    durationMillis = 200,
    easing = FastOutSlowInEasing
)
```

### 自定义 Cubic-Bezier

如果需要更精确的控制：

```kotlin
val customEaseOut = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

val customAnimationSpec = tween<Float>(
    durationMillis = 300,
    easing = customEaseOut
)
```

## 边界处理

### 边界检测逻辑

```kotlin
/**
 * 检查是否可以翻页
 */
fun canFlipPage(
    currentPage: Int,
    totalPages: Int,
    direction: TransitionDirection
): Boolean {
    return when (direction) {
        TransitionDirection.BACKWARD -> currentPage > 0
        TransitionDirection.FORWARD -> currentPage < totalPages - 1
    }
}

/**
 * 边界阻力效果
 */
fun calculateBoundaryResistance(dragDistance: Float, maxResistance: Float = 20f): Float {
    // 使用对数函数创建阻力效果
    val resistance = maxResistance * (1f - kotlin.math.exp(-kotlin.math.abs(dragDistance) / 100f))
    return if (dragDistance > 0) resistance else -resistance
}
```

### 边界视觉反馈

```kotlin
@Composable
fun BoundaryFeedback(
    isAtBoundary: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isAtBoundary) 0.3f else 0f,
        animationSpec = tween(100)
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = alpha))
    )
}
```

## 性能优化

### 1. 硬件加速

确保所有变换使用 GPU 加速：

```kotlin
Modifier.graphicsLayer {
    // ✅ GPU 加速属性
    translationX = dragDistance
    translationY = 0f
    alpha = 1f
    shadowElevation = 8.dp.toPx()
    
    // ❌ 避免使用
    // clip() - 触发软件渲染
    // 复杂的 drawBehind - 性能差
}
```

### 2. 触摸事件优化

```kotlin
// 使用高优先级线程处理触摸事件
.pointerInput(Unit) {
    // Compose 自动在高优先级线程处理
    detectDragGestures { ... }
}

// 批处理触摸事件
var lastUpdateTime = 0L
onDrag = { change, _ ->
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastUpdateTime >= 16) {  // 限制为 60fps
        handleDragUpdate(change.position)
        lastUpdateTime = currentTime
    }
}
```

### 3. 位图预加载

```kotlin
/**
 * 在手势开始时预加载相邻页面
 */
fun preloadAdjacentPages(
    currentPage: Int,
    direction: TransitionDirection,
    preloader: PagePreloader
) {
    when (direction) {
        TransitionDirection.FORWARD -> {
            preloader.preloadPage(currentPage + 1)
        }
        TransitionDirection.BACKWARD -> {
            preloader.preloadPage(currentPage - 1)
        }
    }
}
```

### 4. 内存管理

```kotlin
/**
 * 过渡期间锁定页面，防止被回收
 */
class TransitionMemoryLock(
    private val memoryManager: BitmapMemoryManager
) {
    private val lockedPages = mutableSetOf<Int>()
    
    fun lockPages(pages: List<Int>) {
        lockedPages.addAll(pages)
        pages.forEach { page ->
            memoryManager.pinPage(page)
        }
    }
    
    fun unlockAll() {
        lockedPages.forEach { page ->
            memoryManager.unpinPage(page)
        }
        lockedPages.clear()
    }
}
```

## 错误处理

### 1. 位图加载失败

```kotlin
if (nextBitmap == null) {
    // 取消过渡
    transitionState.controller.cancelTransition(animated = true)
    
    // 显示错误提示
    showSnackbar("无法加载页面")
    
    return
}
```

### 2. 内存不足

```kotlin
try {
    startTransition()
} catch (e: OutOfMemoryError) {
    // 清理缓存
    memoryManager.clearCache()
    
    // 降级到无动画模式
    transitionSettings.update {
        it.copy(mode = TransitionMode.None)
    }
    
    // 直接切换页面
    onPageChange(targetPage)
}
```

### 3. 手势冲突

```kotlin
// 检测多点触控
if (event.changes.size > 1) {
    // 取消翻页手势
    gestureHandler.cancelDrag()
    
    // 转交给缩放处理器
    transformableState.startTransformation()
}
```

## 测试策略

### 单元测试

```kotlin
@Test
fun `drag right should move page right`() {
    val handler = PageGestureHandler(config)
    
    handler.handleDragStart(Offset(100f, 500f), Size(1080f, 1920f), true, true)
    handler.handleDragUpdate(Offset(300f, 500f))  // 向右拖动 200px
    
    // 验证方向和距离
    assertEquals(TransitionDirection.BACKWARD, handler.currentDirection)
    assertEquals(200f, handler.currentDragDistance)
}

@Test
fun `drag below threshold should bounce back`() {
    val handler = PageGestureHandler(config.copy(threshold = 0.3f))
    
    handler.handleDragStart(Offset(100f, 500f), Size(1080f, 1920f), true, true)
    handler.handleDragUpdate(Offset(250f, 500f))  // 拖动 150px (< 30%)
    
    var completed = false
    handler.handleDragEnd(Velocity.Zero, 0.15f) { complete ->
        completed = complete
    }
    
    assertFalse(completed)  // 应该回弹
}
```

### 集成测试

```kotlin
@Test
fun `swipe right on first page should show boundary feedback`() {
    composeTestRule.setContent {
        PageViewWithTransition(
            currentPage = 0,  // 第一页
            totalPages = 10,
            ...
        )
    }
    
    // 尝试向右滑动
    composeTestRule.onNodeWithTag("PageView")
        .performTouchInput {
            swipeRight()
        }
    
    // 验证没有翻页
    assertEquals(0, currentPage)
    
    // 验证显示了边界反馈
    composeTestRule.onNodeWithTag("BoundaryFeedback")
        .assertExists()
}
```

### 性能测试

```kotlin
@Test
fun `drag gesture should maintain 60fps`() {
    val frameTimings = mutableListOf<Long>()
    
    // 模拟拖动手势
    repeat(100) { i ->
        val startTime = System.nanoTime()
        
        handler.handleDragUpdate(Offset(100f + i * 5f, 500f))
        
        val endTime = System.nanoTime()
        frameTimings.add(endTime - startTime)
    }
    
    // 验证平均帧时间 < 16.67ms
    val avgFrameTime = frameTimings.average() / 1_000_000.0
    assertTrue(avgFrameTime < 16.67, "Average frame time: $avgFrameTime ms")
}
```

## 实现优先级

### Phase 1: 核心修复 (高优先级)
1. 修改 `PageGestureHandler.handleDragUpdate` - 实现实时跟随
2. 修改 `PageTransitionContainer` - 修正方向映射
3. 修改 `PageViewWithTransition` - 集成实时更新
4. 测试基本翻页功能

### Phase 2: 边界处理 (中优先级)
1. 实现边界检测逻辑
2. 添加边界阻力效果
3. 添加边界视觉反馈
4. 测试边界情况

### Phase 3: 动画优化 (中优先级)
1. 实现 ease-out 动画曲线
2. 优化完成/取消动画
3. 添加快速滑动检测
4. 性能测试和优化

### Phase 4: 高级功能 (低优先级)
1. 多点触控取消
2. 触觉反馈
3. 无障碍支持
4. 完整测试覆盖

## 依赖关系

### 现有组件
- `PageTransitionController`: 需要修改以支持实时更新
- `PageGestureHandler`: 需要重构手势处理逻辑
- `PageTransitionContainer`: 需要修改渲染逻辑
- `TransitionState`: 需要添加 `dragDistance` 字段

### 新增依赖
- 无新增外部依赖
- 使用现有的 Compose Animation API
- 使用现有的 Compose Gesture API

## 配置更新

### TransitionConfig 扩展

```kotlin
data class TransitionConfig(
    val mode: TransitionMode = TransitionMode.Slide,
    val duration: Int = 300,
    val edgeSensitivity: EdgeSensitivity = EdgeSensitivity.MEDIUM,
    val enableShadow: Boolean = true,
    val enableHaptic: Boolean = true,
    val threshold: Float = 0.3f,
    val boundaryResistance: Float = 20f,  // 新增：边界阻力
    val enableBoundaryFeedback: Boolean = true  // 新增：边界反馈
)
```

## 总结

本设计通过以下关键修改实现自然翻页交互：

1. **实时跟随**: 在 `onDrag` 中实时更新页面位置，而不是等到 `onDragEnd`
2. **方向一致**: 修正方向映射逻辑，确保滑动方向与页面移动方向一致
3. **堆叠效果**: 使用 z-index 和阴影明确层级关系
4. **流畅动画**: 使用 ease-out 曲线和适当的动画时长
5. **边界处理**: 在第一页和最后一页提供正确的反馈

所有修改都基于现有架构，最小化代码变更，同时保持向后兼容性。
