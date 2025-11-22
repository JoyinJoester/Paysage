# 设计文档

## 概述

本设计文档描述了书库侧边栏筛选选项功能的实现方案。该功能将实现收藏、最近阅读和分类三个筛选选项，使用户能够通过侧边栏快速访问不同类别的书籍。

设计采用现有的架构模式，通过扩展BookDao、BookRepository和LibraryViewModel来支持新的筛选功能，并通过路由参数管理不同的筛选视图状态。

## 架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      LibraryScreen (UI)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 全部书籍视图  │  │ 收藏书籍视图  │  │ 最近阅读视图  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ 分类列表视图  │  │ 分类书籍视图  │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    LibraryViewModel                          │
│  - filterMode: StateFlow<FilterMode>                        │
│  - selectedCategory: StateFlow<String?>                     │
│  - recentBooks: StateFlow<List<Book>>                       │
│  - categoriesWithCount: StateFlow<List<CategoryInfo>>       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    BookRepository                            │
│  - getRecentBooksFlow(): Flow<List<Book>>                   │
│  - getCategoriesWithCountFlow(): Flow<List<CategoryInfo>>   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                        BookDao                               │
│  - getRecentBooksFlow(limit: Int): Flow<List<Book>>         │
│  - getCategoriesWithCount(): Flow<List<CategoryInfo>>       │
└─────────────────────────────────────────────────────────────┘
```

### 数据流

1. **用户交互** → SecondaryDrawerContent 点击筛选选项
2. **导航** → 更新路由参数（filter=favorites/recent/categories）
3. **状态更新** → LibraryViewModel 根据路由参数更新 filterMode
4. **数据查询** → Repository 从数据库获取对应的数据
5. **UI 渲染** → LibraryScreen 根据 filterMode 显示对应的视图

## 组件和接口

### 1. 数据模型

#### FilterMode 枚举
```kotlin
enum class FilterMode {
    ALL,           // 全部书籍
    FAVORITES,     // 收藏
    RECENT,        // 最近阅读
    CATEGORIES,    // 分类列表
    CATEGORY       // 特定分类的书籍
}
```

#### CategoryInfo 数据类
```kotlin
data class CategoryInfo(
    val name: String,
    val bookCount: Int
)
```

### 2. 数据访问层 (BookDao)

扩展 BookDao 接口，添加以下查询方法：

```kotlin
@Dao
interface BookDao {
    // 现有方法...
    
    /**
     * 获取最近阅读的书籍（按最后阅读时间降序，限制数量）
     */
    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT :limit")
    fun getRecentBooksFlow(limit: Int = 20): Flow<List<Book>>
    
    /**
     * 获取所有分类及其书籍数量
     */
    @Query("""
        SELECT category as name, COUNT(*) as bookCount 
        FROM books 
        WHERE category IS NOT NULL 
        GROUP BY category 
        ORDER BY category ASC
    """)
    fun getCategoriesWithCount(): Flow<List<CategoryInfo>>
}
```

### 3. 数据仓库层 (BookRepository)

扩展 BookRepository 类，添加以下方法：

```kotlin
class BookRepository(private val context: Context) {
    // 现有代码...
    
    /**
     * 获取最近阅读的书籍流
     */
    fun getRecentBooksFlow(limit: Int = 20): Flow<List<Book>> = 
        bookDao.getRecentBooksFlow(limit)
    
    /**
     * 获取分类及其书籍数量流
     */
    fun getCategoriesWithCountFlow(): Flow<List<CategoryInfo>> = 
        bookDao.getCategoriesWithCount()
    
    /**
     * 获取最近阅读的书籍（挂起函数）
     */
    suspend fun getRecentBooks(limit: Int = 20): List<Book> = 
        withContext(Dispatchers.IO) {
            bookDao.getRecentBooksFlow(limit).first()
        }
}
```

### 4. 视图模型层 (LibraryViewModel)

扩展 LibraryViewModel 类，添加筛选状态管理：

```kotlin
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    // 现有代码...
    
    // 筛选模式
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()
    
    // 选中的分类
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    // 最近阅读的书籍
    val recentBooks: StateFlow<List<Book>> = repository.getRecentBooksFlow(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 分类及其书籍数量
    val categoriesWithCount: StateFlow<List<CategoryInfo>> = 
        repository.getCategoriesWithCountFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 根据筛选模式获取当前显示的书籍列表
    val displayBooks: StateFlow<List<Book>> = combine(
        filterMode,
        selectedCategory,
        allBooks,
        favoriteBooks,
        recentBooks
    ) { mode, category, all, favorites, recent ->
        when (mode) {
            FilterMode.ALL -> all
            FilterMode.FAVORITES -> favorites
            FilterMode.RECENT -> recent
            FilterMode.CATEGORY -> {
                category?.let { cat ->
                    all.filter { it.category == cat }
                } ?: emptyList()
            }
            FilterMode.CATEGORIES -> emptyList() // 分类列表视图不显示书籍
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * 设置筛选模式
     */
    fun setFilterMode(mode: FilterMode, category: String? = null) {
        _filterMode.value = mode
        _selectedCategory.value = category
    }
    
    /**
     * 从路由参数解析筛选模式
     */
    fun parseFilterFromRoute(filter: String?, category: String?) {
        when (filter) {
            "favorites" -> setFilterMode(FilterMode.FAVORITES)
            "recent" -> setFilterMode(FilterMode.RECENT)
            "categories" -> {
                if (category != null) {
                    setFilterMode(FilterMode.CATEGORY, category)
                } else {
                    setFilterMode(FilterMode.CATEGORIES)
                }
            }
            else -> setFilterMode(FilterMode.ALL)
        }
    }
}
```

### 5. UI 层 (LibraryScreen)

修改 LibraryScreen 以支持不同的筛选视图：

```kotlin
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    filter: String? = null,
    category: String? = null,
    viewModel: LibraryViewModel = viewModel()
) {
    // 解析路由参数
    LaunchedEffect(filter, category) {
        viewModel.parseFilterFromRoute(filter, category)
    }
    
    val filterMode by viewModel.filterMode.collectAsState()
    val displayBooks by viewModel.displayBooks.collectAsState()
    val categoriesWithCount by viewModel.categoriesWithCount.collectAsState()
    
    // 根据筛选模式显示不同的内容
    when (filterMode) {
        FilterMode.CATEGORIES -> {
            // 显示分类列表
            CategoriesListView(
                categories = categoriesWithCount,
                onCategoryClick = { categoryName ->
                    // 导航到分类书籍视图
                    navController.navigate("${Screen.Library.route}?filter=categories&category=$categoryName")
                }
            )
        }
        else -> {
            // 显示书籍列表（全部/收藏/最近阅读/特定分类）
            BookListView(
                books = displayBooks,
                filterMode = filterMode,
                selectedCategory = viewModel.selectedCategory.collectAsState().value,
                onBookClick = onBookClick
            )
        }
    }
}
```

### 6. 分类列表视图组件

新增 CategoriesListView 组件：

```kotlin
@Composable
fun CategoriesListView(
    categories: List<CategoryInfo>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        EmptyCategoriesView(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { categoryInfo ->
                CategoryCard(
                    categoryInfo = categoryInfo,
                    onClick = { onCategoryClick(categoryInfo.name) }
                )
            }
        }
    }
}

@Composable
fun CategoryCard(
    categoryInfo: CategoryInfo,
    onClick: () -> Unit,
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
            Column {
                Text(
                    text = categoryInfo.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${categoryInfo.bookCount} 本书籍",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 7. 空状态视图组件

扩展空状态视图以支持不同的筛选模式：

```kotlin
@Composable
fun EmptyFilterView(
    filterMode: FilterMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val (icon, title, message) = when (filterMode) {
            FilterMode.FAVORITES -> Triple(
                Icons.Default.FavoriteBorder,
                "暂无收藏的书籍",
                "点击书籍卡片上的收藏按钮来添加收藏"
            )
            FilterMode.RECENT -> Triple(
                Icons.Default.History,
                "暂无最近阅读记录",
                "开始阅读书籍后，这里会显示最近阅读的内容"
            )
            FilterMode.CATEGORIES -> Triple(
                Icons.Default.Category,
                "暂无分类",
                "书籍的分类信息会在扫描时自动识别"
            )
            FilterMode.CATEGORY -> Triple(
                Icons.Default.Category,
                "该分类下暂无书籍",
                "尝试其他分类或返回全部书籍"
            )
            else -> Triple(
                Icons.Default.Book,
                "暂无书籍",
                "开始扫描以添加书籍"
            )
        }
        
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
```

## 导航和路由

### 路由定义

```kotlin
sealed class Screen(val route: String) {
    object Library : Screen("library") {
        // 支持的查询参数：
        // - filter: all(默认) | favorites | recent | categories
        // - category: 分类名称（仅当 filter=categories 时使用）
        
        fun createRoute(filter: String? = null, category: String? = null): String {
            val params = mutableListOf<String>()
            filter?.let { params.add("filter=$it") }
            category?.let { params.add("category=$it") }
            return if (params.isEmpty()) route else "$route?${params.joinToString("&")}"
        }
    }
}
```

### 导航处理

在 SecondaryDrawerContent 中处理菜单项点击：

```kotlin
@Composable
fun LibraryDrawerContent(
    onItemClick: (SecondaryNavItem) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        LibraryNavItems.getItems(context).forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                icon = { Icon(item.icon, contentDescription = null) },
                selected = false,
                onClick = {
                    // 根据 item.id 导航到对应的筛选视图
                    val route = when (item.id) {
                        "all_books" -> Screen.Library.createRoute()
                        "favorites" -> Screen.Library.createRoute(filter = "favorites")
                        "recent" -> Screen.Library.createRoute(filter = "recent")
                        "categories" -> Screen.Library.createRoute(filter = "categories")
                        else -> Screen.Library.route
                    }
                    navController.navigate(route) {
                        // 避免重复导航
                        launchSingleTop = true
                    }
                    onItemClick(item)
                }
            )
        }
    }
}
```

## 错误处理

### 数据库查询错误

- 使用 try-catch 包装数据库查询操作
- 在 ViewModel 中捕获异常并更新 UI 状态
- 显示用户友好的错误消息

### 空数据处理

- 为每种筛选模式提供专门的空状态视图
- 在空状态视图中提供操作建议
- 确保空状态不会导致应用崩溃

### 导航错误

- 验证路由参数的有效性
- 对无效的分类名称进行处理
- 提供返回到默认视图的机制

## 测试策略

### 单元测试

1. **BookDao 测试**
   - 测试 getRecentBooksFlow() 返回正确的书籍列表
   - 测试 getCategoriesWithCount() 返回正确的分类统计
   - 测试边界条件（空数据库、单个书籍等）

2. **BookRepository 测试**
   - 测试数据流的正确性
   - 测试错误处理逻辑

3. **LibraryViewModel 测试**
   - 测试 setFilterMode() 正确更新状态
   - 测试 parseFilterFromRoute() 正确解析路由参数
   - 测试 displayBooks 根据筛选模式返回正确的数据

### UI 测试

1. **LibraryScreen 测试**
   - 测试不同筛选模式下的视图切换
   - 测试空状态视图的显示
   - 测试用户交互（点击分类、点击书籍等）

2. **CategoriesListView 测试**
   - 测试分类列表的渲染
   - 测试分类点击事件

### 集成测试

1. 测试从侧边栏点击到视图更新的完整流程
2. 测试导航参数的传递和解析
3. 测试数据流从数据库到 UI 的完整链路

## 性能考虑

### 数据库查询优化

- 使用索引优化 lastReadAt 字段的查询（已存在）
- 限制最近阅读列表的数量（默认 20 本）
- 使用 Flow 实现响应式数据更新，避免不必要的查询

### UI 渲染优化

- 使用 LazyColumn/LazyVerticalGrid 实现列表的懒加载
- 使用 key 参数优化列表项的重组
- 避免在 Composable 函数中进行复杂计算

### 内存管理

- 使用 StateFlow 的 WhileSubscribed 策略，在 UI 不可见时停止数据流
- 及时清理不再使用的资源
- 避免在 ViewModel 中持有大量数据

## 可访问性

- 为所有交互元素提供 contentDescription
- 确保文本对比度符合 WCAG 标准
- 支持屏幕阅读器
- 提供足够大的触摸目标（至少 48dp）

## 国际化

- 所有用户可见的文本使用字符串资源
- 支持中文和英文
- 确保 UI 布局适应不同语言的文本长度
