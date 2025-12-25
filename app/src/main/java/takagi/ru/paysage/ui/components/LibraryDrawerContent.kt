package takagi.ru.paysage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.data.model.Folder
import takagi.ru.paysage.data.model.ModuleType


/**
 * Layer 1 icon categories - determines what Layer 2 displays
 */
enum class DrawerLayer1Item {
    LIBRARY,    // 书库视图 (default)
    SOURCES,    // 书源管理
    HISTORY,    // 历史记录
    SETTINGS,   // 设置
    INFO        // 信息
}

/**
 * Layer 2 menu items for Library view
 */
enum class LibraryDrawerItem {
    ALL, AUTHOR, SERIES, YEAR, FOLDER, COLLECTIONS
}

/**
 * Library Drawer with Two-Column Layout:
 * - Layer 1 (Left): Narrow icon strip - controls Layer 2 content
 * - Layer 2 (Right): Content panel - changes based on Layer 1 selection
 */
@Composable
fun LibraryDrawerContent(
    selectedLibraryItem: LibraryDrawerItem = LibraryDrawerItem.ALL,
    onLibraryItemClick: (LibraryDrawerItem) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onModeClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onCloudClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onFilePickerClick: () -> Unit = {},
    onCreateFolderClick: () -> Unit = {},
    onScanSource: (android.net.Uri) -> Unit = {},
    bookCount: Int = 0,
    allBooks: List<takagi.ru.paysage.data.model.Book> = emptyList(),
    customFolders: List<Folder> = emptyList(),
    onCreateFolder: (String) -> Unit = {},
    onRenameFolder: (Folder, String) -> Unit = { _, _ -> },
    onDeleteFolder: (Folder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Track which Layer 1 item is selected
    var selectedLayer1 by remember { mutableStateOf(DrawerLayer1Item.LIBRARY) }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // ========== Layer 1: Icon Strip (Left Column) ==========
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Icons (Actions)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RailIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = onRefreshClick
                    )
                    RailIconButton(
                        icon = Icons.Default.Description,
                        onClick = onFilePickerClick
                    )
                    RailIconButton(
                        icon = Icons.Outlined.Cloud,
                        onClick = onCloudClick
                    )
                }

                // Bottom Icons (Navigation - affects Layer 2)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RailIconButton(
                        icon = Icons.Default.Settings,
                        isSelected = selectedLayer1 == DrawerLayer1Item.SETTINGS,
                        onClick = {
                            selectedLayer1 = DrawerLayer1Item.SETTINGS
                            onSettingsClick()
                        }
                    )
                    RailIconButton(
                        icon = Icons.Default.Info,
                        isSelected = selectedLayer1 == DrawerLayer1Item.INFO,
                        onClick = {
                            selectedLayer1 = DrawerLayer1Item.INFO
                            onInfoClick()
                        }
                    )
                    RailIconButton(
                        icon = Icons.Default.History,
                        isSelected = selectedLayer1 == DrawerLayer1Item.HISTORY,
                        onClick = {
                            selectedLayer1 = DrawerLayer1Item.HISTORY
                            onHistoryClick()
                        }
                    )
                    RailIconButton(
                        icon = Icons.Default.Book,
                        isSelected = selectedLayer1 == DrawerLayer1Item.LIBRARY,
                        onClick = { selectedLayer1 = DrawerLayer1Item.LIBRARY }
                    )
                    RailIconButton(
                        icon = Icons.Outlined.FolderOpen,
                        isSelected = selectedLayer1 == DrawerLayer1Item.SOURCES,
                        onClick = { selectedLayer1 = DrawerLayer1Item.SOURCES }
                    )
                    RailIconButton(
                        icon = Icons.Outlined.List,
                        onClick = { /* Toggle list view */ }
                    )
                }
            }

            // ========== Layer 2: Content Panel (Right Column) ==========
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp)
            ) {
                // Content changes based on Layer 1 selection
                when (selectedLayer1) {
                    DrawerLayer1Item.LIBRARY -> LibraryLayer2Content(
                        selectedItem = selectedLibraryItem,
                        onItemClick = onLibraryItemClick,
                        onModeClick = onModeClick,
                        onCreateFolderClick = onCreateFolderClick,
                        bookCount = bookCount,
                        customFolders = customFolders,
                        onCreateFolder = onCreateFolder,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder
                    )
                    DrawerLayer1Item.SOURCES -> SourcesLayer2Content(
                        onFilePickerClick = onFilePickerClick,
                        onScanSource = onScanSource,
                        allBooks = allBooks
                    )
                    DrawerLayer1Item.HISTORY -> HistoryLayer2Content()
                    DrawerLayer1Item.SETTINGS -> SettingsLayer2Content()
                    DrawerLayer1Item.INFO -> InfoLayer2Content()
                }
            }
        }
    }
}

// ========== Layer 2 Content Variants ==========


@Composable
private fun LibraryLayer2Content(
    selectedItem: LibraryDrawerItem,
    onItemClick: (LibraryDrawerItem) -> Unit,
    onModeClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    bookCount: Int,
    customFolders: List<Folder> = emptyList(),
    onCreateFolder: (String) -> Unit = {},
    onRenameFolder: (Folder, String) -> Unit = { _, _ -> },
    onDeleteFolder: (Folder) -> Unit = {}
) {
    // Edit mode state
    var isEditMode by remember { mutableStateOf(false) }
    
    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Folder?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "书库视图",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "${bookCount}本书",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Menu List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Static menu items
            DrawerMenuItem(Icons.Default.GridView, "显示全部", selectedItem == LibraryDrawerItem.ALL) { onItemClick(LibraryDrawerItem.ALL) }
            DrawerMenuItem(Icons.Default.Person, "按作者显示", selectedItem == LibraryDrawerItem.AUTHOR) { onItemClick(LibraryDrawerItem.AUTHOR) }
            DrawerMenuItem(Icons.Default.ViewCarousel, "按系列显示", selectedItem == LibraryDrawerItem.SERIES) { onItemClick(LibraryDrawerItem.SERIES) }
            DrawerMenuItem(Icons.Default.CalendarToday, "按年度显示", selectedItem == LibraryDrawerItem.YEAR) { onItemClick(LibraryDrawerItem.YEAR) }
            DrawerMenuItem(Icons.Outlined.Folder, "按文件夹显示", selectedItem == LibraryDrawerItem.FOLDER) { onItemClick(LibraryDrawerItem.FOLDER) }
            DrawerMenuItem(Icons.Rounded.CollectionsBookmark, "收藏", selectedItem == LibraryDrawerItem.COLLECTIONS) { onItemClick(LibraryDrawerItem.COLLECTIONS) }
            
            // Custom folders section
            if (customFolders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "自定义文件夹",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                
                customFolders.forEach { folder ->
                    CustomFolderMenuItem(
                        folder = folder,
                        isSelected = false, // TODO: track custom folder selection
                        isEditMode = isEditMode,
                        onClick = { /* TODO: filter by custom folder */ },
                        onRename = { showRenameDialog = folder },
                        onDelete = { showDeleteDialog = folder }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Tool Bar - Updated: Edit, New Folder, Night Mode
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Edit Mode Toggle
                IconButton(onClick = { isEditMode = !isEditMode }) {
                    Icon(
                        if (isEditMode) Icons.Default.Check else Icons.Default.Edit, 
                        contentDescription = if (isEditMode) "完成" else "编辑",
                        tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Create New Folder
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Night Mode Toggle
                IconButton(onClick = onModeClick) {
                    Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    
    // Create Folder Dialog
    if (showCreateDialog) {
        FolderNameDialog(
            title = "新建文件夹",
            initialName = "",
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    
    // Rename Folder Dialog
    showRenameDialog?.let { folder ->
        FolderNameDialog(
            title = "重命名文件夹",
            initialName = folder.name,
            onConfirm = { newName ->
                onRenameFolder(folder, newName)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除文件夹") },
            text = { Text("确定要删除「${folder.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder(folder)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * Custom folder menu item with edit/delete buttons
 */
@Composable
private fun CustomFolderMenuItem(
    folder: Folder,
    isSelected: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // Edit mode actions
            if (isEditMode) {
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "重命名",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Folder name input dialog (for create/rename)
 */
@Composable
private fun FolderNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf(initialName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("文件夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName.trim()) },
                enabled = folderName.isNotBlank()
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


@Composable
private fun HistoryLayer2Content() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(20.dp)
    ) {
        Text(
            text = "阅读历史",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "最近阅读的书籍将在这里显示",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SourcesLayer2Content(
    onFilePickerClick: () -> Unit = {},
    onScanSource: (android.net.Uri) -> Unit = {},
    allBooks: List<takagi.ru.paysage.data.model.Book> = emptyList()
) {
    // 使用独立的 SourcesContent 组件
    takagi.ru.paysage.ui.components.SourcesContent(
        onAddSourceClick = onFilePickerClick,
        onScanSource = onScanSource,
        allBooks = allBooks
    )
}

@Composable
private fun SettingsLayer2Content() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(20.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "应用设置将在这里显示",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoLayer2Content() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(20.dp)
    ) {
        Text(
            text = "关于",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Paysage Reader\n版本 1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ========== Helper Composables ==========

@Composable
private fun RailIconButton(
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
