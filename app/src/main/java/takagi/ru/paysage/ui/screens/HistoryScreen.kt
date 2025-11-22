package takagi.ru.paysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.HistoryItem
import takagi.ru.paysage.ui.components.*
import takagi.ru.paysage.viewmodel.HistoryViewModel

/**
 * 历史记录屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
    onItemClick: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val showClearConfirmDialog by viewModel.showClearConfirmDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // 显示错误提示
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            HistoryTopBar(
                onBackClick = onBackClick,
                onClearAllClick = { viewModel.showClearConfirmDialog() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                historyItems.isEmpty() -> {
                    EmptyHistoryView()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = historyItems,
                            key = { it.id }
                        ) { item ->
                            HistoryListItem(
                                item = item,
                                onClick = { onItemClick(item) },
                                onLongClick = { viewModel.selectItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 清空确认对话框
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideClearConfirmDialog() },
            title = { Text(stringResource(R.string.history_clear_all)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllHistory() }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideClearConfirmDialog() }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
    
    // 历史记录项操作菜单
    selectedItem?.let { item ->
        HistoryItemBottomSheet(
            item = item,
            onDismiss = { viewModel.selectItem(null) },
            onOpenFile = {
                viewModel.selectItem(null)
                onItemClick(item)
            },
            onDeleteItem = {
                viewModel.deleteHistoryItem(item.id)
                viewModel.selectItem(null)
            }
        )
    }
}

/**
 * 历史记录项底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItemBottomSheet(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit,
    onDeleteItem: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // 打开文件
            TextButton(
                onClick = onOpenFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_open_file),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 删除记录
            TextButton(
                onClick = onDeleteItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_delete_item),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
