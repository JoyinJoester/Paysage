package takagi.ru.paysage.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import takagi.ru.paysage.ui.theme.ExpressiveAnimations

/**
 * 第二层抽屉内容主容器
 * 根据选中的第一层菜单显示对应的详细内容
 */
@Composable
fun SecondaryDrawerContent(
    selectedPrimaryItem: PrimaryNavItem,
    onItemClick: (SecondaryNavItem) -> Unit,
    modifier: Modifier = Modifier,
    onVersionClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {},
    onGithubClick: () -> Unit = {},
    showHeader: Boolean = false,
    showSourceSelection: Boolean = false,
    onLocalMangaClick: () -> Unit = {},
    onLocalReadingClick: () -> Unit = {},
    onMangaSourceClick: () -> Unit = {},
    onReadingSourceClick: () -> Unit = {},
    selectedLocalMangaPath: String? = null,
    selectedLocalReadingPath: String? = null,
    onCreateFolderClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 可选的头部
        if (showHeader) {
            DrawerHeader()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 标题 - 使用更大更醒目的字体
        val context = androidx.compose.ui.platform.LocalContext.current
        Text(
            text = context.getString(selectedPrimaryItem.labelRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primaryContainer
        )
        
        // 使用 AnimatedContent 实现内容切换动画
        AnimatedContent(
            targetState = if (showSourceSelection) "source_selection" else selectedPrimaryItem.name,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = ExpressiveAnimations.DURATION_SHORT,
                        easing = ExpressiveAnimations.EmphasizedDecelerateEasing
                    )
                ) togetherWith fadeOut(
                    animationSpec = tween(
                        durationMillis = ExpressiveAnimations.DURATION_SHORT,
                        easing = ExpressiveAnimations.EmphasizedAccelerateEasing
                    )
                )
            },
            label = "secondary_drawer_content"
        ) { targetState ->
            if (targetState == "source_selection") {
                // 显示源选择页面
                takagi.ru.paysage.ui.components.SourceSelectionContent(
                    selectedLocalMangaPath = selectedLocalMangaPath,
                    selectedLocalReadingPath = selectedLocalReadingPath,
                    onLocalMangaClick = onLocalMangaClick,
                    onLocalReadingClick = onLocalReadingClick,
                    onMangaSourceClick = onMangaSourceClick,
                    onReadingSourceClick = onReadingSourceClick
                )
            } else {
                when (selectedPrimaryItem) {
                    PrimaryNavItem.Library -> LibraryDrawerContent(
                        onItemClick = onItemClick,
                        onCreateFolderClick = onCreateFolderClick
                    )
                    PrimaryNavItem.LocalLibrary -> LibraryDrawerContent(
                        onItemClick = onItemClick,
                        onCreateFolderClick = onCreateFolderClick
                    )
                    PrimaryNavItem.OnlineLibrary -> LibraryDrawerContent(
                        onItemClick = onItemClick,
                        onCreateFolderClick = onCreateFolderClick
                    )
                    PrimaryNavItem.Settings -> SettingsDrawerContent(onItemClick)
                    PrimaryNavItem.About -> AboutDrawerContent(
                        onItemClick = onItemClick,
                        onVersionClick = onVersionClick,
                        onLicenseClick = onLicenseClick,
                        onGithubClick = onGithubClick
                    )
                }
            }
        }
    }
}

/**
 * 书库菜单内容
 */
@Composable
fun LibraryDrawerContent(
    onItemClick: (SecondaryNavItem) -> Unit,
    modifier: Modifier = Modifier,
    onCreateFolderClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 获取 FolderViewModel
    val folderViewModel: takagi.ru.paysage.viewmodel.FolderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val database = takagi.ru.paysage.data.PaysageDatabase.getDatabase(context)
                val repository = takagi.ru.paysage.repository.FolderRepositoryImpl(context, database)
                @Suppress("UNCHECKED_CAST")
                return takagi.ru.paysage.viewmodel.FolderViewModel(context, repository) as T
            }
        }
    )
    
    val folders by folderViewModel.folders.collectAsState()
    
    // 编辑模式状态
    var isEditMode by remember { mutableStateOf(false) }
    var selectedFolders by remember { mutableStateOf(setOf<Long>()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<takagi.ru.paysage.data.model.Folder?>(null) }
    
    // 加载文件夹列表
    androidx.compose.runtime.LaunchedEffect(Unit) {
        folderViewModel.refreshFolders(
            path = context.getExternalFilesDir(null)?.absolutePath ?: "",
            moduleType = takagi.ru.paysage.data.model.ModuleType.LOCAL_MANAGEMENT
        )
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 创建文件夹按钮
        takagi.ru.paysage.ui.components.CreateFolderButton(
            onClick = onCreateFolderClick,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // 编辑文件夹按钮
        if (folders.isNotEmpty()) {
            OutlinedButton(
                onClick = { 
                    isEditMode = !isEditMode
                    if (!isEditMode) {
                        selectedFolders = setOf()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isEditMode) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isEditMode)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    else
                        Color.Transparent,
                    contentColor = if (isEditMode)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = if (isEditMode) "退出编辑" else "编辑文件夹",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "退出编辑" else "编辑文件夹")
            }
        }
        
        // 编辑模式工具栏
        if (isEditMode && folders.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选择 ${selectedFolders.size} 项",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { 
                                selectedFolders = if (selectedFolders.size == folders.size) {
                                    setOf()
                                } else {
                                    folders.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Text(if (selectedFolders.size == folders.size) "取消全选" else "全选")
                        }
                        
                        if (selectedFolders.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("删除")
                            }
                        }
                    }
                }
            }
        }
        
        // 显示文件夹列表
        if (folders.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Text(
                text = "文件夹",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            folders.forEach { folder ->
                takagi.ru.paysage.ui.components.FolderListItem(
                    folder = folder,
                    onClick = { 
                        // TODO: 导航到文件夹内容
                    },
                    isEditMode = isEditMode,
                    isSelected = folder.id in selectedFolders,
                    onSelectionToggle = {
                        selectedFolders = if (folder.id in selectedFolders) {
                            selectedFolders - folder.id
                        } else {
                            selectedFolders + folder.id
                        }
                    },
                    onRename = {
                        folderToRename = folder
                        showRenameDialog = true
                    },
                    onDelete = {
                        selectedFolders = setOf(folder.id)
                        showDeleteDialog = true
                    }
                )
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LibraryNavItems.getItems(context).forEach { item ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null
                    )
                },
                selected = false,
                onClick = { onItemClick(item) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.large
            )
        }
    }
    
    // 重命名对话框
    if (showRenameDialog && folderToRename != null) {
        takagi.ru.paysage.ui.components.RenameFolderDialog(
            currentName = folderToRename!!.name,
            onDismiss = { 
                showRenameDialog = false
                folderToRename = null
            },
            onConfirm = { newName ->
                folderViewModel.renameFolder(
                    folderId = folderToRename!!.id,
                    newName = newName,
                    path = context.getExternalFilesDir(null)?.absolutePath ?: "",
                    moduleType = takagi.ru.paysage.data.model.ModuleType.LOCAL_MANAGEMENT
                )
                showRenameDialog = false
                folderToRename = null
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        val selectedFoldersList = folders.filter { it.id in selectedFolders }
        takagi.ru.paysage.ui.components.DeleteConfirmDialog(
            deleteCount = selectedFoldersList.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                if (selectedFolders.size == 1) {
                    folderViewModel.deleteFolder(
                        folderId = selectedFolders.first(),
                        path = context.getExternalFilesDir(null)?.absolutePath ?: "",
                        moduleType = takagi.ru.paysage.data.model.ModuleType.LOCAL_MANAGEMENT
                    )
                } else if (selectedFolders.isNotEmpty()) {
                    // 批量删除：先同步选中状态到 ViewModel，然后调用删除
                    selectedFolders.forEach { folderId ->
                        folderViewModel.toggleFolderSelection(folderId)
                    }
                    folderViewModel.deleteSelectedFolders(
                        path = context.getExternalFilesDir(null)?.absolutePath ?: "",
                        moduleType = takagi.ru.paysage.data.model.ModuleType.LOCAL_MANAGEMENT
                    )
                }
                
                selectedFolders = setOf()
                showDeleteDialog = false
                isEditMode = false
            }
        )
    }
}

/**
 * 设置菜单内容
 * 直接在第二层抽屉中显示所有设置选项
 */
@Composable
fun SettingsDrawerContent(
    onItemClick: (SecondaryNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 获取 SettingsViewModel
    val settingsViewModel: takagi.ru.paysage.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )
    val settings by settingsViewModel.settings.collectAsState()
    val cacheSize by settingsViewModel.cacheSize.collectAsState()
    
    // 对话框状态
    var showLibraryLayoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showReadingDirectionDialog by remember { mutableStateOf(false) }
    var showGridColumnsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 外观设置
        SettingsSection(title = context.getString(takagi.ru.paysage.R.string.settings_appearance)) {
            // 主题模式
            SettingsItemClickable(
                icon = Icons.Default.DarkMode,
                title = context.getString(takagi.ru.paysage.R.string.settings_theme_mode),
                subtitle = when (settings.themeMode) {
                    takagi.ru.paysage.data.model.ThemeMode.LIGHT -> context.getString(takagi.ru.paysage.R.string.settings_theme_light)
                    takagi.ru.paysage.data.model.ThemeMode.DARK -> context.getString(takagi.ru.paysage.R.string.settings_theme_dark)
                    takagi.ru.paysage.data.model.ThemeMode.SYSTEM -> context.getString(takagi.ru.paysage.R.string.settings_theme_system)
                },
                onClick = { showThemeDialog = true }
            )
            
            // 配色方案
            SettingsItemClickable(
                icon = Icons.Default.Palette,
                title = context.getString(takagi.ru.paysage.R.string.settings_color_scheme),
                subtitle = when (settings.colorScheme) {
                    takagi.ru.paysage.data.model.ColorScheme.DEFAULT -> context.getString(takagi.ru.paysage.R.string.settings_color_default)
                    takagi.ru.paysage.data.model.ColorScheme.OCEAN_BLUE -> context.getString(takagi.ru.paysage.R.string.settings_color_ocean_blue)
                    takagi.ru.paysage.data.model.ColorScheme.SUNSET_ORANGE -> context.getString(takagi.ru.paysage.R.string.settings_color_sunset_orange)
                    takagi.ru.paysage.data.model.ColorScheme.FOREST_GREEN -> context.getString(takagi.ru.paysage.R.string.settings_color_forest_green)
                    takagi.ru.paysage.data.model.ColorScheme.TECH_PURPLE -> context.getString(takagi.ru.paysage.R.string.settings_color_tech_purple)
                    takagi.ru.paysage.data.model.ColorScheme.BLACK_MAMBA -> context.getString(takagi.ru.paysage.R.string.settings_color_black_mamba)
                    takagi.ru.paysage.data.model.ColorScheme.GREY_STYLE -> context.getString(takagi.ru.paysage.R.string.settings_color_grey_style)
                    else -> context.getString(takagi.ru.paysage.R.string.settings_color_custom)
                },
                onClick = { showColorSchemeDialog = true }
            )
            
            // 壁纸取色开关
            SettingsCardWithSwitch(
                icon = Icons.Default.Colorize,
                title = context.getString(takagi.ru.paysage.R.string.settings_dynamic_color),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_dynamic_color_desc),
                checked = settings.dynamicColorEnabled,
                onCheckedChange = { settingsViewModel.updateDynamicColorEnabled(it) }
            )
            
            // 语言设置
            SettingsItemClickable(
                icon = Icons.Default.Language,
                title = context.getString(takagi.ru.paysage.R.string.settings_language),
                subtitle = when (settings.language) {
                    takagi.ru.paysage.data.model.Language.SYSTEM -> context.getString(takagi.ru.paysage.R.string.settings_language_system)
                    takagi.ru.paysage.data.model.Language.ENGLISH -> context.getString(takagi.ru.paysage.R.string.settings_language_english)
                    takagi.ru.paysage.data.model.Language.CHINESE -> context.getString(takagi.ru.paysage.R.string.settings_language_chinese)
                },
                onClick = { showLanguageDialog = true }
            )
            
            // 库布局
            SettingsItemClickable(
                icon = Icons.Default.ViewModule,
                title = "库布局",
                subtitle = when (settings.libraryLayout) {
                    takagi.ru.paysage.data.model.LibraryLayout.LIST -> "列表视图"
                    takagi.ru.paysage.data.model.LibraryLayout.COMPACT_GRID -> "紧凑网格"
                    takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY -> "纯封面"
                },
                onClick = { showLibraryLayoutDialog = true }
            )
            
            // 网格列数
            SettingsItemClickable(
                icon = Icons.Default.GridView,
                title = context.getString(takagi.ru.paysage.R.string.settings_grid_columns),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_grid_columns_desc, settings.gridColumns),
                onClick = { showGridColumnsDialog = true }
            )
            
            // 显示进度
            SettingsCardWithSwitch(
                icon = Icons.Default.BarChart,
                title = context.getString(takagi.ru.paysage.R.string.settings_show_progress),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_show_progress_desc),
                checked = settings.showProgress,
                onCheckedChange = { settingsViewModel.updateShowProgress(it) }
            )
        }
        
        // 阅读设置
        SettingsSection(title = context.getString(takagi.ru.paysage.R.string.settings_reading)) {
            // 阅读方向
            SettingsItemClickable(
                icon = Icons.Default.SwapHoriz,
                title = context.getString(takagi.ru.paysage.R.string.settings_reading_direction),
                subtitle = when (settings.readingDirection) {
                    takagi.ru.paysage.data.model.ReadingDirection.LEFT_TO_RIGHT -> context.getString(takagi.ru.paysage.R.string.settings_reading_direction_ltr)
                    takagi.ru.paysage.data.model.ReadingDirection.RIGHT_TO_LEFT -> context.getString(takagi.ru.paysage.R.string.settings_reading_direction_rtl)
                    takagi.ru.paysage.data.model.ReadingDirection.VERTICAL -> context.getString(takagi.ru.paysage.R.string.settings_reading_direction_vertical)
                },
                onClick = { showReadingDirectionDialog = true }
            )
            

            
            // 保持屏幕常亮
            SettingsCardWithSwitch(
                icon = Icons.Default.LightMode,
                title = context.getString(takagi.ru.paysage.R.string.settings_keep_screen_on),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_keep_screen_on_desc),
                checked = settings.keepScreenOn,
                onCheckedChange = { settingsViewModel.updateKeepScreenOn(it) }
            )
            
            // 音量键翻页
            SettingsCardWithSwitch(
                icon = Icons.Default.VolumeDown,
                title = context.getString(takagi.ru.paysage.R.string.settings_volume_key_navigation),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_volume_key_navigation_desc),
                checked = settings.volumeKeyNavigation,
                onCheckedChange = { settingsViewModel.updateVolumeKeyNavigation(it) }
            )
        }
        
        // 存储设置
        SettingsSection(title = context.getString(takagi.ru.paysage.R.string.settings_storage)) {
            // 启动自动扫描
            SettingsCardWithSwitch(
                icon = Icons.Default.Scanner,
                title = context.getString(takagi.ru.paysage.R.string.settings_auto_scan),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_auto_scan_desc),
                checked = settings.autoScanOnStart,
                onCheckedChange = { settingsViewModel.updateAutoScanOnStart(it) }
            )
            
            // 清除缓存
            SettingsItemClickable(
                icon = Icons.Default.DeleteSweep,
                title = context.getString(takagi.ru.paysage.R.string.settings_clear_cache),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_clear_cache_desc, cacheSize),
                onClick = { showClearCacheDialog = true }
            )
        }
        
        // 其他设置
        SettingsSection(title = context.getString(takagi.ru.paysage.R.string.settings_other)) {
            // 分析数据
            SettingsCardWithSwitch(
                icon = Icons.Default.Analytics,
                title = context.getString(takagi.ru.paysage.R.string.settings_analytics),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_analytics_desc),
                checked = settings.enableAnalytics,
                onCheckedChange = { settingsViewModel.updateEnableAnalytics(it) }
            )
            
            // 重置所有设置
            SettingsItemClickable(
                icon = Icons.Default.RestartAlt,
                title = context.getString(takagi.ru.paysage.R.string.settings_reset),
                subtitle = context.getString(takagi.ru.paysage.R.string.settings_reset_desc),
                onClick = { showResetDialog = true }
            )
        }
    }
    
    // 对话框
    if (showLibraryLayoutDialog) {
        LibraryLayoutDialog(
            currentLayout = settings.libraryLayout,
            onDismiss = { showLibraryLayoutDialog = false },
            onSelect = { layout ->
                settingsViewModel.updateLibraryLayout(layout)
                showLibraryLayoutDialog = false
            }
        )
    }
    
    if (showThemeDialog) {
        ThemeModeDialog(
            currentTheme = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                settingsViewModel.updateThemeMode(theme)
                showThemeDialog = false
            }
        )
    }
    
    if (showColorSchemeDialog) {
        ColorSchemeDialog(
            currentScheme = settings.colorScheme,
            onDismiss = { showColorSchemeDialog = false },
            onSelect = { scheme ->
                settingsViewModel.updateColorScheme(scheme)
                showColorSchemeDialog = false
            }
        )
    }
    
    if (showLanguageDialog) {
        val coroutineScope = rememberCoroutineScope()
        LanguageDialog(
            currentLanguage = settings.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = { language ->
                coroutineScope.launch {
                    settingsViewModel.updateLanguage(language)
                    showLanguageDialog = false
                    // 等待 DataStore 保存完成
                    delay(200)
                    // 重启 Activity 以应用语言更改
                    if (context is android.app.Activity) {
                        context.recreate()
                    }
                }
            }
        )
    }
    
    if (showReadingDirectionDialog) {
        ReadingDirectionDialog(
            currentDirection = settings.readingDirection,
            onDismiss = { showReadingDirectionDialog = false },
            onSelect = { direction ->
                settingsViewModel.updateReadingDirection(direction)
                showReadingDirectionDialog = false
            }
        )
    }
    
    if (showGridColumnsDialog) {
        GridColumnsDialog(
            currentColumns = settings.gridColumns,
            onDismiss = { showGridColumnsDialog = false },
            onSelect = { columns ->
                settingsViewModel.updateGridColumns(columns)
                showGridColumnsDialog = false
            }
        )
    }
    
    if (showClearCacheDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(context.getString(takagi.ru.paysage.R.string.dialog_clear_cache_title)) },
            text = { Text(context.getString(takagi.ru.paysage.R.string.dialog_clear_cache_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text(context.getString(takagi.ru.paysage.R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(context.getString(takagi.ru.paysage.R.string.dialog_cancel))
                }
            }
        )
    }
    

    
    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(context.getString(takagi.ru.paysage.R.string.dialog_reset_title)) },
            text = { Text(context.getString(takagi.ru.paysage.R.string.dialog_reset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.resetAllSettings()
                        showResetDialog = false
                    }
                ) {
                    Text(context.getString(takagi.ru.paysage.R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(context.getString(takagi.ru.paysage.R.string.dialog_cancel))
                }
            }
        )
    }
}



/**
 * 关于菜单内容
 */
@Composable
fun AboutDrawerContent(
    onItemClick: (SecondaryNavItem) -> Unit,
    onVersionClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        val items = AboutNavItems.getItems(
            context = context,
            onVersionClick = onVersionClick,
            onLicenseClick = onLicenseClick,
            onGithubClick = onGithubClick
        )
        
        items.forEach { item ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null
                    )
                },
                selected = false,
                onClick = { onItemClick(item) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.large
            )
        }
    }
}


/**
 * 设置分组
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 可点击的设置项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItemClickable(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
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

/**
 * 带开关的设置卡片
 */
@Composable
private fun SettingsCardWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * 主题模式对话框
 */
@Composable
private fun ThemeModeDialog(
    currentTheme: takagi.ru.paysage.data.model.ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (takagi.ru.paysage.data.model.ThemeMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(takagi.ru.paysage.R.string.settings_theme_mode)) },
        text = {
            Column {
                takagi.ru.paysage.data.model.ThemeMode.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { onSelect(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (theme) {
                                takagi.ru.paysage.data.model.ThemeMode.LIGHT -> context.getString(takagi.ru.paysage.R.string.settings_theme_light)
                                takagi.ru.paysage.data.model.ThemeMode.DARK -> context.getString(takagi.ru.paysage.R.string.settings_theme_dark)
                                takagi.ru.paysage.data.model.ThemeMode.SYSTEM -> context.getString(takagi.ru.paysage.R.string.settings_theme_system)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * 配色方案对话框（Monica 风格）
 */
@Composable
private fun ColorSchemeDialog(
    currentScheme: takagi.ru.paysage.data.model.ColorScheme,
    onDismiss: () -> Unit,
    onSelect: (takagi.ru.paysage.data.model.ColorScheme) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(takagi.ru.paysage.R.string.settings_color_scheme)) },
        text = {
            Column {
                // 默认配色
                ColorSchemeOption(
                    name = context.getString(takagi.ru.paysage.R.string.settings_color_default),
                    primaryColor = androidx.compose.ui.graphics.Color(0xFF6650a4),
                    secondaryColor = androidx.compose.ui.graphics.Color(0xFF625b71),
                    tertiaryColor = androidx.compose.ui.graphics.Color(0xFF7D5260),
                    isSelected = currentScheme == takagi.ru.paysage.data.model.ColorScheme.DEFAULT,
                    onClick = { onSelect(takagi.ru.paysage.data.model.ColorScheme.DEFAULT) }
                )
                
                // 海洋蓝
                ColorSchemeOption(
                    name = context.getString(takagi.ru.paysage.R.string.settings_color_ocean_blue),
                    primaryColor = androidx.compose.ui.graphics.Color(0xFF1565C0),
                    secondaryColor = androidx.compose.ui.graphics.Color(0xFF0277BD),
                    tertiaryColor = androidx.compose.ui.graphics.Color(0xFF26C6DA),
                    isSelected = currentScheme == takagi.ru.paysage.data.model.ColorScheme.OCEAN_BLUE,
                    onClick = { onSelect(takagi.ru.paysage.data.model.ColorScheme.OCEAN_BLUE) }
                )
                
                // 日落橙
                ColorSchemeOption(
                    name = context.getString(takagi.ru.paysage.R.string.settings_color_sunset_orange),
                    primaryColor = androidx.compose.ui.graphics.Color(0xFFE65100),
                    secondaryColor = androidx.compose.ui.graphics.Color(0xFFF57C00),
                    tertiaryColor = androidx.compose.ui.graphics.Color(0xFFFFA726),
                    isSelected = currentScheme == takagi.ru.paysage.data.model.ColorScheme.SUNSET_ORANGE,
                    onClick = { onSelect(takagi.ru.paysage.data.model.ColorScheme.SUNSET_ORANGE) }
                )
                
                // 森林绿
                ColorSchemeOption(
                    name = context.getString(takagi.ru.paysage.R.string.settings_color_forest_green),
                    primaryColor = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                    secondaryColor = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    tertiaryColor = androidx.compose.ui.graphics.Color(0xFF388E3C),
                    isSelected = currentScheme == takagi.ru.paysage.data.model.ColorScheme.FOREST_GREEN,
                    onClick = { onSelect(takagi.ru.paysage.data.model.ColorScheme.FOREST_GREEN) }
                )
                
                // 科技紫
                ColorSchemeOption(
                    name = context.getString(takagi.ru.paysage.R.string.settings_color_tech_purple),
                    primaryColor = androidx.compose.ui.graphics.Color(0xFF4A148C),
                    secondaryColor = androidx.compose.ui.graphics.Color(0xFF6A1B9A),
                    tertiaryColor = androidx.compose.ui.graphics.Color(0xFF8E24AA),
                    isSelected = currentScheme == takagi.ru.paysage.data.model.ColorScheme.TECH_PURPLE,
                    onClick = { onSelect(takagi.ru.paysage.data.model.ColorScheme.TECH_PURPLE) }
                )
                
                // 黑曼巴
                ColorSchemeOption(
                    name = context.getString(takagi.ru.paysage.R.string.settings_color_black_mamba),
                    primaryColor = androidx.compose.ui.graphics.Color(0xFF552583),
                    secondaryColor = androidx.compose.ui.graphics.Color(0xFFFDB927),
                    tertiaryColor = androidx.compose.ui.graphics.Color(0xFF2A2A2A),
                    isSelected = currentScheme == takagi.ru.paysage.data.model.ColorScheme.BLACK_MAMBA,
                    onClick = { onSelect(takagi.ru.paysage.data.model.ColorScheme.BLACK_MAMBA) }
                )
                
                // 小黑紫
                ColorSchemeOption(
                    name = context.getString(takagi.ru.paysage.R.string.settings_color_grey_style),
                    primaryColor = androidx.compose.ui.graphics.Color(0xFF424242),
                    secondaryColor = androidx.compose.ui.graphics.Color(0xFF616161),
                    tertiaryColor = androidx.compose.ui.graphics.Color(0xFF9E9E9E),
                    isSelected = currentScheme == takagi.ru.paysage.data.model.ColorScheme.GREY_STYLE,
                    onClick = { onSelect(takagi.ru.paysage.data.model.ColorScheme.GREY_STYLE) }
                )
            }
        },
        confirmButton = {}
    )
}

/**
 * 配色方案选项（Monica 风格）
 */
@Composable
private fun ColorSchemeOption(
    name: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    secondaryColor: androidx.compose.ui.graphics.Color,
    tertiaryColor: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                else androidx.compose.ui.graphics.Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 颜色圆圈
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(secondaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(tertiaryColor)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 名称
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            // 选中标记
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 语言对话框
 */
@Composable
private fun LanguageDialog(
    currentLanguage: takagi.ru.paysage.data.model.Language,
    onDismiss: () -> Unit,
    onSelect: (takagi.ru.paysage.data.model.Language) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(takagi.ru.paysage.R.string.settings_language)) },
        text = {
            Column {
                takagi.ru.paysage.data.model.Language.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = { onSelect(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (language) {
                                takagi.ru.paysage.data.model.Language.SYSTEM -> context.getString(takagi.ru.paysage.R.string.settings_language_system)
                                takagi.ru.paysage.data.model.Language.ENGLISH -> context.getString(takagi.ru.paysage.R.string.settings_language_english)
                                takagi.ru.paysage.data.model.Language.CHINESE -> context.getString(takagi.ru.paysage.R.string.settings_language_chinese)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * 阅读方向对话框
 */
@Composable
private fun ReadingDirectionDialog(
    currentDirection: takagi.ru.paysage.data.model.ReadingDirection,
    onDismiss: () -> Unit,
    onSelect: (takagi.ru.paysage.data.model.ReadingDirection) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(takagi.ru.paysage.R.string.dialog_reading_direction_title)) },
        text = {
            Column {
                takagi.ru.paysage.data.model.ReadingDirection.values().forEach { direction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(direction) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = direction == currentDirection,
                            onClick = { onSelect(direction) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (direction) {
                                takagi.ru.paysage.data.model.ReadingDirection.LEFT_TO_RIGHT -> context.getString(takagi.ru.paysage.R.string.settings_reading_direction_ltr)
                                takagi.ru.paysage.data.model.ReadingDirection.RIGHT_TO_LEFT -> context.getString(takagi.ru.paysage.R.string.settings_reading_direction_rtl)
                                takagi.ru.paysage.data.model.ReadingDirection.VERTICAL -> context.getString(takagi.ru.paysage.R.string.settings_reading_direction_vertical)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * 网格列数对话框
 */
@Composable
private fun GridColumnsDialog(
    currentColumns: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(takagi.ru.paysage.R.string.dialog_grid_columns_title)) },
        text = {
            Column {
                listOf(2, 3, 4, 5).forEach { columns ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(columns) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = columns == currentColumns,
                            onClick = { onSelect(columns) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = context.getString(takagi.ru.paysage.R.string.settings_grid_columns_desc, columns))
                    }
                }
            }
        },
        confirmButton = {}
    )
}


/**
 * 库布局选择对话框
 */
@Composable
private fun LibraryLayoutDialog(
    currentLayout: takagi.ru.paysage.data.model.LibraryLayout,
    onDismiss: () -> Unit,
    onSelect: (takagi.ru.paysage.data.model.LibraryLayout) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("库布局") },
        text = {
            androidx.compose.foundation.layout.Column {
                takagi.ru.paysage.data.model.LibraryLayout.entries.forEach { layout ->
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(layout) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = layout == currentLayout,
                            onClick = { onSelect(layout) }
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Column {
                            androidx.compose.material3.Text(
                                text = when (layout) {
                                    takagi.ru.paysage.data.model.LibraryLayout.LIST -> "列表视图"
                                    takagi.ru.paysage.data.model.LibraryLayout.COMPACT_GRID -> "紧凑网格"
                                    takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY -> "纯封面"
                                },
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                            )
                            androidx.compose.material3.Text(
                                text = when (layout) {
                                    takagi.ru.paysage.data.model.LibraryLayout.LIST -> "详细信息列表"
                                    takagi.ru.paysage.data.model.LibraryLayout.COMPACT_GRID -> "大卡片网格"
                                    takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY -> "纯封面网格"
                                },
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}



