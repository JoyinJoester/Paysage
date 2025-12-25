package takagi.ru.paysage.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import takagi.ru.paysage.ui.components.reader.ReaderContent
import takagi.ru.paysage.ui.components.reader.QuickSettingsPanel
import takagi.ru.paysage.ui.components.reader.ReadingSettingsDialog
import takagi.ru.paysage.reader.touch.TouchZone
import takagi.ru.paysage.viewmodel.ReaderViewModel
import takagi.ru.paysage.data.model.PageMode

/**
 * 阅读器界面 - M3E 沉浸式重构版
 *
 * 设计特性：
 * - 全屏沉浸：隐藏系统状态栏和导航栏
 * - 悬浮控制：工具栏覆盖在内容之上，避免内容跳动
 * - M3E 风格：使用 Surface, Tonal palette, 圆角与间距
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

    // 系统栏控制器
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    
    // 监听工具栏可见性，控制系统栏
    LaunchedEffect(uiState.isToolbarVisible) {
        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, view)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiState.isToolbarVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    // 应用屏幕常亮设置
    LaunchedEffect(uiState.readerConfig.keepScreenOn) {
        window?.let { win ->
            if (uiState.readerConfig.keepScreenOn) {
                win.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // 当非拖拽状态下，同步当前页码到进度条
    LaunchedEffect(uiState.currentPage) {
        if (isDraggingSlider) {
            isDraggingSlider = false
        }
        sliderValue = uiState.currentPage.toFloat()
    }
    
    // 打开书籍
    LaunchedEffect(bookId) {
        viewModel.openBook(bookId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. 底层：阅读内容 (全屏铺满)
        Box(modifier = Modifier.fillMaxSize()) {
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
                    // 保持原有的翻页动画逻辑不改动
                    when (uiState.readerConfig.pageMode) {
                        takagi.ru.paysage.data.model.PageMode.COVER,
                        takagi.ru.paysage.data.model.PageMode.SIMULATION -> {
                            takagi.ru.paysage.reader.animation.SimpleCoverFlipReader(
                                totalPages = uiState.totalPages,
                                currentPage = uiState.currentPage,
                                currentBitmap = pageBitmap,
                                onLoadPage = { page -> viewModel.getPageBitmap(page) },
                                onPreload = { page -> viewModel.preloadPage(page) },
                                onTap = { zone ->
                                    if (zone == TouchZone.CENTER || 
                                        zone == TouchZone.TOP_CENTER || 
                                        zone == TouchZone.BOTTOM_CENTER) {
                                        viewModel.toggleToolbar()
                                        // 任何触摸都关闭快速设置
                                        if (showQuickSettings) showQuickSettings = false
                                    }
                                },
                                onPageChange = { page ->
                                    viewModel.goToPage(page)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
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
                                            if (showQuickSettings) showQuickSettings = false
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
        }


        // 1.5 系统信息层 (全屏模式下显示)
        // 只有在内容加载成功且工具栏隐藏时显示 (或者一直显示，取决于需求，这里设为工具栏隐藏时显示以模拟沉浸式信息) 
        if (pageBitmap != null) {
            ReaderSystemInfo(
                title = uiState.bookTitle,
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                visible = !uiState.isToolbarVisible,
                modifier = Modifier
            )
        }

        // 2. 顶层：控制面板 (Overlay)
        // 顶部栏
        AnimatedVisibility(
            visible = uiState.isToolbarVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            // 添加渐变背景，使白色文字在任何背景下可见
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.bookTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.totalPages > 0) {
                                Text(
                                    text = "${uiState.currentPage + 1} / ${uiState.totalPages}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.cleanup()
                            onBackClick()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: 书签功能 */ }) {
                            Icon(Icons.Default.BookmarkBorder, "添加书签")
                        }
                        IconButton(onClick = { 
                            showFullSettings = true
                            viewModel.toggleToolbar() // 打开全屏设置时隐藏工具栏
                        }) {
                            Icon(Icons.Default.Settings, "设置")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // 底部栏
        AnimatedVisibility(
            visible = uiState.isToolbarVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column {
                // 快速设置面板 (如果显示)
                AnimatedVisibility(
                    visible = showQuickSettings,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        QuickSettingsPanel(
                            config = uiState.readerConfig,
                            onConfigChange = { viewModel.updateConfig(it) },
                            onMoreSettings = {
                                showQuickSettings = false
                                showFullSettings = true
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 底部控制条 (悬浮药丸样式)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                        .navigationBarsPadding(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 上一页
                        IconButton(
                            onClick = { viewModel.previousPage() },
                            enabled = uiState.currentPage > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, "上一页")
                        }
                        
                        // 进度条
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Slider(
                                value = if (isDraggingSlider) sliderValue else uiState.currentPage.toFloat(),
                                onValueChange = { 
                                    sliderValue = it
                                    isDraggingSlider = true
                                },
                                onValueChangeFinished = {
                                    viewModel.goToPage(sliderValue.toInt())
                                },
                                valueRange = 0f..maxOf(0f, (uiState.totalPages - 1).toFloat()),
                            )
                        }
                        
                        // 下一页
                        IconButton(
                            onClick = { viewModel.nextPage() },
                            enabled = uiState.currentPage < uiState.totalPages - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, "下一页")
                        }
                        
                        // 快速设置显隐开关
                        FilledTonalIconButton(
                            onClick = { showQuickSettings = !showQuickSettings }
                        ) {
                            Icon(Icons.Default.Tune, "阅读设置")
                        }
                    }
                }
            }
        }
        
        // 3. 全屏设置对话框
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
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("重试加载")
        }
    }
}

/**
 * 阅读器系统信息覆盖层
 * 显示：左上角标题，左下角页码，右下角时间/电量
 */
@Composable
fun ReaderSystemInfo(
    title: String,
    currentPage: Int,
    totalPages: Int,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(100) }
    val context = LocalContext.current
    
    // 更新时间
    LaunchedEffect(Unit) {
        while (true) {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            currentTime = String.format("%02d:%02d", hour, minute)
            kotlinx.coroutines.delay(1000 * 60) // 每分钟更新
        }
    }
    
    // 监听电量
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                intent?.let {
                    val level = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100 / scale.toFloat()).toInt()
                    }
                }
            }
        }
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp) // 避免贴边
        ) {
            // 左上角标题 (状态栏下方)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding() // 避开状态栏区域
                    .padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 左下角页码
            Text(
                text = "${currentPage + 1}/$totalPages",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // 右下角时间与电量
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                
                // 自定义绘制的电池图标
                BatteryIcon(level = batteryLevel, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun BatteryIcon(level: Int, color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 20.dp, height = 10.dp)) {
        val strokeWidth = 1.dp.toPx()
        val cornerRadius = 1.dp.toPx()
        
        // 电池外框
        drawRoundRect(
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
            size = androidx.compose.ui.geometry.Size(size.width - 3.dp.toPx(), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
        
        // 电池头
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(size.width - 2.dp.toPx(), size.height * 0.3f),
            size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height * 0.4f)
        )
        
        // 电量填充
        val fillWidth = (size.width - 5.dp.toPx()) * (level / 100f)
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(fillWidth, size.height - 4.dp.toPx())
        )
    }
}


