package takagi.ru.paysage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import takagi.ru.paysage.viewmodel.LibraryViewModel
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.FilterMode
import takagi.ru.paysage.data.model.getReadingStatus
import takagi.ru.paysage.ui.components.CategoriesListView
import takagi.ru.paysage.ui.components.CategoryFilterBar
import takagi.ru.paysage.ui.components.EmptyFilterView
import takagi.ru.paysage.ui.components.ExpressiveCard
import takagi.ru.paysage.ui.components.ExpressiveFAB
import takagi.ru.paysage.ui.components.ExpressiveIconButton
import takagi.ru.paysage.ui.components.LastReadingFAB
import takagi.ru.paysage.ui.theme.ExpressiveDimensions
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.data.model.DisplayMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * 书库主界面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onTextBookClick: ((Long, String) -> Unit)? = null,  // bookId, filePath - 用于EPUB/TXT
    onSettingsClick: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    filter: String? = null,
    category: String? = null,
    onNavigateToCategory: ((String) -> Unit)? = null,
    viewModel: LibraryViewModel = viewModel(),
    settingsViewModel: takagi.ru.paysage.viewmodel.SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // 解析路由参数
    LaunchedEffect(filter, category) {
        try {
            viewModel.parseFilterFromRoute(filter, category)
        } catch (e: Exception) {
            // 解析失败时回退到默认状态
            viewModel.setFilterMode(FilterMode.ALL)
        }
    }
    
    val filterMode by viewModel.filterMode.collectAsState()
    val displayBooks by viewModel.displayBooks.collectAsState()
    val categoriesWithCount by viewModel.categoriesWithCount.collectAsState()
    // val books by viewModel.allBooks.collectAsState() // 已优化移除：未使用的订阅
    val uiState by viewModel.uiState.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val lastReadBook by viewModel.lastReadBook.collectAsState()
    val bookDetailUiState by viewModel.bookDetailUiState.collectAsState()
    
    // 分组数据
    val currentGroupedBooks by viewModel.currentGroupedBooks.collectAsState()
    val isGroupedMode by viewModel.isGroupedMode.collectAsState()
    
    // 权限请求
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    )
    
    var showSearchBar by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 过滤栏状态
    var selectedFilter by remember { mutableStateOf(takagi.ru.paysage.ui.components.BookFilterStatus.ALL) }
    var isFilterSearchMode by remember { mutableStateOf(false) }
    var filterSearchQuery by remember { mutableStateOf("") }
    
    // 分类系统状态
    var categoryType by remember { mutableStateOf(CategoryType.MANGA) }
    var displayMode by remember { mutableStateOf(DisplayMode.LOCAL) }
    
    // 智能书籍点击处理 - 根据格式选择适当的阅读器
    val handleBookClick: (Book) -> Unit = { book ->
        if (book.fileFormat.isTextFormat() && onTextBookClick != null) {
            // EPUB/TXT 使用文本阅读器
            onTextBookClick(book.id, book.filePath)
        } else {
            // 其他格式使用图像阅读器
            onBookClick(book.id)
        }
    }

    
    // 同步结果 Snackbar
    LaunchedEffect(uiState.syncResult) {
        uiState.syncResult?.let { result ->
            val message = "同步完成：新增 ${result.newBooks} 本，更新 ${result.updatedBooks} 本"
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "确定",
                duration = SnackbarDuration.Short
            )
            viewModel.clearSyncResult()
        }
    }
    
    // 同步错误 Snackbar
    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let { error ->
            snackbarHostState.showSnackbar(
                message = "同步失败：$error",
                actionLabel = "确定",
                duration = SnackbarDuration.Long
            )
            viewModel.clearSyncResult()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            LastReadingFAB(
                lastReadBook = lastReadBook,
                onClick = {
                    lastReadBook?.let { book ->
                        handleBookClick(book)
                    }
                }
            )
        },
        topBar = {
            if (showSearchBar) {
                SearchAppBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.search(it) },
                    onClose = {
                        showSearchBar = false
                        viewModel.clearSearch()
                    }
                )
            } else {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Paysage")
                            statistics?.let {
                                Text(
                                    "共 ${it.totalBooks} 本",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        onOpenDrawer?.let { openDrawer ->
                            ExpressiveIconButton(onClick = openDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "打开菜单")
                            }
                        }
                    },
                    actions = {
                        ExpressiveIconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        ExpressiveIconButton(onClick = { showSyncDialog = true }) {
                            Icon(Icons.Default.Sync, contentDescription = "同步")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // 根据筛选模式决定显示的书籍列表
        val booksToDisplay = if (searchQuery.isNotBlank()) {
            searchResults
        } else {
            displayBooks
        }
        
        // 根据选中的过滤器过滤书籍（仅在非分类列表模式下）
        val filteredBooks = remember(booksToDisplay, selectedFilter, filterMode) {
            if (filterMode == FilterMode.CATEGORIES) {
                emptyList() // 分类列表模式不显示书籍
            } else {
                when (selectedFilter) {
                    takagi.ru.paysage.ui.components.BookFilterStatus.ALL -> booksToDisplay
                    takagi.ru.paysage.ui.components.BookFilterStatus.LATEST -> booksToDisplay.filter { 
                        it.getReadingStatus() == takagi.ru.paysage.data.model.BookReadingStatus.LATEST 
                    }
                    takagi.ru.paysage.ui.components.BookFilterStatus.READING -> booksToDisplay.filter { 
                        it.getReadingStatus() == takagi.ru.paysage.data.model.BookReadingStatus.READING 
                    }
                    takagi.ru.paysage.ui.components.BookFilterStatus.FINISHED -> booksToDisplay.filter { 
                        it.getReadingStatus() == takagi.ru.paysage.data.model.BookReadingStatus.FINISHED 
                    }
                    takagi.ru.paysage.ui.components.BookFilterStatus.UNREAD -> booksToDisplay.filter { 
                        it.getReadingStatus() == takagi.ru.paysage.data.model.BookReadingStatus.UNREAD 
                    }
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 过滤栏（仅在非分类列表模式下显示）
            if (filterMode != FilterMode.CATEGORIES) {
                takagi.ru.paysage.ui.components.LibraryFilterBar(
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    isSearchMode = isFilterSearchMode,
                    onSearchModeChange = { isFilterSearchMode = it },
                    searchQuery = filterSearchQuery,
                    onSearchQueryChange = { filterSearchQuery = it },
                    currentLayout = settings.libraryLayout,
                    onLayoutChange = {
                        // 切换布局
                        val nextLayout = when (settings.libraryLayout) {
                            takagi.ru.paysage.data.model.LibraryLayout.LIST -> 
                                takagi.ru.paysage.data.model.LibraryLayout.COMPACT_GRID
                            takagi.ru.paysage.data.model.LibraryLayout.COMPACT_GRID -> 
                                takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY
                            takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY -> 
                                takagi.ru.paysage.data.model.LibraryLayout.LIST
                        }
                        settingsViewModel.updateLibraryLayout(nextLayout)
                    },
                    onSortClick = {
                        // TODO: 实现排序功能
                    }
                )
            }
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (filterMode) {
                    FilterMode.CATEGORIES -> {
                        // 显示分类列表
                        if (categoriesWithCount.isEmpty()) {
                            EmptyFilterView(
                                filterMode = FilterMode.CATEGORIES,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            CategoriesListView(
                                categories = categoriesWithCount,
                                onCategoryClick = { categoryName ->
                                    try {
                                        onNavigateToCategory?.invoke(categoryName)
                                    } catch (e: Exception) {
                                        // 导航失败时显示错误消息
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "无法打开分类: ${e.message}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    // 分组模式：按作者/系列/年度/文件夹显示
                    FilterMode.AUTHOR, FilterMode.SERIES, FilterMode.YEAR, FilterMode.SOURCE_FOLDER -> {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (currentGroupedBooks.isEmpty()) {
                            EmptyFilterView(
                                filterMode = filterMode,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            takagi.ru.paysage.ui.components.GroupedBookListView(
                                groups = currentGroupedBooks,
                                filterMode = filterMode,
                                onBookClick = handleBookClick,
                                onBookLongClick = { book -> viewModel.showBookDetail(book) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        // 显示书籍列表
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (filteredBooks.isEmpty()) {
                            EmptyFilterView(
                                filterMode = filterMode,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            // 根据布局设置显示不同的视图
                            when (settings.libraryLayout) {
                                takagi.ru.paysage.data.model.LibraryLayout.LIST -> {
                                    BookListView(
                                        books = filteredBooks,
                                        showProgress = settings.showProgress,
                                        onBookClick = handleBookClick,
                                        onBookLongClick = { book -> viewModel.showBookDetail(book) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                takagi.ru.paysage.data.model.LibraryLayout.COMPACT_GRID -> {
                                    BookCompactGrid(
                                        books = filteredBooks,
                                        gridColumns = settings.gridColumns,
                                        showProgress = settings.showProgress,
                                        onBookClick = handleBookClick,
                                        onBookLongClick = { book -> viewModel.showBookDetail(book) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY -> {
                                    BookGrid(
                                        books = filteredBooks,
                                        gridColumns = settings.gridColumns,
                                        showProgress = settings.showProgress,
                                        onBookClick = handleBookClick,
                                        onBookLongClick = { book -> viewModel.showBookDetail(book) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 扫描结果提示
                uiState.scanResult?.let { result ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearScanResult() }) {
                            Text("确定")
                        }
                    }
                ) {
                        Text("扫描完成: 新增 ${result.newBooks} 本，更新 ${result.updatedBooks} 本")
                    }
                }
            }
        }
    }
    
    // 同步对话框
    if (showSyncDialog) {
        takagi.ru.paysage.ui.components.LibrarySyncDialog(
            onDismiss = { showSyncDialog = false },
            onSync = { type, options ->
                viewModel.performSync(type, options)
                showSyncDialog = false
            }
        )
    }
    
    // 同步进度指示器
    if (uiState.isSyncing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    
    // 书籍详情底部弹窗
    bookDetailUiState.selectedBook?.let { selectedBook ->
        if (bookDetailUiState.isVisible) {
            takagi.ru.paysage.ui.components.BookDetailBottomSheet(
                book = selectedBook,
            onDismiss = { viewModel.hideBookDetail() },
            onOpenBook = { bookId ->
                onBookClick(bookId)
            },
            onToggleFavorite = { bookId ->
                viewModel.toggleFavoriteFromDetail(bookId)
            },
            onEditBook = { bookId ->
                // TODO: 实现编辑功能
            },
            onShareBook = { book ->
                try {
                    val shareIntent = viewModel.shareBook(book)
                    context.startActivity(shareIntent)
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(takagi.ru.paysage.R.string.book_detail_share_failed)
                        )
                    }
                }
            },
            onDeleteBook = { bookId ->
                viewModel.deleteBookFromDetail(bookId)
            },
            onUpdateTags = { bookId, tags ->
                viewModel.updateBookTags(bookId, tags)
            },
            onUpdateSortPreference = { bookId, preference ->
                viewModel.updateSortPreference(bookId, preference)
            }
        )
        }
    }
}

@Composable
fun BookGrid(
    books: List<Book>,
    gridColumns: Int = 3,
    showProgress: Boolean = true,
    onBookClick: (Book) -> Unit,
    onBookLongClick: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        contentPadding = PaddingValues(
            start = ExpressiveDimensions.paddingMedium,
            end = ExpressiveDimensions.paddingMedium,
            top = ExpressiveDimensions.paddingMedium,
            bottom = 80.dp // 为 FAB 留出空间
        ),
        horizontalArrangement = Arrangement.spacedBy(ExpressiveDimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(ExpressiveDimensions.paddingMedium),
        modifier = modifier
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                showProgress = showProgress,
                onClick = { onBookClick(book) },
                onLongClick = if (onBookLongClick != null) { { onBookLongClick(book) } } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(
    book: Book,
    showProgress: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 纯封面 - 只显示封面和进度标签
    if (onLongClick != null) {
        // 有长按功能时，使用 Surface 代替 ExpressiveCard，手动处理手势
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onClick() }
                    )
                },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            BookCardContent(book = book, showProgress = showProgress)
        }
    } else {
        // 没有长按功能时，使用 ExpressiveCard
        ExpressiveCard(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            BookCardContent(book = book, showProgress = showProgress)
        }
    }
}

@Composable
private fun BookCardContent(
    book: Book,
    showProgress: Boolean
) {
    // 检查封面文件是否存在
    val coverExists = remember(book.coverPath) {
        book.coverPath?.let { File(it).exists() } ?: false
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
            // 封面图片
            if (coverExists && book.coverPath != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.coverPath)
                        .crossfade(200)
                        .memoryCacheKey("cover_${book.id}")
                        .diskCacheKey("cover_${book.id}")
                        .size(300, 420)
                        .build(),
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )
            } else {
                // 无封面或加载失败时显示默认图标
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 状态标签（左上角）
            val readingStatus = book.getReadingStatus()
            
            // 阅读中状态 - 使用连接式标签显示进度
            if (readingStatus == takagi.ru.paysage.data.model.BookReadingStatus.READING) {
                ConnectedReadingStatusBadge(
                    statusText = stringResource(takagi.ru.paysage.R.string.status_reading),
                    progressPercentage = (book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) * 100).toInt(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            } else {
                // 其他状态 - 只显示状态文字
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = when (readingStatus) {
                        takagi.ru.paysage.data.model.BookReadingStatus.FINISHED -> 
                            androidx.compose.ui.graphics.Color(0xFF2196F3) // 蓝色
                        takagi.ru.paysage.data.model.BookReadingStatus.LATEST -> 
                            androidx.compose.ui.graphics.Color(0xFFFF5722) // 橙红色
                        takagi.ru.paysage.data.model.BookReadingStatus.UNREAD -> 
                            androidx.compose.ui.graphics.Color(0xFFC62828) // 深红色
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (readingStatus) {
                            takagi.ru.paysage.data.model.BookReadingStatus.FINISHED -> 
                                stringResource(takagi.ru.paysage.R.string.status_finished)
                            takagi.ru.paysage.data.model.BookReadingStatus.LATEST -> 
                                stringResource(takagi.ru.paysage.R.string.status_latest)
                            takagi.ru.paysage.data.model.BookReadingStatus.UNREAD -> 
                                stringResource(takagi.ru.paysage.R.string.status_unread)
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
}

@Composable
fun EmptyLibraryView(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(takagi.ru.paysage.R.string.library_empty_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(takagi.ru.paysage.R.string.library_empty_message),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        takagi.ru.paysage.ui.components.ExpressiveButton(onClick = onScanClick) {
            Text(stringResource(takagi.ru.paysage.R.string.library_start_scan))
        }
    }
}

@Composable
fun EmptyFilteredView(
    selectedFilter: takagi.ru.paysage.ui.components.BookFilterStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无符合条件的书籍",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (selectedFilter) {
                takagi.ru.paysage.ui.components.BookFilterStatus.LATEST -> "当前没有最新添加的书籍"
                takagi.ru.paysage.ui.components.BookFilterStatus.READING -> "当前没有正在阅读的书籍"
                takagi.ru.paysage.ui.components.BookFilterStatus.FINISHED -> "当前没有已读完的书籍"
                takagi.ru.paysage.ui.components.BookFilterStatus.UNREAD -> "当前没有未读的书籍"
                else -> "请尝试其他筛选条件"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("搜索书籍...") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        },
        navigationIcon = {
            ExpressiveIconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        }
    )
}


/**
 * 列表视图 - 详细信息
 */
@Composable
fun BookListView(
    books: List<Book>,
    showProgress: Boolean = true,
    onBookClick: (Book) -> Unit,
    onBookLongClick: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(
            start = ExpressiveDimensions.paddingMedium,
            end = ExpressiveDimensions.paddingMedium,
            top = ExpressiveDimensions.paddingMedium,
            bottom = 80.dp // 为 FAB 留出空间
        ),
        verticalArrangement = Arrangement.spacedBy(ExpressiveDimensions.paddingMedium),
        modifier = modifier
    ) {
        items(books.size, key = { books[it].id }) { index ->
            BookListItem(
                book = books[index],
                showProgress = showProgress,
                onClick = { onBookClick(books[index]) },
                onLongClick = if (onBookLongClick != null) { { onBookLongClick(books[index]) } } else null
            )
        }
    }
}

/**
 * 列表项 - 详细信息卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListItem(
    book: Book,
    showProgress: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onLongClick != null) {
        // 有长按功能时，使用 Surface 代替 ExpressiveCard
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onClick() }
                    )
                },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            BookListItemContent(book = book, showProgress = showProgress)
        }
    } else {
        // 没有长按功能时，使用 ExpressiveCard
        ExpressiveCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth()
        ) {
            BookListItemContent(book = book, showProgress = showProgress)
        }
    }
}

@Composable
private fun BookListItemContent(
    book: Book,
    showProgress: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 检查封面文件是否存在
            val coverExists = remember(book.coverPath) {
                book.coverPath?.let { File(it).exists() } ?: false
            }
            
            // 封面区域
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverExists && book.coverPath != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.coverPath)
                            .crossfade(200)
                            .memoryCacheKey("cover_${book.id}")
                            .diskCacheKey("cover_${book.id}")
                            .size(300, 450)
                            .build(),
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        success = {
                            SubcomposeAsyncImageContent()
                        }
                    )
                } else {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 信息区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 上部：标题和作者
                Column {
                    // 标题 - 放大字体
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // 作者（如果有）- 放大字体
                    book.author?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                
                // 弹性空间 - 把下面的内容推到底部
                Spacer(modifier = Modifier.weight(1f))
                
                // 下部：状态、格式和进度 - 紧贴底部
                Column {
                    // 状态和文件格式行
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 阅读状态标签
                        val readingStatus = book.getReadingStatus()
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (readingStatus) {
                                takagi.ru.paysage.data.model.BookReadingStatus.READING -> 
                                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                takagi.ru.paysage.data.model.BookReadingStatus.FINISHED -> 
                                    androidx.compose.ui.graphics.Color(0xFF2196F3)
                                takagi.ru.paysage.data.model.BookReadingStatus.LATEST -> 
                                    androidx.compose.ui.graphics.Color(0xFFFF5722)
                                takagi.ru.paysage.data.model.BookReadingStatus.UNREAD -> 
                                    androidx.compose.ui.graphics.Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (readingStatus) {
                                    takagi.ru.paysage.data.model.BookReadingStatus.READING -> 
                                        stringResource(takagi.ru.paysage.R.string.status_reading)
                                    takagi.ru.paysage.data.model.BookReadingStatus.FINISHED -> 
                                        stringResource(takagi.ru.paysage.R.string.status_finished)
                                    takagi.ru.paysage.data.model.BookReadingStatus.LATEST -> 
                                        stringResource(takagi.ru.paysage.R.string.status_latest)
                                    takagi.ru.paysage.data.model.BookReadingStatus.UNREAD -> 
                                        stringResource(takagi.ru.paysage.R.string.status_unread)
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // 进度百分比（如果正在阅读）
                        if (readingStatus == takagi.ru.paysage.data.model.BookReadingStatus.READING && book.totalPages > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            ) {
                                Text(
                                    text = "${(book.currentPage.toFloat() / book.totalPages * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        // 文件格式标签
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = book.fileFormat.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // 阅读进度条和页码
                    if (showProgress) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 页码文本
                                Text(
                                    text = "${book.currentPage}/${book.totalPages}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // 进度条 - M3e风格，根据阅读状态显示不同颜色
                                val readingStatus = book.getReadingStatus()
                                val progressColor = when (readingStatus) {
                                    takagi.ru.paysage.data.model.BookReadingStatus.READING -> 
                                        androidx.compose.ui.graphics.Color(0xFF4CAF50) // 绿色
                                    takagi.ru.paysage.data.model.BookReadingStatus.FINISHED -> 
                                        androidx.compose.ui.graphics.Color(0xFF2196F3) // 蓝色
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                ) {
                                    // 背景轨道（未读部分 - 浅灰色）
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.small)
                                            .background(androidx.compose.ui.graphics.Color(0xFFE0E0E0))
                                    )
                                    
                                    // 进度部分（已读部分 - 彩色）
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1))
                                            .clip(MaterialTheme.shapes.small)
                                            .background(progressColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}

/**
 * 紧凑网格 - 大卡片
 */
@Composable
fun BookCompactGrid(
    books: List<Book>,
    gridColumns: Int = 2,
    showProgress: Boolean = true,
    onBookClick: (Book) -> Unit,
    onBookLongClick: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns.coerceIn(2, 3)), // 紧凑网格最多3列
        contentPadding = PaddingValues(
            start = ExpressiveDimensions.paddingMedium,
            end = ExpressiveDimensions.paddingMedium,
            top = ExpressiveDimensions.paddingMedium,
            bottom = 80.dp // 为 FAB 留出空间
        ),
        horizontalArrangement = Arrangement.spacedBy(ExpressiveDimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(ExpressiveDimensions.paddingMedium),
        modifier = modifier
    ) {
        items(books, key = { it.id }) { book ->
            BookCompactCard(
                book = book,
                showProgress = showProgress,
                onClick = { onBookClick(book) },
                onLongClick = if (onBookLongClick != null) { { onBookLongClick(book) } } else null
            )
        }
    }
}

/**
 * 紧凑卡片 - 叠加式布局，标题和进度条叠加在封面上
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCompactCard(
    book: Book,
    showProgress: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onLongClick != null) {
        // 有长按功能时，使用 Surface 代替 ExpressiveCard
        Surface(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onClick() }
                    )
                },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            BookCompactCardContent(book = book, showProgress = showProgress)
        }
    } else {
        // 没有长按功能时，使用 ExpressiveCard
        ExpressiveCard(
            onClick = onClick,
            modifier = modifier
        ) {
            BookCompactCardContent(book = book, showProgress = showProgress)
        }
    }
}

@Composable
private fun BookCompactCardContent(
    book: Book,
    showProgress: Boolean
) {
    // 检查封面文件是否存在
    val coverExists = remember(book.coverPath) {
        book.coverPath?.let { File(it).exists() } ?: false
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
    ) {
            // 1. 封面图片层 - 填充整个卡片
            if (coverExists && book.coverPath != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.coverPath)
                        .crossfade(200)
                        .memoryCacheKey("cover_${book.id}")
                        .diskCacheKey("cover_${book.id}")
                        .size(300, 420)
                        .build(),
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )
            } else {
                // 无封面时显示默认图标
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 2. 状态标签层 - 左上角
            StatusBadge(
                book = book,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
            
            // 3. 底部叠加层 - 渐变背景 + 标题 + 进度条
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f) // 占据底部30%
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    // 标题
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    
                    // 进度条
                    if (showProgress) {
                        LinearProgressIndicator(
                            progress = { book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
}

/**
 * 状态标签组件
 */
@Composable
fun StatusBadge(
    book: Book,
    modifier: Modifier = Modifier
) {
    val readingStatus = book.getReadingStatus()
    
    if (readingStatus == takagi.ru.paysage.data.model.BookReadingStatus.READING) {
        // 连接式标签（阅读中状态）
        ConnectedReadingStatusBadge(
            statusText = stringResource(takagi.ru.paysage.R.string.status_reading),
            progressPercentage = (book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) * 100).toInt(),
            modifier = modifier
        )
    } else {
        // 普通标签
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = when (readingStatus) {
                takagi.ru.paysage.data.model.BookReadingStatus.FINISHED -> 
                    androidx.compose.ui.graphics.Color(0xFF2196F3)
                takagi.ru.paysage.data.model.BookReadingStatus.LATEST -> 
                    androidx.compose.ui.graphics.Color(0xFFFF5722)
                takagi.ru.paysage.data.model.BookReadingStatus.UNREAD -> 
                    androidx.compose.ui.graphics.Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = when (readingStatus) {
                    takagi.ru.paysage.data.model.BookReadingStatus.FINISHED -> 
                        stringResource(takagi.ru.paysage.R.string.status_finished)
                    takagi.ru.paysage.data.model.BookReadingStatus.LATEST -> 
                        stringResource(takagi.ru.paysage.R.string.status_latest)
                    takagi.ru.paysage.data.model.BookReadingStatus.UNREAD -> 
                        stringResource(takagi.ru.paysage.R.string.status_unread)
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * 元数据行组件
 */
@Composable
fun MetadataRow(
    book: Book,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${book.totalPages} 页",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = book.fileFormat.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 进度区域组件
 */
@Composable
fun ProgressSection(
    book: Book,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { book.currentPage.toFloat() / book.totalPages.coerceAtLeast(1) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(MaterialTheme.shapes.small),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Text(
            text = "${book.currentPage}/${book.totalPages}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 连接式阅读状态标签 - 将状态文字和进度百分比无缝连接
 */
@Composable
fun ConnectedReadingStatusBadge(
    statusText: String,
    progressPercentage: Int,
    statusColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
    progressColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // 左侧：状态文字
            Box(
                modifier = Modifier
                    .background(
                        color = statusColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            // 右侧：进度百分比
            Box(
                modifier = Modifier
                    .background(
                        color = progressColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$progressPercentage%",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
