# 设计文档

## 概述

本设计文档详细说明如何将"本地功能"和"在线功能"模块重命名为"本地管理"和"在线管理"，并按照Material 3 Extended (M3e)设计规范对相关UI进行全面重构。同时，在两个管理模块的抽屉菜单中添加"创建文件夹"功能。

设计遵循以下核心原则：
- 保持与现有M3e设计系统的一致性
- 最小化代码变更，优先使用现有组件
- 确保向后兼容性和数据完整性
- 提供流畅的用户体验和无障碍访问

## 架构

### 系统架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                     Paysage Application                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │  本地管理模块     │         │  在线管理模块     │          │
│  │ (LocalManagement)│         │(OnlineManagement)│          │
│  └────────┬─────────┘         └────────┬─────────┘          │
│           │                            │                     │
│           ├─ 导航抽屉 UI               ├─ 导航抽屉 UI        │
│           ├─ 文件夹创建对话框          ├─ 文件夹创建对话框   │
│           ├─ 文件夹列表显示            ├─ 文件夹列表显示     │
│           └─ M3e 组件库               └─ M3e 组件库        │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│                    共享组件层                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 字符串资源    │  │ 主题系统      │  │ 导航系统      │      │
│  │ (strings.xml)│  │ (Theme.kt)   │  │(Navigation.kt)│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│                    数据层                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ ViewModel    │  │ Repository   │  │ Database     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 模块依赖关系

```mermaid
graph TD
    A[UI Layer] --> B[ViewModel Layer]
    B --> C[Repository Layer]
    C --> D[Data Source Layer]
    
    A --> E[Theme System]
    A --> F[Navigation System]
    A --> G[String Resources]
    
    E --> H[M3e Design Tokens]
    F --> I[Navigation State]


## 组件和接口

### 1. 字符串资源更新

#### 文件位置
- `app/src/main/res/values-zh/strings.xml`

#### 修改内容
```xml
<!-- 修改前 -->
<string name="nav_local_library">本地功能</string>
<string name="nav_online_library">在线功能</string>
<string name="nav_open_local_library">打开本地书库菜单</string>
<string name="nav_open_online_library">打开在线书库菜单</string>

<!-- 修改后 -->
<string name="nav_local_library">本地管理</string>
<string name="nav_online_library">在线管理</string>
<string name="nav_open_local_library">打开本地管理菜单</string>
<string name="nav_open_online_library">打开在线管理菜单</string>
```

#### 新增字符串资源
```xml
<!-- 文件夹创建功能 -->
<string name="create_folder">创建文件夹</string>
<string name="create_folder_desc">在当前目录创建新文件夹</string>
<string name="folder_name">文件夹名称</string>
<string name="folder_name_hint">请输入文件夹名称</string>
<string name="folder_name_empty">文件夹名称不能为空</string>
<string name="folder_name_invalid">文件夹名称包含非法字符</string>
<string name="folder_name_exists">文件夹名称已存在</string>
<string name="folder_name_too_long">文件夹名称过长（最多255个字符）</string>
<string name="folder_create_success">文件夹创建成功</string>
<string name="folder_create_failed">文件夹创建失败：%s</string>
<string name="folder_creating">正在创建文件夹...</string>
```

### 2. 导航抽屉组件重构

#### SecondaryDrawerContent.kt 增强

**位置**: `app/src/main/java/takagi/ru/paysage/navigation/SecondaryDrawerContent.kt`

**设计要点**:
- 在本地管理和在线管理的抽屉内容顶部添加"创建文件夹"按钮
- 使用M3e标准的FloatingActionButton或FilledTonalButton
- 按钮位置：标题下方，列表项上方
- 应用M3e标准间距（16dp）

**组件结构**:
```kotlin
@Composable
fun LibraryDrawerContent(
    onItemClick: (SecondaryNavItem) -> Unit,
    onCreateFolderClick: () -> Unit,  // 新增参数
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 创建文件夹按钮
        CreateFolderButton(
            onClick = onCreateFolderClick,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 现有的导航项列表
        LibraryNavItems.getItems(context).forEach { item ->
            // ... 现有代码
        }
    }
}
```

### 3. 文件夹创建对话框组件

#### CreateFolderDialog.kt (新建文件)

**位置**: `app/src/main/java/takagi/ru/paysage/ui/components/CreateFolderDialog.kt`

**接口定义**:
```kotlin
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (folderName: String) -> Unit,
    existingFolderNames: List<String> = emptyList(),
    isCreating: Boolean = false
)
```

**M3e设计规范应用**:
- 使用`AlertDialog`组件
- 标题：使用`MaterialTheme.typography.headlineSmall`
- 输入框：使用`OutlinedTextField`，应用M3e标准样式
- 按钮：使用`TextButton`（取消）和`FilledTonalButton`（确定）
- 间距：遵循8dp基准网格
- 圆角：使用`MaterialTheme.shapes.extraLarge`（28dp）
- 动画：使用`AnimatedVisibility`和M3e标准缓动曲线

**验证逻辑**:
```kotlin
private fun validateFolderName(
    name: String,
    existingNames: List<String>
): FolderNameValidation {
    return when {
        name.isBlank() -> FolderNameValidation.Empty
        name.length > 255 -> FolderNameValidation.TooLong
        name.containsIllegalChars() -> FolderNameValidation.InvalidChars
        name in existingNames -> FolderNameValidation.AlreadyExists
        else -> FolderNameValidation.Valid
    }
}

private fun String.containsIllegalChars(): Boolean {
    val illegalChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    return any { it in illegalChars }
}

sealed class FolderNameValidation {
    object Valid : FolderNameValidation()
    object Empty : FolderNameValidation()
    object TooLong : FolderNameValidation()
    object InvalidChars : FolderNameValidation()
    object AlreadyExists : FolderNameValidation()
}
```


### 4. 文件夹管理ViewModel

#### FolderViewModel.kt (新建文件)

**位置**: `app/src/main/java/takagi/ru/paysage/viewmodel/FolderViewModel.kt`

**职责**:
- 管理文件夹创建状态
- 执行文件夹创建操作
- 提供文件夹列表数据
- 处理错误和反馈

**接口定义**:
```kotlin
class FolderViewModel(
    private val folderRepository: FolderRepository
) : ViewModel() {
    
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()
    
    private val _createFolderState = MutableStateFlow<CreateFolderState>(CreateFolderState.Idle)
    val createFolderState: StateFlow<CreateFolderState> = _createFolderState.asStateFlow()
    
    fun createFolder(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ) {
        viewModelScope.launch {
            _createFolderState.value = CreateFolderState.Creating
            try {
                val result = folderRepository.createFolder(
                    parentPath = parentPath,
                    folderName = folderName,
                    moduleType = moduleType
                )
                _createFolderState.value = CreateFolderState.Success(result)
                refreshFolders(parentPath, moduleType)
            } catch (e: Exception) {
                _createFolderState.value = CreateFolderState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun refreshFolders(path: String, moduleType: ModuleType) {
        viewModelScope.launch {
            _folders.value = folderRepository.getFolders(path, moduleType)
        }
    }
    
    fun resetCreateFolderState() {
        _createFolderState.value = CreateFolderState.Idle
    }
}

sealed class CreateFolderState {
    object Idle : CreateFolderState()
    object Creating : CreateFolderState()
    data class Success(val folder: Folder) : CreateFolderState()
    data class Error(val message: String) : CreateFolderState()
}

enum class ModuleType {
    LOCAL_MANAGEMENT,
    ONLINE_MANAGEMENT
}
```

### 5. 文件夹Repository

#### FolderRepository.kt (新建文件)

**位置**: `app/src/main/java/takagi/ru/paysage/repository/FolderRepository.kt`

**接口定义**:
```kotlin
interface FolderRepository {
    suspend fun createFolder(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ): Folder
    
    suspend fun getFolders(
        path: String,
        moduleType: ModuleType
    ): List<Folder>
    
    suspend fun deleteFolder(
        folderPath: String,
        moduleType: ModuleType
    ): Boolean
}

class FolderRepositoryImpl(
    private val context: Context,
    private val database: PaysageDatabase
) : FolderRepository {
    
    override suspend fun createFolder(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ): Folder = withContext(Dispatchers.IO) {
        val folderPath = File(parentPath, folderName)
        
        // 检查文件夹是否已存在
        if (folderPath.exists()) {
            throw FolderAlreadyExistsException(folderName)
        }
        
        // 创建文件夹
        val created = folderPath.mkdirs()
        if (!created) {
            throw FolderCreationException("Failed to create folder: $folderName")
        }
        
        // 保存到数据库
        val folder = Folder(
            id = 0,
            name = folderName,
            path = folderPath.absolutePath,
            parentPath = parentPath,
            moduleType = moduleType,
            createdAt = System.currentTimeMillis()
        )
        
        val folderId = database.folderDao().insert(folder)
        folder.copy(id = folderId)
    }
    
    override suspend fun getFolders(
        path: String,
        moduleType: ModuleType
    ): List<Folder> = withContext(Dispatchers.IO) {
        database.folderDao().getFoldersByPath(path, moduleType)
            .sortedBy { it.name.lowercase() }
    }
    
    override suspend fun deleteFolder(
        folderPath: String,
        moduleType: ModuleType
    ): Boolean = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        val deleted = folder.deleteRecursively()
        if (deleted) {
            database.folderDao().deleteByPath(folderPath)
        }
        deleted
    }
}

class FolderAlreadyExistsException(folderName: String) : 
    Exception("Folder already exists: $folderName")

class FolderCreationException(message: String) : Exception(message)
```

### 6. 数据模型

#### Folder.kt (新建文件)

**位置**: `app/src/main/java/takagi/ru/paysage/data/model/Folder.kt`

```kotlin
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "path")
    val path: String,
    
    @ColumnInfo(name = "parent_path")
    val parentPath: String,
    
    @ColumnInfo(name = "module_type")
    val moduleType: ModuleType,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = createdAt
)
```

#### FolderDao.kt (新建文件)

**位置**: `app/src/main/java/takagi/ru/paysage/data/dao/FolderDao.kt`

```kotlin
@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder): Long
    
    @Query("SELECT * FROM folders WHERE parent_path = :path AND module_type = :moduleType")
    suspend fun getFoldersByPath(path: String, moduleType: ModuleType): List<Folder>
    
    @Query("DELETE FROM folders WHERE path = :path")
    suspend fun deleteByPath(path: String)
    
    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): Folder?
}
```

## 数据模型

### 文件夹数据结构

```kotlin
data class Folder(
    val id: Long,
    val name: String,
    val path: String,
    val parentPath: String,
    val moduleType: ModuleType,
    val createdAt: Long,
    val updatedAt: Long
)
```

### 模块类型枚举

```kotlin
enum class ModuleType {
    LOCAL_MANAGEMENT,    // 本地管理
    ONLINE_MANAGEMENT    // 在线管理
}
```

### 数据库Schema更新

```sql
CREATE TABLE IF NOT EXISTS folders (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    path TEXT NOT NULL UNIQUE,
    parent_path TEXT NOT NULL,
    module_type TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_folders_parent_path ON folders(parent_path);
CREATE INDEX idx_folders_module_type ON folders(module_type);
```


## M3e设计规范应用

### 1. 设计令牌 (Design Tokens)

#### 间距系统
```kotlin
object M3eSpacing {
    val xs = 4.dp      // 极小间距
    val sm = 8.dp      // 小间距
    val md = 16.dp     // 中等间距（基准）
    val lg = 24.dp     // 大间距
    val xl = 32.dp     // 超大间距
    val xxl = 48.dp    // 特大间距
}
```

#### 圆角系统
```kotlin
object M3eCornerRadius {
    val none = 0.dp
    val xs = 4.dp      // 极小圆角
    val sm = 8.dp      // 小圆角
    val md = 12.dp     // 中等圆角
    val lg = 16.dp     // 大圆角
    val xl = 20.dp     // 超大圆角
    val xxl = 28.dp    // 特大圆角（对话框）
    val full = 9999.dp // 完全圆形
}
```

#### 高程系统
```kotlin
object M3eElevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}
```

### 2. 排版系统

应用现有的Material 3 Typography：

```kotlin
// 已在 Typography.kt 中定义
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
```

### 3. 动画系统

#### 动画时长
```kotlin
object M3eAnimationDuration {
    const val SHORT = 100   // 快速动画
    const val MEDIUM = 200  // 中等动画
    const val LONG = 300    // 长动画
    const val EXTRA_LONG = 500  // 超长动画
}
```

#### 缓动曲线
```kotlin
object M3eEasing {
    // 强调减速 - 用于进入动画
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    
    // 强调加速 - 用于退出动画
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    
    // 标准 - 用于一般动画
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    // 标准减速 - 用于元素进入
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    
    // 标准加速 - 用于元素退出
    val StandardAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
}
```

### 4. 组件样式规范

#### 按钮样式
```kotlin
@Composable
fun CreateFolderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.CreateNewFolder,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.create_folder),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
```

#### 对话框样式
```kotlin
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    existingFolderNames: List<String>,
    isCreating: Boolean
) {
    var folderName by remember { mutableStateOf("") }
    val validation = remember(folderName, existingFolderNames) {
        validateFolderName(folderName, existingFolderNames)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.create_folder),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(spacing = 16.dp) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                    isError = validation !is FolderNameValidation.Valid && folderName.isNotEmpty(),
                    supportingText = {
                        if (validation !is FolderNameValidation.Valid && folderName.isNotEmpty()) {
                            Text(
                                text = getValidationMessage(validation),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isCreating) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(folderName) },
                enabled = validation is FolderNameValidation.Valid && !isCreating
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
```

#### 列表项样式
```kotlin
@Composable
fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatDate(folder.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
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

### 5. 状态层效果

应用M3e标准的状态层：

```kotlin
@Composable
fun InteractiveComponent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        // M3e 状态层会自动应用
        // Hover: 8% opacity overlay
        // Focus: 12% opacity overlay
        // Press: 12% opacity overlay
        // Drag: 16% opacity overlay
    ) {
        content()
    }
}
```

### 6. 配色方案

使用现有的配色系统，确保在明亮和暗色主题下都有良好的对比度：

```kotlin
// 明亮主题
val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    // ... 其他颜色
)

// 暗色主题
val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    // ... 其他颜色
)
```


## 错误处理

### 1. 文件夹创建错误

#### 错误类型
```kotlin
sealed class FolderCreationError {
    object PermissionDenied : FolderCreationError()
    object InsufficientSpace : FolderCreationError()
    object InvalidPath : FolderCreationError()
    object AlreadyExists : FolderCreationError()
    object NetworkError : FolderCreationError()  // 仅在线管理
    data class Unknown(val message: String) : FolderCreationError()
}
```

#### 错误处理策略
```kotlin
fun handleFolderCreationError(error: FolderCreationError): String {
    return when (error) {
        is FolderCreationError.PermissionDenied -> 
            context.getString(R.string.error_permission_denied)
        is FolderCreationError.InsufficientSpace -> 
            context.getString(R.string.error_insufficient_space)
        is FolderCreationError.InvalidPath -> 
            context.getString(R.string.error_invalid_path)
        is FolderCreationError.AlreadyExists -> 
            context.getString(R.string.error_folder_exists)
        is FolderCreationError.NetworkError -> 
            context.getString(R.string.error_network)
        is FolderCreationError.Unknown -> 
            context.getString(R.string.error_unknown, error.message)
    }
}
```

### 2. 用户反馈机制

#### Snackbar显示
```kotlin
@Composable
fun FolderCreationFeedback(
    createFolderState: CreateFolderState,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(createFolderState) {
        when (createFolderState) {
            is CreateFolderState.Success -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.folder_create_success),
                    duration = SnackbarDuration.Short
                )
                onDismiss()
            }
            is CreateFolderState.Error -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.folder_create_failed,
                        createFolderState.message
                    ),
                    duration = SnackbarDuration.Long,
                    actionLabel = context.getString(R.string.retry)
                )
            }
            else -> {}
        }
    }
}
```

### 3. 输入验证错误

实时验证并显示错误提示：

```kotlin
@Composable
fun FolderNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    validation: FolderNameValidation,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.folder_name)) },
        isError = validation !is FolderNameValidation.Valid && value.isNotEmpty(),
        supportingText = {
            when (validation) {
                is FolderNameValidation.Empty -> 
                    Text(stringResource(R.string.folder_name_empty))
                is FolderNameValidation.TooLong -> 
                    Text(stringResource(R.string.folder_name_too_long))
                is FolderNameValidation.InvalidChars -> 
                    Text(stringResource(R.string.folder_name_invalid))
                is FolderNameValidation.AlreadyExists -> 
                    Text(stringResource(R.string.folder_name_exists))
                is FolderNameValidation.Valid -> null
            }
        },
        modifier = modifier
    )
}
```

## 测试策略

### 1. 单元测试

#### 字符串资源测试
```kotlin
@Test
fun testModuleNameStrings() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    assertEquals("本地管理", context.getString(R.string.nav_local_library))
    assertEquals("在线管理", context.getString(R.string.nav_online_library))
}
```

#### 文件夹名称验证测试
```kotlin
@Test
fun testFolderNameValidation() {
    // 空名称
    assertEquals(
        FolderNameValidation.Empty,
        validateFolderName("", emptyList())
    )
    
    // 过长名称
    val longName = "a".repeat(256)
    assertEquals(
        FolderNameValidation.TooLong,
        validateFolderName(longName, emptyList())
    )
    
    // 非法字符
    assertEquals(
        FolderNameValidation.InvalidChars,
        validateFolderName("folder/name", emptyList())
    )
    
    // 重复名称
    assertEquals(
        FolderNameValidation.AlreadyExists,
        validateFolderName("existing", listOf("existing"))
    )
    
    // 有效名称
    assertEquals(
        FolderNameValidation.Valid,
        validateFolderName("valid_folder", emptyList())
    )
}
```

#### ViewModel测试
```kotlin
@Test
fun testCreateFolderSuccess() = runTest {
    val viewModel = FolderViewModel(mockRepository)
    
    viewModel.createFolder(
        parentPath = "/test/path",
        folderName = "new_folder",
        moduleType = ModuleType.LOCAL_MANAGEMENT
    )
    
    advanceUntilIdle()
    
    assertTrue(viewModel.createFolderState.value is CreateFolderState.Success)
}

@Test
fun testCreateFolderError() = runTest {
    val viewModel = FolderViewModel(mockRepository)
    
    // 模拟错误
    whenever(mockRepository.createFolder(any(), any(), any()))
        .thenThrow(FolderAlreadyExistsException("test"))
    
    viewModel.createFolder(
        parentPath = "/test/path",
        folderName = "existing",
        moduleType = ModuleType.LOCAL_MANAGEMENT
    )
    
    advanceUntilIdle()
    
    assertTrue(viewModel.createFolderState.value is CreateFolderState.Error)
}
```

### 2. UI测试

#### 对话框显示测试
```kotlin
@Test
fun testCreateFolderDialogDisplay() {
    composeTestRule.setContent {
        CreateFolderDialog(
            onDismiss = {},
            onConfirm = {},
            existingFolderNames = emptyList(),
            isCreating = false
        )
    }
    
    composeTestRule.onNodeWithText("创建文件夹").assertIsDisplayed()
    composeTestRule.onNodeWithText("文件夹名称").assertIsDisplayed()
    composeTestRule.onNodeWithText("确定").assertIsDisplayed()
    composeTestRule.onNodeWithText("取消").assertIsDisplayed()
}
```

#### 输入验证测试
```kotlin
@Test
fun testFolderNameValidationUI() {
    composeTestRule.setContent {
        var folderName by remember { mutableStateOf("") }
        CreateFolderDialog(
            onDismiss = {},
            onConfirm = {},
            existingFolderNames = listOf("existing"),
            isCreating = false
        )
    }
    
    // 输入非法字符
    composeTestRule.onNodeWithText("文件夹名称")
        .performTextInput("folder/name")
    
    composeTestRule.onNodeWithText("文件夹名称包含非法字符")
        .assertIsDisplayed()
    
    // 确定按钮应该被禁用
    composeTestRule.onNodeWithText("确定")
        .assertIsNotEnabled()
}
```

#### 按钮交互测试
```kotlin
@Test
fun testCreateFolderButtonClick() {
    var clicked = false
    
    composeTestRule.setContent {
        CreateFolderButton(
            onClick = { clicked = true }
        )
    }
    
    composeTestRule.onNodeWithText("创建文件夹").performClick()
    assertTrue(clicked)
}
```

### 3. 集成测试

#### 完整流程测试
```kotlin
@Test
fun testCompleteCreateFolderFlow() = runTest {
    // 1. 打开导航抽屉
    composeTestRule.onNodeWithContentDescription("打开本地管理菜单")
        .performClick()
    
    // 2. 点击创建文件夹按钮
    composeTestRule.onNodeWithText("创建文件夹")
        .performClick()
    
    // 3. 输入文件夹名称
    composeTestRule.onNodeWithText("文件夹名称")
        .performTextInput("test_folder")
    
    // 4. 点击确定
    composeTestRule.onNodeWithText("确定")
        .performClick()
    
    // 5. 验证成功提示
    composeTestRule.onNodeWithText("文件夹创建成功")
        .assertIsDisplayed()
    
    // 6. 验证文件夹出现在列表中
    composeTestRule.onNodeWithText("test_folder")
        .assertIsDisplayed()
}
```

### 4. 无障碍测试

```kotlin
@Test
fun testAccessibility() {
    composeTestRule.setContent {
        CreateFolderButton(onClick = {})
    }
    
    // 验证内容描述
    composeTestRule.onNodeWithContentDescription("在当前目录创建新文件夹")
        .assertExists()
    
    // 验证最小触摸目标
    composeTestRule.onNodeWithText("创建文件夹")
        .assertHeightIsAtLeast(48.dp)
        .assertWidthIsAtLeast(48.dp)
}
```

### 5. 性能测试

```kotlin
@Test
fun testFolderListPerformance() {
    val largeFolderList = (1..1000).map { index ->
        Folder(
            id = index.toLong(),
            name = "folder_$index",
            path = "/test/folder_$index",
            parentPath = "/test",
            moduleType = ModuleType.LOCAL_MANAGEMENT,
            createdAt = System.currentTimeMillis()
        )
    }
    
    val startTime = System.currentTimeMillis()
    
    composeTestRule.setContent {
        LazyColumn {
            items(largeFolderList) { folder ->
                FolderListItem(
                    folder = folder,
                    onClick = {}
                )
            }
        }
    }
    
    val renderTime = System.currentTimeMillis() - startTime
    
    // 渲染时间应小于300ms
    assertTrue(renderTime < 300)
}
```


## 实现细节

### 1. 迁移策略

#### 数据库迁移
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 创建folders表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                path TEXT NOT NULL UNIQUE,
                parent_path TEXT NOT NULL,
                module_type TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
        
        // 创建索引
        database.execSQL("""
            CREATE INDEX idx_folders_parent_path ON folders(parent_path)
        """)
        database.execSQL("""
            CREATE INDEX idx_folders_module_type ON folders(module_type)
        """)
    }
}
```

#### 字符串资源迁移
无需特殊迁移，直接更新strings.xml文件即可。应用更新后，新的字符串会自动生效。

### 2. 向后兼容性

#### 保留旧的字符串键（可选）
```xml
<!-- 为了向后兼容，可以保留旧的字符串键作为别名 -->
<string name="nav_local_function">@string/nav_local_library</string>
<string name="nav_online_function">@string/nav_online_library</string>
```

#### 数据完整性
- 所有现有的书籍数据保持不变
- 所有现有的用户设置保持不变
- 所有现有的书源配置保持不变

### 3. 性能优化

#### 文件夹列表加载优化
```kotlin
class FolderRepository {
    // 使用缓存减少数据库查询
    private val folderCache = LruCache<String, List<Folder>>(maxSize = 50)
    
    suspend fun getFolders(
        path: String,
        moduleType: ModuleType
    ): List<Folder> {
        val cacheKey = "$path:$moduleType"
        
        // 先检查缓存
        folderCache.get(cacheKey)?.let { return it }
        
        // 从数据库加载
        val folders = database.folderDao()
            .getFoldersByPath(path, moduleType)
            .sortedBy { it.name.lowercase() }
        
        // 更新缓存
        folderCache.put(cacheKey, folders)
        
        return folders
    }
    
    fun invalidateCache(path: String, moduleType: ModuleType) {
        val cacheKey = "$path:$moduleType"
        folderCache.remove(cacheKey)
    }
}
```

#### UI渲染优化
```kotlin
@Composable
fun FolderList(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = folders,
            key = { it.id }  // 使用稳定的key提升性能
        ) { folder ->
            FolderListItem(
                folder = folder,
                onClick = { onFolderClick(folder) },
                modifier = Modifier.animateItemPlacement()  // 平滑的列表动画
            )
        }
    }
}
```

### 4. 无障碍访问实现

#### 语义标签
```kotlin
@Composable
fun CreateFolderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = "在当前目录创建新文件夹"
            role = Role.Button
        }
    ) {
        // ... 按钮内容
    }
}
```

#### 键盘导航支持
```kotlin
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    existingFolderNames: List<String>,
    isCreating: Boolean
) {
    var folderName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter && 
                            keyEvent.type == KeyEventType.KeyUp) {
                            onConfirm(folderName)
                            true
                        } else {
                            false
                        }
                    }
            )
        },
        // ... 其他参数
    )
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
```

#### 屏幕阅读器支持
```kotlin
@Composable
fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "文件夹 ${folder.name}，创建于 ${formatDate(folder.createdAt)}"
            role = Role.Button
        }
    ) {
        // ... 列表项内容
    }
}
```

### 5. 主题适配实现

#### 动态配色支持
```kotlin
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    existingFolderNames: List<String>,
    isCreating: Boolean
) {
    // 自动适配当前主题
    val containerColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        // ... 其他参数
    )
}
```

#### 明暗主题切换
```kotlin
// 主题会自动根据系统设置或用户选择切换
// 所有组件使用MaterialTheme.colorScheme自动适配
@Composable
fun FolderManagementScreen() {
    // 使用主题颜色
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxSize()
    ) {
        // ... 内容
    }
}
```

## 部署和发布

### 1. 版本控制

#### 版本号更新
```gradle
// app/build.gradle
android {
    defaultConfig {
        versionCode 2  // 递增版本号
        versionName "1.1.0"  // 更新版本名称
    }
}
```

#### 变更日志
```markdown
## v1.1.0 (2025-XX-XX)

### 新功能
- 将"本地功能"重命名为"本地管理"
- 将"在线功能"重命名为"在线管理"
- 在本地管理和在线管理模块中添加"创建文件夹"功能
- 全面应用M3e设计规范

### 改进
- 优化导航抽屉UI设计
- 提升无障碍访问体验
- 改进动画流畅度

### 修复
- 无
```

### 2. 发布检查清单

- [ ] 所有单元测试通过
- [ ] 所有UI测试通过
- [ ] 所有集成测试通过
- [ ] 无障碍测试通过
- [ ] 性能测试通过
- [ ] 在不同屏幕尺寸上测试
- [ ] 在明亮和暗色主题下测试
- [ ] 在不同Android版本上测试（API 21-34）
- [ ] 代码审查完成
- [ ] 文档更新完成
- [ ] 变更日志更新
- [ ] 版本号更新

### 3. 回滚计划

如果发现严重问题，可以快速回滚：

1. 恢复strings.xml中的模块名称
2. 移除文件夹创建功能相关代码
3. 回滚数据库迁移（如果需要）
4. 发布热修复版本

## 未来扩展

### 1. 文件夹管理增强

- 文件夹重命名功能
- 文件夹删除功能
- 文件夹移动功能
- 文件夹排序选项（按名称、日期、大小）
- 文件夹搜索功能

### 2. 批量操作

- 批量创建文件夹
- 批量删除文件夹
- 批量移动文件夹

### 3. 云同步

- 在线管理模块的云端文件夹同步
- 跨设备文件夹结构同步

### 4. 高级功能

- 文件夹图标自定义
- 文件夹颜色标记
- 文件夹加密
- 文件夹共享

## 设计决策记录

### 决策1：使用AlertDialog而非BottomSheet

**理由**：
- AlertDialog更符合M3e设计规范中的对话框模式
- 对于简单的输入操作，AlertDialog提供更好的焦点管理
- AlertDialog在不同屏幕尺寸上有更好的适配性

### 决策2：文件夹数据存储在本地数据库

**理由**：
- 提供离线访问能力
- 减少网络请求
- 提升性能
- 便于实现缓存和搜索

### 决策3：使用FilledTonalButton而非FloatingActionButton

**理由**：
- FilledTonalButton在抽屉中的视觉层次更合适
- 提供更清晰的操作标签
- 符合M3e的按钮使用指南

### 决策4：实时验证文件夹名称

**理由**：
- 提供即时反馈，改善用户体验
- 减少无效提交
- 符合M3e的表单设计最佳实践

## 参考资料

- [Material Design 3 Guidelines](https://m3.material.io/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material Design 3 Components](https://m3.material.io/components)
- [Android Accessibility Guidelines](https://developer.android.com/guide/topics/ui/accessibility)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
