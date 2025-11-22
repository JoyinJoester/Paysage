package takagi.ru.paysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.BookSource
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.ui.components.ExpressiveIconButton
import takagi.ru.paysage.ui.components.BookSourceCard
import takagi.ru.paysage.viewmodel.OnlineSourceViewModel

/**
 * 在线书源管理屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineSourceScreen(
    categoryType: CategoryType,
    onBackClick: () -> Unit,
    onBookClick: (Long) -> Unit = {},
    viewModel: OnlineSourceViewModel = viewModel()
) {
    val sources by viewModel.sourcesByCategory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 设置当前分类类型
    LaunchedEffect(categoryType) {
        viewModel.setSelectedCategoryType(categoryType)
    }
    
    // 显示消息
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (categoryType == CategoryType.MANGA)
                                R.string.manga_sources
                            else
                                R.string.novel_sources
                        )
                    )
                },
                navigationIcon = {
                    ExpressiveIconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    ExpressiveIconButton(
                        onClick = { /* TODO: 打开添加书源对话框 */ }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_source)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                sources.isEmpty() -> {
                    EmptySourcesView(
                        categoryType = categoryType,
                        onAddClick = { /* TODO: 打开添加书源对话框 */ },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sources, key = { it.id }) { source ->
                            BookSourceCard(
                                source = source,
                                onClick = { /* TODO: 打开书源详情 */ },
                                onToggleEnabled = {
                                    viewModel.toggleSourceEnabled(source.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 空书源视图
 */
@Composable
private fun EmptySourcesView(
    categoryType: CategoryType,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "暂无书源",
            style = MaterialTheme.typography.titleLarge
        )
        
        Text(
            text = if (categoryType == CategoryType.MANGA)
                "添加漫画书源以开始在线阅读"
            else
                "添加小说书源以开始在线阅读",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(onClick = onAddClick) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_source))
        }
    }
}
