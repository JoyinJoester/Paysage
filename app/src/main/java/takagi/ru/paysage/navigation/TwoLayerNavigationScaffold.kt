package takagi.ru.paysage.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.ui.components.ExpressiveIconButton
import takagi.ru.paysage.util.WindowSizeClass
import takagi.ru.paysage.util.rememberWindowSizeClass
import takagi.ru.paysage.util.statusBarsPadding

/**
 * 两层导航脚手架主容器
 * 整合第一层导航栏和第二层抽屉，提供完整的导航体验
 * 支持响应式布局适配
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
    content: @Composable (windowSizeClass: WindowSizeClass, onOpenDrawer: () -> Unit) -> Unit
) {
    // 检测窗口尺寸
    val windowSizeClass = rememberWindowSizeClass()
    
    // 提供打开抽屉的回调
    val onOpenDrawer = remember {
        { onDrawerStateChange(true) }
    }
    
    // 根据窗口尺寸选择布局
    when (windowSizeClass) {
        WindowSizeClass.Compact -> {
            CompactNavigationLayout(
                navigationState = navigationState,
                onPrimaryItemClick = onPrimaryItemClick,
                onSecondaryItemClick = onSecondaryItemClick,
                onDrawerStateChange = onDrawerStateChange,
                onSourceSelectionClick = onSourceSelectionClick,
                onLocalMangaClick = onLocalMangaClick,
                onLocalReadingClick = onLocalReadingClick,
                onMangaSourceClick = onMangaSourceClick,
                onReadingSourceClick = onReadingSourceClick,
                selectedLocalMangaPath = selectedLocalMangaPath,
                selectedLocalReadingPath = selectedLocalReadingPath,
                onVersionClick = onVersionClick,
                onLicenseClick = onLicenseClick,
                onGithubClick = onGithubClick,
                onCreateFolderClick = onCreateFolderClick,
                modifier = modifier
            ) {
                content(windowSizeClass, onOpenDrawer)
            }
        }
        WindowSizeClass.Medium -> {
            MediumNavigationLayout(
                navigationState = navigationState,
                onPrimaryItemClick = onPrimaryItemClick,
                onSecondaryItemClick = onSecondaryItemClick,
                onDrawerStateChange = onDrawerStateChange,
                onSourceSelectionClick = onSourceSelectionClick,
                onLocalMangaClick = onLocalMangaClick,
                onLocalReadingClick = onLocalReadingClick,
                onMangaSourceClick = onMangaSourceClick,
                onReadingSourceClick = onReadingSourceClick,
                selectedLocalMangaPath = selectedLocalMangaPath,
                selectedLocalReadingPath = selectedLocalReadingPath,
                onVersionClick = onVersionClick,
                onLicenseClick = onLicenseClick,
                onGithubClick = onGithubClick,
                onCreateFolderClick = onCreateFolderClick,
                modifier = modifier
            ) {
                content(windowSizeClass, onOpenDrawer)
            }
        }
        WindowSizeClass.Expanded -> {
            ExpandedNavigationLayout(
                navigationState = navigationState,
                onPrimaryItemClick = onPrimaryItemClick,
                onSecondaryItemClick = onSecondaryItemClick,
                onDrawerStateChange = onDrawerStateChange,
                onSourceSelectionClick = onSourceSelectionClick,
                onLocalMangaClick = onLocalMangaClick,
                onLocalReadingClick = onLocalReadingClick,
                onMangaSourceClick = onMangaSourceClick,
                onReadingSourceClick = onReadingSourceClick,
                selectedLocalMangaPath = selectedLocalMangaPath,
                selectedLocalReadingPath = selectedLocalReadingPath,
                onVersionClick = onVersionClick,
                onLicenseClick = onLicenseClick,
                onGithubClick = onGithubClick,
                onCreateFolderClick = onCreateFolderClick,
                modifier = modifier
            ) {
                content(windowSizeClass, onOpenDrawer)
            }
        }
    }
}

/**
 * Compact 布局 - 手机
 * 真正的两层嵌套：第一层固定图标栏 + 第二层滑动内容抽屉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactNavigationLayout(
    navigationState: NavigationState,
    onPrimaryItemClick: (PrimaryNavItem) -> Unit,
    onSecondaryItemClick: (SecondaryNavItem) -> Unit,
    onDrawerStateChange: (Boolean) -> Unit,
    onSourceSelectionClick: () -> Unit,
    onLocalMangaClick: () -> Unit,
    onLocalReadingClick: () -> Unit,
    onMangaSourceClick: () -> Unit,
    onReadingSourceClick: () -> Unit,
    selectedLocalMangaPath: String?,
    selectedLocalReadingPath: String?,
    onVersionClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    
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
    
    // 第二层抽屉（内容抽屉）
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // 两层嵌套结构
            Row(modifier = Modifier.fillMaxHeight()) {
                // 第一层：固定的图标导航栏
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 源选择按钮
                        Surface(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .size(56.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = onSourceSelectionClick
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.FolderOpen,
                                    contentDescription = "选择源",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier
                                .width(48.dp)
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 第一层导航图标
                        val context = androidx.compose.ui.platform.LocalContext.current
                        PrimaryNavItem.values().forEach { item ->
                            val isSelected = navigationState.selectedPrimaryItem == item
                            
                            Surface(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(56.dp),
                                shape = MaterialTheme.shapes.large,
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    androidx.compose.ui.graphics.Color.Transparent,
                                onClick = { onPrimaryItemClick(item) }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = context.getString(item.contentDescriptionRes),
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 第二层：内容区域
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    SecondaryDrawerContent(
                        selectedPrimaryItem = navigationState.selectedPrimaryItem,
                        onItemClick = onSecondaryItemClick,
                        onVersionClick = onVersionClick,
                        onLicenseClick = onLicenseClick,
                        onGithubClick = onGithubClick,
                        showHeader = false,
                        showSourceSelection = navigationState.showSourceSelection,
                        onLocalMangaClick = onLocalMangaClick,
                        onLocalReadingClick = onLocalReadingClick,
                        onMangaSourceClick = onMangaSourceClick,
                        onReadingSourceClick = onReadingSourceClick,
                        selectedLocalMangaPath = selectedLocalMangaPath,
                        selectedLocalReadingPath = selectedLocalReadingPath,
                        onCreateFolderClick = onCreateFolderClick
                    )
                }
            }
        },
        modifier = modifier,
        gesturesEnabled = true
    ) {
        content()
    }
}

/**
 * Medium 布局 - 小平板
 * 显示第一层导航栏，第二层模态显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediumNavigationLayout(
    navigationState: NavigationState,
    onPrimaryItemClick: (PrimaryNavItem) -> Unit,
    onSecondaryItemClick: (SecondaryNavItem) -> Unit,
    onDrawerStateChange: (Boolean) -> Unit,
    onSourceSelectionClick: () -> Unit,
    onLocalMangaClick: () -> Unit,
    onLocalReadingClick: () -> Unit,
    onMangaSourceClick: () -> Unit,
    onReadingSourceClick: () -> Unit,
    selectedLocalMangaPath: String?,
    selectedLocalReadingPath: String?,
    onVersionClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    
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
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                SecondaryDrawerContent(
                    selectedPrimaryItem = navigationState.selectedPrimaryItem,
                    onItemClick = onSecondaryItemClick,
                    onVersionClick = onVersionClick,
                    onLicenseClick = onLicenseClick,
                    onGithubClick = onGithubClick,
                    showSourceSelection = navigationState.showSourceSelection,
                    onLocalMangaClick = onLocalMangaClick,
                    onLocalReadingClick = onLocalReadingClick,
                    onMangaSourceClick = onMangaSourceClick,
                    onReadingSourceClick = onReadingSourceClick,
                    selectedLocalMangaPath = selectedLocalMangaPath,
                    selectedLocalReadingPath = selectedLocalReadingPath,
                    onCreateFolderClick = onCreateFolderClick
                )
            }
        },
        modifier = modifier,
        gesturesEnabled = true
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 第一层导航栏
            PrimaryNavigationRail(
                selectedItem = navigationState.selectedPrimaryItem,
                onItemClick = onPrimaryItemClick,
                onFolderPickerClick = onSourceSelectionClick
            )
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/**
 * Expanded 布局 - 大平板/桌面
 * 两层都固定显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedNavigationLayout(
    navigationState: NavigationState,
    onPrimaryItemClick: (PrimaryNavItem) -> Unit,
    onSecondaryItemClick: (SecondaryNavItem) -> Unit,
    onDrawerStateChange: (Boolean) -> Unit,
    onSourceSelectionClick: () -> Unit,
    onLocalMangaClick: () -> Unit,
    onLocalReadingClick: () -> Unit,
    onMangaSourceClick: () -> Unit,
    onReadingSourceClick: () -> Unit,
    selectedLocalMangaPath: String?,
    selectedLocalReadingPath: String?,
    onVersionClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(modifier = modifier.fillMaxSize()) {
        // 第一层导航栏
        PrimaryNavigationRail(
            selectedItem = navigationState.selectedPrimaryItem,
            onItemClick = onPrimaryItemClick,
            onFolderPickerClick = onSourceSelectionClick
        )
        
        // 第二层永久显示
        Surface(
            modifier = Modifier.width(320.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            SecondaryDrawerContent(
                selectedPrimaryItem = navigationState.selectedPrimaryItem,
                onItemClick = onSecondaryItemClick,
                onVersionClick = onVersionClick,
                onLicenseClick = onLicenseClick,
                onGithubClick = onGithubClick,
                showSourceSelection = navigationState.showSourceSelection,
                onLocalMangaClick = onLocalMangaClick,
                onLocalReadingClick = onLocalReadingClick,
                onMangaSourceClick = onMangaSourceClick,
                onReadingSourceClick = onReadingSourceClick,
                selectedLocalMangaPath = selectedLocalMangaPath,
                selectedLocalReadingPath = selectedLocalReadingPath,
                onCreateFolderClick = onCreateFolderClick
            )
        }
        
        // 内容区域
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
