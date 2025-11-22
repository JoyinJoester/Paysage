package takagi.ru.paysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.ui.components.*
import takagi.ru.paysage.viewmodel.FolderViewModel

/**
 * 文件夹管理屏幕
 * 
 * @param moduleType 模块类型（本地或在线）
 * @param onNavigateBack 返回回调
 * @param viewModel 文件夹 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagementScreen(
    moduleType: ModuleType,
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val editUiState by viewModel.editUiState.collectAsState()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 对话框状态
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    
    // 加载文件夹列表
    LaunchedEffect(moduleType) {
        viewModel.refreshFolders(moduleType)
    }
    
    Scaffold(
        topBar = {
            if (!editUiState.isEditMode) {
                TopAppBar(
                    title = { Text("文件夹管理") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        // 编辑按钮
                        ExpressiveIconButton(
                            onClick = { viewModel.enterEditMode() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!editUiState.isEditMode) {
                ExpressiveFAB(
                    onClick = { showCreateDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "创建文件夹"
                    )
                }
            }
        },
        snackbarHost = {
            ErrorSnackbarHost(snackbarHostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 编辑模式头部
                EditModeContainer(visible = editUiState.isEditMode) {
                    EditModeHeader(
                        selectedCount = selectedFolderIds.size,
                        totalCount = folders.size,
                        onCancel = { viewModel.exitEditMode() },
                        onSelectAll = { viewModel.selectAll() },
                        onDeselectAll = { viewModel.deselectAll() }
                    )
                }
                
                // 文件夹列表
                if (folders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无文件夹",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = folders,
                            key = { _, folder -> folder.id }
                        ) { index, folder ->
                            AnimatedFolderCard(
                                folder = folder,
                                isSelected = folder.id in selectedFolderIds,
                                isEditMode = editUiState.isEditMode,
                                index = index,
                                onSelect = { viewModel.toggleFolderSelection(folder.id) },
                                onClick = { /* 导航到文件夹内容 */ }
                            )
                        }
                    }
                }
                
                // 编辑模式底部操作栏
                EditModeContainer(visible = editUiState.isEditMode) {
                    EditModeBottomBar(
                        selectedCount = selectedFolderIds.size,
                        onRename = { showRenameDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onSort = { showSortDialog = true }
                    )
                }
            }
        }
    }
    
    // 创建文件夹对话框
    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { folderName ->
                viewModel.createFolder(folderName, moduleType)
                showCreateDialog = false
            }
        )
    }
    
    // 重命名对话框
    if (showRenameDialog && selectedFolderIds.size == 1) {
        val selectedFolder = folders.find { it.id == selectedFolderIds.first() }
        selectedFolder?.let { folder ->
            RenameFolderDialog(
                currentName = folder.name,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    val parentPath = viewModel.getModulePath(moduleType)
                    viewModel.renameFolder(folder.id, newName, parentPath, moduleType)
                    showRenameDialog = false
                }
            )
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog && selectedFolderIds.isNotEmpty()) {
        DeleteConfirmDialog(
            deleteCount = selectedFolderIds.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                val parentPath = viewModel.getModulePath(moduleType)
                viewModel.deleteSelectedFolders(parentPath, moduleType)
                showDeleteDialog = false
            }
        )
    }
    
    // 排序选项对话框
    if (showSortDialog) {
        SortOptionsDialog(
            currentOption = FolderSortOption.NAME_ASC,
            onDismiss = { showSortDialog = false },
            onConfirm = { _ ->
                // TODO: 实现排序逻辑
                showSortDialog = false
            }
        )
    }
}
