package takagi.ru.paysage.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import takagi.ru.paysage.ui.components.reader.ReaderContent
import takagi.ru.paysage.ui.components.reader.QuickSettingsPanel
import takagi.ru.paysage.ui.components.reader.ReadingSettingsDialog
import takagi.ru.paysage.reader.touch.TouchZone
import takagi.ru.paysage.viewmodel.ReaderViewModel
import takagi.ru.paysage.data.model.PageMode

/**
 * 阅读器界面 - 简化版
 * 
 * 功能：
 * - 显示当前页面图片
 * - 顶部工具栏（返回、标题、页码）
 * - 底部工具栏（上一页、进度条、下一页）
 * - 点击切换工具栏显示/隐藏
 * - 滑动翻页
 * - 双击/捏合缩放
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    initialPage: Int = 0,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()
    
    // 快速设置面板状态
    var showQuickSettings by remember { mutableStateOf(false) }
    
    // 完整设置对话框状态
    var showFullSettings by remember { mutableStateOf(false) }

    // 进度条拖拽状态
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    // 当非拖拽状态下，同步当前页码到进度条
    LaunchedEffect(uiState.currentPage) {
        // 只有当页面真正改变时，才重置拖拽状态并同步值
        // 这样可以防止松手瞬间滑块跳回旧位置
        if (isDraggingSlider) {
            isDraggingSlider = false
        }
        sliderValue = uiState.currentPage.toFloat()
    }
    
    // 打开书籍
    LaunchedEffect(bookId) {
        viewModel.openBook(bookId)
    }
    
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = uiState.isToolbarVisible,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(uiState.bookTitle)
                            Text(
                                "${uiState.currentPage + 1} / ${uiState.totalPages}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.cleanup()
                            onBackClick()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.isToolbarVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousPage() },
                            enabled = uiState.currentPage > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "上一页"
                            )
                        }
                        
                        Slider(
                            value = if (isDraggingSlider) sliderValue else uiState.currentPage.toFloat(),
                            onValueChange = { 
                                sliderValue = it
                                isDraggingSlider = true
                            },
                            onValueChangeFinished = {
                                // 延迟一小段时间再重置拖拽状态，防止 UI 闪烁
                                // 因为 goToPage 是异步的，currentPage 不会立即更新
                                viewModel.goToPage(sliderValue.toInt())
                                // 注意：这里不立即设置 isDraggingSlider = false
                                // 而是等待 currentPage 发生变化后再重置，或者使用一个短暂的延迟
                            },
                            valueRange = 0f..maxOf(0f, (uiState.totalPages - 1).toFloat()),
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = { viewModel.nextPage() },
                            enabled = uiState.currentPage < uiState.totalPages - 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "下一页"
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // 快速设置 FAB
            AnimatedVisibility(
                visible = uiState.isToolbarVisible && !showQuickSettings,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { showQuickSettings = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "快速设置"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    if (uiState.isToolbarVisible) paddingValues else PaddingValues(0.dp)
                )
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    ErrorView(
                        error = uiState.error!!,
                        onRetry = { viewModel.openBook(bookId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pageBitmap != null -> {
                    // 根据翻页模式选择不同的渲染组件
                    when (uiState.readerConfig.pageMode) {
                        takagi.ru.paysage.data.model.PageMode.COVER,
                        takagi.ru.paysage.data.model.PageMode.SIMULATION -> {
                            // 覆盖翻页模式 - 优化版实现
                            // 注意：这里暂时用覆盖翻页替代仿真翻页，因为仿真翻页尚未实现
                            takagi.ru.paysage.reader.animation.SimpleCoverFlipReader(
                                totalPages = uiState.totalPages,
                                currentPage = uiState.currentPage,
                                currentBitmap = pageBitmap, // 传入当前页的 Bitmap
                                onLoadPage = { page -> viewModel.getPageBitmap(page) },
                                onPreload = { page -> viewModel.preloadPage(page) },
                                onTap = { zone ->
                                    when (zone) {
                                        TouchZone.CENTER, 
                                        TouchZone.TOP_CENTER, 
                                        TouchZone.BOTTOM_CENTER -> {
                                            viewModel.toggleToolbar()
                                        }
                                        else -> {
                                            // 其他区域不处理，翻页由手势控制
                                        }
                                    }
                                },
                                onPageChange = { page ->
                                    viewModel.goToPage(page)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            // 其他翻页模式使用原有的 ReaderContent
                            ReaderContent(
                                currentPageBitmap = pageBitmap,
                                nextPageBitmap = viewModel.getNextPageBitmap(),
                                previousPageBitmap = viewModel.getPreviousPageBitmap(),
                                config = uiState.readerConfig,
                                onTap = { zone ->
                                    when (zone) {
                                        TouchZone.CENTER, 
                                        TouchZone.TOP_CENTER, 
                                        TouchZone.BOTTOM_CENTER -> {
                                            viewModel.toggleToolbar()
                                        }
                                        TouchZone.MIDDLE_LEFT,
                                        TouchZone.TOP_LEFT,
                                        TouchZone.BOTTOM_LEFT -> {
                                            viewModel.previousPage()
                                        }
                                        TouchZone.MIDDLE_RIGHT,
                                        TouchZone.TOP_RIGHT,
                                        TouchZone.BOTTOM_RIGHT -> {
                                            viewModel.nextPage()
                                        }
                                    }
                                },
                                onSwipeLeft = { viewModel.nextPage() },
                                onSwipeRight = { viewModel.previousPage() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            
            // 快速设置面板
            AnimatedVisibility(
                visible = showQuickSettings,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                QuickSettingsPanel(
                    config = uiState.readerConfig,
                    onConfigChange = { viewModel.updateConfig(it) },
                    onMoreSettings = {
                        showQuickSettings = false
                        showFullSettings = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // 完整设置对话框
        if (showFullSettings) {
            ReadingSettingsDialog(
                config = uiState.readerConfig,
                onConfigChange = { viewModel.updateConfig(it) },
                onDismiss = { showFullSettings = false }
            )
        }
    }
}

/**
 * 错误视图组件
 */
@Composable
fun ErrorView(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
