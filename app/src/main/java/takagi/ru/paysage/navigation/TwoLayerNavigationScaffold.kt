package takagi.ru.paysage.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import takagi.ru.paysage.ui.components.LibraryDrawerContent
import takagi.ru.paysage.ui.components.LibraryDrawerItem
import takagi.ru.paysage.util.WindowSizeClass
import takagi.ru.paysage.util.rememberWindowSizeClass

/**
 * 两层导航脚手架主容器
 * 使用简化的 LibraryDrawerContent 实现两层导航
 * 支持响应式布局适配
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoLayerNavigationScaffold(
    onLibraryItemClick: (LibraryDrawerItem) -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onCloudClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onModeClick: () -> Unit = {},
    onFilePickerClick: () -> Unit = {},
    onCreateFolderClick: () -> Unit = {},
    onScanSource: (android.net.Uri) -> Unit = {},
    bookCount: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable (windowSizeClass: WindowSizeClass, onOpenDrawer: () -> Unit) -> Unit
) {
    // 检测窗口尺寸
    val windowSizeClass = rememberWindowSizeClass()
    
    // 抽屉状态
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 选中的书库菜单项
    var selectedLibraryItem by remember { mutableStateOf(LibraryDrawerItem.ALL) }
    
    // 打开抽屉的回调
    val onOpenDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }
    
    // 根据窗口尺寸选择布局
    when (windowSizeClass) {
        WindowSizeClass.Compact, WindowSizeClass.Medium -> {
            // Compact/Medium: 使用模态抽屉
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    LibraryDrawerContent(
                        selectedLibraryItem = selectedLibraryItem,
                        onLibraryItemClick = { item ->
                            selectedLibraryItem = item
                            onLibraryItemClick(item)
                            scope.launch { drawerState.close() }
                        },
                        onSettingsClick = onSettingsClick,
                        onInfoClick = onInfoClick,
                        onModeClick = onModeClick,
                        onRefreshClick = onRefreshClick,
                        onCloudClick = onCloudClick,
                        onHistoryClick = onHistoryClick,
                        onFilePickerClick = onFilePickerClick,
                        onCreateFolderClick = onCreateFolderClick,
                        onScanSource = onScanSource,
                        bookCount = bookCount
                    )
                },
                modifier = modifier,
                gesturesEnabled = true
            ) {
                content(windowSizeClass, onOpenDrawer)
            }
        }
        WindowSizeClass.Expanded -> {
            // Expanded: 永久显示侧边栏
            Row(modifier = modifier.fillMaxSize()) {
                // 左侧侧边栏
                LibraryDrawerContent(
                    selectedLibraryItem = selectedLibraryItem,
                    onLibraryItemClick = { item ->
                        selectedLibraryItem = item
                        onLibraryItemClick(item)
                    },
                    onSettingsClick = onSettingsClick,
                    onInfoClick = onInfoClick,
                    onModeClick = onModeClick,
                    onRefreshClick = onRefreshClick,
                    onCloudClick = onCloudClick,
                    onHistoryClick = onHistoryClick,
                    onFilePickerClick = onFilePickerClick,
                    onCreateFolderClick = onCreateFolderClick,
                    onScanSource = onScanSource,
                    bookCount = bookCount,
                    modifier = Modifier.width(344.dp) // 64 + 280
                )
                
                // 内容区域
                Box(modifier = Modifier.fillMaxSize()) {
                    content(windowSizeClass, onOpenDrawer)
                }
            }
        }
    }
}

// ========== 保留向后兼容的签名 ==========

/**
 * 向后兼容的 TwoLayerNavigationScaffold
 * 支持旧的 NavigationState 参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoLayerNavigationScaffold(
    navigationState: NavigationState,
    onPrimaryItemClick: (PrimaryNavItem) -> Unit,
    onSecondaryItemClick: (SecondaryNavItem) -> Unit,
    onDrawerStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSourceSelectionClick: () -> Unit = {},
    onLocalMangaClick: () -> Unit = {},
    onLocalReadingClick: () -> Unit = {},
    onMangaSourceClick: () -> Unit = {},
    onReadingSourceClick: () -> Unit = {},
    selectedLocalMangaPath: String? = null,
    selectedLocalReadingPath: String? = null,
    onVersionClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {},
    onGithubClick: () -> Unit = {},
    onCreateFolderClick: () -> Unit = {},
    onScanSource: (android.net.Uri) -> Unit = {},
    content: @Composable (windowSizeClass: WindowSizeClass, onOpenDrawer: () -> Unit) -> Unit
) {
    // 检测窗口尺寸
    val windowSizeClass = rememberWindowSizeClass()
    
    // 抽屉状态
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 选中的书库菜单项
    var selectedLibraryItem by remember { mutableStateOf(LibraryDrawerItem.ALL) }
    
    // 同步抽屉状态
    LaunchedEffect(navigationState.isSecondaryDrawerOpen) {
        if (navigationState.isSecondaryDrawerOpen && !drawerState.isOpen) {
            drawerState.open()
        } else if (!navigationState.isSecondaryDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }
    
    LaunchedEffect(drawerState.currentValue) {
        val isOpen = drawerState.currentValue == DrawerValue.Open
        if (isOpen != navigationState.isSecondaryDrawerOpen) {
            onDrawerStateChange(isOpen)
        }
    }
    
    // 打开抽屉的回调
    val onOpenDrawer: () -> Unit = {
        onDrawerStateChange(true)
    }
    
    // 根据窗口尺寸选择布局
    when (windowSizeClass) {
        WindowSizeClass.Compact, WindowSizeClass.Medium -> {
            // Compact/Medium: 使用模态抽屉
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    LibraryDrawerContent(
                        selectedLibraryItem = selectedLibraryItem,
                        onLibraryItemClick = { item ->
                            selectedLibraryItem = item
                            scope.launch { drawerState.close() }
                        },
                        onSettingsClick = { onPrimaryItemClick(PrimaryNavItem.Settings) },
                        onInfoClick = { onPrimaryItemClick(PrimaryNavItem.About) },
                        onModeClick = {},
                        onRefreshClick = {},
                        onCloudClick = {},
                        onHistoryClick = {},
                        onFilePickerClick = onSourceSelectionClick,
                        onCreateFolderClick = onCreateFolderClick,
                        onScanSource = onScanSource,
                        bookCount = 0
                    )
                },
                modifier = modifier,
                gesturesEnabled = true
            ) {
                content(windowSizeClass, onOpenDrawer)
            }
        }
        WindowSizeClass.Expanded -> {
            // Expanded: 永久显示侧边栏
            Row(modifier = modifier.fillMaxSize()) {
                // 左侧侧边栏
                LibraryDrawerContent(
                    selectedLibraryItem = selectedLibraryItem,
                    onLibraryItemClick = { item ->
                        selectedLibraryItem = item
                    },
                    onSettingsClick = { onPrimaryItemClick(PrimaryNavItem.Settings) },
                    onInfoClick = { onPrimaryItemClick(PrimaryNavItem.About) },
                    onModeClick = {},
                    onRefreshClick = {},
                    onCloudClick = {},
                    onHistoryClick = {},
                    onFilePickerClick = onSourceSelectionClick,
                    onCreateFolderClick = onCreateFolderClick,
                    onScanSource = onScanSource,
                    bookCount = 0,
                    modifier = Modifier.width(344.dp) // 64 + 280
                )
                
                // 内容区域
                Box(modifier = Modifier.fillMaxSize()) {
                    content(windowSizeClass, onOpenDrawer)
                }
            }
        }
    }
}
