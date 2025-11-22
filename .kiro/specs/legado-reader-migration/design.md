# Design Document - Legado Reader Migration

## Overview

本设计文档描述了将legado阅读器的ReaderScreen完全重构并移植到paysage项目的技术方案。该方案采用混合架构:保留legado的核心View系统(ReadView、PageDelegate、排版引擎)以确保功能完整性和性能,同时使用Jetpack Compose重写所有UI层组件,采用Material 3设计风格,并通过AndroidView桥接实现两者的无缝集成。

### 设计目标

1. **功能完整性**: 100%保留legado阅读器的所有核心功能
2. **现代化UI**: 使用Jetpack Compose和Material 3实现现代化界面
3. **架构兼容**: 与paysage项目的MVVM架构完美集成
4. **性能保证**: 确保性能不低于legado原实现
5. **可维护性**: 清晰的模块划分和代码组织

### 核心设计决策

1. **混合架构**: 保留legado的View系统核心,使用Compose重写UI层
2. **AndroidView桥接**: 通过AndroidView将legado的ReadView嵌入Compose
3. **状态管理**: 使用ViewModel + StateFlow管理状态
4. **单向数据流**: 遵循Compose的单向数据流原则
5. **模块化设计**: 清晰的模块边界和依赖关系

## Architecture

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer (Compose)             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LegadoReaderScreen (Composable)                     │  │
│  │  - TopBar, BottomBar, Dialogs (M3 Components)        │  │
│  │  - LegadoReadViewWrapper (AndroidView Bridge)        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                     ViewModel Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LegadoReaderViewModel                               │  │
│  │  - State Management (StateFlow)                      │  │
│  │  - Event Handling                                    │  │
│  │  - Configuration Management                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Use Cases                                           │  │
│  │  - LoadChapterUseCase                                │  │
│  │  - SaveProgressUseCase                               │  │
│  │  - ManageBookmarkUseCase                             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Repositories                                        │  │
│  │  - BookRepository                                    │  │
│  │  - ReaderConfigRepository                            │  │
│  │  - BookmarkRepository                                │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                  Legado Core Layer (View System)             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LegadoReadView (Custom View)                        │  │
│  │  ├─ PageView (Canvas Drawing)                        │  │
│  │  ├─ PageDelegate (Flip Animations)                   │  │
│  │  ├─ TextPageFactory (Page Generation)                │  │
│  │  └─ ChapterProvider (Text Layout Engine)             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```


### 模块划分

#### 1. Presentation Layer (Compose)

**职责**: UI渲染、用户交互、状态展示

**主要组件**:
- `LegadoReaderScreen`: 主屏幕Composable
- `LegadoReaderTopBar`: 顶部工具栏
- `LegadoReaderBottomBar`: 底部控制栏
- `ReadStyleDialog`: 阅读样式配置对话框
- `BgTextConfigDialog`: 背景和文字配置对话框
- `MoreConfigDialog`: 更多配置对话框
- `TipConfigDialog`: 提示信息配置对话框
- `PaddingConfigDialog`: 边距配置对话框
- `ClickActionConfigDialog`: 点击动作配置对话框
- `BookmarkDialog`: 书签管理对话框
- `SearchDialog`: 搜索对话框
- `LegadoReadViewWrapper`: AndroidView桥接组件

#### 2. ViewModel Layer

**职责**: 状态管理、业务逻辑协调、事件处理

**主要组件**:
- `LegadoReaderViewModel`: 主ViewModel
  - 管理阅读状态(当前页、章节、进度等)
  - 管理配置状态(字体、颜色、翻页模式等)
  - 处理用户事件(翻页、配置变更、书签操作等)
  - 协调Use Cases执行

#### 3. Domain Layer

**职责**: 业务逻辑封装、用例定义

**主要组件**:
- `LoadChapterUseCase`: 加载章节内容
- `SaveProgressUseCase`: 保存阅读进度
- `ManageBookmarkUseCase`: 管理书签
- `ApplyReaderConfigUseCase`: 应用阅读配置
- `SearchTextUseCase`: 搜索文本
- `ManageReplaceRuleUseCase`: 管理替换规则

#### 4. Data Layer

**职责**: 数据访问、持久化、缓存

**主要组件**:
- `BookRepository`: 书籍数据访问
- `ReaderConfigRepository`: 阅读配置持久化
- `BookmarkRepository`: 书签数据访问
- `ProgressRepository`: 阅读进度持久化
- `ReplaceRuleRepository`: 替换规则管理

#### 5. Legado Core Layer

**职责**: 核心阅读功能实现(保留legado原实现)

**主要组件**:
- `LegadoReadView`: 核心阅读视图
- `PageView`: 页面视图和Canvas绘制
- `PageDelegate`: 翻页动画委托基类
  - `NoAnimPageDelegate`: 无动画
  - `CoverPageDelegate`: 覆盖翻页
  - `SlidePageDelegate`: 滑动翻页
  - `SimulationPageDelegate`: 仿真翻页
  - `ScrollPageDelegate`: 滚动翻页
  - `HorizontalPageDelegate`: 水平滚动
- `TextPageFactory`: 页面生成工厂
- `ChapterProvider`: 章节内容提供者
- `TextMeasure`: 文本测量工具
- `TextChapterLayout`: 文本排版布局
- `ZhLayout`: 中文排版优化
- `ContentTextView`: 文本选择视图
- `TextActionMenu`: 文本操作菜单
- `AutoPager`: 自动翻页控制器
- `ReadBookConfig`: 阅读配置管理

## Components and Interfaces

### 1. LegadoReaderScreen (Composable)

主阅读界面屏幕组件。

**接口**:
```kotlin
@Composable
fun LegadoReaderScreen(
    bookId: String,
    viewModel: LegadoReaderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
)
```

**状态**:
```kotlin
data class LegadoReaderUiState(
    val book: Book? = null,
    val currentChapter: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val chapterTitle: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTopBar: Boolean = false,
    val showBottomBar: Boolean = false,
    val config: ReaderConfig = ReaderConfig(),
    val bookmarks: List<Bookmark> = emptyList()
)
```

**UI结构**:
```
Scaffold(
  topBar = { if (showTopBar) LegadoReaderTopBar(...) },
  bottomBar = { if (showBottomBar) LegadoReaderBottomBar(...) }
) {
  Box {
    LegadoReadViewWrapper(...)  // AndroidView桥接
    
    // 对话框
    if (showReadStyleDialog) ReadStyleDialog(...)
    if (showBgTextConfigDialog) BgTextConfigDialog(...)
    // ... 其他对话框
  }
}
```


### 2. LegadoReadViewWrapper (AndroidView Bridge)

将legado的ReadView嵌入Compose的桥接组件。

**接口**:
```kotlin
@Composable
fun LegadoReadViewWrapper(
    modifier: Modifier = Modifier,
    config: ReaderConfig,
    onPageChange: (chapter: Int, page: Int) -> Unit,
    onMenuToggle: () -> Unit,
    onTextSelected: (text: String) -> Unit,
    // ... 其他回调
)
```

**实现要点**:
- 使用`AndroidView`包装`LegadoReadView`
- 监听配置变化并更新ReadView
- 实现`ReadView.CallBack`接口转发事件到Compose
- 管理ReadView的生命周期

**示例代码**:
```kotlin
@Composable
fun LegadoReadViewWrapper(
    modifier: Modifier = Modifier,
    config: ReaderConfig,
    onPageChange: (Int, Int) -> Unit,
    onMenuToggle: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LegadoReadView(context).apply {
                setCallBack(object : ReadView.CallBack {
                    override fun onPageChange(chapter: Int, page: Int) {
                        onPageChange(chapter, page)
                    }
                    override fun onMenuToggle() {
                        onMenuToggle()
                    }
                    // ... 其他回调实现
                })
            }
        },
        update = { view ->
            view.updateConfig(config)
        }
    )
}
```

### 3. LegadoReaderViewModel

管理阅读器状态和业务逻辑的ViewModel。

**接口**:
```kotlin
class LegadoReaderViewModel @Inject constructor(
    private val loadChapterUseCase: LoadChapterUseCase,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val manageBookmarkUseCase: ManageBookmarkUseCase,
    private val readerConfigRepository: ReaderConfigRepository
) : ViewModel() {
    
    // 状态
    private val _uiState = MutableStateFlow(LegadoReaderUiState())
    val uiState: StateFlow<LegadoReaderUiState> = _uiState.asStateFlow()
    
    // 事件处理
    fun loadBook(bookId: String)
    fun onPageChange(chapter: Int, page: Int)
    fun onMenuToggle()
    fun updateConfig(config: ReaderConfig)
    fun addBookmark()
    fun deleteBookmark(bookmarkId: String)
    fun searchText(query: String)
    fun toggleAutoRead()
    fun toggleReadAloud()
    // ... 其他方法
}
```

**状态管理**:
- 使用`StateFlow`暴露UI状态
- 使用`MutableStateFlow`内部管理状态
- 所有状态更新通过`_uiState.update { ... }`进行

**事件处理流程**:
```
User Action → Composable → ViewModel Event Method → Use Case → Repository → Update State → Recompose UI
```

### 4. LegadoReadView (Legado Core)

legado的核心阅读视图,保留原实现。

**主要职责**:
- 管理三个PageView(上一页、当前页、下一页)
- 处理触摸事件和手势识别
- 协调PageDelegate执行翻页动画
- 管理TextPageFactory生成页面内容
- 实现九宫格触摸区域检测

**关键方法**:
```kotlin
class LegadoReadView(context: Context) : FrameLayout(context) {
    
    // 回调接口
    interface CallBack {
        fun onPageChange(chapter: Int, page: Int)
        fun onMenuToggle()
        fun onTextSelected(text: String)
        fun onBookmarkAdd()
        // ... 其他回调
    }
    
    // 配置更新
    fun updateConfig(config: ReaderConfig)
    
    // 翻页控制
    fun nextPage()
    fun prevPage()
    fun gotoChapter(chapter: Int)
    fun gotoPage(page: Int)
    
    // 生命周期
    fun onResume()
    fun onPause()
    fun onDestroy()
}
```

### 5. PageDelegate (Flip Animation System)

翻页动画委托系统,保留legado原实现。

**基类**:
```kotlin
abstract class PageDelegate(protected val readView: ReadView) {
    
    // 设置翻页方向
    abstract fun setDirection(direction: Direction)
    
    // 设置触摸点
    abstract fun setStartPoint(x: Float, y: Float)
    abstract fun setTouchPoint(x: Float, y: Float)
    
    // 动画控制
    abstract fun onAnimStart()
    abstract fun onAnimStop()
    abstract fun onDraw(canvas: Canvas)
    
    // 滚动计算
    abstract fun computeScroll(): Boolean
    
    // 取消动画
    abstract fun abortAnim()
}
```

**实现类**:
- `NoAnimPageDelegate`: 直接切换,无动画
- `CoverPageDelegate`: 新页覆盖旧页
- `SlidePageDelegate`: 滑动切换
- `SimulationPageDelegate`: 仿真翻页效果
- `ScrollPageDelegate`: 垂直滚动
- `HorizontalPageDelegate`: 水平滚动


### 6. ChapterProvider & TextPageFactory (Text Layout Engine)

文本排版引擎,保留legado原实现。

**ChapterProvider**:
```kotlin
object ChapterProvider {
    
    // 获取章节内容
    fun getChapterContent(book: Book, chapter: Int): TextChapter
    
    // 排版章节
    fun layoutChapter(
        textChapter: TextChapter,
        config: ReaderConfig
    ): List<TextPage>
    
    // 测量文本
    fun measureText(
        text: String,
        paint: TextPaint,
        width: Float
    ): Float
}
```

**TextPageFactory**:
```kotlin
class TextPageFactory(private val readView: ReadView) {
    
    // 加载章节
    fun loadChapter(chapter: Int)
    
    // 移动到指定位置
    fun moveTo(chapter: Int, page: Int)
    
    // 获取页面
    fun getCurrentPage(): TextPage?
    fun getNextPage(): TextPage?
    fun getPrevPage(): TextPage?
    
    // 预加载
    fun preloadNextChapter()
    fun preloadPrevChapter()
}
```

**TextPage数据结构**:
```kotlin
data class TextPage(
    val chapterIndex: Int,
    val pageIndex: Int,
    val title: String,
    val lines: List<TextLine>,
    val height: Float,
    val text: String
)

data class TextLine(
    val text: String,
    val columns: List<TextColumn>,
    val lineTop: Float,
    val lineBase: Float,
    val lineBottom: Float
)

data class TextColumn(
    val char: String,
    val start: Float,
    val end: Float
)
```

## Data Models

### 1. ReaderConfig

阅读配置数据模型。

```kotlin
data class ReaderConfig(
    // 字体设置
    val textFont: String = "",
    val textSize: Int = 18,
    val textBold: Boolean = false,
    val textColor: Int = Color.Black.toArgb(),
    
    // 背景设置
    val bgType: Int = 0, // 0=颜色, 1=图片
    val bgColor: Int = Color.White.toArgb(),
    val bgImagePath: String = "",
    
    // 间距设置
    val lineSpacing: Float = 1.0f,
    val paragraphSpacing: Float = 1.0f,
    val paddingTop: Int = 0,
    val paddingBottom: Int = 0,
    val paddingLeft: Int = 0,
    val paddingRight: Int = 0,
    
    // 翻页设置
    val pageMode: PageMode = PageMode.SIMULATION,
    val clickAllNext: Boolean = false,
    
    // 显示设置
    val hideStatusBar: Boolean = true,
    val hideNavigationBar: Boolean = true,
    val screenOrientation: Int = 0, // 0=跟随系统, 1=竖屏, 2=横屏
    val keepScreenOn: Boolean = true,
    val volumeKeyPage: Boolean = true,
    
    // 提示信息设置
    val showTitle: Boolean = true,
    val showTime: Boolean = true,
    val showBattery: Boolean = true,
    val showProgress: Boolean = true,
    val showPageNumber: Boolean = true,
    
    // 其他设置
    val textSelectAble: Boolean = true,
    val autoReadSpeed: Int = 50,
    val readAloudSpeed: Int = 5
)

enum class PageMode {
    SIMULATION,  // 仿真
    COVER,       // 覆盖
    SLIDE,       // 滑动
    SCROLL,      // 滚动
    HORIZONTAL,  // 水平滚动
    NONE         // 无动画
}
```

### 2. Bookmark

书签数据模型。

```kotlin
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val chapterIndex: Int,
    val pageIndex: Int,
    val chapterName: String,
    val content: String,
    val createTime: Long = System.currentTimeMillis()
)
```

### 3. ReadProgress

阅读进度数据模型。

```kotlin
@Entity(tableName = "read_progress")
data class ReadProgress(
    @PrimaryKey
    val bookId: String,
    val chapterIndex: Int,
    val pageIndex: Int,
    val chapterPos: Int,
    val updateTime: Long = System.currentTimeMillis()
)
```

### 4. ReplaceRule

替换规则数据模型。

```kotlin
@Entity(tableName = "replace_rules")
data class ReplaceRule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pattern: String,
    val replacement: String,
    val isRegex: Boolean = false,
    val isEnabled: Boolean = true,
    val scope: ReplaceScope = ReplaceScope.ALL,
    val order: Int = 0
)

enum class ReplaceScope {
    ALL,      // 全部书籍
    BOOK,     // 指定书籍
    SOURCE    // 指定书源
}
```


## Error Handling

### 错误类型和处理策略

#### 1. 章节加载错误

**错误场景**:
- 文件不存在或无法访问
- 文件格式不支持
- 解析失败

**处理策略**:
```kotlin
sealed class ChapterLoadError {
    data class FileNotFound(val path: String) : ChapterLoadError()
    data class ParseError(val message: String) : ChapterLoadError()
    data class PermissionDenied(val path: String) : ChapterLoadError()
}

// ViewModel中的处理
fun loadChapter(chapter: Int) {
    viewModelScope.launch {
        try {
            val content = loadChapterUseCase(bookId, chapter)
            _uiState.update { it.copy(currentChapter = content, error = null) }
        } catch (e: ChapterLoadError) {
            _uiState.update { 
                it.copy(
                    error = when (e) {
                        is ChapterLoadError.FileNotFound -> "章节文件不存在"
                        is ChapterLoadError.ParseError -> "章节解析失败: ${e.message}"
                        is ChapterLoadError.PermissionDenied -> "没有文件访问权限"
                    }
                )
            }
        }
    }
}
```

#### 2. 排版错误

**错误场景**:
- 文本测量失败
- 内存不足
- 配置参数无效

**处理策略**:
- 捕获异常并记录日志
- 使用默认配置重试
- 显示降级内容(纯文本)

```kotlin
fun layoutChapter(chapter: TextChapter, config: ReaderConfig): List<TextPage> {
    return try {
        ChapterProvider.layoutChapter(chapter, config)
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM during layout, using simplified layout", e)
        simplifiedLayout(chapter)
    } catch (e: Exception) {
        Log.e(TAG, "Layout error, using default config", e)
        ChapterProvider.layoutChapter(chapter, ReaderConfig())
    }
}
```

#### 3. 配置加载错误

**错误场景**:
- 配置文件损坏
- 版本不兼容
- 数据库访问失败

**处理策略**:
- 使用默认配置
- 提示用户配置已重置
- 尝试从备份恢复

```kotlin
suspend fun loadConfig(bookId: String): ReaderConfig {
    return try {
        readerConfigRepository.getConfig(bookId)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load config, using default", e)
        ReaderConfig().also {
            // 提示用户
            _uiState.update { state ->
                state.copy(message = "配置加载失败,已使用默认配置")
            }
        }
    }
}
```

#### 4. 动画执行错误

**错误场景**:
- Canvas绘制失败
- 动画状态异常
- 内存不足

**处理策略**:
- 降级到无动画模式
- 清理缓存并重试
- 记录错误日志

```kotlin
override fun onDraw(canvas: Canvas) {
    try {
        super.onDraw(canvas)
        pageDelegate.onDraw(canvas)
    } catch (e: Exception) {
        Log.e(TAG, "Draw error, fallback to no animation", e)
        pageDelegate = NoAnimPageDelegate(this)
        invalidate()
    }
}
```

### 错误日志和监控

**日志策略**:
- 使用分级日志(ERROR, WARN, INFO, DEBUG)
- 关键错误记录堆栈信息
- 性能问题记录耗时信息

**监控指标**:
- 章节加载成功率
- 排版耗时统计
- 动画帧率监控
- 内存使用监控
- 崩溃率统计

## Testing Strategy

### 1. 单元测试

**测试范围**:
- ViewModel业务逻辑
- Use Cases
- Repository数据访问
- 工具类和辅助函数

**测试框架**:
- JUnit 4
- MockK (Kotlin mocking)
- Turbine (Flow testing)
- Coroutines Test

**示例测试**:
```kotlin
@Test
fun `loadBook should update state with book data`() = runTest {
    // Given
    val bookId = "test-book-id"
    val expectedBook = Book(id = bookId, title = "Test Book")
    coEvery { loadChapterUseCase(bookId, 0) } returns expectedBook
    
    // When
    viewModel.loadBook(bookId)
    
    // Then
    viewModel.uiState.test {
        val state = awaitItem()
        assertEquals(expectedBook, state.book)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }
}
```

### 2. Compose UI测试

**测试范围**:
- Composable函数渲染
- 用户交互响应
- 状态变化更新UI
- 对话框显示和关闭

**测试框架**:
- Compose Test
- Compose UI Test Manifest

**示例测试**:
```kotlin
@Test
fun `clicking menu button should toggle menu visibility`() {
    composeTestRule.setContent {
        LegadoReaderScreen(
            bookId = "test-book",
            onNavigateBack = {}
        )
    }
    
    // 点击屏幕中心显示菜单
    composeTestRule.onNodeWithTag("reader_content").performClick()
    
    // 验证菜单显示
    composeTestRule.onNodeWithTag("top_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
}
```

### 3. 集成测试

**测试范围**:
- AndroidView桥接
- ViewModel与Repository交互
- 端到端阅读流程
- 配置持久化

**测试框架**:
- AndroidX Test
- Hilt Test
- Room Test

**示例测试**:
```kotlin
@Test
fun `reading progress should be saved automatically`() = runTest {
    // Given
    val bookId = "test-book"
    val chapter = 5
    val page = 10
    
    // When
    viewModel.loadBook(bookId)
    viewModel.onPageChange(chapter, page)
    delay(1000) // 等待自动保存
    
    // Then
    val progress = progressRepository.getProgress(bookId)
    assertEquals(chapter, progress.chapterIndex)
    assertEquals(page, progress.pageIndex)
}
```

### 4. 性能测试

**测试范围**:
- 章节加载耗时
- 排版性能
- 动画帧率
- 内存使用

**测试工具**:
- Android Profiler
- Benchmark库
- 自定义性能监控

**示例测试**:
```kotlin
@Test
fun `chapter layout should complete within 500ms`() {
    val chapter = createTestChapter(10000) // 10000字章节
    val config = ReaderConfig()
    
    val duration = measureTimeMillis {
        ChapterProvider.layoutChapter(chapter, config)
    }
    
    assertTrue("Layout took ${duration}ms", duration < 500)
}
```

### 5. 兼容性测试

**测试范围**:
- 不同Android版本
- 不同屏幕尺寸
- 不同DPI
- 横竖屏切换

**测试设备**:
- Android 8.0 (API 26)
- Android 10.0 (API 29)
- Android 12.0 (API 31)
- Android 14.0 (API 34)
- 平板设备
- 折叠屏设备


## Implementation Phases

### Phase 1: 基础架构搭建 (1-2周)

**目标**: 建立项目基础架构和核心模块

**任务**:
1. 创建模块目录结构
2. 移植legado核心数据实体(TextPage, TextLine, TextColumn等)
3. 创建基础ViewModel和UiState
4. 创建基础Repository接口
5. 搭建Compose主屏幕框架

**交付物**:
- 完整的目录结构
- 核心数据模型
- 基础架构代码
- 可运行的空白阅读界面

### Phase 2: Legado核心移植 (2-3周)

**目标**: 移植legado的核心View系统

**任务**:
1. 移植ReadView和PageView
2. 移植PageDelegate基类和所有实现类
3. 移植TextPageFactory
4. 移植ChapterProvider和排版引擎
5. 实现AndroidView桥接

**交付物**:
- 完整的ReadView系统
- 所有翻页动画模式
- 文本排版引擎
- 可用的AndroidView桥接

### Phase 3: Compose UI实现 (2-3周)

**目标**: 使用Compose实现所有UI组件

**任务**:
1. 实现TopBar和BottomBar
2. 实现所有配置对话框
3. 实现书签对话框
4. 实现搜索对话框
5. 应用Material 3设计风格

**交付物**:
- 完整的Compose UI组件
- M3风格的界面
- 所有对话框实现

### Phase 4: 高级功能实现 (2-3周)

**目标**: 实现高级阅读功能

**任务**:
1. 实现文本选择和操作菜单
2. 实现自动阅读功能
3. 实现朗读功能
4. 实现触摸区域配置
5. 实现替换规则

**交付物**:
- 文本选择功能
- 自动阅读和朗读
- 触摸区域配置
- 替换规则系统

### Phase 5: 数据层集成 (1-2周)

**目标**: 集成数据持久化和状态管理

**任务**:
1. 实现所有Repository
2. 实现数据库DAO
3. 实现配置持久化
4. 实现进度自动保存
5. 实现书签管理

**交付物**:
- 完整的数据层
- 配置和进度持久化
- 书签管理功能

### Phase 6: 性能优化 (1-2周)

**目标**: 优化性能达到或超过legado

**任务**:
1. 优化内存管理
2. 优化渲染性能
3. 优化排版性能
4. 优化状态同步
5. 实现缓存策略

**交付物**:
- 性能测试报告
- 优化后的代码
- 性能监控工具

### Phase 7: 测试和质量保证 (1-2周)

**目标**: 确保功能正确性和稳定性

**任务**:
1. 编写单元测试
2. 编写UI测试
3. 编写集成测试
4. 进行兼容性测试
5. Bug修复

**交付物**:
- 完整的测试套件
- 测试报告
- Bug修复记录

### Phase 8: 文档和发布 (1周)

**目标**: 完善文档并准备发布

**任务**:
1. 编写技术文档
2. 编写用户文档
3. 编写API文档
4. 准备发布说明
5. 代码审查和清理

**交付物**:
- 完整的文档
- 发布说明
- 清理后的代码

**总计**: 11-18周

## Performance Considerations

### 1. 内存优化

**策略**:
- Bitmap复用池
- 页面缓存限制(最多缓存5页)
- 及时释放不用的资源
- 使用WeakReference缓存

**实现**:
```kotlin
object BitmapPool {
    private val pool = mutableListOf<Bitmap>()
    private val maxSize = 10
    
    fun obtain(width: Int, height: Int): Bitmap {
        synchronized(pool) {
            val bitmap = pool.find { 
                it.width == width && it.height == height 
            }
            if (bitmap != null) {
                pool.remove(bitmap)
                return bitmap
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
    
    fun recycle(bitmap: Bitmap) {
        synchronized(pool) {
            if (pool.size < maxSize) {
                pool.add(bitmap)
            } else {
                bitmap.recycle()
            }
        }
    }
}
```

### 2. 渲染优化

**策略**:
- 启用硬件加速
- 使用Canvas.saveLayer优化
- 减少过度绘制
- 使用ViewStub延迟加载

**实现**:
```kotlin
class PageView(context: Context) : View(context) {
    
    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    override fun onDraw(canvas: Canvas) {
        // 使用硬件加速的Canvas绘制
        canvas.drawBitmap(pageBitmap, 0f, 0f, null)
    }
}
```

### 3. 排版优化

**策略**:
- 异步排版
- 增量排版(只排版可见部分)
- 缓存排版结果
- 使用StaticLayout优化

**实现**:
```kotlin
suspend fun layoutChapterAsync(
    chapter: TextChapter,
    config: ReaderConfig
): List<TextPage> = withContext(Dispatchers.Default) {
    // 在后台线程排版
    ChapterProvider.layoutChapter(chapter, config)
}
```

### 4. 状态同步优化

**策略**:
- 减少不必要的重组
- 使用derivedStateOf
- 使用remember缓存计算结果
- 使用key避免重组

**实现**:
```kotlin
@Composable
fun LegadoReaderScreen(viewModel: LegadoReaderViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 使用derivedStateOf避免不必要的重组
    val showMenu by remember {
        derivedStateOf { uiState.showTopBar || uiState.showBottomBar }
    }
    
    // 使用key避免重组
    key(uiState.config.pageMode) {
        LegadoReadViewWrapper(config = uiState.config)
    }
}
```

### 5. 预加载优化

**策略**:
- 预加载相邻章节
- 智能预测阅读方向
- 后台预加载
- 限制预加载数量

**实现**:
```kotlin
class PreloadManager(
    private val textPageFactory: TextPageFactory
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    fun preloadAdjacent(currentChapter: Int) {
        scope.launch {
            // 预加载下一章
            textPageFactory.preloadChapter(currentChapter + 1)
            // 预加载上一章
            textPageFactory.preloadChapter(currentChapter - 1)
        }
    }
}
```

## Security and Privacy

### 1. 文件访问安全

**措施**:
- 使用Scoped Storage
- 验证文件路径
- 限制访问权限
- 沙箱隔离

### 2. 配置数据安全

**措施**:
- 加密敏感配置
- 使用EncryptedSharedPreferences
- 验证配置完整性
- 定期备份

### 3. 用户隐私保护

**措施**:
- 不收集用户数据
- 本地存储阅读记录
- 可选的数据清理
- 隐私模式支持

## Accessibility

### 1. 无障碍支持

**措施**:
- 提供内容描述
- 支持TalkBack
- 支持大字体
- 支持高对比度

**实现**:
```kotlin
@Composable
fun LegadoReaderTopBar(
    onNavigateBack: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.semantics {
                    contentDescription = "返回"
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}
```

### 2. 键盘导航

**措施**:
- 支持Tab键导航
- 支持方向键翻页
- 支持快捷键
- 焦点管理

### 3. 屏幕阅读器

**措施**:
- 提供语义化标签
- 支持朗读顺序
- 提供操作提示
- 支持手势导航

## Maintenance and Extensibility

### 1. 代码组织

**原则**:
- 单一职责原则
- 开闭原则
- 依赖倒置原则
- 接口隔离原则

### 2. 模块化设计

**结构**:
```
app/
├── presentation/
│   ├── reader/
│   │   ├── LegadoReaderScreen.kt
│   │   ├── LegadoReaderViewModel.kt
│   │   └── components/
│   └── dialogs/
├── domain/
│   ├── usecases/
│   └── models/
├── data/
│   ├── repositories/
│   └── local/
└── legado/
    ├── view/
    ├── delegate/
    └── provider/
```

### 3. 版本管理

**策略**:
- 语义化版本号
- 变更日志
- 迁移脚本
- 向后兼容

### 4. 扩展点

**设计**:
- 插件化翻页动画
- 可扩展的配置项
- 自定义主题支持
- 第三方集成接口

## Conclusion

本设计文档提供了将legado阅读器ReaderScreen完全重构并移植到paysage项目的完整技术方案。通过混合架构设计,我们既保留了legado的核心功能和性能优势,又实现了现代化的Compose UI和Material 3设计风格。

关键设计决策包括:
1. 使用AndroidView桥接保留legado的View系统核心
2. 使用Compose重写所有UI层组件
3. 采用MVVM架构确保与paysage项目兼容
4. 实现完整的测试覆盖保证质量
5. 优化性能确保不低于legado原实现

该方案预计需要11-18周完成,分8个阶段逐步实施,确保每个阶段都有明确的目标和交付物。
