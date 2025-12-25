package takagi.ru.paysage.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.ui.book.read.entities.TextPage
import takagi.ru.paysage.ui.book.read.provider.TextChapterLayouter
import takagi.ru.paysage.ui.components.reader.QuickSettingsPanel
import takagi.ru.paysage.ui.components.reader.ReadingSettingsDialog
import takagi.ru.paysage.ui.components.reader.TextPageContent
import takagi.ru.paysage.viewmodel.TextReaderViewModel
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "TextReaderScreen"

/**
 * 文本阅读器界面 - 完全重构版
 * 
 * 核心架构：
 * 1. 使用 HorizontalPager 实现左右滑动翻页
 * 2. 使用 TextChapterLayouter + TextMeasurer 进行文本分页
 * 3. 保持与 ReaderScreen 一致的沉浸式 UI
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TextReaderScreen(
    bookId: Long,
    filePath: String,
    onBackClick: () -> Unit,
    viewModel: TextReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chapterTitles by viewModel.chapterTitles.collectAsState()
    val allChapterContents = viewModel.allChapterContents
    
    // UI 状态
    var isToolbarVisible by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    var showFullSettings by remember { mutableStateOf(false) }
    
    // 视图尺寸
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }
    
    // 分页相关
    val textMeasurer = rememberTextMeasurer()
    val layouter = remember(viewWidth, viewHeight, uiState.config) {
        if (viewWidth > 0 && viewHeight > 0) {
            TextChapterLayouter(
                visibleWidth = viewWidth,
                visibleHeight = viewHeight,
                config = uiState.config
            )
        } else null
    }
    val density = LocalDensity.current
    
    // 所有页面（跨章节展平）
    var allPages by remember { mutableStateOf<List<TextPage>>(emptyList()) }
    var isLayouting by remember { mutableStateOf(false) }
    
    // 待更新的页面（在滑动期间缓冲，避免中断动画）
    var pendingPages by remember { mutableStateOf<List<TextPage>?>(null) }
    var pendingTargetPage by remember { mutableIntStateOf(-1) }
    
    // 进度恢复标记：防止初始化时覆盖进度
    var hasRestoredProgress by remember { mutableStateOf(false) }
    var isUserNavigating by remember { mutableStateOf(false) }
    
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    val scope = rememberCoroutineScope()
    
    // Pager 状态
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { maxOf(1, allPages.size) }
    )
    
    // 当前页面信息
    val currentPage = allPages.getOrNull(pagerState.currentPage)
    val currentChapterIndex = currentPage?.chapterIndex ?: 0
    val currentPageInChapter = currentPage?.index ?: 0
    val totalPagesInCurrentChapter = allPages.count { it.chapterIndex == currentChapterIndex }
    
    // 监听返回键
    BackHandler(enabled = true) {
        when {
            showChapterList -> showChapterList = false
            showQuickSettings -> showQuickSettings = false
            showFullSettings -> showFullSettings = false
            isToolbarVisible -> isToolbarVisible = false
            else -> onBackClick()
        }
    }
    
    // 控制系统栏 (沉浸式)
    LaunchedEffect(isToolbarVisible) {
        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, view)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isToolbarVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    // 屏幕常亮
    LaunchedEffect(uiState.config.keepScreenOn) {
        window?.let { win ->
            if (uiState.config.keepScreenOn) {
                win.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    
    // 打开书籍
    LaunchedEffect(bookId, filePath) {
        viewModel.openBook(bookId, filePath)
    }
    
    // 当章节内容或配置变化时重新分页
    // 使用章节数量作为稳定依赖，避免 keys.toList() 每次创建新对象导致无限循环
    val loadedChapterCount = allChapterContents.size
    LaunchedEffect(
        loadedChapterCount,
        uiState.config,
        viewWidth,
        viewHeight
    ) {
        if (viewWidth <= 0 || viewHeight <= 0) return@LaunchedEffect
        if (allChapterContents.isEmpty()) return@LaunchedEffect
        if (layouter == null) return@LaunchedEffect
        
        // 记住当前阅读位置，用于分页后恢复
        val currentPageData = allPages.getOrNull(pagerState.currentPage)
        val currentChapterIdx = currentPageData?.chapterIndex ?: uiState.currentChapter
        val currentPageInChapter = currentPageData?.index ?: 0
        
        isLayouting = true
        Log.d(TAG, "开始分页: ${allChapterContents.size} 章节, 视图大小: ${viewWidth}x${viewHeight}")
        val paddingHorizontal = with(density) { 
            (uiState.config.paddingLeft + uiState.config.paddingRight).dp.toPx().toInt() 
        }
        val paddingVertical = with(density) { 
            (uiState.config.paddingTop + uiState.config.paddingBottom).dp.toPx().toInt() 
        }
        val contentWidth = viewWidth - paddingHorizontal
        val contentHeight = viewHeight - paddingVertical
        
        // 复制章节内容到本地变量，避免在后台线程访问可变状态
        val chaptersCopy = allChapterContents.toMap()
        
        // 在后台线程执行分页 (CPU 密集型)
        val pages = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val result = mutableListOf<TextPage>()
            val sortedChapterIndices = chaptersCopy.keys.sorted()
            
            for (chapterIndex in sortedChapterIndices) {
                val chapter = chaptersCopy[chapterIndex] ?: continue
                
                // 让出线程，避免阻塞
                kotlinx.coroutines.yield()
                
                val chapterPages = layouter.layoutChapter(
                    htmlContent = chapter.content,  // 使用 HTML 内容以支持图片
                    chapterIndex = chapterIndex,
                    chapterTitle = chapter.title
                )
                result.addAll(chapterPages.pages)
            }
            result
        }
        
        // 在新页面列表中查找对应位置
        val targetPageIndex = if (hasRestoredProgress && currentPageData != null) {
            // 用户已经在阅读中，尝试恢复到相同的章节和页面
            pages.indexOfFirst { 
                it.chapterIndex == currentChapterIdx && it.index == currentPageInChapter 
            }.takeIf { it >= 0 }
                ?: pages.indexOfFirst { it.chapterIndex == currentChapterIdx } // 同章节第一页
                ?: pagerState.currentPage.coerceIn(0, maxOf(0, pages.size - 1))
        } else if (!hasRestoredProgress && pages.isNotEmpty()) {
            // 首次加载，跳转到保存的章节
            val savedChapter = uiState.currentChapter
            pages.indexOfFirst { it.chapterIndex == savedChapter }.takeIf { it > 0 } ?: 0
        } else {
            pagerState.currentPage.coerceIn(0, maxOf(0, pages.size - 1))
        }
        
        // 关键修复：如果用户正在滑动，延迟更新以避免中断动画
        if (pagerState.isScrollInProgress) {
            Log.d(TAG, "用户正在滑动，缓存页面更新: ${pages.size} 页")
            pendingPages = pages
            pendingTargetPage = targetPageIndex
            isLayouting = false
        } else {
            allPages = pages
            isLayouting = false
            
            // 滚动到目标位置（仅当位置变化时）
            if (pages.isNotEmpty() && targetPageIndex != pagerState.currentPage) {
                scope.launch {
                    pagerState.scrollToPage(targetPageIndex)
                }
                Log.d(TAG, "恢复阅读位置: 章节 $currentChapterIdx, 页码 $targetPageIndex")
            }
        }
        
        if (!hasRestoredProgress) {
            hasRestoredProgress = true
        }
        
        Log.d(TAG, "分页完成: 共 ${pages.size} 页")
    }
    
    // 当滑动结束时，应用缓存的页面更新
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pendingPages != null) {
            Log.d(TAG, "滑动结束，应用缓存的页面更新: ${pendingPages!!.size} 页")
            allPages = pendingPages!!
            val target = pendingTargetPage
            pendingPages = null
            pendingTargetPage = -1
            
            // 如果需要跳转到特定页面
            if (target >= 0 && target != pagerState.currentPage && target < allPages.size) {
                pagerState.scrollToPage(target)
            }
        }
    }
    
    // 预加载相邻章节（更积极的预加载策略）
    LaunchedEffect(currentChapterIndex) {
        val total = uiState.totalChapters
        // 预加载当前章节的前后各 2 章
        for (offset in -2..2) {
            val targetChapter = currentChapterIndex + offset
            if (targetChapter in 0 until total) {
                viewModel.ensureChapterLoaded(targetChapter)
            }
        }
    }
    
    // 当接近章节末尾时，提前预加载下一章
    LaunchedEffect(pagerState.currentPage, allPages.size) {
        if (allPages.isEmpty()) return@LaunchedEffect
        val currentPageData = allPages.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        
        // 获取当前章节的页面列表
        val currentChapterPagesList = allPages.filter { it.chapterIndex == currentPageData.chapterIndex }
        val pageIndexInChapter = currentChapterPagesList.indexOf(currentPageData)
        val totalPagesInChapter = currentChapterPagesList.size
        
        // 如果在当前章节的最后 2 页，确保下一章已加载
        if (pageIndexInChapter >= totalPagesInChapter - 2) {
            val nextChapter = currentPageData.chapterIndex + 1
            if (nextChapter < uiState.totalChapters) {
                viewModel.ensureChapterLoaded(nextChapter)
            }
        }
        
        // 如果在当前章节的前 2 页，确保上一章已加载
        if (pageIndexInChapter <= 1) {
            val prevChapter = currentPageData.chapterIndex - 1
            if (prevChapter >= 0) {
                viewModel.ensureChapterLoaded(prevChapter)
            }
        }
    }

    // 监听页码变化并更新 ViewModel（UI 同步）
    // 只有用户主动导航后才保存进度
    LaunchedEffect(pagerState.currentPage) {
        val page = allPages.getOrNull(pagerState.currentPage)
        if (page != null && hasRestoredProgress) {
            // 已经恢复过进度，现在可以保存新的进度
            viewModel.updateRealtimeProgress(page.chapterIndex, page.index + 1, allPages.size)
        }
    }
    
    // 主界面
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(uiState.config.bgColor))
    ) {
        // 测量视图尺寸
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = with(density) { maxWidth.toPx().toInt() }
            val heightPx = with(density) { maxHeight.toPx().toInt() }
            
            LaunchedEffect(widthPx, heightPx) {
                if (viewWidth != widthPx || viewHeight != heightPx) {
                    viewWidth = widthPx
                    viewHeight = heightPx
                }
            }
            
            // [Layer 1] 内容层 - HorizontalPager
            if (uiState.isLoading || isLayouting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                TextReaderErrorView(
                    error = uiState.error!!,
                    onRetry = { viewModel.openBook(bookId, filePath) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (allPages.isEmpty()) {
                Text(
                    text = "没有可显示的内容",
                    color = Color(uiState.config.textColor),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val page = allPages.getOrNull(pageIndex)
                    if (page != null) {
                        // 将点击手势放入页面内部，避免与滑动手势冲突
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val width = size.width
                                        val x = offset.x
                                        when {
                                            x < width * 0.3f -> {
                                                // 左侧点击 - 上一页
                                                scope.launch {
                                                    if (pagerState.currentPage > 0) {
                                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                    }
                                                }
                                            }
                                            x > width * 0.7f -> {
                                                // 右侧点击 - 下一页
                                                scope.launch {
                                                    if (pagerState.currentPage < allPages.size - 1) {
                                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                    }
                                                }
                                            }
                                            else -> {
                                                // 中间点击 - 切换工具栏
                                                isToolbarVisible = !isToolbarVisible
                                                if (showQuickSettings) showQuickSettings = false
                                            }
                                        }
                                    }
                                }
                        ) {
                            TextPageContent(
                                page = page,
                                config = uiState.config
                            )
                        }
                    }
                }
            }
        }
        
        // [Layer 2] 系统信息层 (沉浸模式下显示)
        if (!uiState.isLoading && uiState.error == null && !isToolbarVisible) {
            TextReaderSystemInfo(
                title = currentPage?.chapterTitle ?: uiState.bookTitle,
                currentPage = pagerState.currentPage + 1,
                totalPages = allPages.size,
                currentChapter = currentChapterIndex + 1,
                totalChapters = uiState.totalChapters,
                modifier = Modifier
            )
        }
        
        // [Layer 3] 顶部栏
        AnimatedVisibility(
            visible = isToolbarVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
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
                            Text(
                                text = currentPage?.chapterTitle ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isToolbarVisible = false
                            onBackClick()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(Icons.Default.Menu, "目录")
                        }
                        IconButton(onClick = { 
                            showFullSettings = true
                            isToolbarVisible = false
                        }) {
                            Icon(Icons.Default.Settings, "设置")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        
        // [Layer 3] 底部栏
        AnimatedVisibility(
            visible = isToolbarVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column {
                // 快速设置面板
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
                            config = uiState.config,
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

                // 底部浮动控制条
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
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // 页码进度条（章节进度）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 计算当前章节的页面进度
                            val currentChapterPages = allPages.filter { it.chapterIndex == currentChapterIndex }
                            val currentPageInChapter = if (currentChapterPages.isNotEmpty()) {
                                val currentPageIndex = allPages.getOrNull(pagerState.currentPage)?.index ?: 0
                                currentPageIndex + 1
                            } else {
                                1
                            }
                            val totalPagesInChapter = currentChapterPages.size.coerceAtLeast(1)
                            
                            Text(
                                text = "$currentPageInChapter",
                                style = MaterialTheme.typography.labelMedium
                            )
                            
                            // 使用本地状态预览 Slider 位置，避免拖动时频繁跳转
                            var sliderPosition by remember(currentPageInChapter) { 
                                mutableFloatStateOf((currentPageInChapter - 1).toFloat()) 
                            }
                            
                            Slider(
                                value = sliderPosition,
                                onValueChange = { newValue ->
                                    // 仅更新本地预览状态，不执行页面跳转
                                    sliderPosition = newValue
                                },
                                onValueChangeFinished = {
                                    // 用户释放滑块时才执行页面跳转
                                    scope.launch {
                                        val firstPageOfChapter = allPages.indexOfFirst { it.chapterIndex == currentChapterIndex }
                                        if (firstPageOfChapter >= 0) {
                                            val targetPage = firstPageOfChapter + sliderPosition.toInt()
                                            if (targetPage < allPages.size && allPages[targetPage].chapterIndex == currentChapterIndex) {
                                                pagerState.scrollToPage(targetPage)
                                            }
                                        }
                                    }
                                },
                                valueRange = 0f..maxOf(0f, (totalPagesInChapter - 1).toFloat()),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )
                            
                            Text(
                                text = "$totalPagesInChapter",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        
                        // 控制按钮行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 上一章
                            TextButton(
                                onClick = { 
                                    // 找到上一章的第一页
                                    val prevChapterPages = allPages.filter { it.chapterIndex == currentChapterIndex - 1 }
                                    if (prevChapterPages.isNotEmpty()) {
                                        val targetIndex = allPages.indexOf(prevChapterPages.first())
                                        scope.launch { pagerState.animateScrollToPage(targetIndex) }
                                    }
                                },
                                enabled = currentChapterIndex > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                                Text("上一章")
                            }
                            
                            // 快速设置
                            IconButton(onClick = { showQuickSettings = !showQuickSettings }) {
                                Icon(
                                    if (showQuickSettings) Icons.Default.ExpandMore else Icons.Default.Tune,
                                    "快速设置"
                                )
                            }
                            
                            // 下一章
                            TextButton(
                                onClick = {
                                    // 找到下一章的第一页
                                    val nextChapterPages = allPages.filter { it.chapterIndex == currentChapterIndex + 1 }
                                    if (nextChapterPages.isNotEmpty()) {
                                        val targetIndex = allPages.indexOf(nextChapterPages.first())
                                        scope.launch { pagerState.animateScrollToPage(targetIndex) }
                                    }
                                },
                                enabled = currentChapterIndex < uiState.totalChapters - 1
                            ) {
                                Text("下一章")
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 章节列表对话框
    if (showChapterList) {
        ChapterListDialog(
            chapterTitles = chapterTitles,
            currentChapter = currentChapterIndex,
            onChapterSelected = { chapterIndex ->
                // 找到该章节的第一页
                val chapterPages = allPages.filter { it.chapterIndex == chapterIndex }
                if (chapterPages.isNotEmpty()) {
                    val targetIndex = allPages.indexOf(chapterPages.first())
                    scope.launch { pagerState.animateScrollToPage(targetIndex) }
                } else {
                    // 如果章节未加载，先加载
                    viewModel.ensureChapterLoaded(chapterIndex)
                }
                showChapterList = false
            },
            onDismiss = { showChapterList = false }
        )
    }
    
    // 全屏设置对话框
    if (showFullSettings) {
        ReadingSettingsDialog(
            config = uiState.config,
            onConfigChange = { viewModel.updateConfig(it) },
            onDismiss = { showFullSettings = false }
        )
    }
}

/**
 * 系统信息覆盖层 (沉浸模式下显示)
 */
@Composable
private fun TextReaderSystemInfo(
    title: String,
    currentPage: Int,
    totalPages: Int,
    currentChapter: Int,
    totalChapters: Int,
    modifier: Modifier = Modifier
) {
    val currentTime = remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(30000)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 顶部 - 章节标题
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp)
        )
        
        // 底部左 - 章节进度
        Text(
            text = "第 $currentChapter/$totalChapters 章",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 4.dp)
        )
        
        // 底部中 - 页码
        Text(
            text = "$currentPage / $totalPages",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        )
        
        // 底部右 - 时间
        Text(
            text = currentTime.value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 4.dp)
        )
    }
}

/**
 * 章节列表对话框
 */
@Composable
private fun ChapterListDialog(
    chapterTitles: List<String>,
    currentChapter: Int,
    onChapterSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目录") },
        text = {
            LazyColumn {
                itemsIndexed(chapterTitles) { index, title ->
                    val isSelected = index == currentChapter
                    Surface(
                        onClick = { onChapterSelected(index) },
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            Color.Transparent,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title.ifBlank { "第 ${index + 1} 章" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 错误视图
 */
@Composable
private fun TextReaderErrorView(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
