package takagi.ru.paysage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import takagi.ru.paysage.ui.components.CreateFolderDialog
import takagi.ru.paysage.viewmodel.CreateFolderState
import takagi.ru.paysage.data.model.ModuleType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import takagi.ru.paysage.navigation.Screen
import takagi.ru.paysage.navigation.TwoLayerNavigationScaffold
import takagi.ru.paysage.repository.SettingsRepository
import takagi.ru.paysage.ui.screen.AppearanceSettingsScreen
import takagi.ru.paysage.ui.screen.BookmarksScreen
import takagi.ru.paysage.ui.screens.LibraryScreen
import takagi.ru.paysage.ui.screens.ReaderScreen
import takagi.ru.paysage.ui.theme.PaysageTheme
import takagi.ru.paysage.util.LocaleHelper
import takagi.ru.paysage.viewmodel.NavigationViewModel
import takagi.ru.paysage.viewmodel.SettingsViewModel
import takagi.ru.paysage.viewmodel.FolderViewModel
import takagi.ru.paysage.repository.FolderRepositoryImpl
import takagi.ru.paysage.data.PaysageDatabase
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()
    private val libraryViewModel: takagi.ru.paysage.viewmodel.LibraryViewModel by viewModels()
    private val sourceSelectionViewModel: takagi.ru.paysage.viewmodel.SourceSelectionViewModel by viewModels()
    
    private val folderViewModel: FolderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = PaysageDatabase.getDatabase(applicationContext)
                val repository = FolderRepositoryImpl(applicationContext, database)
                @Suppress("UNCHECKED_CAST")
                return FolderViewModel(applicationContext, repository) as T
            }
        }
    }
    
    private val historyViewModel: takagi.ru.paysage.viewmodel.HistoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = PaysageDatabase.getDatabase(applicationContext)
                val repository = takagi.ru.paysage.repository.HistoryRepositoryImpl(database.historyDao())
                @Suppress("UNCHECKED_CAST")
                return takagi.ru.paysage.viewmodel.HistoryViewModel(repository) as T
            }
        }
    }
    

    
    // 本地漫画文件夹选择器
    private val mangaFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // 获取持久化权限
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                Log.d("MainActivity", "Selected manga folder: $it")
                Toast.makeText(this, "开始扫描漫画文件夹...", Toast.LENGTH_SHORT).show()
                
                // 保存路径
                sourceSelectionViewModel.updateLocalMangaPath(it.toString())
                
                // 关闭源选择页面
                navigationViewModel.toggleSourceSelection(false)
                
                // 扫描文件夹中的漫画文件
                libraryViewModel.scanBooksFromUri(it)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error scanning manga folder", e)
                Toast.makeText(this, "扫描失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // 本地阅读文件夹选择器
    private val readingFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // 获取持久化权限
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                Log.d("MainActivity", "Selected reading folder: $it")
                Toast.makeText(this, "开始扫描阅读文件夹...", Toast.LENGTH_SHORT).show()
                
                // 保存路径
                sourceSelectionViewModel.updateLocalReadingPath(it.toString())
                
                // 关闭源选择页面
                navigationViewModel.toggleSourceSelection(false)
                
                // 扫描文件夹中的阅读文件
                libraryViewModel.scanBooksFromUri(it)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error scanning reading folder", e)
                Toast.makeText(this, "扫描失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val settingsRepository = SettingsRepository(newBase)
            val language = runBlocking {
                settingsRepository.settingsFlow.first().language
            }
            super.attachBaseContext(LocaleHelper.setLocale(newBase, language))
        } else {
            super.attachBaseContext(newBase)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化文件夹结构
        takagi.ru.paysage.util.FolderPathManager.initializeFolderStructure(this)
        
        enableEdgeToEdge()
        // 启用沉浸式状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 监听语言切换事件，触发 Activity 重建
        lifecycleScope.launch {
            settingsViewModel.recreateActivityEvent.collect {
                try {
                    Log.d("MainActivity", "Recreating activity for language change")
                    recreate()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to recreate activity", e)
                }
            }
        }
        
        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            
            PaysageTheme(
                themeMode = settings.themeMode,
                colorScheme = settings.colorScheme,
                dynamicColor = settings.dynamicColorEnabled,
                customPrimaryColor = settings.customPrimaryColor,
                customSecondaryColor = settings.customSecondaryColor,
                customTertiaryColor = settings.customTertiaryColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PaysageApp(
                        navigationViewModel = navigationViewModel,
                        sourceSelectionViewModel = sourceSelectionViewModel,
                        onLocalMangaClick = { mangaFolderPickerLauncher.launch(null) },
                        onLocalReadingClick = { readingFolderPickerLauncher.launch(null) },
                        folderViewModel = folderViewModel,
                        historyViewModel = historyViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun PaysageApp(
    navigationViewModel: NavigationViewModel,
    sourceSelectionViewModel: takagi.ru.paysage.viewmodel.SourceSelectionViewModel,
    onLocalMangaClick: () -> Unit = {},
    onLocalReadingClick: () -> Unit = {},
    folderViewModel: FolderViewModel? = null,
    historyViewModel: takagi.ru.paysage.viewmodel.HistoryViewModel? = null
) {
    val navController = rememberNavController()
    val navigationState by navigationViewModel.navigationState.collectAsState()
    val selectedLocalMangaPath by sourceSelectionViewModel.selectedLocalMangaPath.collectAsState()
    val selectedLocalReadingPath by sourceSelectionViewModel.selectedLocalReadingPath.collectAsState()
    val error by sourceSelectionViewModel.error.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 文件夹创建对话框状态
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val createFolderState by (folderViewModel?.createFolderState ?: remember { 
        kotlinx.coroutines.flow.MutableStateFlow<CreateFolderState>(CreateFolderState.Idle) 
    }).collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示错误提示
    androidx.compose.runtime.LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            sourceSelectionViewModel.clearError()
        }
    }
    
    // 处理文件夹创建状态
    LaunchedEffect(createFolderState) {
        when (val state = createFolderState) {
            is CreateFolderState.Success -> {
                // 先关闭对话框
                showCreateFolderDialog = false
                // 显示成功提示
                snackbarHostState.showSnackbar(
                    context.getString(R.string.folder_create_success)
                )
                // 重置状态
                folderViewModel?.resetCreateFolderState()
            }
            is CreateFolderState.Error -> {
                // 显示错误提示但不关闭对话框，让用户可以重试
                snackbarHostState.showSnackbar(
                    context.getString(R.string.folder_create_failed, state.message)
                )
                // 重置状态
                folderViewModel?.resetCreateFolderState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        TwoLayerNavigationScaffold(
            navigationState = navigationState,
            onPrimaryItemClick = { item ->
                navigationViewModel.selectPrimaryItem(item)
            },
        onSecondaryItemClick = { item ->
            navigationViewModel.selectSecondaryItem(item)
            // 如果有路由，导航到对应页面
            item.route?.let { route ->
                navController.navigate(route) {
                    // 避免重复导航到同一页面
                    launchSingleTop = true
                }
            }
            // 如果有 action，执行 action
            item.action?.invoke()
        },
        onDrawerStateChange = { isOpen ->
            navigationViewModel.toggleSecondaryDrawer(isOpen)
        },
        onSourceSelectionClick = {
            navigationViewModel.toggleSourceSelection(true)
            navigationViewModel.toggleSecondaryDrawer(true)
        },
        onLocalMangaClick = onLocalMangaClick,
        onLocalReadingClick = onLocalReadingClick,
        onMangaSourceClick = {
            navController.navigate("online?category=manga") {
                launchSingleTop = true
            }
            navigationViewModel.toggleSourceSelection(false)
            navigationViewModel.toggleSecondaryDrawer(false)
        },
        onReadingSourceClick = {
            navController.navigate("online?category=novel") {
                launchSingleTop = true
            }
            navigationViewModel.toggleSourceSelection(false)
            navigationViewModel.toggleSecondaryDrawer(false)
        },
        selectedLocalMangaPath = selectedLocalMangaPath,
        selectedLocalReadingPath = selectedLocalReadingPath,
        onVersionClick = {
            // TODO: 显示版本信息对话框
        },
        onLicenseClick = {
            // TODO: 显示开源许可对话框
        },
        onGithubClick = {
            // TODO: 打开 GitHub 链接
        },
        onCreateFolderClick = { showCreateFolderDialog = true }
    ) { windowSizeClass, onOpenDrawer ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route
        ) {
        // 书库界面
        composable(
            route = Screen.Library.route,
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter")
            val category = backStackEntry.arguments?.getString("category")
            
            // 使用LibraryWithHistoryPager替代LibraryScreen，支持右滑进入历史记录
            if (historyViewModel != null) {
                takagi.ru.paysage.ui.screens.LibraryWithHistoryPager(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.Reader.createRoute(bookId))
                    },
                    onSettingsClick = {
                        // 不再需要，导航由侧边栏处理
                    },
                    onOpenDrawer = if (windowSizeClass == takagi.ru.paysage.util.WindowSizeClass.Compact) {
                        onOpenDrawer
                    } else {
                        null
                    },
                    filter = filter,
                    category = category,
                    onNavigateToCategory = { categoryName ->
                        navController.navigate(Screen.Library.createRoute(filter = "categories", category = categoryName)) {
                            launchSingleTop = true
                        }
                    },
                    historyViewModel = historyViewModel,
                    onHistoryItemClick = { item ->
                        // 打开历史记录中的文件
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(item.filePath), getMimeType(item.fileType))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "打开文件"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.history_file_not_found),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } else {
                LibraryScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.Reader.createRoute(bookId))
                    },
                    onSettingsClick = {
                        // 不再需要，导航由侧边栏处理
                    },
                    onOpenDrawer = if (windowSizeClass == takagi.ru.paysage.util.WindowSizeClass.Compact) {
                        onOpenDrawer
                    } else {
                        null
                    },
                    filter = filter,
                    category = category,
                    onNavigateToCategory = { categoryName ->
                        navController.navigate(Screen.Library.createRoute(filter = "categories", category = categoryName)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        
        // 阅读器界面
        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType },
                navArgument("page") { 
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val page = backStackEntry.arguments?.getInt("page") ?: -1
            
            ReaderScreen(
                bookId = bookId,
                initialPage = page,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // 书签列表界面
        composable(
            route = Screen.Bookmarks.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType },
                navArgument("bookTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            val bookTitle = backStackEntry.arguments?.getString("bookTitle") ?: "书籍"
            
            BookmarksScreen(
                bookId = bookId,
                bookTitle = bookTitle.replace("_", "/"),
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBookmarkClick = { pageNumber ->
                    // 返回阅读器并跳转到指定页
                    navController.popBackStack()
                    navController.navigate(Screen.Reader.createRoute(bookId, pageNumber))
                }
            )
        }
        
        // 设置界面
        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                navArgument("section") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section") ?: ""
            
            when (section) {
                "appearance" -> {
                    AppearanceSettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                else -> {
                    // 设置现在在侧边栏中，不需要单独的页面
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("请使用侧边栏中的设置")
                    }
                }
            }
        }
    }
        }
        
        // 创建文件夹对话框
        if (showCreateFolderDialog && folderViewModel != null) {
            CreateFolderDialog(
                onDismiss = { showCreateFolderDialog = false },
                onConfirm = { folderName ->
                    // 使用应用私有目录作为父路径
                    val parentPath = context.getExternalFilesDir(null)?.absolutePath
                    if (parentPath != null) {
                        folderViewModel.createFolder(
                            parentPath = parentPath,
                            folderName = folderName,
                            moduleType = ModuleType.LOCAL_MANAGEMENT
                        )
                    } else {
                        // 显示错误提示
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.error_invalid_path),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showCreateFolderDialog = false
                    }
                },
                existingFolderNames = emptyList(), // TODO: 从ViewModel获取现有文件夹列表
                isCreating = createFolderState is CreateFolderState.Creating
            )
        }
    }
}


/**
 * 根据文件类型获取MIME类型
 */
private fun getMimeType(fileType: String): String {
    return when (fileType.uppercase()) {
        "ZIP", "CBZ" -> "application/zip"
        "RAR", "CBR" -> "application/x-rar-compressed"
        "PDF" -> "application/pdf"
        "EPUB" -> "application/epub+zip"
        else -> "application/octet-stream"
    }
}
