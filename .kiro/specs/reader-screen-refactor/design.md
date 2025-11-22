# Design Document

## Overview

本设计文档描述了 ReaderScreen 的完全重构方案。重构的核心目标是删除所有复杂的翻页动画系统，创建一个简洁、稳定、易维护的阅读器界面。新的实现将专注于核心阅读功能，使用 Jetpack Compose 的标准组件和简单的动画效果。

## Architecture

### 组件层次结构

```
ReaderScreen (主组件)
├── Scaffold
│   ├── TopAppBar (顶部工具栏)
│   │   ├── 返回按钮
│   │   ├── 书籍标题
│   │   └── 页码显示
│   ├── BottomBar (底部工具栏)
│   │   ├── 上一页按钮
│   │   ├── 进度滑块
│   │   └── 下一页按钮
│   └── Content (内容区域)
│       └── PageImageView (页面图片视图)
│           ├── 图片显示
│           ├── 缩放手势处理
│           └── 翻页手势处理
```

### 数据流

```
ReaderViewModel
    ↓ (State Flow)
ReaderScreen
    ↓ (User Actions)
ReaderViewModel
    ↓ (Update State)
ReaderScreen (Recompose)
```

## Components and Interfaces

### 1. ReaderScreen 组件

**职责：** 主阅读器界面组件

**Props:**
```kotlin
@Composable
fun ReaderScreen(
    bookId: Long,
    initialPage: Int = 0,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
)
```

**状态管理：**
```kotlin
data class ReaderUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isLoading: Boolean = false,
    val isToolbarVisible: Boolean = true,
    val error: String? = null,
    val bookTitle: String = ""
)
```

**关键功能：**
- 显示当前页面图片
- 处理工具栏显示/隐藏
- 处理翻页手势
- 显示加载状态和错误信息

### 2. PageImageView 组件

**职责：** 显示和处理单个页面图片

**Props:**
```kotlin
@Composable
fun PageImageView(
    bitmap: Bitmap?,
    onTap: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
)
```

**功能：**
- 显示图片
- 双击缩放（1.0x ↔ 2.0x）
- 双指捏合缩放（0.5x - 3.0x）
- 拖动平移（仅在缩放时）
- 左右滑动翻页
- 点击切换工具栏

### 3. ReaderViewModel 简化

**需要保留的方法：**
```kotlin
class ReaderViewModel : ViewModel() {
    // 状态
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    
    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()
    
    // 核心方法
    fun openBook(bookId: Long)
    fun goToPage(page: Int)
    fun nextPage()
    fun previousPage()
    fun toggleToolbar()
    fun cleanup()
}
```

**需要删除的方法：**
- 所有翻页动画相关方法
- TouchZone 相关方法
- PageFlip 相关方法
- 复杂的过渡效果方法

### 4. AppSettings 数据模型更新

**需要删除的字段：**
```kotlin
// 删除这些字段
val touchZoneEnabled: Boolean
val touchZoneHapticFeedback: Boolean
val touchZoneDebugMode: Boolean
val pageTransitionMode: String
val animationSpeed: String
val edgeSensitivity: String
val enableTransitionEffects: Boolean
val enableTransitionHaptic: Boolean
val pageFlipMode: String
val pageFlipAnimationSpeed: Int
```

**保留的阅读相关字段：**
```kotlin
val readingDirection: ReadingDirection
val readingMode: ReadingMode
val keepScreenOn: Boolean
val volumeKeyNavigation: Boolean
val imageFilter: ImageFilter
```

## Data Models

### ReaderUiState

```kotlin
data class ReaderUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isLoading: Boolean = false,
    val isToolbarVisible: Boolean = true,
    val error: String? = null,
    val bookTitle: String = "",
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
)
```

### 简化的 Book 模型使用

```kotlin
// 只使用必要的字段
book.id
book.title
book.totalPages
book.filePath
```

## Error Handling

### 错误类型

1. **加载错误：** 书籍文件不存在或无法读取
2. **页面错误：** 页面索引超出范围
3. **内存错误：** 图片加载失败或内存不足

### 错误处理策略

```kotlin
sealed class ReaderError {
    data class LoadError(val message: String) : ReaderError()
    data class PageError(val message: String) : ReaderError()
    data class MemoryError(val message: String) : ReaderError()
}

// 在 UI 中显示错误
@Composable
fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Error, contentDescription = null)
        Text(error)
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
```

## UI/UX Design

### 动画效果

**工具栏动画：**
```kotlin
AnimatedVisibility(
    visible = isToolbarVisible,
    enter = fadeIn() + slideInVertically { -it },
    exit = fadeOut() + slideOutVertically { -it }
)
```

**页面切换动画：**
```kotlin
AnimatedContent(
    targetState = currentPage,
    transitionSpec = {
        fadeIn(animationSpec = tween(300)) with
        fadeOut(animationSpec = tween(300))
    }
) { page ->
    PageImageView(bitmap = pageBitmap)
}
```

### 手势处理

**翻页手势：**
- 水平滑动距离 > 50dp 触发翻页
- 使用 `detectDragGestures` 检测滑动
- 滑动方向根据 `readingDirection` 设置决定

**缩放手势：**
- 双击：1.0x ↔ 2.0x
- 双指捏合：0.5x - 3.0x
- 使用 `rememberTransformableState` 处理

**点击手势：**
- 单击屏幕中央：切换工具栏
- 使用 `detectTapGestures` 检测

### 布局规则

1. **全屏模式：** 工具栏隐藏时，内容占据整个屏幕
2. **工具栏模式：** 工具栏显示时，内容区域避开工具栏
3. **图片适配：** 图片默认 fit 到屏幕，保持宽高比
4. **缩放中心：** 双击缩放以点击位置为中心

## Testing Strategy

### 单元测试

1. **ReaderViewModel 测试：**
   - 测试页面导航逻辑
   - 测试状态更新
   - 测试边界条件（第一页/最后一页）

2. **手势处理测试：**
   - 测试滑动方向判断
   - 测试缩放范围限制
   - 测试点击区域检测

### UI 测试

1. **基本交互测试：**
   - 测试工具栏显示/隐藏
   - 测试翻页按钮
   - 测试进度滑块

2. **手势测试：**
   - 测试滑动翻页
   - 测试双击缩放
   - 测试拖动平移

## Migration Strategy

### 删除文件清单

**需要完全删除的文件：**
1. `app/src/main/java/takagi/ru/paysage/ui/screens/EnhancedReaderScreen.kt`
2. `app/src/main/java/takagi/ru/paysage/ui/screen/PageFlipSettings.kt`
3. `app/src/main/java/takagi/ru/paysage/reader/pageflip/` 目录下所有文件
4. `app/src/main/java/takagi/ru/paysage/reader/transition/` 目录下所有文件
5. `app/src/main/java/takagi/ru/paysage/reader/TouchZone.kt`
6. `app/src/main/java/takagi/ru/paysage/reader/TouchZoneDetector.kt`
7. `app/src/main/java/takagi/ru/paysage/ui/components/TouchZoneDebugOverlay.kt`

**需要重写的文件：**
1. `app/src/main/java/takagi/ru/paysage/ui/screens/ReaderScreen.kt` - 完全重写
2. `app/src/main/java/takagi/ru/paysage/viewmodel/ReaderViewModel.kt` - 简化
3. `app/src/main/java/takagi/ru/paysage/data/model/AppSettings.kt` - 删除字段

### 迁移步骤

1. **Phase 1: 删除旧代码**
   - 删除所有翻页动画相关文件
   - 删除 TouchZone 相关文件
   - 清理 AppSettings 中的废弃字段

2. **Phase 2: 重写 ReaderScreen**
   - 创建新的简化版 ReaderScreen
   - 实现基本的图片显示
   - 实现工具栏显示/隐藏

3. **Phase 3: 实现交互**
   - 实现翻页手势
   - 实现缩放功能
   - 实现进度控制

4. **Phase 4: 清理和测试**
   - 删除 ViewModel 中的废弃方法
   - 更新设置界面
   - 进行完整测试

## Performance Considerations

### 内存管理

1. **Bitmap 缓存：** 只缓存当前页和下一页
2. **及时释放：** 页面切换时释放旧的 Bitmap
3. **内存监控：** 监控内存使用，必要时触发 GC

### 渲染优化

1. **避免过度重组：** 使用 `remember` 和 `derivedStateOf`
2. **懒加载：** 只在需要时加载图片
3. **动画优化：** 使用简单的淡入淡出，避免复杂动画

### 响应性

1. **异步加载：** 在协程中加载图片
2. **加载指示：** 显示加载进度
3. **错误恢复：** 提供重试机制

## Dependencies

### 保留的依赖

```kotlin
// Jetpack Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.foundation:foundation")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
```

### 可以移除的依赖

- 任何专门用于复杂翻页动画的第三方库
- TouchZone 相关的自定义库

## Future Enhancements

虽然当前重构专注于简化，但保留以下扩展点：

1. **书签功能：** 可以在工具栏添加书签按钮
2. **亮度调节：** 可以添加快速亮度调节
3. **阅读设置：** 可以添加简单的阅读设置面板
4. **双页模式：** 横屏时可以支持双页显示

这些功能可以在基础版本稳定后逐步添加。
