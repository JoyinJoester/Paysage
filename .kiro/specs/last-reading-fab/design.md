# 设计文档

## 概述

本设计文档描述了"上次阅读悬浮按钮"功能的技术实现方案。该功能在书库界面右下角添加一个Material 3风格的悬浮操作按钮（FAB），显示用户上次阅读的书籍信息和进度，点击后可快速跳转到该书籍的阅读位置。

## 架构

### 组件层次结构

```
LibraryScreen
├── Scaffold
│   ├── TopAppBar
│   ├── Content (书籍列表)
│   └── LastReadingFAB (新增)
│       ├── FloatingActionButton
│       ├── CircularProgressIndicator (进度环)
│       └── Icon (书籍图标)
```

### 数据流

```
BookRepository → LibraryViewModel → LibraryScreen → LastReadingFAB
                                                    ↓
                                            点击事件 → 导航到阅读界面
```

## 组件和接口

### 1. LastReadingFAB 组件

**位置**: `app/src/main/java/takagi/ru/paysage/ui/components/LastReadingFAB.kt`

**职责**:
- 显示悬浮操作按钮
- 渲染圆形进度指示器
- 处理点击事件
- 根据是否有上次阅读记录控制显示/隐藏

**接口**:
```kotlin
@Composable
fun LastReadingFAB(
    lastReadBook: Book?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**参数说明**:
- `lastReadBook`: 上次阅读的书籍对象，为null时不显示FAB
- `onClick`: 点击回调函数
- `modifier`: 修饰符，用于定位和样式调整

### 2. LibraryViewModel 扩展

**位置**: `app/src/main/java/takagi/ru/paysage/viewmodel/LibraryViewModel.kt`

**新增功能**:
- 添加 `lastReadBook` StateFlow，追踪最近阅读的书籍
- 实现 `getLastReadBook()` 方法，从数据库获取最近阅读的书籍

**接口**:
```kotlin
class LibraryViewModel : ViewModel() {
    // 现有代码...
    
    // 新增
    val lastReadBook: StateFlow<Book?> = bookRepository
        .getLastReadBookFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
```

### 3. BookRepository 扩展

**位置**: `app/src/main/java/takagi/ru/paysage/repository/BookRepository.kt`

**新增功能**:
- 添加 `getLastReadBookFlow()` 方法，返回最近阅读书籍的Flow
- 添加 `getLastReadBook()` 挂起函数，返回最近阅读的书籍

**接口**:
```kotlin
class BookRepository {
    // 现有代码...
    
    // 新增
    fun getLastReadBookFlow(): Flow<Book?> = bookDao.getLastReadBookFlow()
    
    suspend fun getLastReadBook(): Book? = bookDao.getLastReadBook()
}
```

### 4. BookDao 扩展

**位置**: `app/src/main/java/takagi/ru/paysage/data/dao/BookDao.kt`

**新增功能**:
- 添加查询方法，按 `lastReadAt` 降序排序，获取最近阅读的书籍

**接口**:
```kotlin
@Dao
interface BookDao {
    // 现有代码...
    
    // 新增
    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 1")
    fun getLastReadBookFlow(): Flow<Book?>
    
    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun getLastReadBook(): Book?
}
```

### 5. LibraryScreen 集成

**位置**: `app/src/main/java/takagi/ru/paysage/ui/screens/LibraryScreen.kt`

**修改内容**:
- 在 Scaffold 中添加 `floatingActionButton` 参数
- 从 ViewModel 收集 `lastReadBook` 状态
- 点击FAB时导航到阅读界面

**代码示例**:
```kotlin
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    // 现有参数...
) {
    val lastReadBook by viewModel.lastReadBook.collectAsState()
    
    Scaffold(
        // 现有参数...
        floatingActionButton = {
            LastReadingFAB(
                lastReadBook = lastReadBook,
                onClick = {
                    lastReadBook?.let { book ->
                        onBookClick(book.id)
                    }
                }
            )
        }
    ) { paddingValues ->
        // 现有内容...
    }
}
```

## 数据模型

### Book 模型（已存在）

Book 模型已包含所需字段：
- `lastReadAt: Long?` - 最后阅读时间戳
- `currentPage: Int` - 当前阅读页码
- `totalPages: Int` - 总页数

无需修改数据模型。

## UI 设计

### 悬浮按钮样式

**Material 3 规范**:
- 尺寸: 56dp x 56dp (标准FAB)
- 形状: 圆形
- 阴影: Elevation Level 3 (6dp)
- 颜色: `MaterialTheme.colorScheme.primaryContainer`
- 图标颜色: `MaterialTheme.colorScheme.onPrimaryContainer`

**位置**:
- 右下角
- 距离右边缘: 16dp
- 距离底部边缘: 16dp
- 考虑系统导航栏高度（使用 WindowInsets）

### 进度指示器

**样式**:
- 类型: 圆形进度条（CircularProgressIndicator）
- 位置: 环绕FAB外围
- 宽度: 4dp
- 颜色: `MaterialTheme.colorScheme.primary`
- 背景轨道颜色: `MaterialTheme.colorScheme.surfaceVariant`

**实现方式**:
```kotlin
Box(contentAlignment = Alignment.Center) {
    // 背景轨道
    CircularProgressIndicator(
        progress = { 1f },
        modifier = Modifier.size(64.dp),
        strokeWidth = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
    
    // 进度指示器
    CircularProgressIndicator(
        progress = { currentPage / totalPages.toFloat() },
        modifier = Modifier.size(64.dp),
        strokeWidth = 4.dp,
        color = MaterialTheme.colorScheme.primary
    )
    
    // FAB
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp)
    ) {
        Icon(Icons.Default.MenuBook, contentDescription = "继续阅读")
    }
}
```

### 图标选择

使用 Material Icons 中的 `MenuBook` 图标，表示"继续阅读"的语义。

### 动画效果

**显示/隐藏动画**:
- 使用 `AnimatedVisibility` 实现淡入淡出效果
- 进入动画: `fadeIn() + scaleIn()`
- 退出动画: `fadeOut() + scaleOut()`
- 动画时长: 300ms

**点击波纹效果**:
- Material 3 默认的 Ripple 效果
- 无需额外配置

**进度更新动画**:
- 使用 `animateFloatAsState` 平滑过渡进度值
- 动画时长: 500ms
- 缓动曲线: `FastOutSlowInEasing`

## 错误处理

### 场景1: 书籍文件不存在

**检测时机**: 点击FAB时

**处理方式**:
1. 在 `onClick` 回调中检查文件是否存在
2. 如果文件不存在，显示 Snackbar 提示用户
3. 提示内容: "该书籍文件已被移动或删除"
4. 自动从数据库中移除该书籍记录

**代码示例**:
```kotlin
onClick = {
    lastReadBook?.let { book ->
        val file = File(book.filePath)
        if (file.exists()) {
            onBookClick(book.id)
        } else {
            // 显示错误提示
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "该书籍文件已被移动或删除",
                    actionLabel = "确定"
                )
            }
            // 可选：从数据库删除
            viewModel.deleteBook(book)
        }
    }
}
```

### 场景2: 数据库查询失败

**检测时机**: ViewModel 初始化时

**处理方式**:
1. 在 Flow 收集时捕获异常
2. 记录错误日志
3. 返回 null，隐藏FAB
4. 不向用户显示错误（静默失败）

### 场景3: 无阅读记录

**检测时机**: 渲染时

**处理方式**:
- `lastReadBook` 为 null 时，不渲染FAB
- 使用条件渲染: `if (lastReadBook != null) { ... }`

## 无障碍支持

### 内容描述

为FAB提供清晰的内容描述：
```kotlin
contentDescription = "继续阅读 ${lastReadBook?.title}"
```

### 语义信息

使用 `semantics` 修饰符提供额外信息：
```kotlin
.semantics {
    stateDescription = "阅读进度 ${progress}%"
    role = Role.Button
}
```

### 触摸目标

- FAB 尺寸 56dp 已满足最小触摸目标要求（48dp）
- 无需额外调整

### TalkBack 支持

- 自动支持，无需额外配置
- 点击时会朗读内容描述和状态描述

## 性能优化

### 数据库查询优化

**策略**:
- 使用索引加速查询
- 在 `lastReadAt` 字段上创建索引

**实现**:
```kotlin
@Entity(
    tableName = "books",
    indices = [Index(value = ["lastReadAt"], name = "index_books_lastReadAt")]
)
data class Book(...)
```

### 内存优化

**策略**:
- 只查询必要字段（id, title, currentPage, totalPages, filePath）
- 避免加载封面图片等大数据

**实现**:
创建轻量级数据类：
```kotlin
data class LastReadBookInfo(
    val id: Long,
    val title: String,
    val currentPage: Int,
    val totalPages: Int,
    val filePath: String
)
```

### UI 渲染优化

**策略**:
- 使用 `remember` 缓存计算结果
- 避免不必要的重组

**实现**:
```kotlin
val progress = remember(lastReadBook) {
    lastReadBook?.let { 
        it.currentPage.toFloat() / it.totalPages.coerceAtLeast(1)
    } ?: 0f
}
```

## 测试策略

### 单元测试

**测试目标**: BookDao, BookRepository

**测试用例**:
1. `getLastReadBook_returnsNull_whenNoBooks()` - 无书籍时返回null
2. `getLastReadBook_returnsNull_whenNoReadBooks()` - 无阅读记录时返回null
3. `getLastReadBook_returnsMostRecent_whenMultipleBooks()` - 多本书时返回最近阅读的
4. `getLastReadBook_ignoresNullLastReadAt()` - 忽略lastReadAt为null的书籍

### UI 测试

**测试目标**: LastReadingFAB, LibraryScreen

**测试用例**:
1. `fab_notDisplayed_whenNoLastReadBook()` - 无阅读记录时不显示FAB
2. `fab_displayed_whenHasLastReadBook()` - 有阅读记录时显示FAB
3. `fab_showsCorrectProgress()` - 显示正确的进度
4. `fab_navigatesToBook_whenClicked()` - 点击后导航到书籍
5. `fab_showsError_whenFileNotExists()` - 文件不存在时显示错误

### 集成测试

**测试场景**:
1. 用户打开应用 → 显示上次阅读的书籍FAB
2. 用户点击FAB → 跳转到阅读界面并定位到上次位置
3. 用户阅读书籍 → 更新lastReadAt → FAB更新
4. 用户删除书籍文件 → 点击FAB → 显示错误提示

## 国际化

### 字符串资源

**位置**: `app/src/main/res/values/strings.xml`

**新增字符串**:
```xml
<string name="continue_reading">继续阅读</string>
<string name="continue_reading_with_title">继续阅读 %1$s</string>
<string name="reading_progress">阅读进度 %1$d%%</string>
<string name="book_file_not_found">该书籍文件已被移动或删除</string>
```

**中文版本**: `app/src/main/res/values-zh/strings.xml`
```xml
<string name="continue_reading">继续阅读</string>
<string name="continue_reading_with_title">继续阅读 %1$s</string>
<string name="reading_progress">阅读进度 %1$d%%</string>
<string name="book_file_not_found">该书籍文件已被移动或删除</string>
```

## 依赖关系

### 现有依赖

- Jetpack Compose (UI框架)
- Material 3 (设计系统)
- Room (数据库)
- Kotlin Coroutines (异步处理)
- Kotlin Flow (响应式数据流)

### 无需新增依赖

所有功能都可以使用现有依赖实现。

## 实现顺序

1. **数据层** - 扩展 BookDao 和 BookRepository
2. **ViewModel层** - 扩展 LibraryViewModel
3. **UI组件** - 创建 LastReadingFAB 组件
4. **集成** - 在 LibraryScreen 中集成FAB
5. **字符串资源** - 添加国际化字符串
6. **测试** - 编写单元测试和UI测试

## 潜在问题和解决方案

### 问题1: FAB 遮挡底部书籍

**解决方案**: 
- 为书籍列表添加底部内边距
- 使用 `contentPadding` 参数，底部增加 80dp 空间

### 问题2: 横屏时FAB位置不合适

**解决方案**:
- 使用 `WindowInsets` 自动适配
- 在横屏时可能需要调整位置到右侧中间

### 问题3: 多个FAB冲突

**解决方案**:
- 如果未来添加其他FAB，使用 `FloatingActionButtonMenu` 组合多个操作
- 或者使用 `ExtendedFloatingActionButton` 显示文字标签

### 问题4: 进度环性能问题

**解决方案**:
- 使用 `remember` 缓存进度值
- 限制动画帧率
- 考虑使用自定义绘制代替多层Composable

## 未来扩展

### 可能的增强功能

1. **长按显示详情** - 长按FAB显示书籍详情卡片
2. **滑动切换** - 支持左右滑动切换最近阅读的多本书
3. **自定义位置** - 允许用户自定义FAB位置
4. **主题适配** - 根据书籍封面颜色动态调整FAB颜色
5. **阅读时长统计** - 显示累计阅读时长

### 技术债务

- 考虑将FAB逻辑抽象为独立的ViewModel
- 评估是否需要缓存策略避免频繁查询数据库
- 考虑使用 WorkManager 定期清理过期的阅读记录
