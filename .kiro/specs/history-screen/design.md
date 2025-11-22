# 历史记录功能设计文档

## 概述

本文档描述历史记录功能的详细设计，包括数据模型、UI组件、导航集成和状态管理。该功能允许用户通过右滑手势从主页进入历史记录页面，查看和管理下载历史。

## 架构

### 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────────────┐         ┌──────────────────────┐  │
│  │  LibraryScreen   │◄────────┤  HistoryScreen       │  │
│  │  (主页)          │  Swipe  │  (历史记录页面)      │  │
│  └────────┬─────────┘         └──────────┬───────────┘  │
│           │                               │              │
│           │                               │              │
│  ┌────────▼─────────────────────────────▼───────────┐   │
│  │           HistoryViewModel                        │   │
│  │  - historyItems: StateFlow<List<HistoryItem>>    │   │
│  │  - loadHistory()                                  │   │
│  │  - deleteHistoryItem()                            │   │
│  │  - clearAllHistory()                              │   │
│  └────────────────────┬──────────────────────────────┘   │
└─────────────────────────┼──────────────────────────────┘
                          │
┌─────────────────────────▼──────────────────────────────┐
│                    Domain Layer                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │           HistoryRepository                       │  │
│  │  - getHistoryItems(): Flow<List<HistoryItem>>    │  │
│  │  - insertHistoryItem(item: HistoryItem)          │  │
│  │  - deleteHistoryItem(id: Long)                   │  │
│  │  - clearAllHistory()                             │  │
│  │  - updateProgress(id: Long, progress: Float)     │  │
│  └────────────────────┬─────────────────────────────┘  │
└─────────────────────────┼──────────────────────────────┘
                          │
┌─────────────────────────▼──────────────────────────────┐
│                    Data Layer                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │           HistoryDao (Room)                       │  │
│  │  - getAllHistory(): Flow<List<HistoryEntity>>    │  │
│  │  - insertHistory(entity: HistoryEntity)          │  │
│  │  - deleteHistory(id: Long)                       │  │
│  │  - deleteAllHistory()                            │  │
│  │  - updateProgress(id: Long, progress: Float)     │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │           PaysageDatabase                         │  │
│  │  - historyDao(): HistoryDao                      │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 导航流程

```
┌──────────────┐    Right Swipe    ┌──────────────┐
│              │──────────────────►│              │
│ LibraryScreen│                   │HistoryScreen │
│   (主页)     │◄──────────────────│  (历史记录)  │
└──────────────┘    Left Swipe     └──────────────┘
```

## 组件和接口

### 1. 数据模型

#### HistoryItem (数据类)

```kotlin
data class HistoryItem(
    val id: Long = 0,
    val title: String,              // 内容标题
    val thumbnailPath: String?,     // 缩略图路径
    val fileType: String,           // 文件类型 (ZIP, PDF, etc.)
    val fileSize: Long,             // 文件大小（字节）
    val filePath: String,           // 文件路径
    val downloadTime: Long,         // 下载时间（时间戳）
    val progress: Float,            // 下载进度 (0.0 - 1.0)
    val status: DownloadStatus      // 下载状态
)

enum class DownloadStatus {
    DOWNLOADING,    // 下载中
    COMPLETED,      // 已完成
    FAILED,         // 失败
    PAUSED          // 暂停
}
```

#### HistoryEntity (Room实体)

```kotlin
@Entity(
    tableName = "download_history",
    indices = [
        Index(value = ["download_time"], name = "idx_history_download_time")
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String?,
    
    @ColumnInfo(name = "file_type")
    val fileType: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "download_time")
    val downloadTime: Long,
    
    @ColumnInfo(name = "progress")
    val progress: Float,
    
    @ColumnInfo(name = "status")
    val status: String
)
```

### 2. 数据访问层

#### HistoryDao

```kotlin
@Dao
interface HistoryDao {
    @Query("SELECT * FROM download_history ORDER BY download_time DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM download_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): HistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: HistoryEntity): Long
    
    @Update
    suspend fun updateHistory(entity: HistoryEntity)
    
    @Query("UPDATE download_history SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Float)
    
    @Query("UPDATE download_history SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    
    @Delete
    suspend fun deleteHistory(entity: HistoryEntity)
    
    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
    
    @Query("DELETE FROM download_history")
    suspend fun deleteAllHistory()
    
    @Query("SELECT COUNT(*) FROM download_history")
    suspend fun getHistoryCount(): Int
}
```

### 3. Repository层

#### HistoryRepository

```kotlin
interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryItem>>
    suspend fun getHistoryById(id: Long): HistoryItem?
    suspend fun insertHistory(item: HistoryItem): Long
    suspend fun updateProgress(id: Long, progress: Float)
    suspend fun updateStatus(id: Long, status: DownloadStatus)
    suspend fun deleteHistory(id: Long)
    suspend fun clearAllHistory()
    suspend fun getHistoryCount(): Int
}

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao
) : HistoryRepository {
    // 实现接口方法
    // 负责Entity和HistoryItem之间的转换
}
```

### 4. ViewModel层

#### HistoryViewModel

```kotlin
class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {
    
    // 历史记录列表
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // 选中的历史记录项（用于操作菜单）
    private val _selectedItem = MutableStateFlow<HistoryItem?>(null)
    val selectedItem: StateFlow<HistoryItem?> = _selectedItem.asStateFlow()
    
    init {
        loadHistory()
    }
    
    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllHistory().collect { items ->
                    _historyItems.value = items
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteHistory(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllHistory()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun selectItem(item: HistoryItem?) {
        _selectedItem.value = item
    }
    
    fun clearError() {
        _error.value = null
    }
}
```

### 5. UI组件

#### HistoryScreen

主要的历史记录屏幕组件，包含：
- 顶部应用栏（标题和菜单按钮）
- 历史记录列表
- 空状态视图
- 加载指示器

```kotlin
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
    onItemClick: (HistoryItem) -> Unit
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            HistoryTopBar(
                onBackClick = onBackClick,
                onClearAllClick = { /* 显示确认对话框 */ }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> LoadingView()
            historyItems.isEmpty() -> EmptyHistoryView()
            else -> HistoryList(
                items = historyItems,
                onItemClick = onItemClick,
                onItemLongClick = { /* 显示操作菜单 */ }
            )
        }
    }
}
```

#### HistoryListItem

单个历史记录项的UI组件：

```kotlin
@Composable
fun HistoryListItem(
    item: HistoryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 缩略图
            HistoryThumbnail(
                thumbnailPath = item.thumbnailPath,
                modifier = Modifier.size(80.dp)
            )
            
            // 内容信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 文件信息行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 文件类型标签
                    FileTypeChip(fileType = item.fileType)
                    
                    // 文件大小
                    Text(
                        text = formatFileSize(item.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 下载时间
                Text(
                    text = formatDownloadTime(item.downloadTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 进度条
                LinearProgressIndicator(
                    progress = item.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
    }
}
```

#### 辅助组件

```kotlin
// 缩略图组件
@Composable
fun HistoryThumbnail(
    thumbnailPath: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (thumbnailPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailPath)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 文件类型标签
@Composable
fun FileTypeChip(fileType: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = fileType,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// 空状态视图
@Composable
fun EmptyHistoryView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 6. 滑动手势集成

使用HorizontalPager实现主页和历史记录页面之间的滑动切换：

```kotlin
@Composable
fun LibraryWithHistoryPager(
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> LibraryScreen(
                onBookClick = onBookClick,
                onSettingsClick = onSettingsClick,
                onOpenDrawer = onOpenDrawer
            )
            1 -> HistoryScreen(
                onBackClick = {
                    // 滑动回主页
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                onItemClick = { item ->
                    // 打开下载的文件
                }
            )
        }
    }
}
```

## 错误处理

### 错误类型

1. **数据库错误**: 读写历史记录失败
2. **文件访问错误**: 缩略图或文件路径无效
3. **内存错误**: 历史记录列表过大

### 错误处理策略

```kotlin
sealed class HistoryError {
    data class DatabaseError(val message: String) : HistoryError()
    data class FileAccessError(val path: String) : HistoryError()
    data class UnknownError(val throwable: Throwable) : HistoryError()
}

// 在ViewModel中处理错误
private fun handleError(error: Throwable) {
    val historyError = when (error) {
        is SQLiteException -> HistoryError.DatabaseError(error.message ?: "Database error")
        is FileNotFoundException -> HistoryError.FileAccessError(error.message ?: "File not found")
        else -> HistoryError.UnknownError(error)
    }
    
    _error.value = historyError.toUserMessage()
}
```

## 测试策略

### 单元测试

1. **HistoryRepository测试**
   - 测试CRUD操作
   - 测试数据转换
   - 测试错误处理

2. **HistoryViewModel测试**
   - 测试状态管理
   - 测试用户操作
   - 测试错误处理

### UI测试

1. **HistoryScreen测试**
   - 测试列表显示
   - 测试空状态
   - 测试加载状态
   - 测试用户交互

2. **滑动手势测试**
   - 测试页面切换
   - 测试动画效果

### 集成测试

1. **端到端测试**
   - 测试从主页滑动到历史记录
   - 测试历史记录的创建和显示
   - 测试历史记录的删除

## 性能考虑

### 优化策略

1. **分页加载**: 当历史记录超过100条时，使用Paging 3库实现分页
2. **图片缓存**: 使用Coil的内存和磁盘缓存
3. **懒加载**: 使用LazyColumn实现列表的懒加载
4. **数据库索引**: 在download_time字段上创建索引以优化查询

### 内存管理

```kotlin
// 限制历史记录数量
companion object {
    const val MAX_HISTORY_ITEMS = 500
}

// 定期清理旧记录
suspend fun cleanOldHistory() {
    val count = repository.getHistoryCount()
    if (count > MAX_HISTORY_ITEMS) {
        // 删除最旧的记录
        repository.deleteOldestHistory(count - MAX_HISTORY_ITEMS)
    }
}
```

## 国际化

需要添加的字符串资源：

```xml
<!-- strings.xml -->
<string name="history_title">历史记录</string>
<string name="history_empty">暂无历史记录</string>
<string name="history_clear_all">清空历史记录</string>
<string name="history_clear_confirm">确定要清空所有历史记录吗？</string>
<string name="history_delete_item">删除记录</string>
<string name="history_redownload">重新下载</string>
<string name="history_open_file">打开文件</string>
<string name="history_file_not_found">文件不存在</string>
```

## 数据库迁移

添加历史记录表的迁移：

```kotlin
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS download_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                thumbnail_path TEXT,
                file_type TEXT NOT NULL,
                file_size INTEGER NOT NULL,
                file_path TEXT NOT NULL,
                download_time INTEGER NOT NULL,
                progress REAL NOT NULL,
                status TEXT NOT NULL
            )
        """)
        
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_history_download_time ON download_history(download_time)"
        )
    }
}
```

## 安全性考虑

1. **文件路径验证**: 确保文件路径在应用的沙盒内
2. **SQL注入防护**: 使用Room的参数化查询
3. **权限检查**: 访问文件前检查存储权限

## 可访问性

1. **内容描述**: 为所有图标和图片提供contentDescription
2. **触摸目标**: 确保所有可点击元素至少48dp
3. **对比度**: 确保文本和背景的对比度符合WCAG标准
4. **屏幕阅读器**: 支持TalkBack等屏幕阅读器
