# Legado-Inspired Reader Enhancement Plan

## 概述

本计划旨在参考 Legado 的优秀设计，增强 Paysage 现有的阅读器功能。我们将使用独立实现，避免许可证冲突。

## Paysage 现有功能分析

### ✅ 已实现的功能
1. **基础阅读器** (`ReaderScreen.kt`)
   - Bitmap 渲染
   - 基础翻页手势
   - 图片过滤
   - 双页模式
   - 书签功能

2. **翻页动画** (`reader/pageflip/`)
   - Slide 滑动
   - Cover 覆盖
   - Simulation 仿真
   - PageDelegate 架构

3. **性能优化** (`reader/`)
   - BitmapMemoryManager
   - PageCacheManager
   - PagePreloader
   - PerformanceMonitor

4. **触摸交互** (`reader/TouchZone.kt`)
   - 九宫格触摸区域
   - 触摸区域配置
   - 调试可视化

### ❌ 缺失的关键功能（从 Legado 学习）

1. **文本内容阅读**
   - 当前只支持图片（漫画）
   - 需要添加文本排版和渲染

2. **高级阅读菜单**
   - 配置对话框较简单
   - 缺少详细的阅读设置

3. **文本选择和操作**
   - 无法选择文本
   - 无法复制、搜索

4. **自动翻页**
   - 无自动阅读功能

5. **搜索功能**
   - 无章节内搜索

## 增强计划

### Phase 1: 文本渲染基础（核心功能）

#### 1.1 创建文本内容数据模型
```kotlin
// app/src/main/java/takagi/ru/paysage/reader/text/TextContent.kt
data class TextContent(
    val text: String,
    val chapterIndex: Int,
    val chapterTitle: String
)

data class TextPage(
    val pageIndex: Int,
    val lines: List<TextLine>,
    val chapterIndex: Int
)

data class TextLine(
    val text: String,
    val y: Float,
    val baseline: Float,
    val height: Float
)
```

#### 1.2 实现简单的文本排版引擎
```kotlin
// app/src/main/java/takagi/ru/paysage/reader/text/TextLayoutEngine.kt
class TextLayoutEngine(
    private val config: ReaderConfig
) {
    fun layoutText(
        content: TextContent,
        width: Int,
        height: Int
    ): List<TextPage> {
        // 使用 Android StaticLayout 进行文本排版
        // 计算每页可容纳的行数
        // 生成 TextPage 列表
    }
}
```

#### 1.3 创建文本渲染组件
```kotlin
// app/src/main/java/takagi/ru/paysage/reader/text/TextPageRenderer.kt
class TextPageRenderer(
    private val config: ReaderConfig
) {
    fun renderPage(
        canvas: Canvas,
        page: TextPage
    ) {
        // 使用 Canvas.drawText 渲染文本
        // 应用配置（字体、颜色、间距等）
    }
}
```

### Phase 2: 增强阅读菜单

#### 2.1 创建详细的阅读设置对话框
```kotlin
// app/src/main/java/takagi/ru/paysage/ui/components/reader/ReadingSettingsDialog.kt
@Composable
fun ReadingSettingsDialog(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    // 字体设置
    // 颜色设置
    // 间距设置
    // 翻页模式设置
}
```

#### 2.2 添加快速设置面板
```kotlin
// app/src/main/java/takagi/ru/paysage/ui/components/reader/QuickSettingsPanel.kt
@Composable
fun QuickSettingsPanel(
    onBrightnessChange: (Float) -> Unit,
    onFontSizeChange: (Int) -> Unit
) {
    // 亮度快速调节
    // 字体大小快速调节
    // 翻页模式快速切换
}
```

### Phase 3: 文本选择功能

#### 3.1 实现文本选择检测
```kotlin
// app/src/main/java/takagi/ru/paysage/reader/text/TextSelectionDetector.kt
class TextSelectionDetector {
    fun detectTextAt(x: Float, y: Float, page: TextPage): TextPosition?
    fun selectRange(start: TextPosition, end: TextPosition): TextSelection
}
```

#### 3.2 创建选择手柄
```kotlin
// app/src/main/java/takagi/ru/paysage/ui/components/reader/SelectionHandles.kt
@Composable
fun SelectionHandles(
    selection: TextSelection,
    onSelectionChange: (TextSelection) -> Unit
) {
    // 绘制选择手柄
    // 处理拖动
}
```

#### 3.3 实现文本操作菜单
```kotlin
// app/src/main/java/takagi/ru/paysage/ui/components/reader/TextActionMenu.kt
@Composable
fun TextActionMenu(
    selectedText: String,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onShare: () -> Unit
) {
    // 复制
    // 搜索
    // 分享
    // 词典查询
}
```

### Phase 4: 搜索功能

#### 4.1 实现章节内搜索
```kotlin
// app/src/main/java/takagi/ru/paysage/reader/search/TextSearchEngine.kt
class TextSearchEngine {
    fun search(
        query: String,
        content: TextContent
    ): List<SearchResult> {
        // 在文本中查找匹配项
        // 返回位置和上下文
    }
}
```

#### 4.2 创建搜索 UI
```kotlin
// app/src/main/java/takagi/ru/paysage/ui/components/reader/SearchBar.kt
@Composable
fun ReaderSearchBar(
    onSearch: (String) -> Unit,
    results: List<SearchResult>,
    currentIndex: Int,
    onNavigate: (Int) -> Unit
) {
    // 搜索输入框
    // 结果导航
    // 高亮显示
}
```

### Phase 5: 自动翻页

#### 5.1 实现自动翻页控制器
```kotlin
// app/src/main/java/takagi/ru/paysage/reader/auto/AutoPageController.kt
class AutoPageController(
    private val onPageTurn: () -> Unit
) {
    fun start(speedMs: Long)
    fun pause()
    fun resume()
    fun stop()
}
```

#### 5.2 创建自动阅读设置
```kotlin
// app/src/main/java/takagi/ru/paysage/ui/components/reader/AutoReadDialog.kt
@Composable
fun AutoReadDialog(
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    // 速度调节
    // 开始/停止控制
}
```

## 实施优先级

### 高优先级（MVP）
1. ✅ 文本内容数据模型
2. ✅ 基础文本排版引擎
3. ✅ 文本渲染组件
4. ✅ 增强的阅读设置对话框

### 中优先级
5. 文本选择检测
6. 文本操作菜单
7. 章节内搜索
8. 快速设置面板

### 低优先级
9. 自动翻页
10. 高级搜索功能
11. 词典集成
12. 朗读功能（TTS）

## 技术实现细节

### 文本排版算法

```kotlin
fun layoutText(content: String, width: Int, height: Int): List<TextPage> {
    val paint = TextPaint().apply {
        textSize = config.textSize.toFloat()
        color = config.textColor
    }
    
    val layout = StaticLayout.Builder
        .obtain(content, 0, content.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(config.lineSpacing, 1f)
        .build()
    
    val pages = mutableListOf<TextPage>()
    var currentPage = mutableListOf<TextLine>()
    var currentY = 0f
    
    for (i in 0 until layout.lineCount) {
        val lineTop = layout.getLineTop(i).toFloat()
        val lineBottom = layout.getLineBottom(i).toFloat()
        val lineHeight = lineBottom - lineTop
        
        if (currentY + lineHeight > height) {
            // 开始新页
            pages.add(TextPage(pages.size, currentPage.toList(), 0))
            currentPage.clear()
            currentY = 0f
        }
        
        val lineText = content.substring(
            layout.getLineStart(i),
            layout.getLineEnd(i)
        )
        
        currentPage.add(TextLine(
            text = lineText,
            y = currentY,
            baseline = currentY + layout.getLineBaseline(i) - lineTop,
            height = lineHeight
        ))
        
        currentY += lineHeight
    }
    
    if (currentPage.isNotEmpty()) {
        pages.add(TextPage(pages.size, currentPage, 0))
    }
    
    return pages
}
```

### 文本渲染

```kotlin
fun renderTextPage(canvas: Canvas, page: TextPage, config: ReaderConfig) {
    val paint = TextPaint().apply {
        textSize = config.textSize.toFloat()
        color = config.textColor
        isAntiAlias = true
    }
    
    for (line in page.lines) {
        canvas.drawText(
            line.text,
            config.paddingLeft.toFloat(),
            line.baseline,
            paint
        )
    }
}
```

### 集成到现有 ReaderScreen

```kotlin
@Composable
fun ReaderScreen(
    bookId: Long,
    contentType: ContentType, // IMAGE or TEXT
    // ... 其他参数
) {
    when (contentType) {
        ContentType.IMAGE -> {
            // 现有的图片渲染逻辑
            PageFlipContainer(...)
        }
        ContentType.TEXT -> {
            // 新的文本渲染逻辑
            TextReaderView(...)
        }
    }
}
```

## 测试策略

### 单元测试
- TextLayoutEngine 的分页逻辑
- TextSelectionDetector 的选择检测
- TextSearchEngine 的搜索算法

### 集成测试
- 文本渲染性能
- 翻页流畅度
- 内存使用

### UI 测试
- 文本选择交互
- 搜索功能
- 配置对话框

## 时间估算

- Phase 1: 1-2 周
- Phase 2: 1 周
- Phase 3: 1-2 周
- Phase 4: 1 周
- Phase 5: 1 周
- 测试和优化: 1-2 周

**总计**: 6-9 周

## 下一步

1. 开始实现 Phase 1.1 - 创建文本内容数据模型
2. 实现 Phase 1.2 - 基础文本排版引擎
3. 实现 Phase 1.3 - 文本渲染组件
4. 集成到现有 ReaderScreen
5. 测试和迭代

这个计划专注于实用性和可实现性，同时学习 Legado 的优秀设计思想。
