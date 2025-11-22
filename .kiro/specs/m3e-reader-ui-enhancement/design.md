# Design Document - M3E Reader UI Enhancement

## Overview

本设计文档描述了如何使用 Material 3 Expressive (M3E) 设计风格完善 Paysage 阅读页面的技术方案。该方案的核心是学习 Legado 阅读器的优秀交互设计，特别是其翻页动画系统和九宫格触摸区域，并结合 M3E 的现代化设计语言，为用户提供流畅、愉悦、富有表现力的阅读体验。

### 设计目标

1. **学习 Legado 翻页动画** - 深入研究并实现 Legado 的多种翻页动画效果
2. **九宫格触摸系统** - 实现灵活可配置的九宫格触摸区域
3. **M3E 设计风格** - 使用 M3E 的组件、颜色和动画系统
4. **沉浸式体验** - 提供专注的阅读环境
5. **性能优化** - 确保 60fps 的流畅动画

### 核心设计决策

1. **翻页动画架构** - 参考 Legado 的 PageDelegate 模式，使用策略模式实现多种翻页效果
2. **九宫格触摸** - 使用 Compose 的 Modifier.pointerInput 实现精确的触摸区域检测
3. **M3E 组件复用** - 复用现有的 ExpressiveButton、ExpressiveFAB 等组件
4. **状态管理** - 使用 ViewModel + StateFlow 管理阅读状态
5. **Canvas 绘制** - 使用 Compose Canvas 实现复杂的翻页动画效果

## Architecture

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                  M3E Reader Screen (Compose)                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ReaderScreen                                        │  │
│  │  ├─ TopBar (M3E Style)                               │  │
│  │  ├─ ReaderContent                                    │  │
│  │  │   ├─ TouchZoneDetector (九宫格)                   │  │
│  │  │   └─ PageFlipAnimationLayer                       │  │
│  │  ├─ BottomBar (M3E Style)                            │  │
│  │  ├─ QuickSettingsPanel                               │  │
│  │  └─ ReadingSettingsDialog                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                     ViewModel Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ReaderViewModel                                     │  │
│  │  ├─ UI State Management                              │  │
│  │  ├─ Touch Zone Configuration                         │  │
│  │  ├─ Page Flip Mode Selection                         │  │
│  │  └─ Reading Progress Tracking                        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│              Page Flip Animation System                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  PageFlipAnimator (Strategy Pattern)                │  │
│  │  ├─ SimulationPageFlip (仿真翻页)                    │  │
│  │  ├─ CoverPageFlip (覆盖翻页)                         │  │
│  │  ├─ SlidePageFlip (滑动翻页)                         │  │
│  │  ├─ ScrollPageFlip (滚动翻页)                        │  │
│  │  └─ NonePageFlip (无动画)                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                Touch Zone Detection System                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  TouchZoneDetector                                   │  │
│  │  ├─ 九宫格区域划分                                    │  │
│  │  ├─ 触摸事件分发                                      │  │
│  │  └─ 动作配置管理                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. ReaderScreen (Main Composable)

主阅读界面屏幕组件，采用 M3E 设计风格。

**接口**:
```kotlin
@Composable
fun ReaderScreen(
    bookId: String,
    viewModel: ReaderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
)
```

**状态**:
```kotlin
data class ReaderUiState(
    val book: Book? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val chapterTitle: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 工具栏状态
    val showTopBar: Boolean = false,
    val showBottomBar: Boolean = false,
    val showQuickSettings: Boolean = false,
    val showReadingSettings: Boolean = false,
    
    // 阅读配置
    val config: ReaderConfig = ReaderConfig(),
    
    // 翻页动画
    val pageFlipMode: PageFlipMode = PageFlipMode.SIMULATION,
    val isFlipping: Boolean = false,
    
    // 触摸区域
    val touchZoneConfig: TouchZoneConfig = TouchZoneConfig.default(),
    val showTouchZoneOverlay: Boolean = false
)
```


### 2. TouchZoneDetector (九宫格触摸检测)

实现九宫格触摸区域检测和动作分发。

**数据模型**:
```kotlin
/**
 * 触摸区域枚举
 */
enum class TouchZone {
    TOP_LEFT,       // 左上
    TOP_CENTER,     // 上中
    TOP_RIGHT,      // 右上
    MIDDLE_LEFT,    // 左中
    CENTER,         // 中心
    MIDDLE_RIGHT,   // 右中
    BOTTOM_LEFT,    // 左下
    BOTTOM_CENTER,  // 下中
    BOTTOM_RIGHT    // 右下
}

/**
 * 触摸动作枚举
 */
enum class TouchAction {
    PREVIOUS_PAGE,      // 上一页
    NEXT_PAGE,          // 下一页
    TOGGLE_TOOLBAR,     // 切换工具栏
    SHOW_MENU,          // 显示菜单
    BOOKMARK,           // 添加书签
    NONE                // 无动作
}

/**
 * 触摸区域配置
 */
data class TouchZoneConfig(
    val zoneActions: Map<TouchZone, TouchAction> = mapOf(
        TouchZone.TOP_LEFT to TouchAction.PREVIOUS_PAGE,
        TouchZone.TOP_CENTER to TouchAction.PREVIOUS_PAGE,
        TouchZone.TOP_RIGHT to TouchAction.NEXT_PAGE,
        TouchZone.MIDDLE_LEFT to TouchAction.PREVIOUS_PAGE,
        TouchZone.CENTER to TouchAction.TOGGLE_TOOLBAR,
        TouchZone.MIDDLE_RIGHT to TouchAction.NEXT_PAGE,
        TouchZone.BOTTOM_LEFT to TouchAction.PREVIOUS_PAGE,
        TouchZone.BOTTOM_CENTER to TouchAction.NEXT_PAGE,
        TouchZone.BOTTOM_RIGHT to TouchAction.NEXT_PAGE
    )
) {
    companion object {
        fun default() = TouchZoneConfig()
    }
}
```

**实现**:
```kotlin
@Composable
fun TouchZoneDetector(
    modifier: Modifier = Modifier,
    config: TouchZoneConfig,
    onZoneTapped: (TouchZone, TouchAction) -> Unit,
    showOverlay: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(config) {
                detectTapGestures { offset ->
                    val zone = detectTouchZone(offset, size)
                    val action = config.zoneActions[zone] ?: TouchAction.NONE
                    onZoneTapped(zone, action)
                }
            }
    ) {
        content()
        
        // 显示九宫格覆盖层（用于调试和配置）
        if (showOverlay) {
            TouchZoneOverlay(config = config)
        }
    }
}

/**
 * 检测触摸点所在的区域
 */
private fun detectTouchZone(offset: Offset, size: IntSize): TouchZone {
    val x = offset.x
    val y = offset.y
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    
    val col = when {
        x < width / 3 -> 0  // 左
        x < width * 2 / 3 -> 1  // 中
        else -> 2  // 右
    }
    
    val row = when {
        y < height / 3 -> 0  // 上
        y < height * 2 / 3 -> 1  // 中
        else -> 2  // 下
    }
    
    return when (row * 3 + col) {
        0 -> TouchZone.TOP_LEFT
        1 -> TouchZone.TOP_CENTER
        2 -> TouchZone.TOP_RIGHT
        3 -> TouchZone.MIDDLE_LEFT
        4 -> TouchZone.CENTER
        5 -> TouchZone.MIDDLE_RIGHT
        6 -> TouchZone.BOTTOM_LEFT
        7 -> TouchZone.BOTTOM_CENTER
        8 -> TouchZone.BOTTOM_RIGHT
        else -> TouchZone.CENTER
    }
}
```

**九宫格覆盖层**:
```kotlin
@Composable
fun TouchZoneOverlay(
    config: TouchZoneConfig,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // 绘制九宫格线条
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(width / 3, 0f),
            end = Offset(width / 3, height),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(width * 2 / 3, 0f),
            end = Offset(width * 2 / 3, height),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height / 3),
            end = Offset(width, height / 3),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height * 2 / 3),
            end = Offset(width, height * 2 / 3),
            strokeWidth = 2f
        )
        
        // 在每个区域中心显示动作文本
        // （实现省略，使用 drawText 或 Text composable）
    }
}
```


### 3. PageFlipAnimator (翻页动画系统 - 核心)

学习 Legado 的 PageDelegate 架构，使用策略模式实现多种翻页动画。

**翻页模式枚举**:
```kotlin
enum class PageFlipMode {
    SIMULATION,  // 仿真翻页
    COVER,       // 覆盖翻页
    SLIDE,       // 滑动翻页
    SCROLL,      // 滚动翻页
    NONE         // 无动画
}
```

**翻页动画接口**:
```kotlin
/**
 * 翻页动画器接口
 * 参考 Legado 的 PageDelegate 设计
 */
interface PageFlipAnimator {
    /**
     * 开始翻页动画
     * @param direction 翻页方向（NEXT 或 PREVIOUS）
     * @param startOffset 起始触摸点（用于手势跟随）
     * @param onComplete 动画完成回调
     */
    fun startFlip(
        direction: FlipDirection,
        startOffset: Offset? = null,
        onComplete: () -> Unit
    )
    
    /**
     * 更新手势位置（用于拖动翻页）
     * @param offset 当前触摸点
     */
    fun updateGesture(offset: Offset)
    
    /**
     * 取消翻页动画
     */
    fun cancelFlip()
    
    /**
     * 绘制翻页动画帧
     * @param drawScope Canvas 绘制作用域
     * @param currentPage 当前页面内容
     * @param nextPage 下一页内容
     * @param progress 动画进度 [0, 1]
     */
    fun DrawScope.drawFlipFrame(
        currentPage: ImageBitmap,
        nextPage: ImageBitmap,
        progress: Float
    )
}

enum class FlipDirection {
    NEXT,      // 下一页
    PREVIOUS   // 上一页
}
```

**仿真翻页实现（SimulationPageFlip）**:
```kotlin
/**
 * 仿真翻页动画
 * 模拟真实书籍的翻页效果，包括页面卷曲、阴影和光照
 * 
 * 核心算法：
 * 1. 使用贝塞尔曲线计算页面卷曲路径
 * 2. 计算卷曲区域的变换矩阵
 * 3. 绘制阴影渐变
 * 4. 绘制光照效果
 */
class SimulationPageFlip : PageFlipAnimator {
    
    private var touchPoint = Offset.Zero
    private var cornerPoint = Offset.Zero
    private val path = Path()
    
    override fun DrawScope.drawFlipFrame(
        currentPage: ImageBitmap,
        nextPage: ImageBitmap,
        progress: Float
    ) {
        // 1. 绘制下一页（背景）
        drawImage(nextPage)
        
        // 2. 计算卷曲路径
        calculateCurlPath(progress)
        
        // 3. 绘制卷曲页面的背面
        drawCurlBackSide()
        
        // 4. 绘制卷曲页面的正面
        drawCurlFrontSide(currentPage)
        
        // 5. 绘制阴影
        drawCurlShadow()
        
        // 6. 绘制光照效果
        drawCurlHighlight()
    }
    
    /**
     * 计算贝塞尔曲线卷曲路径
     * 参考 Legado 的实现
     */
    private fun calculateCurlPath(progress: Float) {
        // 计算触摸点和角点
        // 使用二次贝塞尔曲线绘制卷曲边缘
        // （详细实现参考 Legado SimulationPageDelegate）
    }
    
    /**
     * 绘制卷曲页面的背面
     */
    private fun DrawScope.drawCurlBackSide() {
        // 使用渐变色模拟纸张背面
        // 应用变换矩阵实现透视效果
    }
    
    /**
     * 绘制卷曲页面的正面
     */
    private fun DrawScope.drawCurlFrontSide(page: ImageBitmap) {
        // 裁剪并绘制卷曲区域的页面内容
        // 应用变换矩阵
    }
    
    /**
     * 绘制阴影渐变
     */
    private fun DrawScope.drawCurlShadow() {
        // 在卷曲边缘绘制阴影
        // 使用径向渐变实现柔和效果
    }
    
    /**
     * 绘制光照效果
     */
    private fun DrawScope.drawCurlHighlight() {
        // 在卷曲区域绘制高光
        // 模拟光线照射效果
    }
}
```

**覆盖翻页实现（CoverPageFlip）**:
```kotlin
/**
 * 覆盖翻页动画
 * 新页面从右侧覆盖旧页面
 */
class CoverPageFlip : PageFlipAnimator {
    
    override fun DrawScope.drawFlipFrame(
        currentPage: ImageBitmap,
        nextPage: ImageBitmap,
        progress: Float
    ) {
        // 1. 绘制当前页（背景）
        drawImage(currentPage)
        
        // 2. 计算新页面的位置
        val offsetX = size.width * (1 - progress)
        
        // 3. 绘制新页面（从右侧滑入）
        translate(left = offsetX) {
            drawImage(nextPage)
        }
        
        // 4. 绘制阴影（增强层次感）
        drawRect(
            color = Color.Black.copy(alpha = 0.3f * progress),
            topLeft = Offset(offsetX - 10f, 0f),
            size = Size(10f, size.height)
        )
    }
}
```

**滑动翻页实现（SlidePageFlip）**:
```kotlin
/**
 * 滑动翻页动画
 * 当前页和下一页同时左右平移
 */
class SlidePageFlip : PageFlipAnimator {
    
    override fun DrawScope.drawFlipFrame(
        currentPage: ImageBitmap,
        nextPage: ImageBitmap,
        progress: Float
    ) {
        val offsetX = -size.width * progress
        
        // 1. 绘制当前页（向左滑出）
        translate(left = offsetX) {
            drawImage(currentPage)
        }
        
        // 2. 绘制下一页（从右侧滑入）
        translate(left = offsetX + size.width) {
            drawImage(nextPage)
        }
    }
}
```

**滚动翻页实现（ScrollPageFlip）**:
```kotlin
/**
 * 滚动翻页动画
 * 垂直连续滚动
 */
class ScrollPageFlip : PageFlipAnimator {
    
    override fun DrawScope.drawFlipFrame(
        currentPage: ImageBitmap,
        nextPage: ImageBitmap,
        progress: Float
    ) {
        val offsetY = -size.height * progress
        
        // 1. 绘制当前页（向上滚动）
        translate(top = offsetY) {
            drawImage(currentPage)
        }
        
        // 2. 绘制下一页（从下方进入）
        translate(top = offsetY + size.height) {
            drawImage(nextPage)
        }
    }
}
```

**翻页动画管理器**:
```kotlin
/**
 * 翻页动画管理器
 * 负责创建和管理不同的翻页动画器
 */
class PageFlipAnimationManager {
    
    private var currentAnimator: PageFlipAnimator? = null
    
    /**
     * 根据模式创建动画器
     */
    fun createAnimator(mode: PageFlipMode): PageFlipAnimator {
        return when (mode) {
            PageFlipMode.SIMULATION -> SimulationPageFlip()
            PageFlipMode.COVER -> CoverPageFlip()
            PageFlipMode.SLIDE -> SlidePageFlip()
            PageFlipMode.SCROLL -> ScrollPageFlip()
            PageFlipMode.NONE -> NonePageFlip()
        }.also {
            currentAnimator = it
        }
    }
    
    /**
     * 执行翻页
     */
    suspend fun performFlip(
        direction: FlipDirection,
        currentPage: ImageBitmap,
        nextPage: ImageBitmap,
        onProgress: (Float) -> Unit
    ) {
        val animator = currentAnimator ?: return
        
        // 使用 Animatable 控制动画进度
        val progress = Animatable(0f)
        
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) {
            onProgress(value)
        }
    }
}
```


### 4. M3E 风格的阅读界面组件

**顶部工具栏**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            ExpressiveIconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        modifier = modifier
    )
}
```

**底部控制栏**:
```kotlin
@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上一页按钮
            ExpressiveIconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 0
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "上一页"
                )
            }
            
            // 进度滑块
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { onPageChange(it.toInt()) },
                    valueRange = 0f..maxOf(0f, (totalPages - 1).toFloat()),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 下一页按钮
            ExpressiveIconButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages - 1
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "下一页"
                )
            }
        }
    }
}
```

**快速设置 FAB**:
```kotlin
@Composable
fun ReaderQuickSettingsFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveFAB(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            Icons.Default.Tune,
            contentDescription = "快速设置"
        )
    }
}
```

### 5. 阅读内容渲染

**ReaderContent Composable**:
```kotlin
@Composable
fun ReaderContent(
    currentPage: ImageBitmap?,
    nextPage: ImageBitmap?,
    previousPage: ImageBitmap?,
    pageFlipMode: PageFlipMode,
    touchZoneConfig: TouchZoneConfig,
    onZoneTapped: (TouchZone, TouchAction) -> Unit,
    onSwipe: (FlipDirection) -> Unit,
    showTouchZoneOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    val animationManager = remember { PageFlipAnimationManager() }
    var isFlipping by remember { mutableStateOf(false) }
    var flipProgress by remember { mutableStateOf(0f) }
    
    TouchZoneDetector(
        modifier = modifier,
        config = touchZoneConfig,
        onZoneTapped = onZoneTapped,
        showOverlay = showTouchZoneOverlay
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 使用 Canvas 绘制页面和翻页动画
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isFlipping && currentPage != null && nextPage != null) {
                    // 绘制翻页动画帧
                    val animator = animationManager.createAnimator(pageFlipMode)
                    with(animator) {
                        drawFlipFrame(currentPage, nextPage, flipProgress)
                    }
                } else if (currentPage != null) {
                    // 绘制静态页面
                    drawImage(currentPage)
                }
            }
            
            // 加载指示器
            if (currentPage == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
```

## Data Models

### ReaderConfig

```kotlin
data class ReaderConfig(
    // 文字设置
    val textSize: Int = 18,
    val textColor: Int = Color.Black.toArgb(),
    val lineSpacing: Float = 1.5f,
    val paragraphSpacing: Float = 1.0f,
    
    // 背景设置
    val bgColor: Int = Color.White.toArgb(),
    val bgImagePath: String = "",
    
    // 边距设置
    val paddingTop: Int = 20,
    val paddingBottom: Int = 20,
    val paddingLeft: Int = 20,
    val paddingRight: Int = 20,
    
    // 翻页设置
    val pageFlipMode: String = "SIMULATION",
    val volumeKeyNavigation: Boolean = true,
    
    // 显示设置
    val keepScreenOn: Boolean = true,
    val hideStatusBar: Boolean = true,
    val hideNavigationBar: Boolean = true,
    
    // 触摸区域
    val touchZoneEnabled: Boolean = true
)
```

### Page Data

```kotlin
/**
 * 页面数据
 */
data class PageData(
    val pageIndex: Int,
    val chapterIndex: Int,
    val content: String,
    val bitmap: ImageBitmap? = null
)

/**
 * 页面缓存
 * 维护三页缓存：上一页、当前页、下一页
 */
data class PageCache(
    val previousPage: PageData? = null,
    val currentPage: PageData? = null,
    val nextPage: PageData? = null
)
```

## Animation Specifications

### M3E 动画时长和曲线

```kotlin
object ReaderAnimations {
    // 工具栏显示/隐藏
    val toolbarDuration = 300
    val toolbarEasing = EmphasizedEasing
    
    // 翻页动画
    val pageFlipDuration = 300
    val pageFlipEasing = FastOutSlowInEasing
    
    // 快速设置面板
    val panelDuration = 400
    val panelEasing = EmphasizedDecelerateEasing
    
    // 按钮按压
    val buttonPressDuration = 100
    val buttonPressEasing = LinearEasing
    
    // 对话框
    val dialogDuration = 300
    val dialogEasing = EmphasizedEasing
}

/**
 * M3E Emphasized Easing
 * 用于重要的界面过渡
 */
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * M3E Emphasized Decelerate Easing
 * 用于进入动画
 */
val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * M3E Emphasized Accelerate Easing
 * 用于退出动画
 */
val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
```

### 工具栏动画

```kotlin
@Composable
fun AnimatedToolbar(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(
                durationMillis = ReaderAnimations.toolbarDuration,
                easing = ReaderAnimations.toolbarEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ReaderAnimations.toolbarDuration
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(
                durationMillis = ReaderAnimations.toolbarDuration,
                easing = ReaderAnimations.toolbarEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ReaderAnimations.toolbarDuration
            )
        )
    ) {
        content()
    }
}
```


## Performance Optimization

### 1. 页面预渲染和缓存

```kotlin
/**
 * 页面预渲染管理器
 * 在后台预渲染相邻页面，提高翻页响应速度
 */
class PagePreRenderer(
    private val scope: CoroutineScope
) {
    private val cache = mutableMapOf<Int, ImageBitmap>()
    private val maxCacheSize = 5
    
    /**
     * 预渲染页面
     */
    fun prerenderPage(
        pageIndex: Int,
        content: String,
        config: ReaderConfig
    ) {
        scope.launch(Dispatchers.Default) {
            val bitmap = renderPageToBitmap(content, config)
            cache[pageIndex] = bitmap
            
            // 限制缓存大小
            if (cache.size > maxCacheSize) {
                val oldestKey = cache.keys.minOrNull()
                oldestKey?.let { cache.remove(it) }
            }
        }
    }
    
    /**
     * 获取缓存的页面
     */
    fun getCachedPage(pageIndex: Int): ImageBitmap? {
        return cache[pageIndex]
    }
    
    /**
     * 渲染页面到 Bitmap
     */
    private suspend fun renderPageToBitmap(
        content: String,
        config: ReaderConfig
    ): ImageBitmap {
        // 使用 Canvas 渲染文本到 Bitmap
        // （实现细节省略）
        return ImageBitmap(800, 1200)
    }
}
```

### 2. 动画性能优化

```kotlin
/**
 * 翻页动画性能监控
 */
class PageFlipPerformanceMonitor {
    private val frameTimeHistory = mutableListOf<Long>()
    private val maxHistorySize = 60
    
    /**
     * 记录帧时间
     */
    fun recordFrameTime(frameTime: Long) {
        frameTimeHistory.add(frameTime)
        if (frameTimeHistory.size > maxHistorySize) {
            frameTimeHistory.removeAt(0)
        }
    }
    
    /**
     * 获取平均帧率
     */
    fun getAverageFps(): Float {
        if (frameTimeHistory.isEmpty()) return 0f
        val avgFrameTime = frameTimeHistory.average()
        return (1000_000_000 / avgFrameTime).toFloat()
    }
    
    /**
     * 检查是否需要降级
     */
    fun shouldDowngrade(): Boolean {
        return getAverageFps() < 45f
    }
}

/**
 * 动画降级策略
 * 当性能不足时，自动降级到更简单的动画
 */
fun getOptimalPageFlipMode(
    preferredMode: PageFlipMode,
    performanceMonitor: PageFlipPerformanceMonitor
): PageFlipMode {
    return if (performanceMonitor.shouldDowngrade()) {
        when (preferredMode) {
            PageFlipMode.SIMULATION -> PageFlipMode.SLIDE
            PageFlipMode.COVER -> PageFlipMode.SLIDE
            else -> preferredMode
        }
    } else {
        preferredMode
    }
}
```

### 3. 内存管理

```kotlin
/**
 * Bitmap 内存池
 * 复用 Bitmap 对象，减少内存分配
 */
object BitmapPool {
    private val pool = mutableListOf<ImageBitmap>()
    private val maxPoolSize = 10
    
    /**
     * 获取 Bitmap
     */
    fun obtain(width: Int, height: Int): ImageBitmap {
        synchronized(pool) {
            val bitmap = pool.find {
                it.width == width && it.height == height
            }
            if (bitmap != null) {
                pool.remove(bitmap)
                return bitmap
            }
        }
        return ImageBitmap(width, height)
    }
    
    /**
     * 回收 Bitmap
     */
    fun recycle(bitmap: ImageBitmap) {
        synchronized(pool) {
            if (pool.size < maxPoolSize) {
                pool.add(bitmap)
            }
        }
    }
    
    /**
     * 清空池
     */
    fun clear() {
        synchronized(pool) {
            pool.clear()
        }
    }
}
```

## Testing Strategy

### 1. UI 测试

```kotlin
@Test
fun testTouchZoneDetection() {
    composeTestRule.setContent {
        TouchZoneDetector(
            config = TouchZoneConfig.default(),
            onZoneTapped = { zone, action ->
                // 验证触摸区域检测
            }
        ) {
            Box(Modifier.fillMaxSize())
        }
    }
    
    // 测试左上角
    composeTestRule.onRoot().performTouchInput {
        click(Offset(100f, 100f))
    }
    
    // 验证触发了正确的动作
}

@Test
fun testPageFlipAnimation() {
    val animator = SimulationPageFlip()
    
    // 测试动画进度
    // 验证绘制结果
}
```

### 2. 性能测试

```kotlin
@Test
fun testPageFlipPerformance() {
    val monitor = PageFlipPerformanceMonitor()
    
    repeat(60) {
        val startTime = System.nanoTime()
        // 执行翻页动画帧
        val endTime = System.nanoTime()
        monitor.recordFrameTime(endTime - startTime)
    }
    
    // 验证帧率 >= 60fps
    assertTrue(monitor.getAverageFps() >= 60f)
}
```

## Implementation Plan

### Phase 1: 基础架构 (1周)

1. 创建 ReaderScreen 基础结构
2. 实现 TouchZoneDetector 组件
3. 实现九宫格触摸区域检测
4. 创建 PageFlipAnimator 接口

### Phase 2: 翻页动画实现 (2-3周)

1. 实现 SlidePageFlip（最简单）
2. 实现 CoverPageFlip
3. 实现 SimulationPageFlip（最复杂，重点学习 Legado）
4. 实现 ScrollPageFlip
5. 实现手势跟随和拖动翻页
6. 优化动画性能

### Phase 3: M3E UI 组件 (1周)

1. 实现 ReaderTopBar
2. 实现 ReaderBottomBar
3. 实现 QuickSettingsPanel（复用现有组件）
4. 实现 ReadingSettingsDialog（复用现有组件）
5. 应用 M3E 动画和过渡

### Phase 4: 功能完善 (1周)

1. 实现页面预渲染和缓存
2. 实现阅读进度保存
3. 实现触摸区域配置
4. 实现主题切换
5. 实现无障碍支持

### Phase 5: 性能优化和测试 (1周)

1. 优化翻页动画性能
2. 实现动画降级策略
3. 编写 UI 测试
4. 编写性能测试
5. Bug 修复

**总计**: 6-7周

## Key Learnings from Legado

### 1. PageDelegate 架构

Legado 使用策略模式设计 PageDelegate 系统，每种翻页模式都是一个独立的 Delegate：

- **优点**: 易于扩展新的翻页模式，代码解耦
- **实现**: 定义统一的接口，各个 Delegate 实现具体的绘制逻辑
- **应用**: 我们的 PageFlipAnimator 接口借鉴了这个设计

### 2. 仿真翻页算法

Legado 的仿真翻页使用贝塞尔曲线计算页面卷曲：

- **核心**: 根据触摸点和角点计算贝塞尔曲线控制点
- **阴影**: 使用径向渐变绘制卷曲边缘的阴影
- **光照**: 在卷曲区域添加高光效果
- **性能**: 使用 Canvas 硬件加速，避免过度绘制

### 3. 三页缓存机制

Legado 维护三页缓存（上一页、当前页、下一页）：

- **优点**: 翻页响应快，无需等待加载
- **内存**: 限制缓存大小，及时释放不用的页面
- **预加载**: 在后台预加载相邻页面

### 4. 触摸事件处理

Legado 的触摸事件处理非常精细：

- **九宫格**: 将屏幕划分为九个区域，每个区域可配置动作
- **手势识别**: 区分点击、长按、滑动等手势
- **拖动跟随**: 翻页动画实时跟随手指移动
- **边界检测**: 在边界提供物理反馈

### 5. 性能优化技巧

- **Canvas 优化**: 使用 saveLayer 减少重绘
- **Bitmap 复用**: 使用对象池复用 Bitmap
- **异步渲染**: 在后台线程预渲染页面
- **降级策略**: 性能不足时自动降级到简单动画

## Conclusion

本设计文档提供了使用 M3E 风格完善 Paysage 阅读页面的完整技术方案。核心重点是：

1. **学习 Legado 翻页动画** - 深入研究并实现多种流畅的翻页效果
2. **九宫格触摸系统** - 提供灵活可配置的触摸区域
3. **M3E 设计风格** - 使用现代化的组件和动画
4. **性能优化** - 确保 60fps 的流畅体验

通过学习 Legado 的优秀设计并结合 M3E 的现代化风格，我们将为 Paysage 用户提供一个既美观又实用的阅读体验。
