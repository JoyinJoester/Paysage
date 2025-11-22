# 快速开始指南

## 5分钟快速集成

### 步骤1：添加权限（如果需要）

在 `AndroidManifest.xml` 中添加存储权限：

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 步骤2：在MainActivity中添加ViewModel

```kotlin
import androidx.lifecycle.ViewModelProvider
import takagi.ru.paysage.viewmodel.FolderViewModel
import takagi.ru.paysage.repository.FolderRepositoryImpl
import takagi.ru.paysage.data.PaysageDatabase

class MainActivity : ComponentActivity() {
    
    private val folderViewModel: FolderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = PaysageDatabase.getDatabase(applicationContext)
                val repository = FolderRepositoryImpl(applicationContext, database)
                @Suppress("UNCHECKED_CAST")
                return FolderViewModel(repository) as T
            }
        }
    }
    
    // ... 其余代码
}
```

### 步骤3：添加对话框状态和逻辑

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        PaysageTheme {
            var showCreateFolderDialog by remember { mutableStateOf(false) }
            val createFolderState by folderViewModel.createFolderState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            
            // 处理创建结果
            LaunchedEffect(createFolderState) {
                when (val state = createFolderState) {
                    is CreateFolderState.Success -> {
                        snackbarHostState.showSnackbar(
                            getString(R.string.folder_create_success)
                        )
                        showCreateFolderDialog = false
                        folderViewModel.resetCreateFolderState()
                    }
                    is CreateFolderState.Error -> {
                        snackbarHostState.showSnackbar(
                            getString(R.string.folder_create_failed, state.message)
                        )
                        folderViewModel.resetCreateFolderState()
                    }
                    else -> {}
                }
            }
            
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                // 你的主内容
                YourMainContent(
                    onCreateFolderClick = { showCreateFolderDialog = true },
                    modifier = Modifier.padding(padding)
                )
            }
            
            // 显示对话框
            if (showCreateFolderDialog) {
                CreateFolderDialog(
                    onDismiss = { showCreateFolderDialog = false },
                    onConfirm = { folderName ->
                        val parentPath = getExternalFilesDir(null)?.absolutePath ?: ""
                        folderViewModel.createFolder(
                            parentPath = parentPath,
                            folderName = folderName,
                            moduleType = ModuleType.LOCAL_MANAGEMENT
                        )
                    },
                    existingFolderNames = emptyList(),
                    isCreating = createFolderState is CreateFolderState.Creating
                )
            }
        }
    }
}
```

### 步骤4：测试

1. 运行应用
2. 触发创建文件夹功能
3. 输入文件夹名称
4. 验证创建成功

## 完整示例

查看 `INTEGRATION_GUIDE.md` 获取更详细的集成说明。

## 常见问题

### Q: 如何获取现有文件夹列表？
```kotlin
val folders by folderViewModel.folders.collectAsState()

LaunchedEffect(Unit) {
    folderViewModel.refreshFolders(parentPath, ModuleType.LOCAL_MANAGEMENT)
}
```

### Q: 如何处理权限？
```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // 创建文件夹
    }
}

// 请求权限
permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
```

### Q: 如何自定义父路径？
```kotlin
// 使用Environment获取标准目录
val parentPath = Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOCUMENTS
).absolutePath

// 或使用应用私有目录
val parentPath = context.getExternalFilesDir(null)?.absolutePath ?: ""
```

## 下一步

- 查看 `FINAL_SUMMARY.md` 了解完整功能
- 查看 `INTEGRATION_GUIDE.md` 了解详细集成步骤
- 查看 `IMPLEMENTATION_PROGRESS.md` 了解实现进度
