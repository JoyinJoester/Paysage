# 设计文档 - 文件夹编辑管理功能

## 概述

本设计文档描述了文件夹编辑管理功能的技术实现方案。该功能扩展现有的文件夹管理系统，添加重命名、删除、排序等编辑能力。设计遵循 **Material 3 Expressive (M3E)** 设计规范，提供生动、富有表现力的用户体验。

### M3E 设计原则应用

1. **生动性（Vibrancy）**
   - 使用流畅的动画过渡（300ms 标准交互）
   - 鲜艳的主色调和渐变效果
   - 明显的交互反馈（按压缩放、悬停提升）

2. **表现力（Expressiveness）**
   - 圆润的形状设计（16-24dp 圆角）
   - 丰富的视觉层次（卡片阴影、渐变背景）
   - 情感化的配色和图标

3. **一致性（Consistency）**
   - 统一使用 ExpressiveCard、ExpressiveButton 组件
   - 可预测的交互模式
   - 清晰的视觉层级

### UI 改进重点

**当前问题**：
- 传统的选择模式界面（顶部栏 + 复选框）
- 缺乏动画和视觉反馈
- 色彩单调，缺少层次感
- 按钮样式过于朴素

**M3E 改进方案**：
- 使用底部操作栏（Bottom App Bar）替代顶部栏
- 卡片式文件夹项，带悬停和选中动画
- 渐变色背景和圆润形状
- ExpressiveButton 和 ExpressiveFAB 组件
- 流畅的进入/退出动画

## 架构

### 整体架构

`
UI Layer (Compose)
    ├── FolderEditComponents.kt (新增)
    ├── FolderManagementScreen.kt (扩展)
    └── Dialogs (RenameFolderDialog, DeleteConfirmDialog, SortOptionsDialog)
    
ViewModel Layer
    └── FolderViewModel.kt (扩展)
        ├── 重命名状态管理
        ├── 删除状态管理
        ├── 排序状态管理
        └── 批量选择状态管理
    
Repository Layer
    └── FolderRepository.kt (扩展)
        ├── renameFolder()
        ├── deleteFolders()
        └── updateFolderOrder()
    
Data Layer
    ├── Folder.kt (扩展 - 添加 sortOrder 字段)
    └── FolderDao.kt (新增/扩展)
`

### 数据流

1. 用户交互 → UI组件
2. UI组件 → ViewModel (通过事件)
3. ViewModel → Repository (业务逻辑)
4. Repository → DAO/文件系统 (数据持久化)
5. DAO → ViewModel (通过 Flow)
6. ViewModel → UI (通过 StateFlow)

## UI 组件设计（M3E 风格）

### 1. 文件夹编辑模式界面

#### 整体布局
```
┌─────────────────────────────────┐
│  [渐变背景头部]                    │
│  编辑文件夹                        │
│  已选择 0 项                       │
└─────────────────────────────────┘
│                                 │
│  ┌───────────────────────────┐  │
│  │ 📁 文件夹 1    [选中动画]  │  │ ← ExpressiveCard
│  │ 213 本书                   │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ 📁 文件夹 2               │  │
│  │ 45 本书                    │  │
│  └───────────────────────────┘  │
│                                 │
└─────────────────────────────────┘
│  [底部操作栏 - 渐变背景]          │
│  ✏️ 重命名  🗑️ 删除  ↕️ 排序    │ ← ExpressiveButtons
└─────────────────────────────────┘
```

#### 视觉规格

**头部区域**：
- 渐变背景：`primaryContainer → surface`
- 高度：120dp
- 圆角：底部 24dp（Large）
- 标题：Headline Medium (28sp, SemiBold)
- 副标题：Body Medium (14sp)

**文件夹卡片**：
- 形状：RoundedCornerShape(16.dp) - Medium
- 阴影：默认 4dp，选中 8dp
- 内边距：16dp
- 图标尺寸：36dp (Extra Large)
- 选中状态：
  - 边框：2dp，primary 色
  - 背景：primaryContainer (10% 透明度)
  - 缩放：1.02x
  - 动画：300ms EmphasizedEasing

**底部操作栏**：
- 高度：80dp
- 渐变背景：`surface → surfaceVariant`
- 圆角：顶部 24dp
- 阴影：8dp（提升感）
- 按钮间距：16dp

### 2. 动画规格

#### 进入动画
```kotlin
// 编辑模式进入
slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(
        durationMillis = 300,
        easing = EmphasizedDecelerateEasing
    )
)

// 卡片依次出现
items.forEachIndexed { index, item ->
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 200,
                delayMillis = index * 50
            )
        ) + slideInVertically()
    )
}
```

#### 选中动画
```kotlin
// 卡片选中
val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.02f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)

val elevation by animateDpAsState(
    targetValue = if (isSelected) 8.dp else 4.dp,
    animationSpec = tween(200)
)
```

#### 按钮交互
```kotlin
// ExpressiveButton 按压
val buttonScale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy
    )
)
```

### 3. 色彩应用

#### 主题色
- **Primary**: `#FF6B35` - 操作按钮、选中边框
- **Primary Container**: `#FFDBCC` - 选中背景、头部渐变
- **Surface Variant**: `#E7E0EC` - 卡片背景
- **On Surface**: `#1C1B1F` - 文字

#### 渐变效果
```kotlin
// 头部渐变
Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.surface
    )
)

// 底部栏渐变
Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant
    )
)
```

### 4. 交互反馈

#### 触摸反馈
- 使用 `Modifier.clickable` 的 ripple 效果
- Ripple 颜色：primary (20% 透明度)
- 按压时缩放：0.92x - 0.95x

#### 视觉反馈
- 选中：边框 + 背景色 + 缩放
- 悬停（平板）：阴影提升
- 长按：震动反馈 + 进入编辑模式

## 组件和接口

### 1. 数据模型扩展

#### Folder 模型更新

```kotlin
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val bookCount: Int = 0,
    val sortOrder: Int = 0,  // 新增：用于自定义排序
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 2. UI 状态管理

#### FolderEditUiState
```kotlin
data class FolderEditUiState(
    val isEditMode: Boolean = false,
    val selectedFolders: Set<Long> = emptySet(),
    val folders: List<Folder> = emptyList(),
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class SortOption {
    NAME_ASC,      // 名称升序
    NAME_DESC,     // 名称降序
    DATE_ASC,      // 日期升序
    DATE_DESC,     // 日期降序
    BOOK_COUNT_ASC,  // 书籍数量升序
    BOOK_COUNT_DESC, // 书籍数量降序
    CUSTOM         // 自定义排序
}
```

### 3. M3E 组件实现

#### ExpressiveFolderCard
```kotlin
@Composable
fun ExpressiveFolderCard(
    folder: Folder,
    isSelected: Boolean,
    isEditMode: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 4.dp,
        animationSpec = tween(200)
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            Color.Transparent,
        animationSpec = tween(200)
    )
    
    ExpressiveCard(
        onClick = if (isEditMode) onSelect else onClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件夹图标
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文件夹信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${folder.bookCount} 本书",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 选中指示器
            AnimatedVisibility(
                visible = isEditMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
```

#### EditModeBottomBar
```kotlin
@Composable
fun EditModeBottomBar(
    selectedCount: Int,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSort: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 重命名按钮
                ExpressiveButton(
                    onClick = onRename,
                    enabled = selectedCount == 1,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重命名")
                }
                
                // 删除按钮
                ExpressiveButton(
                    onClick = onDelete,
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除")
                }
                
                // 排序按钮
                ExpressiveButton(
                    onClick = onSort,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("排序")
                }
            }
        }
    }
}
```

#### EditModeHeader
```kotlin
@Composable
fun EditModeHeader(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // 取消按钮
            IconButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 全选按钮
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = if (selectedCount == totalCount) "取消全选" else "全选",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 标题和计数
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "编辑文件夹",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已选择 $selectedCount 项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### 4. 对话框组件（M3E 风格）

#### RenameFolderDialog
```kotlin
@Composable
fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,  // 32dp 圆角
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "重命名文件夹",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { 
                        newName = it
                        error = null
                    },
                    label = { Text("文件夹名称") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ExpressiveButton(
                onClick = {
                    when {
                        newName.isBlank() -> error = "名称不能为空"
                        newName == currentName -> onDismiss()
                        else -> onConfirm(newName)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
```

#### DeleteConfirmDialog
```kotlin
@Composable
fun DeleteConfirmDialog(
    folderCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "确认删除",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "确定要删除选中的 $folderCount 个文件夹吗？此操作不可撤销。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            ExpressiveButton(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
```

#### SortOptionsDialog
```kotlin
@Composable
fun SortOptionsDialog(
    currentOption: SortOption,
    onDismiss: () -> Unit,
    onSelect: (SortOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                SortOption.values().forEach { option ->
                    val isSelected = option == currentOption
                    
                    Surface(
                        onClick = { onSelect(option) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(option) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

val SortOption.displayName: String
    get() = when (this) {
        SortOption.NAME_ASC -> "名称 (A-Z)"
        SortOption.NAME_DESC -> "名称 (Z-A)"
        SortOption.DATE_ASC -> "日期 (旧→新)"
        SortOption.DATE_DESC -> "日期 (新→旧)"
        SortOption.BOOK_COUNT_ASC -> "书籍数量 (少→多)"
        SortOption.BOOK_COUNT_DESC -> "书籍数量 (多→少)"
        SortOption.CUSTOM -> "自定义排序"
    }
```

## ViewModel 实现

### FolderViewModel 扩展

```kotlin
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FolderEditUiState())
    val uiState: StateFlow<FolderEditUiState> = _uiState.asStateFlow()
    
    init {
        loadFolders()
    }
    
    private fun loadFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            folderRepository.getAllFolders()
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
                .collect { folders ->
                    _uiState.update { 
                        it.copy(
                            folders = folders.sortedBy(it.sortOption),
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun enterEditMode() {
        _uiState.update { it.copy(isEditMode = true) }
    }
    
    fun exitEditMode() {
        _uiState.update { 
            it.copy(
                isEditMode = false,
                selectedFolders = emptySet()
            )
        }
    }
    
    fun toggleFolderSelection(folderId: Long) {
        _uiState.update { state ->
            val newSelection = if (folderId in state.selectedFolders) {
                state.selectedFolders - folderId
            } else {
                state.selectedFolders + folderId
            }
            state.copy(selectedFolders = newSelection)
        }
    }
    
    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                selectedFolders = state.folders.map { it.id }.toSet()
            )
        }
    }
    
    fun deselectAll() {
        _uiState.update { it.copy(selectedFolders = emptySet()) }
    }
    
    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            try {
                folderRepository.renameFolder(folderId, newName)
                exitEditMode()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun deleteSelectedFolders() {
        viewModelScope.launch {
            try {
                val selectedIds = _uiState.value.selectedFolders.toList()
                folderRepository.deleteFolders(selectedIds)
                exitEditMode()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun updateSortOption(option: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = option,
                folders = state.folders.sortedBy(option)
            )
        }
    }
    
    private fun List<Folder>.sortedBy(option: SortOption): List<Folder> {
        return when (option) {
            SortOption.NAME_ASC -> sortedBy { it.name }
            SortOption.NAME_DESC -> sortedByDescending { it.name }
            SortOption.DATE_ASC -> sortedBy { it.createdAt }
            SortOption.DATE_DESC -> sortedByDescending { it.createdAt }
            SortOption.BOOK_COUNT_ASC -> sortedBy { it.bookCount }
            SortOption.BOOK_COUNT_DESC -> sortedByDescending { it.bookCount }
            SortOption.CUSTOM -> sortedBy { it.sortOrder }
        }
    }
}
```

## Repository 实现

### FolderRepository 扩展

```kotlin
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao
) {
    
    fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders()
    }
    
    suspend fun renameFolder(folderId: Long, newName: String) {
        val folder = folderDao.getFolderById(folderId)
        folder?.let {
            val updated = it.copy(
                name = newName,
                updatedAt = System.currentTimeMillis()
            )
            folderDao.updateFolder(updated)
        }
    }
    
    suspend fun deleteFolders(folderIds: List<Long>) {
        folderDao.deleteFolders(folderIds)
    }
    
    suspend fun updateFolderOrder(folderId: Long, newOrder: Int) {
        val folder = folderDao.getFolderById(folderId)
        folder?.let {
            val updated = it.copy(
                sortOrder = newOrder,
                updatedAt = System.currentTimeMillis()
            )
            folderDao.updateFolder(updated)
        }
    }
}
```

## 数据层实现

### FolderDao

```kotlin
@Dao
interface FolderDao {
    
    @Query("SELECT * FROM folders ORDER BY sortOrder ASC")
    fun getAllFolders(): Flow<List<Folder>>
    
    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: Long): Folder?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long
    
    @Update
    suspend fun updateFolder(folder: Folder)
    
    @Query("DELETE FROM folders WHERE id IN (:folderIds)")
    suspend fun deleteFolders(folderIds: List<Long>)
    
    @Query("UPDATE folders SET sortOrder = :newOrder WHERE id = :folderId")
    suspend fun updateFolderOrder(folderId: Long, newOrder: Int)
}
```

## 错误处理

### 错误类型

```kotlin
sealed class FolderEditError {
    data class RenameError(val message: String) : FolderEditError()
    data class DeleteError(val message: String) : FolderEditError()
    data class LoadError(val message: String) : FolderEditError()
}
```

### 错误处理策略

1. **重命名错误**：
   - 名称为空
   - 名称重复
   - 文件系统错误

2. **删除错误**：
   - 文件夹不存在
   - 文件夹包含书籍（需确认）
   - 文件系统错误

3. **加载错误**：
   - 数据库访问失败
   - 数据损坏

### 错误提示（M3E 风格）

```kotlin
@Composable
fun ErrorSnackbar(
    error: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        action = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(error)
        }
    }
}
```

## 测试策略

### 单元测试

1. **ViewModel 测试**：
   - 编辑模式切换
   - 文件夹选择/取消选择
   - 全选/取消全选
   - 重命名逻辑
   - 删除逻辑
   - 排序逻辑

2. **Repository 测试**：
   - CRUD 操作
   - 错误处理
   - 数据流

### UI 测试

1. **组件测试**：
   - ExpressiveFolderCard 交互
   - 选中状态动画
   - 对话框显示/隐藏

2. **集成测试**：
   - 完整编辑流程
   - 多选操作
   - 错误场景

## 性能考虑

### 优化策略

1. **列表渲染**：
   - 使用 `LazyColumn` 虚拟化
   - `key` 参数优化重组
   - 避免不必要的重组

2. **动画性能**：
   - 使用 `remember` 缓存动画状态
   - 限制同时播放的动画数量
   - 使用硬件加速

3. **数据库操作**：
   - 批量操作
   - 事务处理
   - 索引优化

## 可访问性

### 无障碍支持

1. **语义描述**：
   - 所有交互元素添加 `contentDescription`
   - 状态变化语音提示

2. **触摸目标**：
   - 最小 48dp 触摸区域
   - 足够的间距

3. **对比度**：
   - 符合 WCAG AA 标准
   - 暗色模式支持

---

**设计版本**: 2.0 (M3E)  
**更新日期**: 2025-10-28  
**设计系统**: Material 3 Expressive
