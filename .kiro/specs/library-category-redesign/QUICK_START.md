# 书库分类系统 - 快速入门指南

## 🚀 快速开始

本指南帮助你快速集成和使用已完成的书库分类系统功能。

## 📋 前置条件

1. 项目已编译通过
2. 数据库已迁移到版本4
3. 所有依赖已正确配置

## 🔧 基础集成

### 1. 在ViewModel中使用分类功能

```kotlin
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.data.model.DisplayMode
import takagi.ru.paysage.repository.getBooksByCategoryFlow

class YourViewModel : ViewModel() {
    private val repository = BookRepository(context)
    
    // 获取漫画分类的本地书籍
    val mangaBooks = repository.getBooksByCategoryFlow(
        categoryType = CategoryType.MANGA,
        displayMode = DisplayMode.LOCAL
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 获取小说分类的在线书籍
    val onlineNovels = repository.getBooksByCategoryFlow(
        categoryType = CategoryType.NOVEL,
        displayMode = DisplayMode.ONLINE
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### 2. 在UI中使用CategoryFilterBar

```kotlin
import takagi.ru.paysage.ui.components.CategoryFilterBar
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.data.model.DisplayMode

@Composable
fun YourScreen() {
    var selectedCategory by remember { mutableStateOf(CategoryType.MANGA) }
    var displayMode by remember { mutableStateOf(DisplayMode.LOCAL) }
    
    Column {
        CategoryFilterBar(
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it },
            displayMode = displayMode,
            onDisplayModeChange = { displayMode = it }
        )
        
        // 根据选择显示内容
        when (selectedCategory) {
            CategoryType.MANGA -> MangaContent(displayMode)
            CategoryType.NOVEL -> NovelContent(displayMode)
        }
    }
}
```

### 3. 使用在线书源功能

```kotlin
import takagi.ru.paysage.viewmodel.OnlineSourceViewModel
import takagi.ru.paysage.ui.screens.OnlineSourceScreen

@Composable
fun OnlineSourcesRoute(
    categoryType: CategoryType,
    onBackClick: () -> Unit
) {
    OnlineSourceScreen(
        categoryType = categoryType,
        onBackClick = onBackClick
    )
}
```

## 📚 常用API

### Repository扩展

```kotlin
// 获取指定分类的书籍
suspend fun getBooksByCategory(
    categoryType: CategoryType,
    displayMode: DisplayMode
): List<Book>

// 获取指定分类的书籍（Flow）
fun getBooksByCategoryFlow(
    categoryType: CategoryType,
    displayMode: DisplayMode
): Flow<List<Book>>

// 更新书籍分类
suspend fun updateBookCategoryType(
    bookId: Long,
    categoryType: CategoryType
)

// 获取分类统计
suspend fun getBookCountByCategoryType(
    categoryType: CategoryType
): Int
```

### ViewModel扩展

```kotlin
// 设置分类类型
fun setCategoryType(type: CategoryType)

// 设置显示模式
fun setDisplayMode(mode: DisplayMode)

// 切换分类类型
fun toggleCategoryType()

// 切换显示模式
fun toggleDisplayMode()

// 获取分类统计信息
suspend fun getCategoryStatistics(): CategoryStatistics
```

### 在线书源管理

```kotlin
// OnlineSourceViewModel
val allSources: StateFlow<List<BookSource>>
val enabledSources: StateFlow<List<BookSource>>

fun addSource(source: BookSource)
fun updateSource(source: BookSource)
fun deleteSource(source: BookSource)
fun toggleSourceEnabled(id: Long)
```

## 🎨 使用M3E颜色

```kotlin
import takagi.ru.paysage.ui.theme.CategoryColors
import takagi.ru.paysage.ui.theme.getCategoryColor
import takagi.ru.paysage.ui.theme.getCategoryContainerColor

@Composable
fun CategoryBadge(categoryType: CategoryType) {
    val isDark = isSystemInDarkTheme()
    
    Surface(
        color = getCategoryContainerColor(categoryType, isDark)
    ) {
        Text(
            text = when (categoryType) {
                CategoryType.MANGA -> "漫画"
                CategoryType.NOVEL -> "阅读"
            },
            color = getCategoryColor(categoryType, isDark)
        )
    }
}
```

## 🔄 数据迁移

### 自动迁移

数据库会自动从版本3迁移到版本4，包括：
- 添加新字段
- 创建索引
- 根据文件格式自动分类现有书籍

### 手动分类

如果需要手动更改书籍分类：

```kotlin
viewModel.updateBookCategory(bookId, CategoryType.NOVEL)
```

## 🌐 导航集成

### 使用新的导航项

```kotlin
import takagi.ru.paysage.navigation.PrimaryNavItem
import takagi.ru.paysage.navigation.LocalLibraryNavItems
import takagi.ru.paysage.navigation.OnlineLibraryNavItems

// 本地书库导航
val localItems = LocalLibraryNavItems.getItems(context)
// 包含：漫画、阅读

// 在线书库导航
val onlineItems = OnlineLibraryNavItems.getItems(context)
// 包含：漫画书源、小说书源
```

### 路由配置

```kotlin
// 本地漫画
"library?category=manga"

// 本地小说
"library?category=novel"

// 在线漫画书源
"online?category=manga"

// 在线小说书源
"online?category=novel"
```

## 🐛 常见问题

### 1. 扩展函数找不到

**问题**: 编译错误，找不到扩展函数

**解决**: 手动导入扩展函数

```kotlin
import takagi.ru.paysage.repository.getBooksByCategory
import takagi.ru.paysage.repository.getBooksByCategoryFlow
```

### 2. 数据库迁移失败

**问题**: 应用崩溃，数据库版本不匹配

**解决**: 
- 清除应用数据重新安装
- 或者使用 `fallbackToDestructiveMigration()`

### 3. 缓存不更新

**问题**: 数据更新后UI不刷新

**解决**: 清除缓存

```kotlin
repository.clearBookCache()
```

### 4. 颜色不显示

**问题**: 分类颜色显示为默认颜色

**解决**: 确保导入了CategoryColors

```kotlin
import takagi.ru.paysage.ui.theme.CategoryColors
```

## 📖 示例代码

### 完整的分类书库屏幕

```kotlin
@Composable
fun CategoryLibraryScreen(
    viewModel: LibraryViewModel = viewModel()
) {
    var selectedCategory by remember { mutableStateOf(CategoryType.MANGA) }
    var displayMode by remember { mutableStateOf(DisplayMode.LOCAL) }
    
    val books by viewModel.getBooksByCategoryFlow(
        selectedCategory,
        displayMode
    ).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("书库") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 分类筛选栏
            CategoryFilterBar(
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it },
                displayMode = displayMode,
                onDisplayModeChange = { displayMode = it }
            )
            
            // 书籍列表
            LazyColumn {
                items(books, key = { it.id }) { book ->
                    BookListItem(
                        book = book,
                        onClick = { /* 打开书籍 */ }
                    )
                }
            }
        }
    }
}
```

### 书源管理屏幕

```kotlin
@Composable
fun SourceManagementScreen(
    categoryType: CategoryType,
    viewModel: OnlineSourceViewModel = viewModel()
) {
    LaunchedEffect(categoryType) {
        viewModel.setSelectedCategoryType(categoryType)
    }
    
    val sources by viewModel.sourcesByCategory.collectAsState()
    
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* 添加书源 */ }
            ) {
                Icon(Icons.Default.Add, "添加")
            }
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
                    onClick = { /* 打开书源详情 */ },
                    onToggleEnabled = {
                        viewModel.toggleSourceEnabled(source.id)
                    }
                )
            }
        }
    }
}
```

## 🔗 相关文档

- [完整设计文档](design.md)
- [需求文档](requirements.md)
- [实现状态](IMPLEMENTATION_STATUS.md)
- [最终总结](FINAL_SUMMARY.md)
- [M3E设计指南](../../M3_EXPRESSIVE_DESIGN.md)

## 💡 最佳实践

### 1. 使用Flow而不是挂起函数

```kotlin
// ✅ 推荐
val books = repository.getBooksByCategoryFlow(type, mode)
    .collectAsState(initial = emptyList())

// ❌ 不推荐（需要手动管理生命周期）
LaunchedEffect(type, mode) {
    val books = repository.getBooksByCategory(type, mode)
}
```

### 2. 利用缓存提升性能

```kotlin
// Repository会自动缓存查询结果
// 相同的查询会直接返回缓存数据
val books1 = repository.getBooksByCategory(CategoryType.MANGA, DisplayMode.LOCAL)
val books2 = repository.getBooksByCategory(CategoryType.MANGA, DisplayMode.LOCAL) // 从缓存读取
```

### 3. 使用分类颜色保持一致性

```kotlin
// 始终使用getCategoryColor而不是硬编码颜色
val color = getCategoryColor(categoryType, isDark)
```

### 4. 处理空状态

```kotlin
when {
    books.isEmpty() -> EmptyView()
    else -> BookList(books)
}
```

## 🎯 下一步

1. 集成CategoryFilterBar到现有的LibraryScreen
2. 添加路由配置支持分类导航
3. 实现AddSourceDialog组件
4. 添加分类切换动画
5. 完善错误处理和加载状态

## 📞 获取帮助

如果遇到问题：
1. 查看[常见问题](#-常见问题)
2. 阅读[完整文档](design.md)
3. 检查[实现状态](IMPLEMENTATION_STATUS.md)

---

**版本**: 1.0.0-alpha  
**更新日期**: 2025-10-28
