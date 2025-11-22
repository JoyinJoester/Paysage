# 书库分类系统重设计 - 设计文档

## 概述

本设计文档描述了如何重新设计Paysage应用的书库页面，实现符合Material 3 Expressive (M3E)设计风格的双分类系统（漫画和阅读），支持独立的文件管理和在线阅读功能。

### 设计目标

1. 采用M3E设计系统，提供生动、富有表现力的用户界面
2. 实现漫画和阅读两大分类的独立管理
3. 支持本地阅读和在线阅读的无缝切换
4. 优化导航结构，提升用户体验
5. 确保数据库性能和响应速度

### 核心原则

- **分离关注点**: 漫画和阅读内容完全独立管理
- **渐进增强**: 先实现本地功能，再扩展在线功能
- **性能优先**: 使用索引、分页和缓存优化性能
- **一致性**: 遵循M3E设计规范，保持视觉和交互一致性

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                    │
│  ┌────────────────────────────────────────────────┐     │
│  │  TwoLayerNavigationScaffold                    │     │
│  │    ├─ PrimaryDrawer (主导航)                   │     │
│  │    │   ├─ 本地功能                             │     │
│  │    │   ├─ 在线功能                             │     │
│  │    │   └─ 设置                                 │     │
│  │    └─ SecondaryDrawer (次级导航)               │     │
│  │        ├─ 漫画分类                             │     │
│  │        └─ 阅读分类                             │     │
│  └────────────────────────────────────────────────┘     │
│                                                           │
│  ┌────────────────────────────────────────────────┐     │
│  │  LibraryScreen (重构)                          │     │
│  │    ├─ CategoryFilterBar (分类筛选栏)           │     │
│  │    ├─ BookGrid/List (书籍展示)                 │     │
│  │    └─ OnlineSourceView (在线书源)              │     │
│  └────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  ViewModel Layer                         │
│  ┌────────────────────────────────────────────────┐     │
│  │  LibraryViewModel (扩展)                       │     │
│  │    ├─ categoryType: StateFlow<CategoryType>    │     │
│  │    ├─ displayMode: StateFlow<DisplayMode>      │     │
│  │    └─ onlineBooks: StateFlow<List<OnlineBook>> │     │
│  └────────────────────────────────────────────────┘     │
│                                                           │
│  ┌────────────────────────────────────────────────┐     │
│  │  OnlineSourceViewModel (新增)                  │     │
│  │    ├─ bookSources: StateFlow<List<BookSource>> │     │
│  │    └─ searchResults: StateFlow<List<Book>>     │     │
│  └────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  Repository Layer                        │
│  ┌────────────────────────────────────────────────┐     │
│  │  BookRepository (扩展)                          │     │
│  │    ├─ getBooksByCategory(type)                 │     │
│  │    └─ updateBookCategory(id, type)             │     │
│  └────────────────────────────────────────────────┘     │
│                                                           │
│  ┌────────────────────────────────────────────────┐     │
│  │  OnlineSourceRepository (新增)                 │     │
│  │    ├─ getAllSources()                           │     │
│  │    ├─ searchBooks(source, query)               │     │
│  │    └─ downloadChapter(book, chapter)           │     │
│  └────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                     Data Layer                           │
│  ┌────────────────────────────────────────────────┐     │
│  │  Room Database                                  │     │
│  │    ├─ books (扩展categoryType字段)             │     │
│  │    ├─ book_sources (新增)                      │     │
│  │    └─ online_books (新增)                      │     │
│  └────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### 数据模型设计

#### 1. 分类类型枚举

```kotlin
/**
 * 书籍分类类型
 */
enum class CategoryType {
    MANGA,    // 漫画
    NOVEL     // 阅读（小说）
}
```

#### 2. 显示模式枚举

```kotlin
/**
 * 书库显示模式
 */
enum class DisplayMode {
    LOCAL,    // 本地功能
    ONLINE    // 在线功能
}
```

#### 3. Book实体扩展

```kotlin
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["lastReadAt"], name = "index_books_lastReadAt"),
        Index(value = ["categoryType"], name = "index_books_categoryType")  // 新增索引
    ]
)
data class Book(
    // ... 现有字段 ...
    
    // 新增字段
    val categoryType: CategoryType = CategoryType.MANGA,  // 分类类型
    val isOnline: Boolean = false,                        // 是否为在线书籍
    val sourceId: Long? = null,                           // 书源ID（在线书籍）
    val sourceUrl: String? = null                         // 书源URL（在线书籍）
)
```

#### 4. 在线书源实体

```kotlin
@Entity(tableName = "book_sources")
data class BookSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // 书源名称
    val baseUrl: String,                 // 基础URL
    val categoryType: CategoryType,      // 支持的分类类型
    val isEnabled: Boolean = true,       // 是否启用
    val priority: Int = 0,               // 优先级
    val lastUsedAt: Long? = null,        // 最后使用时间
    val addedAt: Long = System.currentTimeMillis()
)
```

## 组件设计

### 1. 导航系统重构

#### PrimaryNavItem扩展

```kotlin
enum class PrimaryNavItem(
    val icon: ImageVector,
    val labelRes: Int,
    val contentDescriptionRes: Int,
    val hasSecondaryMenu: Boolean = false
) {
    LocalLibrary(
        icon = Icons.Default.Folder,
        labelRes = R.string.nav_local_library,
        contentDescriptionRes = R.string.nav_open_local_library,
        hasSecondaryMenu = true
    ),
    OnlineLibrary(
        icon = Icons.Default.Cloud,
        labelRes = R.string.nav_online_library,
        contentDescriptionRes = R.string.nav_open_online_library,
        hasSecondaryMenu = true
    ),
    Settings(
        icon = Icons.Default.Settings,
        labelRes = R.string.nav_settings,
        contentDescriptionRes = R.string.nav_open_settings
    )
}
```

#### SecondaryNavItem配置

```kotlin
object LocalLibraryNavItems {
    fun getItems(context: Context) = listOf(
        SecondaryNavItem(
            id = "manga",
            icon = Icons.Default.Book,
            label = context.getString(R.string.category_manga),
            route = "library?category=manga"
        ),
        SecondaryNavItem(
            id = "novel",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = context.getString(R.string.category_novel),
            route = "library?category=novel"
        )
    )
}

object OnlineLibraryNavItems {
    fun getItems(context: Context) = listOf(
        SecondaryNavItem(
            id = "manga_sources",
            icon = Icons.Default.CloudQueue,
            label = context.getString(R.string.manga_sources),
            route = "online?category=manga"
        ),
        SecondaryNavItem(
            id = "novel_sources",
            icon = Icons.Default.CloudQueue,
            label = context.getString(R.string.novel_sources),
            route = "online?category=novel"
        )
    )
}
```

### 2. LibraryScreen重构

#### 状态管理

```kotlin
@Composable
fun LibraryScreen(
    categoryType: CategoryType = CategoryType.MANGA,
    displayMode: DisplayMode = DisplayMode.LOCAL,
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: LibraryViewModel = viewModel()
) {
    // 根据categoryType和displayMode过滤书籍
    val books by viewModel.getBooksByCategory(categoryType, displayMode)
        .collectAsState(initial = emptyList())
    
    // ... UI实现
}
```

#### 分类筛选栏

```kotlin
@Composable
fun CategoryFilterBar(
    selectedCategory: CategoryType,
    onCategoryChange: (CategoryType) -> Unit,
    displayMode: DisplayMode,
    onDisplayModeChange: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 分类切换
        ExpressiveSegmentedButton(
            selected = selectedCategory == CategoryType.MANGA,
            onClick = { onCategoryChange(CategoryType.MANGA) }
        ) {
            Icon(Icons.Default.Book, contentDescription = null)
            Text("漫画")
        }
        
        ExpressiveSegmentedButton(
            selected = selectedCategory == CategoryType.NOVEL,
            onClick = { onCategoryChange(CategoryType.NOVEL) }
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
            Text("阅读")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 本地/在线切换
        ExpressiveIconButton(
            onClick = { 
                onDisplayModeChange(
                    if (displayMode == DisplayMode.LOCAL) 
                        DisplayMode.ONLINE 
                    else 
                        DisplayMode.LOCAL
                )
            }
        ) {
            Icon(
                if (displayMode == DisplayMode.LOCAL) 
                    Icons.Default.Folder 
                else 
                    Icons.Default.Cloud,
                contentDescription = null
            )
        }
    }
}
```

### 3. 在线书源管理

#### OnlineSourceScreen

```kotlin
@Composable
fun OnlineSourceScreen(
    categoryType: CategoryType,
    onBookClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    viewModel: OnlineSourceViewModel = viewModel()
) {
    val sources by viewModel.getSourcesByCategory(categoryType)
        .collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (categoryType == CategoryType.MANGA) 
                            "漫画书源" 
                        else 
                            "小说书源"
                    ) 
                },
                navigationIcon = {
                    ExpressiveIconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    ExpressiveIconButton(onClick = { /* 添加书源 */ }) {
                        Icon(Icons.Default.Add, "添加书源")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sources, key = { it.id }) { source ->
                BookSourceCard(
                    source = source,
                    onClick = { /* 打开书源 */ },
                    onToggleEnabled = { viewModel.toggleSourceEnabled(source.id) }
                )
            }
        }
    }
}
```

#### BookSourceCard

```kotlin
@Composable
fun BookSourceCard(
    source: BookSource,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = source.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = source.isEnabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}
```

## M3E设计实现

### 1. 色彩应用

```kotlin
// 分类特定颜色
object CategoryColors {
    val MangaPrimary = Color(0xFFFF6B35)      // 活力橙色
    val MangaContainer = Color(0xFFFFDBCC)    // 柔和橙色背景
    
    val NovelPrimary = Color(0xFF6A4C9C)      // 优雅紫色
    val NovelContainer = Color(0xFFE9DDFF)    // 浅紫色背景
    
    val OnlinePrimary = Color(0xFF00BFA5)     // 清新青绿色
    val OnlineContainer = Color(0xFF73F7DD)   // 浅青绿色背景
}

@Composable
fun getCategoryColor(categoryType: CategoryType): Color {
    return when (categoryType) {
        CategoryType.MANGA -> CategoryColors.MangaPrimary
        CategoryType.NOVEL -> CategoryColors.NovelPrimary
    }
}
```

### 2. 形状系统

```kotlin
object ExpressiveShapes {
    val categoryCard = RoundedCornerShape(16.dp)
    val sourceCard = RoundedCornerShape(12.dp)
    val filterChip = RoundedCornerShape(20.dp)
    val bottomSheet = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp
    )
}
```

### 3. 动画效果

```kotlin
// 分类切换动画
@Composable
fun CategorySwitchAnimation(
    targetCategory: CategoryType,
    content: @Composable (CategoryType) -> Unit
) {
    AnimatedContent(
        targetState = targetCategory,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = ExpressiveEasing
                )
            ) with fadeOut(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = ExpressiveEasing
                )
            )
        }
    ) { category ->
        content(category)
    }
}

// 列表项进入动画
@Composable
fun BookListItemAnimation(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L)  // 错开动画
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn()
    ) {
        content()
    }
}
```

## 数据库优化

### 1. 索引策略

```kotlin
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["lastReadAt"], name = "index_books_lastReadAt"),
        Index(value = ["categoryType"], name = "index_books_categoryType"),
        Index(value = ["categoryType", "isOnline"], name = "index_books_category_online"),
        Index(value = ["categoryType", "lastReadAt"], name = "index_books_category_read")
    ]
)
```

### 2. 分页查询

```kotlin
@Dao
interface BookDao {
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType 
        AND isOnline = :isOnline
        ORDER BY lastReadAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getBooksByCategory(
        categoryType: CategoryType,
        isOnline: Boolean,
        limit: Int = 50,
        offset: Int = 0
    ): List<Book>
    
    @Query("""
        SELECT * FROM books 
        WHERE categoryType = :categoryType 
        AND isOnline = :isOnline
        ORDER BY lastReadAt DESC
    """)
    fun getBooksByCategoryFlow(
        categoryType: CategoryType,
        isOnline: Boolean
    ): Flow<List<Book>>
}
```

### 3. 缓存策略

```kotlin
class BookRepository(context: Context) {
    private val bookCache = LruCache<String, List<Book>>(10)
    
    suspend fun getBooksByCategory(
        categoryType: CategoryType,
        displayMode: DisplayMode
    ): List<Book> {
        val cacheKey = "${categoryType}_${displayMode}"
        
        // 先检查缓存
        bookCache.get(cacheKey)?.let { return it }
        
        // 从数据库查询
        val books = bookDao.getBooksByCategory(
            categoryType = categoryType,
            isOnline = displayMode == DisplayMode.ONLINE
        )
        
        // 更新缓存
        bookCache.put(cacheKey, books)
        
        return books
    }
}
```

## 错误处理

### 1. 网络错误处理

```kotlin
sealed class OnlineSourceResult<out T> {
    data class Success<T>(val data: T) : OnlineSourceResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : OnlineSourceResult<Nothing>()
    object Loading : OnlineSourceResult<Nothing>()
}

class OnlineSourceRepository(context: Context) {
    suspend fun searchBooks(
        source: BookSource,
        query: String
    ): OnlineSourceResult<List<Book>> {
        return try {
            // 网络请求
            val books = performSearch(source, query)
            OnlineSourceResult.Success(books)
        } catch (e: IOException) {
            OnlineSourceResult.Error("网络连接失败", e)
        } catch (e: Exception) {
            OnlineSourceResult.Error("搜索失败: ${e.message}", e)
        }
    }
}
```

### 2. UI错误显示

```kotlin
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ExpressiveButton(onClick = onRetry) {
            Text("重试")
        }
    }
}
```

## 测试策略

### 1. 单元测试

```kotlin
class LibraryViewModelTest {
    @Test
    fun `test category filtering`() = runTest {
        val viewModel = LibraryViewModel(application)
        
        // 设置漫画分类
        viewModel.setCategoryType(CategoryType.MANGA)
        
        // 验证只返回漫画书籍
        val books = viewModel.displayBooks.first()
        assertTrue(books.all { it.categoryType == CategoryType.MANGA })
    }
}
```

### 2. UI测试

```kotlin
@Test
fun testCategorySwitching() {
    composeTestRule.setContent {
        LibraryScreen(
            categoryType = CategoryType.MANGA,
            displayMode = DisplayMode.LOCAL,
            onBookClick = {},
            onSettingsClick = {}
        )
    }
    
    // 点击阅读分类
    composeTestRule.onNodeWithText("阅读").performClick()
    
    // 验证显示阅读分类的书籍
    composeTestRule.onNodeWithTag("book_list")
        .assertExists()
}
```

## 性能考虑

### 1. 懒加载

- 使用LazyColumn/LazyVerticalGrid实现虚拟滚动
- 分页加载，每次加载50本书籍
- 图片懒加载，使用Coil的内存缓存

### 2. 数据库优化

- 为categoryType字段创建索引
- 使用复合索引优化常见查询
- 定期清理过期缓存数据

### 3. 内存管理

- 使用LruCache缓存书籍列表
- 及时释放不再使用的Bitmap
- 限制同时加载的图片数量

## 可访问性

### 1. 内容描述

```kotlin
Icon(
    Icons.Default.Book,
    contentDescription = stringResource(R.string.category_manga_icon)
)
```

### 2. 语义化标签

```kotlin
Text(
    text = book.title,
    modifier = Modifier.semantics {
        contentDescription = "书籍标题: ${book.title}"
    }
)
```

### 3. 触摸目标

- 所有可点击元素最小尺寸48dp
- 按钮之间间距至少8dp
- 支持键盘导航

## 国际化

### 字符串资源

```xml
<!-- strings.xml -->
<string name="category_manga">漫画</string>
<string name="category_novel">阅读</string>
<string name="display_mode_local">本地</string>
<string name="display_mode_online">在线</string>
<string name="nav_local_library">本地功能</string>
<string name="nav_online_library">在线功能</string>
<string name="manga_sources">漫画书源</string>
<string name="novel_sources">小说书源</string>
```

## 迁移策略

### 数据库迁移

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加categoryType字段
        database.execSQL(
            "ALTER TABLE books ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'MANGA'"
        )
        
        // 添加isOnline字段
        database.execSQL(
            "ALTER TABLE books ADD COLUMN isOnline INTEGER NOT NULL DEFAULT 0"
        )
        
        // 创建索引
        database.execSQL(
            "CREATE INDEX index_books_categoryType ON books(categoryType)"
        )
        
        // 创建book_sources表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS book_sources (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                baseUrl TEXT NOT NULL,
                categoryType TEXT NOT NULL,
                isEnabled INTEGER NOT NULL DEFAULT 1,
                priority INTEGER NOT NULL DEFAULT 0,
                lastUsedAt INTEGER,
                addedAt INTEGER NOT NULL
            )
        """)
    }
}
```

### 用户数据迁移

```kotlin
suspend fun migrateUserData() {
    // 根据文件类型自动分类现有书籍
    val books = bookDao.getAllBooks()
    books.forEach { book ->
        val categoryType = when (book.fileFormat) {
            BookFormat.CBZ, BookFormat.CBR, BookFormat.CBT, 
            BookFormat.CB7, BookFormat.ZIP, BookFormat.RAR -> 
                CategoryType.MANGA
            BookFormat.PDF -> 
                CategoryType.NOVEL  // 默认PDF为小说
            else -> 
                CategoryType.MANGA
        }
        bookDao.updateCategoryType(book.id, categoryType)
    }
}
```

## 总结

本设计文档详细描述了书库分类系统的重设计方案，包括：

1. **架构设计**: 清晰的分层架构，支持本地和在线功能
2. **数据模型**: 扩展Book实体，新增BookSource实体
3. **UI组件**: 重构LibraryScreen，新增OnlineSourceScreen
4. **M3E设计**: 应用M3E色彩、形状和动画系统
5. **性能优化**: 索引、分页、缓存策略
6. **错误处理**: 完善的错误处理和用户反馈
7. **测试策略**: 单元测试和UI测试
8. **可访问性**: 内容描述、语义化标签、触摸目标
9. **迁移策略**: 数据库迁移和用户数据迁移

该设计确保了功能的完整性、性能的优化和用户体验的提升。
