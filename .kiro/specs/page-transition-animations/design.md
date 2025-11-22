# 页面翻转动画系统设计文档

## 概述

本设计文档描述了一个高性能、可配置的页面翻转动画系统，为阅读器应用提供多种翻页效果。系统基于 Jetpack Compose 构建，利用硬件加速实现流畅的 60fps 动画，同时通过智能预加载和内存管理确保性能优化。

### 设计目标

1. **流畅性**: 所有动画保持 60fps，无卡顿
2. **多样性**: 提供 5 种不同的翻页效果
3. **响应性**: 触摸交互延迟 < 16ms
4. **高效性**: 使用硬件加速和智能缓存
5. **可配置性**: 用户可自定义动画参数和行为

### 技术栈

- **UI 框架**: Jetpack Compose
- **动画引擎**: Compose Animation API
- **图形加速**: GraphicsLayer (GPU 加速)
- **手势检测**: Compose Gesture API
- **内存管理**: 现有的 BitmapMemoryManager
- **缓存系统**: 扩展现有的 PageCacheManager

## 架构设计

### 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                    ReaderScreen (UI)                     │
│  ┌────────────────────────────────────────────────────┐ │
│  │         PageTransitionContainer                     │ │
│  │  ┌──────────────┐  ┌──────────────┐               │ │
│  │  │ Current Page │  │  Next Page   │               │ │
│  │  └──────────────┘  └──────────────┘               │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              PageTransitionController                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │  TransitionMode  │  GestureHandler  │  Animator  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Overlay    │  │ Side-by-Side │  │     Curl     │
│   Animator   │  │   Animator   │  │   Animator   │
└──────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  ReaderViewModel                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  PagePreloader  │  CacheManager  │  MemoryMgr   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 核心组件

#### 1. PageTransitionController

负责协调所有翻页动画的核心控制器。

**职责**:
- 管理当前激活的过渡模式
- 处理手势输入并转换为动画参数
- 协调页面预加载和缓存
- 监控性能指标

**关键方法**:
```kotlin
class PageTransitionController(
    private val viewModel: ReaderViewModel,
    private val config: TransitionConfig
) {
    fun startTransition(from: Int, to: Int, mode: TransitionMode)
    fun updateTransition(progress: Float)
    fun completeTransition()
    fun cancelTransition()
}
```

#### 2. TransitionMode (密封类)

定义所有支持的过渡模式。

```kotlin
sealed class TransitionMode {
    object Overlay : TransitionMode()
    object SideBySide : TransitionMode()
    object Curl : TransitionMode()
    object Slide : TransitionMode()
    object Fade : TransitionMode()
    object None : TransitionMode()  // 无动画模式
}
```

#### 3. PageTransitionContainer (Composable)

包装页面内容并应用过渡动画的容器组件。

**职责**:
- 渲染当前页和下一页
- 应用 GraphicsLayer 变换
- 处理触摸手势
- 显示视觉反馈（阴影、深度）

#### 4. TransitionAnimator (接口)

每种过渡模式的动画实现接口。

```kotlin
interface TransitionAnimator {
    fun calculateTransform(progress: Float): PageTransform
    fun getShadowAlpha(progress: Float): Float
    fun getElevation(progress: Float): Dp
}
```

#### 5. GestureHandler

处理触摸手势并转换为过渡进度。

**职责**:
- 检测滑动手势
- 计算拖动进度
- 判断是否完成翻页
- 处理边缘灵敏度

## 数据模型

### TransitionConfig

```kotlin
data class TransitionConfig(
    val mode: TransitionMode = TransitionMode.Slide,
    val duration: Int = 300,  // 毫秒
    val edgeSensitivity: EdgeSensitivity = EdgeSensitivity.MEDIUM,
    val enableShadow: Boolean = true,
    val enableHaptic: Boolean = true,
    val threshold: Float = 0.3f  // 完成翻页的阈值
)

enum class EdgeSensitivity(val widthRatio: Float) {
    LOW(0.2f),
    MEDIUM(0.4f),
    HIGH(1.0f)
}
```

### PageTransform

```kotlin
data class PageTransform(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val rotationY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val alpha: Float = 1f,
    val shadowAlpha: Float = 0f,
    val elevation: Dp = 0.dp
)
```

### TransitionState

```kotlin
data class TransitionState(
    val isActive: Boolean = false,
    val fromPage: Int = 0,
    val toPage: Int = 0,
    val progress: Float = 0f,
    val direction: TransitionDirection = TransitionDirection.FORWARD
)

enum class TransitionDirection {
    FORWARD,
    BACKWARD
}
```

## 过渡模式详细设计

### 1. Overlay Mode (覆盖模式)

**视觉效果**: 当前页覆盖在下一页之上，向左/右滑动时逐渐揭开下一页。

**实现细节**:


```kotlin
class OverlayAnimator : TransitionAnimator {
    override fun calculateTransform(progress: Float): PageTransform {
        return PageTransform(
            translationX = progress * screenWidth,
            shadowAlpha = progress * 0.4f,
            elevation = 4.dp
        )
    }
}
```

**动画参数**:
- 当前页: translationX 从 0 到 screenWidth
- 下一页: 固定位置，无变换
- 阴影: alpha 从 0 到 0.4
- 时长: 300ms (可配置)

**手势映射**:
- 拖动距离直接映射到 translationX
- 超过 30% 屏幕宽度时自动完成
- 释放时根据速度判断方向

### 2. Side-by-Side Mode (并排模式)

**视觉效果**: 当前页和下一页并排显示，一起向左/右移动。

**实现细节**:

```kotlin
class SideBySideAnimator : TransitionAnimator {
    override fun calculateTransform(progress: Float): PageTransform {
        return PageTransform(
            translationX = progress * screenWidth,
            shadowAlpha = 0.3f  // 固定阴影
        )
    }
    
    fun calculateNextPageTransform(progress: Float): PageTransform {
        return PageTransform(
            translationX = screenWidth + progress * screenWidth
        )
    }
}
```

**动画参数**:
- 当前页: translationX 从 0 到 screenWidth
- 下一页: translationX 从 screenWidth 到 0
- 分隔线: 2dp 宽度的阴影
- 时长: 300ms

**布局**:
```
┌──────────┬──────────┐
│ Current  │   Next   │
│   Page   │   Page   │
│          │          │
└──────────┴──────────┘
```

### 3. Curl Effect (卷曲效果)

**视觉效果**: 模拟真实书页翻转，页面边缘卷曲并显示背面。

**实现细节**:

```kotlin
class CurlAnimator : TransitionAnimator {
    override fun calculateTransform(progress: Float): PageTransform {
        val angle = progress * 180f  // 0° 到 180°
        
        return PageTransform(
            rotationY = angle,
            translationX = calculateCurlOffset(angle),
            shadowAlpha = calculateCurlShadow(angle),
            elevation = 8.dp
        )
    }
    
    private fun calculateCurlOffset(angle: Float): Float {
        // 使用三角函数计算卷曲偏移
        return sin(angle * PI / 180) * screenWidth * 0.5f
    }
    
    private fun calculateCurlShadow(angle: Float): Float {
        // 阴影随角度增加
        return (angle / 180f) * 0.4f
    }
}
```

**动画参数**:
- 旋转角度: 0° 到 180°
- 渐变阴影: 模拟光照效果
- 背面亮度: 50% 降低
- 时长: 500ms (较慢以展示效果)

**性能优化**:
- 使用 GraphicsLayer 的 rotationY 实现硬件加速
- 预计算三角函数值
- 限制渲染区域

### 4. Slide Mode (滑动模式)

**视觉效果**: 简单的水平或垂直滑动切换。

**实现细节**:

```kotlin
class SlideAnimator(
    private val direction: SlideDirection
) : TransitionAnimator {
    override fun calculateTransform(progress: Float): PageTransform {
        return when (direction) {
            SlideDirection.HORIZONTAL -> PageTransform(
                translationX = progress * screenWidth,
                alpha = 1f - progress * 0.3f
            )
            SlideDirection.VERTICAL -> PageTransform(
                translationY = progress * screenHeight,
                alpha = 1f - progress * 0.3f
            )
        }
    }
}

enum class SlideDirection {
    HORIZONTAL,
    VERTICAL
}
```

**动画参数**:
- 平移距离: 0 到屏幕宽度/高度
- 透明度: 1.0 到 0.7 (可选)
- 时长: 200ms (快速)

### 5. Fade Mode (淡入淡出模式)

**视觉效果**: 当前页淡出，下一页淡入。

**实现细节**:

```kotlin
class FadeAnimator : TransitionAnimator {
    override fun calculateTransform(progress: Float): PageTransform {
        return PageTransform(
            alpha = 1f - progress
        )
    }
    
    fun calculateNextPageTransform(progress: Float): PageTransform {
        return PageTransform(
            alpha = progress
        )
    }
}
```

**动画参数**:
- 当前页 alpha: 1.0 到 0.0
- 下一页 alpha: 0.0 到 1.0
- 时长: 300ms
- 无障碍模式推荐

## 手势交互设计

### 触摸手势处理流程

```
用户触摸屏幕
    │
    ▼
检测手势类型
    │
    ├─→ 点击 (Tap)
    │   └─→ 判断触摸区域
    │       ├─→ 左侧 → 上一页
    │       ├─→ 右侧 → 下一页
    │       └─→ 中央 → 切换UI
    │
    ├─→ 滑动 (Swipe)
    │   └─→ 检测边缘灵敏度
    │       └─→ 开始过渡动画
    │
    ├─→ 拖动 (Drag)
    │   └─→ 实时更新进度
    │       └─→ 渲染中间状态
    │
    └─→ 释放 (Release)
        └─→ 判断完成条件
            ├─→ 完成翻页
            └─→ 回弹到原页
```

### GestureHandler 实现

```kotlin
class PageGestureHandler(
    private val config: TransitionConfig,
    private val onTransitionStart: (direction: TransitionDirection) -> Unit,
    private val onTransitionUpdate: (progress: Float) -> Unit,
    private val onTransitionEnd: (complete: Boolean) -> Unit
) {
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false
    
    fun handleDragStart(offset: Offset, screenSize: Size): Boolean {
        // 检查边缘灵敏度
        val edgeWidth = screenSize.width * config.edgeSensitivity.widthRatio
        
        return when {
            offset.x < edgeWidth -> {
                // 左边缘，向右滑动
                dragStartX = offset.x
                isDragging = true
                onTransitionStart(TransitionDirection.BACKWARD)
                true
            }
            offset.x > screenSize.width - edgeWidth -> {
                // 右边缘，向左滑动
                dragStartX = offset.x
                isDragging = true
                onTransitionStart(TransitionDirection.FORWARD)
                true
            }
            else -> false
        }
    }
    
    fun handleDrag(offset: Offset, screenSize: Size) {
        if (!isDragging) return
        
        val dragDistance = offset.x - dragStartX
        val progress = abs(dragDistance) / screenSize.width
        
        onTransitionUpdate(progress.coerceIn(0f, 1f))
    }
    
    fun handleDragEnd(velocity: Velocity, screenSize: Size) {
        if (!isDragging) return
        
        val currentProgress = /* 从状态获取 */
        val shouldComplete = currentProgress > config.threshold || 
                            abs(velocity.x) > 1000f
        
        onTransitionEnd(shouldComplete)
        isDragging = false
    }
}
```

## 视觉反馈设计

### 阴影效果

**实现方式**: 使用 Compose 的 `drawBehind` 或 `graphicsLayer.shadowElevation`

```kotlin
@Composable
fun PageWithShadow(
    bitmap: Bitmap,
    shadowAlpha: Float,
    elevation: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                this.shadowElevation = elevation.toPx()
            }
            .drawBehind {
                // 绘制渐变阴影
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = shadowAlpha),
                            Color.Transparent
                        )
                    )
                )
            }
    ) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null)
    }
}
```

### 深度效果

使用 Material 3 的 elevation 系统：
- 静止状态: 0dp
- 拖动中: 4dp
- 卷曲效果: 8dp

### 触觉反馈

```kotlin
val hapticFeedback = LocalHapticFeedback.current

// 开始拖动时
hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

// 完成翻页时
hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
```

## 性能优化策略

### 1. 硬件加速

**关键原则**: 只使用 GPU 友好的属性

```kotlin
Modifier.graphicsLayer {
    // ✅ GPU 加速属性
    translationX = offset
    translationY = offset
    rotationY = angle
    scaleX = scale
    scaleY = scale
    alpha = opacity
    
    // ❌ 避免使用
    // clip() - 触发软件渲染
    // drawBehind with complex paths - 性能差
}
```

### 2. 页面预加载策略

```kotlin
class TransitionPreloader(
    private val preloader: PagePreloader,
    private val cacheManager: PageCacheManager
) {
    fun preloadForTransition(
        currentPage: Int,
        direction: TransitionDirection,
        mode: TransitionMode
    ) {
        when (mode) {
            is TransitionMode.SideBySide -> {
                // 并排模式需要同时加载两页
                preloader.preloadPage(currentPage)
                preloader.preloadPage(currentPage + 1)
            }
            is TransitionMode.Overlay,
            is TransitionMode.Curl -> {
                // 覆盖和卷曲模式需要当前页和下一页
                preloader.preloadPage(currentPage)
                preloader.preloadPage(
                    if (direction == TransitionDirection.FORWARD) 
                        currentPage + 1 
                    else 
                        currentPage - 1
                )
            }
            else -> {
                // 其他模式只需要目标页
                val targetPage = if (direction == TransitionDirection.FORWARD)
                    currentPage + 1
                else
                    currentPage - 1
                preloader.preloadPage(targetPage)
            }
        }
    }
}
```

### 3. 内存管理

**策略**:
1. 过渡期间保持当前页和目标页在内存中
2. 完成过渡后立即释放旧页面
3. 监控内存压力，必要时降级动画

```kotlin
class TransitionMemoryManager(
    private val memoryManager: BitmapMemoryManager
) {
    private val transitionPages = mutableSetOf<Int>()
    
    fun lockPages(pages: List<Int>) {
        transitionPages.addAll(pages)
    }
    
    fun unlockPages() {
        transitionPages.forEach { page ->
            // 释放不再需要的页面
            memoryManager.recyclePage(page)
        }
        transitionPages.clear()
    }
    
    fun checkMemoryPressure(): Boolean {
        return memoryManager.getAvailableMemory() < 
               memoryManager.getMinRequiredMemory()
    }
}
```

### 4. 帧率监控

```kotlin
class TransitionPerformanceMonitor {
    private val frameTimings = mutableListOf<Long>()
    
    fun recordFrame(timestamp: Long) {
        frameTimings.add(timestamp)
        
        if (frameTimings.size >= 60) {
            val avgFrameTime = frameTimings.zipWithNext()
                .map { (a, b) -> b - a }
                .average()
            
            if (avgFrameTime > 16.67) {
                // 低于 60fps，触发降级
                Log.w(TAG, "Frame rate below 60fps: ${1000 / avgFrameTime}fps")
            }
            
            frameTimings.clear()
        }
    }
}
```

### 5. 动画降级策略

当检测到性能问题时自动降级：

```kotlin
fun selectOptimalTransitionMode(
    preferredMode: TransitionMode,
    performanceLevel: PerformanceLevel
): TransitionMode {
    return when (performanceLevel) {
        PerformanceLevel.HIGH -> preferredMode
        PerformanceLevel.MEDIUM -> when (preferredMode) {
            is TransitionMode.Curl -> TransitionMode.Slide
            else -> preferredMode
        }
        PerformanceLevel.LOW -> TransitionMode.Fade
    }
}
```

## 接口设计

### 用户配置接口

```kotlin
data class PageTransitionSettings(
    val mode: TransitionMode = TransitionMode.Slide,
    val speed: AnimationSpeed = AnimationSpeed.NORMAL,
    val edgeSensitivity: EdgeSensitivity = EdgeSensitivity.MEDIUM,
    val enableVisualEffects: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val reduceMotion: Boolean = false
)

enum class AnimationSpeed(val durationMs: Int) {
    FAST(200),
    NORMAL(300),
    SLOW(500)
}
```

### ViewModel 接口扩展

```kotlin
class ReaderViewModel {
    // 现有代码...
    
    private val _transitionSettings = MutableStateFlow(PageTransitionSettings())
    val transitionSettings: StateFlow<PageTransitionSettings> = 
        _transitionSettings.asStateFlow()
    
    fun updateTransitionMode(mode: TransitionMode) {
        _transitionSettings.update { it.copy(mode = mode) }
    }
    
    fun updateAnimationSpeed(speed: AnimationSpeed) {
        _transitionSettings.update { it.copy(speed = speed) }
    }
    
    fun updateEdgeSensitivity(sensitivity: EdgeSensitivity) {
        _transitionSettings.update { it.copy(edgeSensitivity = sensitivity) }
    }
}
```

## 测试策略

### 单元测试

1. **动画计算测试**
```kotlin
@Test
fun `overlay animator calculates correct transform`() {
    val animator = OverlayAnimator()
    val transform = animator.calculateTransform(0.5f)
    
    assertEquals(screenWidth * 0.5f, transform.translationX)
    assertEquals(0.2f, transform.shadowAlpha, 0.01f)
}
```

2. **手势处理测试**
```kotlin
@Test
fun `gesture handler detects edge swipe`() {
    val handler = PageGestureHandler(config)
    val result = handler.handleDragStart(
        Offset(50f, 500f),
        Size(1080f, 1920f)
    )
    
    assertTrue(result)
}
```

### 性能测试

1. **帧率测试**: 验证动画保持 60fps
2. **内存测试**: 验证内存使用在限制内
3. **加载时间测试**: 验证页面加载 < 100ms

### UI 测试

1. **手势测试**: 验证各种手势正确触发动画
2. **视觉回归测试**: 验证动画效果一致性
3. **无障碍测试**: 验证减少动画模式工作正常

## 错误处理

### 异常情况处理

1. **页面加载失败**
```kotlin
if (nextPageBitmap == null) {
    // 取消过渡，显示错误
    transitionController.cancelTransition()
    showError("无法加载下一页")
}
```

2. **内存不足**
```kotlin
try {
    startTransition()
} catch (e: OutOfMemoryError) {
    // 降级到无动画模式
    transitionSettings.update { 
        it.copy(mode = TransitionMode.None) 
    }
    memoryManager.handleLowMemory()
}
```

3. **性能降级**
```kotlin
if (frameRate < 45) {
    // 自动切换到更简单的动画
    transitionSettings.update {
        it.copy(
            mode = TransitionMode.Fade,
            enableVisualEffects = false
        )
    }
}
```

## 无障碍支持

### 减少动画模式

```kotlin
val reduceMotion = LocalAccessibilityManager.current.isReduceMotionEnabled

val effectiveMode = if (reduceMotion) {
    TransitionMode.Fade  // 简单淡入淡出
} else {
    settings.mode
}
```

### 语音提示

```kotlin
LaunchedEffect(currentPage) {
    if (talkBackEnabled) {
        announceForAccessibility("第 ${currentPage + 1} 页，共 $totalPages 页")
    }
}
```

## 实现优先级

### Phase 1: 核心功能 (MVP)
1. Slide 模式实现
2. 基础手势处理
3. 页面预加载集成

### Phase 2: 增强效果
1. Overlay 模式
2. Side-by-Side 模式
3. 视觉反馈（阴影）

### Phase 3: 高级功能
1. Curl 效果
2. Fade 模式
3. 性能监控和自动降级

### Phase 4: 优化和完善
1. 无障碍支持
2. 配置界面
3. 性能优化

## 依赖关系

### 现有组件依赖
- `ReaderViewModel`: 页面管理和状态
- `PagePreloader`: 预加载逻辑
- `PageCacheManager`: 缓存管理
- `BitmapMemoryManager`: 内存管理
- `TouchZoneDetector`: 触摸区域检测

### 新增依赖
- Compose Animation API
- Compose Gesture API
- Kotlin Coroutines (已有)

## 配置文件更新

### AppSettings 扩展

```kotlin
data class AppSettings(
    // 现有字段...
    
    // 新增翻页动画设置
    val pageTransitionMode: String = "slide",
    val animationSpeed: String = "normal",
    val edgeSensitivity: String = "medium",
    val enableTransitionEffects: Boolean = true,
    val enableTransitionHaptic: Boolean = true
)
```

### 数据库迁移

不需要数据库迁移，所有设置存储在 DataStore 中。

## 总结

本设计提供了一个完整的页面翻转动画系统，具有以下特点：

1. **模块化**: 每种动画模式独立实现，易于扩展
2. **高性能**: 使用硬件加速和智能缓存
3. **可配置**: 用户可自定义所有参数
4. **健壮性**: 完善的错误处理和降级策略
5. **无障碍**: 支持减少动画和语音提示

系统设计充分利用了现有的性能优化基础设施，只需要添加动画层即可实现所有功能。
