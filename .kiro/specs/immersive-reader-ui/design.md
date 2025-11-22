# 沉浸式阅读界面优化设计文档

## 概述

本设计文档描述了如何在现有的阅读器系统中实现沉浸式阅读体验。通过将屏幕划分为9个触摸区域，提供直观的翻页和工具栏控制，同时保持与现有手势系统的兼容性。

## 架构

### 系统组件关系

```
ReaderScreen (UI层)
    ├── TouchZoneDetector (新增)
    │   ├── 检测触摸位置
    │   ├── 映射到功能区域
    │   └── 处理手势优先级
    │
    ├── ReaderViewModel (现有)
    │   ├── 管理UI状态
    │   ├── 处理翻页逻辑
    │   └── 控制工具栏可见性
    │
    └── PageView/DualPageView (现有)
        ├── 显示页面内容
        ├── 处理缩放/平移
        └── 集成触摸区域检测
```

### 数据流

```
用户触摸屏幕
    ↓
TouchZoneDetector 检测位置
    ↓
判断手势类型（tap/swipe/zoom）
    ↓
根据优先级处理
    ├── 缩放手势 → 直接处理
    ├── 滑动手势 → 翻页
    └── 点击手势 → 检查触摸区域
        ├── 中间区域 → 切换工具栏
        └── 周边区域 → 翻页导航
```

## 组件和接口

### 1. TouchZone 数据类

定义触摸区域的枚举和配置。

```kotlin
/**
 * 触摸区域枚举
 */
enum class TouchZone {
    TOP_LEFT,       // 上一页（左上）
    TOP_CENTER,     // 上一页（上中）
    TOP_RIGHT,      // 下一页（右上）
    
    MIDDLE_LEFT,    // 上一页（左中）
    CENTER,         // 切换工具栏（中心）
    MIDDLE_RIGHT,   // 下一页（右中）
    
    BOTTOM_LEFT,    // 上一页（左下）
    BOTTOM_CENTER,  // 下一页（下中）
    BOTTOM_RIGHT;   // 下一页（右下）
    
    /**
     * 根据阅读方向判断是否为"下一页"区域
     */
    fun isNextPage(readingDirection: ReadingDirection): Boolean {
        return when (readingDirection) {
            ReadingDirection.LEFT_TO_RIGHT -> this in listOf(
                TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT, BOTTOM_CENTER
            )
            ReadingDirection.RIGHT_TO_LEFT -> this in listOf(
                TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT, BOTTOM_CENTER
            )
            ReadingDirection.VERTICAL -> this in listOf(
                BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
            )
        }
    }
    
    /**
     * 根据阅读方向判断是否为"上一页"区域
     */
    fun isPreviousPage(readingDirection: ReadingDirection): Boolean {
        return when (readingDirection) {
            ReadingDirection.LEFT_TO_RIGHT -> this in listOf(
                TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT, TOP_CENTER
            )
            ReadingDirection.RIGHT_TO_LEFT -> this in listOf(
                TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT, TOP_CENTER
            )
            ReadingDirection.VERTICAL -> this in listOf(
                TOP_LEFT, TOP_CENTER, TOP_RIGHT
            )
        }
    }
    
    /**
     * 是否为中心区域
     */
    fun isCenter(): Boolean = this == CENTER
}

/**
 * 触摸区域配置
 */
data class TouchZoneConfig(
    val enabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val debugVisualization: Boolean = false
)
```

### 2. TouchZoneDetector 类

检测和处理触摸区域。

```kotlin
/**
 * 触摸区域检测器
 */
class TouchZoneDetector(
    private val config: TouchZoneConfig = TouchZoneConfig()
) {
    /**
     * 根据触摸位置和屏幕尺寸确定触摸区域
     */
    fun detectZone(
        offset: Offset,
        screenWidth: Float,
        screenHeight: Float
    ): TouchZone {
        val x = offset.x
        val y = offset.y
        
        // 将屏幕划分为3x3网格
        val columnWidth = screenWidth / 3f
        val rowHeight = screenHeight / 3f
        
        val column = when {
            x < columnWidth -> 0
            x < columnWidth * 2 -> 1
            else -> 2
        }
        
        val row = when {
            y < rowHeight -> 0
            y < rowHeight * 2 -> 1
            else -> 2
        }
        
        return when (row * 3 + column) {
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
    
    /**
     * 获取区域边界（用于调试可视化）
     */
    fun getZoneBounds(
        zone: TouchZone,
        screenWidth: Float,
        screenHeight: Float
    ): androidx.compose.ui.geometry.Rect {
        val columnWidth = screenWidth / 3f
        val rowHeight = screenHeight / 3f
        
        val (row, column) = when (zone) {
            TouchZone.TOP_LEFT -> Pair(0, 0)
            TouchZone.TOP_CENTER -> Pair(0, 1)
            TouchZone.TOP_RIGHT -> Pair(0, 2)
            TouchZone.MIDDLE_LEFT -> Pair(1, 0)
            TouchZone.CENTER -> Pair(1, 1)
            TouchZone.MIDDLE_RIGHT -> Pair(1, 2)
            TouchZone.BOTTOM_LEFT -> Pair(2, 0)
            TouchZone.BOTTOM_CENTER -> Pair(2, 1)
            TouchZone.BOTTOM_RIGHT -> Pair(2, 2)
        }
        
        return androidx.compose.ui.geometry.Rect(
            left = column * columnWidth,
            top = row * rowHeight,
            right = (column + 1) * columnWidth,
            bottom = (row + 1) * rowHeight
        )
    }
}
```

### 3. TouchZoneDebugOverlay 组件

调试可视化覆盖层。

```kotlin
/**
 * 触摸区域调试覆盖层
 */
@Composable
fun TouchZoneDebugOverlay(
    detector: TouchZoneDetector,
    readingDirection: ReadingDirection,
    lastTappedZone: TouchZone?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            TouchZone.values().forEach { zone ->
                val bounds = detector.getZoneBounds(zone, screenWidth, screenHeight)
                
                // 绘制区域边界
                drawRect(
                    color = if (zone == lastTappedZone) {
                        Color.Yellow.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.1f)
                    },
                    topLeft = Offset(bounds.left, bounds.top),
                    size = Size(bounds.width, bounds.height),
                    style = Stroke(width = 2f)
                )
                
                // 绘制区域标签
                val label = when {
                    zone.isCenter() -> "显示/隐藏"
                    zone.isNextPage(readingDirection) -> "下一页"
                    zone.isPreviousPage(readingDirection) -> "上一页"
                    else -> ""
                }
                
                // 标签文本（简化版，实际需要使用 drawText）
                // 这里仅作示意，实际实现需要使用 TextMeasurer
            }
        }
    }
}
```

### 4. ReaderViewModel 扩展

添加触摸区域相关的状态管理。

```kotlin
// 在 ReaderUiState 中添加
data class ReaderUiState(
    // ... 现有字段
    val isUiVisible: Boolean = false,  // 修改默认值为 false（默认隐藏）
    val touchZoneConfig: TouchZoneConfig = TouchZoneConfig(),
    val lastTappedZone: TouchZone? = null
)

// 在 ReaderViewModel 中添加方法
class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    // ... 现有代码
    
    /**
     * 处理触摸区域点击
     */
    fun handleTouchZone(
        zone: TouchZone,
        readingDirection: ReadingDirection,
        isDoublePageMode: Boolean
    ) {
        _uiState.update { it.copy(lastTappedZone = zone) }
        
        when {
            zone.isCenter() -> {
                // 切换工具栏可见性
                toggleUiVisibility()
            }
            zone.isNextPage(readingDirection) -> {
                // 下一页
                if (isDoublePageMode) {
                    val currentPage = _uiState.value.currentPage
                    val book = currentBook.value ?: return
                    goToPage((currentPage + 2).coerceAtMost(book.totalPages - 1))
                } else {
                    nextPage()
                }
            }
            zone.isPreviousPage(readingDirection) -> {
                // 上一页
                if (isDoublePageMode) {
                    val currentPage = _uiState.value.currentPage
                    goToPage((currentPage - 2).coerceAtLeast(0))
                } else {
                    previousPage()
                }
            }
        }
    }
    
    /**
     * 更新触摸区域配置
     */
    fun updateTouchZoneConfig(config: TouchZoneConfig) {
        _uiState.update { it.copy(touchZoneConfig = config) }
    }
}
```

### 5. PageView 修改

集成触摸区域检测到现有的 PageView 组件。

```kotlin
@Composable
fun PageView(
    bitmap: android.graphics.Bitmap,
    scale: Float,
    offset: Offset,
    readingDirection: ReadingDirection,
    touchZoneConfig: TouchZoneConfig,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onTouchZone: (TouchZone) -> Unit,  // 新增
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val detector = remember { TouchZoneDetector(touchZoneConfig) }
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    
    BoxWithConstraints(modifier = modifier) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        
        val state = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(0.5f, 3f)
            onScaleChange(newScale)
            if (newScale > 1f) {
                onOffsetChange(offset + panChange)
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = state)
                .pointerInput(scale, readingDirection, touchZoneConfig) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // 只在未缩放时处理触摸区域
                            if (scale <= 1f && touchZoneConfig.enabled) {
                                val zone = detector.detectZone(
                                    tapOffset,
                                    screenWidth,
                                    screenHeight
                                )
                                
                                // 触觉反馈
                                if (touchZoneConfig.hapticFeedback) {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                }
                                
                                onTouchZone(zone)
                            }
                        },
                        onDoubleTap = {
                            // 双击缩放
                            if (scale > 1f) {
                                onScaleChange(1f)
                                onOffsetChange(Offset.Zero)
                            } else {
                                onScaleChange(2f)
                            }
                        }
                    )
                }
                .pointerInput(readingDirection, scale) {
                    // 水平滑动 - 只在未缩放时才允许翻页手势
                    if (readingDirection != ReadingDirection.VERTICAL && scale <= 1f) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 50) {
                                onSwipeRight()
                            } else if (dragAmount < -50) {
                                onSwipeLeft()
                            }
                        }
                    }
                }
                .pointerInput(readingDirection, scale) {
                    // 垂直滑动
                    if (readingDirection == ReadingDirection.VERTICAL && scale <= 1f) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 50) {
                                onSwipeDown()
                            } else if (dragAmount < -50) {
                                onSwipeUp()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
            
            // 调试覆盖层
            if (touchZoneConfig.debugVisualization) {
                TouchZoneDebugOverlay(
                    detector = detector,
                    readingDirection = readingDirection,
                    lastTappedZone = null  // 从 ViewModel 获取
                )
            }
        }
    }
}
```

### 6. ReaderScreen 修改

更新 ReaderScreen 以使用新的触摸区域功能。

```kotlin
@Composable
fun ReaderScreen(
    bookId: Long,
    initialPage: Int = -1,
    onBackClick: () -> Unit,
    onBookmarksClick: (String) -> Unit = {},
    viewModel: ReaderViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    // ... 现有状态
    
    // 修改默认 UI 可见性
    LaunchedEffect(bookId) {
        viewModel.openBook(bookId)
        // 打开书籍时默认隐藏工具栏
        if (uiState.isUiVisible) {
            viewModel.toggleUiVisibility()
        }
    }
    
    Scaffold(
        topBar = {
            // 使用 AnimatedVisibility 实现平滑过渡
            AnimatedVisibility(
                visible = uiState.isUiVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TopAppBar(/* ... */)
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.isUiVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                BottomAppBar(/* ... */)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    // 只在工具栏可见时应用 padding
                    if (uiState.isUiVisible) paddingValues else PaddingValues(0.dp)
                )
        ) {
            when {
                // ... 现有逻辑
                pageBitmap != null -> {
                    if (effectiveReadingMode == ReadingMode.DOUBLE_PAGE && secondPageBitmap != null) {
                        DualPageView(
                            firstBitmap = pageBitmap!!,
                            secondBitmap = secondPageBitmap!!,
                            scale = scale,
                            offset = offset,
                            readingDirection = settings.readingDirection,
                            touchZoneConfig = uiState.touchZoneConfig,
                            onScaleChange = { scale = it },
                            onOffsetChange = { offset = it },
                            onTouchZone = { zone ->
                                viewModel.handleTouchZone(
                                    zone,
                                    settings.readingDirection,
                                    isDoublePageMode = true
                                )
                            },
                            // ... 其他参数
                        )
                    } else {
                        PageView(
                            bitmap = pageBitmap!!,
                            scale = scale,
                            offset = offset,
                            readingDirection = settings.readingDirection,
                            touchZoneConfig = uiState.touchZoneConfig,
                            onScaleChange = { scale = it },
                            onOffsetChange = { offset = it },
                            onTouchZone = { zone ->
                                viewModel.handleTouchZone(
                                    zone,
                                    settings.readingDirection,
                                    isDoublePageMode = false
                                )
                            },
                            // ... 其他参数
                        )
                    }
                }
            }
        }
    }
}
```

## 数据模型

### AppSettings 扩展

在 AppSettings 中添加触摸区域配置。

```kotlin
data class AppSettings(
    // ... 现有字段
    
    // 触摸区域设置
    val touchZoneEnabled: Boolean = true,
    val touchZoneHapticFeedback: Boolean = true,
    val touchZoneDebugMode: Boolean = false
)
```

## 错误处理

### 触摸检测错误

1. **边界情况**: 确保触摸坐标在屏幕范围内
2. **并发手势**: 使用手势优先级系统避免冲突
3. **性能问题**: 触摸检测应在 UI 线程完成，避免阻塞

### 状态同步错误

1. **工具栏状态**: 确保工具栏可见性状态在配置变更后保持
2. **触摸区域映射**: 阅读方向改变时重新映射触摸区域功能

## 测试策略

### 单元测试

1. **TouchZoneDetector 测试**
   - 测试各个位置的区域检测准确性
   - 测试边界条件
   - 测试不同屏幕尺寸

2. **TouchZone 逻辑测试**
   - 测试不同阅读方向下的区域映射
   - 测试 isNextPage/isPreviousPage 逻辑

### UI 测试

1. **触摸区域交互测试**
   - 测试点击各个区域的响应
   - 测试工具栏切换动画
   - 测试翻页功能

2. **手势优先级测试**
   - 测试滑动手势优先于点击
   - 测试缩放时禁用触摸区域
   - 测试双击缩放功能

### 集成测试

1. **完整阅读流程测试**
   - 打开书籍 → 默认隐藏工具栏
   - 点击中间 → 显示工具栏
   - 点击周边 → 翻页
   - 双击 → 缩放

2. **配置变更测试**
   - 旋转屏幕后触摸区域仍然正常
   - 切换阅读方向后区域映射正确

## 性能考虑

### 触摸检测性能

- 触摸区域计算使用简单的数学运算，性能开销可忽略
- 避免在触摸事件中进行复杂计算
- 使用 remember 缓存 TouchZoneDetector 实例

### 动画性能

- 使用 AnimatedVisibility 实现流畅的工具栏过渡
- 动画时长控制在 300ms 以内
- 避免在动画期间进行重量级操作

### 内存影响

- TouchZoneDetector 是轻量级对象，内存占用极小
- 调试覆盖层仅在开发模式启用
- 不影响现有的图片缓存和内存管理

## 可访问性

### 触觉反馈

- 触摸区域激活时提供触觉反馈
- 可通过设置禁用触觉反馈

### 视觉反馈

- 调试模式下提供视觉边界
- 工具栏切换使用平滑动画

### 兼容性

- 保留现有的音量键翻页功能
- 保留现有的滑动手势
- 不影响辅助功能服务

## 向后兼容性

- 触摸区域功能可通过设置完全禁用
- 禁用后行为与原版本完全一致
- 现有用户数据和设置不受影响
- 所有现有手势功能保持不变
