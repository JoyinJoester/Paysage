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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                        bookCount = bookCount
                    )
                    DrawerLayer1Item.SOURCES -> SourcesLayer2Content(
                        onFilePickerClick = onFilePickerClick,
                        onScanSource = onScanSource
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
    bookCount: Int
) {
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
            DrawerMenuItem(Icons.Default.GridView, "显示全部", selectedItem == LibraryDrawerItem.ALL) { onItemClick(LibraryDrawerItem.ALL) }
            DrawerMenuItem(Icons.Default.Person, "按作者显示", selectedItem == LibraryDrawerItem.AUTHOR) { onItemClick(LibraryDrawerItem.AUTHOR) }
            DrawerMenuItem(Icons.Default.ViewCarousel, "按系列显示", selectedItem == LibraryDrawerItem.SERIES) { onItemClick(LibraryDrawerItem.SERIES) }
            DrawerMenuItem(Icons.Default.CalendarToday, "按年度显示", selectedItem == LibraryDrawerItem.YEAR) { onItemClick(LibraryDrawerItem.YEAR) }
            DrawerMenuItem(Icons.Outlined.Folder, "按文件夹显示", selectedItem == LibraryDrawerItem.FOLDER) { onItemClick(LibraryDrawerItem.FOLDER) }
            DrawerMenuItem(Icons.Rounded.CollectionsBookmark, "收藏", selectedItem == LibraryDrawerItem.COLLECTIONS) { onItemClick(LibraryDrawerItem.COLLECTIONS) }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Tool Bar
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
                IconButton(onClick = { /* Sliders/Filter */ }) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onCreateFolderClick) {
                    Icon(Icons.Default.CreateNewFolder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onModeClick) {
                    Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
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
    onScanSource: (android.net.Uri) -> Unit = {}
) {
    // 使用独立的 SourcesContent 组件
    takagi.ru.paysage.ui.components.SourcesContent(
        onAddSourceClick = onFilePickerClick,
        onScanSource = onScanSource
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
