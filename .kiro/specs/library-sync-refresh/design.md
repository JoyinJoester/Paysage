# Design Document

## Overview

本设计文档描述了书库同步下拉刷新功能的技术实现方案。该功能通过Material 3 Extended风格的下拉刷新组件和模态对话框，为用户提供灵活的书库同步配置和操作选项。

核心设计理念：
- **渐进式交互**：通过下拉手势自然触发同步对话框
- **配置优先**：在执行同步前允许用户配置选项
- **视觉一致性**：遵循Material 3 Extended设计规范
- **性能优化**：支持增量同步和并行处理
- **状态持久化**：保存用户的同步偏好设置

## Architecture

### 组件层次结构

```
LibraryScreen
├── PullToRefreshBox (Compose Material3)
│   ├── CircularProgressIndicator (M3E Style)
│   └── Content (Book Grid/List)
└── LibrarySyncDialog (Modal Dialog)
    ├── FileTypeCard
    │   ├── ComicFilesSection
    │   └── ArchiveFilesSection
    ├── MaintenanceCard
    │   ├── RemoveDeletedCheckbox
    │   ├── UpdateModifiedCheckbox
    │   └── GenerateThumbnailsCheckbox
    ├── MoreOptionsCard
    │   ├── ScanSubfoldersCheckbox
    │   ├── SkipHiddenCheckbox
    │   └── ParallelSyncCheckbox
    └── ActionButtons
        ├── MaintenanceButton
        ├── SyncButton
        └── FullSyncButton
```

### 数据流

```
User Pull Gesture
    ↓
PullToRefreshBox (threshold reached)
    ↓
Show LibrarySyncDialog
    ↓
User Configures Options → SyncOptions (DataStore)
    ↓
User Clicks Action Button
    ↓
LibraryViewModel.performSync(type, options)
    ↓
BookRepository.sync(type, options)
    ↓
Update UI State → Show Progress
    ↓
Sync Complete → Show Result Snackbar
```


## Components and Interfaces

### 1. PullToRefreshBox Integration

使用Compose Material3的`PullToRefreshBox`组件实现下拉刷新：

```kotlin
@Composable
fun LibraryScreen(...) {
    var isRefreshing by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = false
            showSyncDialog = true
        },
        indicator = { state ->
            M3EPullRefreshIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        // Existing content
    }
    
    if (showSyncDialog) {
        LibrarySyncDialog(
            onDismiss = { showSyncDialog = false },
            onSync = { type, options -> 
                viewModel.performSync(type, options)
                showSyncDialog = false
            }
        )
    }
}
```

### 2. M3EPullRefreshIndicator

自定义M3E风格的下拉刷新指示器：

```kotlin
@Composable
fun M3EPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = state.distanceFraction.coerceIn(0f, 1f)
    val scale by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else progress,
        animationSpec = ExpressiveAnimations.bouncySpring
    )
    val alpha by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else progress,
        animationSpec = ExpressiveAnimations.standardEasing
    )
    
    Surface(
        modifier = modifier
            .padding(top = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { if (isRefreshing) 1f else progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}
```


### 3. LibrarySyncDialog

主同步对话框组件：

```kotlin
@Composable
fun LibrarySyncDialog(
    onDismiss: () -> Unit,
    onSync: (SyncType, SyncOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    val syncOptions by rememberSyncOptions()
    var currentOptions by remember { mutableStateOf(syncOptions) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.8f)
                .widthIn(max = 600.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.library_sync_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // File Type Card
                FileTypeCard()
                
                // Maintenance Card
                MaintenanceCard(
                    options = currentOptions,
                    onOptionsChange = { currentOptions = it }
                )
                
                // More Options Card
                MoreOptionsCard(
                    options = currentOptions,
                    onOptionsChange = { currentOptions = it }
                )
                
                // Action Buttons
                ActionButtons(
                    onMaintenance = { 
                        onSync(SyncType.MAINTENANCE, currentOptions)
                    },
                    onSync = { 
                        onSync(SyncType.INCREMENTAL, currentOptions)
                    },
                    onFullSync = { 
                        onSync(SyncType.FULL, currentOptions)
                    }
                )
            }
        }
    }
}
```


### 4. FileTypeCard

文件类型展示卡片：

```kotlin
@Composable
fun FileTypeCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.file_types),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Comic Files Section
            FileTypeSection(
                title = stringResource(R.string.comic_files),
                formats = listOf(".cbz", ".cbr", ".cbt", ".cb7")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Archive Files Section
            FileTypeSection(
                title = stringResource(R.string.archive_files),
                formats = listOf(".zip", ".rar", ".7z", ".tar")
            )
        }
    }
}

@Composable
fun FileTypeSection(
    title: String,
    formats: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            formats.forEach { format ->
                AssistChip(
                    onClick = { },
                    label = { Text(format) },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}
```


### 5. MaintenanceCard

维护选项卡片：

```kotlin
@Composable
fun MaintenanceCard(
    options: SyncOptions,
    onOptionsChange: (SyncOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.maintenance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            SyncCheckbox(
                checked = options.removeDeletedFiles,
                onCheckedChange = { 
                    onOptionsChange(options.copy(removeDeletedFiles = it))
                },
                label = stringResource(R.string.remove_deleted_files),
                contentDescription = stringResource(R.string.remove_deleted_files_desc)
            )
            
            SyncCheckbox(
                checked = options.updateModifiedFiles,
                onCheckedChange = { 
                    onOptionsChange(options.copy(updateModifiedFiles = it))
                },
                label = stringResource(R.string.update_modified_files),
                contentDescription = stringResource(R.string.update_modified_files_desc)
            )
            
            SyncCheckbox(
                checked = options.generateMissingThumbnails,
                onCheckedChange = { 
                    onOptionsChange(options.copy(generateMissingThumbnails = it))
                },
                label = stringResource(R.string.generate_missing_thumbnails),
                contentDescription = stringResource(R.string.generate_missing_thumbnails_desc)
            )
        }
    }
}
```


### 6. MoreOptionsCard

更多选项卡片：

```kotlin
@Composable
fun MoreOptionsCard(
    options: SyncOptions,
    onOptionsChange: (SyncOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.more_options),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            SyncCheckbox(
                checked = options.scanSubfolders,
                onCheckedChange = { 
                    onOptionsChange(options.copy(scanSubfolders = it))
                },
                label = stringResource(R.string.scan_subfolders),
                contentDescription = stringResource(R.string.scan_subfolders_desc)
            )
            
            SyncCheckbox(
                checked = options.skipHiddenFolders,
                onCheckedChange = { 
                    onOptionsChange(options.copy(skipHiddenFolders = it))
                },
                label = stringResource(R.string.skip_hidden_folders),
                contentDescription = stringResource(R.string.skip_hidden_folders_desc)
            )
            
            SyncCheckbox(
                checked = options.parallelSync,
                onCheckedChange = { 
                    onOptionsChange(options.copy(parallelSync = it))
                },
                label = stringResource(R.string.parallel_sync),
                contentDescription = stringResource(R.string.parallel_sync_desc)
            )
        }
    }
}
```


### 7. SyncCheckbox

带波纹效果的复选框组件：

```kotlin
@Composable
fun SyncCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ),
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 4.dp)
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = if (checked) "已选中" else "未选中"
                this.role = Role.Checkbox
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            interactionSource = interactionSource
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```


### 8. ActionButtons

操作按钮区域：

```kotlin
@Composable
fun ActionButtons(
    onMaintenance: () -> Unit,
    onSync: () -> Unit,
    onFullSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Full Sync Button (Primary)
        ExpressiveButton(
            onClick = onFullSync,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.start_full_sync))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Maintenance Button
            ExpressiveButton(
                onClick = onMaintenance,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text(stringResource(R.string.maintenance))
            }
            
            // Incremental Sync Button
            ExpressiveButton(
                onClick = onSync,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Text(stringResource(R.string.sync))
            }
        }
    }
}
```


## Data Models

### SyncOptions

同步选项数据类：

```kotlin
data class SyncOptions(
    // Maintenance options
    val removeDeletedFiles: Boolean = false,
    val updateModifiedFiles: Boolean = false,
    val generateMissingThumbnails: Boolean = false,
    
    // Scan options
    val scanSubfolders: Boolean = true,
    val skipHiddenFolders: Boolean = true,
    val parallelSync: Boolean = false
)
```

### SyncType

同步类型枚举：

```kotlin
enum class SyncType {
    MAINTENANCE,    // 仅执行维护操作
    INCREMENTAL,    // 增量同步（仅新增和修改）
    FULL            // 完整同步（重新扫描所有文件）
}
```

### SyncResult

同步结果数据类（扩展现有的ScanResult）：

```kotlin
data class SyncResult(
    val newBooks: Int = 0,
    val updatedBooks: Int = 0,
    val deletedBooks: Int = 0,
    val generatedThumbnails: Int = 0,
    val duration: Long = 0,
    val errors: List<String> = emptyList()
)
```


## Error Handling

### 错误场景处理

1. **权限错误**
   - 检测：在执行同步前检查存储权限
   - 处理：显示权限请求对话框
   - 反馈：通过Snackbar提示用户授予权限

2. **文件访问错误**
   - 检测：捕获IOException和SecurityException
   - 处理：跳过无法访问的文件，继续处理其他文件
   - 反馈：在SyncResult中记录错误，完成后显示错误摘要

3. **数据库错误**
   - 检测：捕获SQLiteException
   - 处理：回滚事务，保持数据一致性
   - 反馈：显示错误对话框，建议用户重试

4. **内存不足**
   - 检测：捕获OutOfMemoryError
   - 处理：降低并行度，使用分批处理
   - 反馈：提示用户关闭其他应用或使用非并行模式

5. **网络错误（未来扩展）**
   - 检测：捕获网络相关异常
   - 处理：使用本地缓存，标记需要重试的项
   - 反馈：显示离线模式提示

### 错误恢复策略

```kotlin
suspend fun performSync(type: SyncType, options: SyncOptions): Result<SyncResult> {
    return try {
        // Check permissions
        if (!hasStoragePermission()) {
            return Result.failure(PermissionException("需要存储权限"))
        }
        
        // Execute sync with retry logic
        val result = withRetry(maxAttempts = 3) {
            when (type) {
                SyncType.MAINTENANCE -> executeMaintenance(options)
                SyncType.INCREMENTAL -> executeIncrementalSync(options)
                SyncType.FULL -> executeFullSync(options)
            }
        }
        
        Result.success(result)
    } catch (e: Exception) {
        Log.e(TAG, "Sync failed", e)
        Result.failure(e)
    }
}

suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 5000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Attempt ${attempt + 1} failed", e)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // Last attempt
}
```


## Testing Strategy

### 单元测试

1. **SyncOptions持久化测试**
   ```kotlin
   @Test
   fun `syncOptions should persist across app restarts`() = runTest {
       val options = SyncOptions(
           removeDeletedFiles = true,
           parallelSync = true
       )
       
       syncOptionsRepository.save(options)
       val loaded = syncOptionsRepository.load()
       
       assertEquals(options, loaded)
   }
   ```

2. **同步逻辑测试**
   ```kotlin
   @Test
   fun `incremental sync should only process new and modified files`() = runTest {
       // Given: existing books in database
       val existingBooks = listOf(book1, book2)
       repository.insertAll(existingBooks)
       
       // When: incremental sync with new file
       val result = repository.performSync(
           SyncType.INCREMENTAL,
           SyncOptions()
       )
       
       // Then: only new book is added
       assertEquals(1, result.newBooks)
       assertEquals(0, result.updatedBooks)
   }
   ```

3. **错误处理测试**
   ```kotlin
   @Test
   fun `sync should handle permission errors gracefully`() = runTest {
       // Given: no storage permission
       whenever(permissionChecker.hasStoragePermission()).thenReturn(false)
       
       // When: attempting sync
       val result = viewModel.performSync(SyncType.FULL, SyncOptions())
       
       // Then: error is reported
       assertTrue(result.isFailure)
       assertTrue(result.exceptionOrNull() is PermissionException)
   }
   ```

### UI测试

1. **下拉刷新测试**
   ```kotlin
   @Test
   fun `pull to refresh should show sync dialog`() {
       composeTestRule.setContent {
           LibraryScreen(...)
       }
       
       // Perform pull gesture
       composeTestRule.onNodeWithTag("library_content")
           .performTouchInput {
               swipeDown(startY = 100f, endY = 500f)
           }
       
       // Verify dialog is shown
       composeTestRule.onNodeWithText("书库同步")
           .assertIsDisplayed()
   }
   ```

2. **复选框交互测试**
   ```kotlin
   @Test
   fun `checkbox should toggle with ripple effect`() {
       composeTestRule.setContent {
           LibrarySyncDialog(...)
       }
       
       // Click checkbox
       composeTestRule.onNodeWithText("扫描子文件夹")
           .performClick()
       
       // Verify state changed
       composeTestRule.onNode(
           hasStateDescription("已选中")
       ).assertExists()
   }
   ```

3. **按钮操作测试**
   ```kotlin
   @Test
   fun `sync button should trigger incremental sync`() {
       var syncType: SyncType? = null
       
       composeTestRule.setContent {
           LibrarySyncDialog(
               onSync = { type, _ -> syncType = type }
           )
       }
       
       composeTestRule.onNodeWithText("同步")
           .performClick()
       
       assertEquals(SyncType.INCREMENTAL, syncType)
   }
   ```

### 集成测试

1. **端到端同步流程测试**
   ```kotlin
   @Test
   fun `complete sync flow should update library`() = runTest {
       // Setup test files
       val testDir = createTestDirectory()
       createTestComicFile(testDir, "test.cbz")
       
       // Execute sync
       val result = repository.performSync(
           SyncType.FULL,
           SyncOptions(scanSubfolders = true)
       )
       
       // Verify results
       assertEquals(1, result.newBooks)
       val books = repository.getAllBooks()
       assertEquals(1, books.size)
       assertEquals("test.cbz", books[0].fileName)
   }
   ```

### 性能测试

1. **大量文件扫描性能**
   ```kotlin
   @Test
   fun `parallel sync should be faster than sequential`() = runTest {
       val testFiles = createLargeTestSet(1000)
       
       val sequentialTime = measureTimeMillis {
           repository.performSync(
               SyncType.FULL,
               SyncOptions(parallelSync = false)
           )
       }
       
       val parallelTime = measureTimeMillis {
           repository.performSync(
               SyncType.FULL,
               SyncOptions(parallelSync = true)
           )
       }
       
       assertTrue(parallelTime < sequentialTime)
   }
   ```


## Implementation Details

### 1. DataStore集成

使用Preferences DataStore存储同步选项：

```kotlin
class SyncOptionsRepository(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "sync_options")
    
    private object Keys {
        val REMOVE_DELETED = booleanPreferencesKey("remove_deleted_files")
        val UPDATE_MODIFIED = booleanPreferencesKey("update_modified_files")
        val GENERATE_THUMBNAILS = booleanPreferencesKey("generate_thumbnails")
        val SCAN_SUBFOLDERS = booleanPreferencesKey("scan_subfolders")
        val SKIP_HIDDEN = booleanPreferencesKey("skip_hidden_folders")
        val PARALLEL_SYNC = booleanPreferencesKey("parallel_sync")
    }
    
    val syncOptionsFlow: Flow<SyncOptions> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SyncOptions(
                removeDeletedFiles = preferences[Keys.REMOVE_DELETED] ?: false,
                updateModifiedFiles = preferences[Keys.UPDATE_MODIFIED] ?: false,
                generateMissingThumbnails = preferences[Keys.GENERATE_THUMBNAILS] ?: false,
                scanSubfolders = preferences[Keys.SCAN_SUBFOLDERS] ?: true,
                skipHiddenFolders = preferences[Keys.SKIP_HIDDEN] ?: true,
                parallelSync = preferences[Keys.PARALLEL_SYNC] ?: false
            )
        }
    
    suspend fun updateSyncOptions(options: SyncOptions) {
        context.dataStore.edit { preferences ->
            preferences[Keys.REMOVE_DELETED] = options.removeDeletedFiles
            preferences[Keys.UPDATE_MODIFIED] = options.updateModifiedFiles
            preferences[Keys.GENERATE_THUMBNAILS] = options.generateMissingThumbnails
            preferences[Keys.SCAN_SUBFOLDERS] = options.scanSubfolders
            preferences[Keys.SKIP_HIDDEN] = options.skipHiddenFolders
            preferences[Keys.PARALLEL_SYNC] = options.parallelSync
        }
    }
}

@Composable
fun rememberSyncOptions(): State<SyncOptions> {
    val context = LocalContext.current
    val repository = remember { SyncOptionsRepository(context) }
    return repository.syncOptionsFlow.collectAsState(initial = SyncOptions())
}
```

### 2. ViewModel扩展

扩展LibraryViewModel以支持新的同步功能：

```kotlin
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val syncOptionsRepository = SyncOptionsRepository(application)
    
    val syncOptions = syncOptionsRepository.syncOptionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncOptions())
    
    fun performSync(type: SyncType, options: SyncOptions) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanError = null) }
            
            try {
                // Save options for next time
                syncOptionsRepository.updateSyncOptions(options)
                
                // Execute sync
                val result = when (type) {
                    SyncType.MAINTENANCE -> repository.executeMaintenance(options)
                    SyncType.INCREMENTAL -> repository.executeIncrementalSync(options)
                    SyncType.FULL -> repository.executeFullSync(options)
                }
                
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        syncResult = result
                    )
                }
                loadStatistics()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        scanError = e.message ?: "同步失败"
                    )
                }
            }
        }
    }
}
```


### 3. Repository同步实现

在BookRepository中实现不同类型的同步操作：

```kotlin
class BookRepository(private val context: Context) {
    
    suspend fun executeMaintenance(options: SyncOptions): SyncResult {
        val startTime = System.currentTimeMillis()
        var deletedCount = 0
        var updatedCount = 0
        var thumbnailCount = 0
        val errors = mutableListOf<String>()
        
        try {
            // Remove deleted files
            if (options.removeDeletedFiles) {
                deletedCount = removeDeletedFiles()
            }
            
            // Update modified files
            if (options.updateModifiedFiles) {
                updatedCount = updateModifiedFiles()
            }
            
            // Generate missing thumbnails
            if (options.generateMissingThumbnails) {
                thumbnailCount = generateMissingThumbnails()
            }
        } catch (e: Exception) {
            errors.add(e.message ?: "维护操作失败")
        }
        
        return SyncResult(
            newBooks = 0,
            updatedBooks = updatedCount,
            deletedBooks = deletedCount,
            generatedThumbnails = thumbnailCount,
            duration = System.currentTimeMillis() - startTime,
            errors = errors
        )
    }
    
    suspend fun executeIncrementalSync(options: SyncOptions): SyncResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        // Get last sync timestamp
        val lastSyncTime = getLastSyncTimestamp()
        
        // Scan for new and modified files only
        val scanResult = scanFiles(
            scanSubfolders = options.scanSubfolders,
            skipHidden = options.skipHiddenFolders,
            parallel = options.parallelSync,
            modifiedAfter = lastSyncTime
        )
        
        // Update last sync timestamp
        updateLastSyncTimestamp(System.currentTimeMillis())
        
        return SyncResult(
            newBooks = scanResult.newBooks,
            updatedBooks = scanResult.updatedBooks,
            deletedBooks = 0,
            generatedThumbnails = 0,
            duration = System.currentTimeMillis() - startTime,
            errors = errors
        )
    }
    
    suspend fun executeFullSync(options: SyncOptions): SyncResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        // Clear existing cache (optional)
        // clearCache()
        
        // Scan all files
        val scanResult = scanFiles(
            scanSubfolders = options.scanSubfolders,
            skipHidden = options.skipHiddenFolders,
            parallel = options.parallelSync,
            modifiedAfter = null // Scan all files
        )
        
        // Update last sync timestamp
        updateLastSyncTimestamp(System.currentTimeMillis())
        
        return SyncResult(
            newBooks = scanResult.newBooks,
            updatedBooks = scanResult.updatedBooks,
            deletedBooks = 0,
            generatedThumbnails = 0,
            duration = System.currentTimeMillis() - startTime,
            errors = errors
        )
    }
    
    private suspend fun scanFiles(
        scanSubfolders: Boolean,
        skipHidden: Boolean,
        parallel: Boolean,
        modifiedAfter: Long?
    ): ScanResult {
        val scanPaths = getScanPaths()
        
        return if (parallel) {
            scanPaths.map { path ->
                async(Dispatchers.IO) {
                    scanDirectory(
                        File(path),
                        scanSubfolders,
                        skipHidden,
                        modifiedAfter
                    )
                }
            }.awaitAll().reduce { acc, result ->
                ScanResult(
                    newBooks = acc.newBooks + result.newBooks,
                    updatedBooks = acc.updatedBooks + result.updatedBooks
                )
            }
        } else {
            scanPaths.fold(ScanResult()) { acc, path ->
                val result = scanDirectory(
                    File(path),
                    scanSubfolders,
                    skipHidden,
                    modifiedAfter
                )
                ScanResult(
                    newBooks = acc.newBooks + result.newBooks,
                    updatedBooks = acc.updatedBooks + result.updatedBooks
                )
            }
        }
    }
}
```


### 4. 字符串资源

需要添加的字符串资源（values/strings.xml和values-zh/strings.xml）：

```xml
<!-- English (values/strings.xml) -->
<string name="library_sync_title">Library Sync</string>
<string name="file_types">File Types</string>
<string name="comic_files">Comic Files</string>
<string name="archive_files">Archive Files</string>
<string name="maintenance">Maintenance</string>
<string name="more_options">More Options</string>
<string name="remove_deleted_files">Remove deleted files</string>
<string name="remove_deleted_files_desc">Remove books whose files no longer exist</string>
<string name="update_modified_files">Update modified files</string>
<string name="update_modified_files_desc">Refresh data for files that have been modified</string>
<string name="generate_missing_thumbnails">Generate missing thumbnails</string>
<string name="generate_missing_thumbnails_desc">Create thumbnails for books without covers</string>
<string name="scan_subfolders">Scan subfolders</string>
<string name="scan_subfolders_desc">Include files in subdirectories</string>
<string name="skip_hidden_folders">Skip hidden folders</string>
<string name="skip_hidden_folders_desc">Ignore folders starting with a dot</string>
<string name="parallel_sync">Parallel sync</string>
<string name="parallel_sync_desc">Process multiple folders simultaneously</string>
<string name="start_full_sync">Start Full Sync</string>
<string name="sync">Sync</string>
<string name="sync_complete">Sync complete: %1$d new, %2$d updated</string>
<string name="sync_error">Sync failed: %1$s</string>

<!-- Chinese (values-zh/strings.xml) -->
<string name="library_sync_title">书库同步</string>
<string name="file_types">文件类型</string>
<string name="comic_files">漫画文件</string>
<string name="archive_files">压缩文件</string>
<string name="maintenance">维护</string>
<string name="more_options">更多</string>
<string name="remove_deleted_files">移出已删除文件</string>
<string name="remove_deleted_files_desc">移除文件已不存在的书籍</string>
<string name="update_modified_files">从修改过的文件更新数据</string>
<string name="update_modified_files_desc">刷新已修改文件的数据</string>
<string name="generate_missing_thumbnails">生成已删除的缩略图</string>
<string name="generate_missing_thumbnails_desc">为没有封面的书籍创建缩略图</string>
<string name="scan_subfolders">扫描子文件夹</string>
<string name="scan_subfolders_desc">包含子目录中的文件</string>
<string name="skip_hidden_folders">跳过被定义为隐藏的库文件夹</string>
<string name="skip_hidden_folders_desc">忽略以点开头的文件夹</string>
<string name="parallel_sync">并行同步</string>
<string name="parallel_sync_desc">同时处理多个文件夹</string>
<string name="start_full_sync">开始完整同步</string>
<string name="sync">同步</string>
<string name="sync_complete">同步完成：新增 %1$d 本，更新 %2$d 本</string>
<string name="sync_error">同步失败：%1$s</string>
```

## Performance Considerations

### 1. 内存优化
- 使用Flow进行流式处理，避免一次性加载所有文件
- 实现分页扫描，每批处理100个文件
- 及时释放Bitmap资源，使用WeakReference缓存

### 2. 并行处理
- 使用Dispatchers.IO进行文件I/O操作
- 限制并行协程数量（最多4个）
- 使用Semaphore控制并发访问

### 3. 数据库优化
- 使用批量插入减少事务次数
- 为常用查询字段添加索引
- 使用Room的@Transaction确保数据一致性

### 4. UI响应性
- 所有同步操作在后台线程执行
- 使用StateFlow实时更新进度
- 避免在主线程进行文件操作

## Accessibility

### 1. 语义化标注
- 所有交互元素包含contentDescription
- 复选框包含stateDescription表示当前状态
- 按钮包含role语义信息

### 2. 键盘导航
- 支持Tab键在元素间切换
- 支持Enter/Space键激活元素
- 支持Escape键关闭对话框

### 3. 屏幕阅读器支持
- 使用semantics修饰符提供额外信息
- 状态变化时提供音频反馈
- 错误信息通过announceForAccessibility播报

### 4. 视觉辅助
- 确保对比度符合WCAG AA标准（4.5:1）
- 支持系统字体缩放
- 提供足够的触摸目标尺寸（最小48dp）

## Future Enhancements

1. **云同步支持**
   - 支持WebDAV、FTP等协议
   - 增量云同步，仅上传变更
   - 冲突解决策略

2. **智能同步**
   - 基于使用频率优先同步
   - 自动识别重复文件
   - 智能分类和标签建议

3. **同步调度**
   - 定时自动同步
   - 基于网络状态的智能同步
   - 低电量时暂停同步

4. **高级过滤**
   - 自定义文件类型过滤
   - 基于文件大小的过滤
   - 正则表达式文件名匹配

5. **同步历史**
   - 记录每次同步的详细日志
   - 支持回滚到之前的状态
   - 导出同步报告
