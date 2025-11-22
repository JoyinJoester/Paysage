package takagi.ru.paysage.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.BookFormat
import takagi.ru.paysage.data.model.CategoryInfo
import takagi.ru.paysage.data.model.FilterMode
import takagi.ru.paysage.data.model.SyncOptions
import takagi.ru.paysage.data.model.SyncType
import takagi.ru.paysage.repository.BookRepository
import takagi.ru.paysage.repository.LibraryStatistics
import takagi.ru.paysage.repository.ScanResult
import takagi.ru.paysage.repository.SettingsRepository
import takagi.ru.paysage.repository.SyncOptionsRepository
import java.io.File

/**
 * 书库 ViewModel
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    internal val repository = BookRepository(application)
    private val syncOptionsRepository = SyncOptionsRepository(application)
    private val settingsRepository = SettingsRepository(application)
    
    // 保存最后扫描的文件夹 URI
    private val _lastScannedUri = MutableStateFlow<android.net.Uri?>(null)
    val lastScannedUri: StateFlow<android.net.Uri?> = _lastScannedUri.asStateFlow()
    
    // 书籍列表
    val allBooks: StateFlow<List<Book>> = repository.getAllBooksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val favoriteBooks: StateFlow<List<Book>> = repository.getFavoriteBooksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 上次阅读的书籍
    val lastReadBook: StateFlow<Book?> = repository.getLastReadBookFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // 同步选项
    val syncOptions: StateFlow<SyncOptions> = syncOptionsRepository.syncOptionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncOptions())
    
    // UI 状态
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    // 书籍详情状态
    private val _bookDetailUiState = MutableStateFlow(takagi.ru.paysage.ui.state.BookDetailUiState())
    val bookDetailUiState: StateFlow<takagi.ru.paysage.ui.state.BookDetailUiState> = _bookDetailUiState.asStateFlow()
    
    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 搜索结果
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Book>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchBooksFlow(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 分类列表
    val categories: StateFlow<List<String>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 筛选模式
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()
    
    // 选中的分类
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    // 最近阅读的书籍
    val recentBooks: StateFlow<List<Book>> = repository.getRecentBooksFlow(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 分类及其书籍数量
    val categoriesWithCount: StateFlow<List<CategoryInfo>> = 
        repository.getCategoriesWithCountFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 根据筛选模式获取当前显示的书籍列表
    val displayBooks: StateFlow<List<Book>> = combine(
        filterMode,
        selectedCategory,
        allBooks,
        favoriteBooks,
        recentBooks
    ) { mode, category, all, favorites, recent ->
        try {
            when (mode) {
                FilterMode.ALL -> all
                FilterMode.FAVORITES -> favorites
                FilterMode.RECENT -> recent
                FilterMode.CATEGORY -> {
                    if (category != null && category.isNotBlank()) {
                        all.filter { it.category == category }
                    } else {
                        emptyList()
                    }
                }
                FilterMode.CATEGORIES -> emptyList() // 分类列表视图不显示书籍
            }
        } catch (e: Exception) {
            // 发生错误时返回空列表
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 统计信息
    private val _statistics = MutableStateFlow<LibraryStatistics?>(null)
    val statistics: StateFlow<LibraryStatistics?> = _statistics.asStateFlow()
    
    init {
        loadStatistics()
        // 从设置中加载最后扫描的URI
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                settings.lastScannedFolderUri?.let { uriString ->
                    _lastScannedUri.value = android.net.Uri.parse(uriString)
                }
            }
        }
    }
    
    /**
     * 搜索书籍
     */
    fun search(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 清空搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * 扫描书籍
     */
    fun scanBooks(directory: File? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanError = null) }
            
            try {
                val result = repository.scanAndImportBooks(directory)
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        scanResult = result
                    )
                }
                loadStatistics()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        scanError = e.message ?: "扫描失败"
                    )
                }
            }
        }
    }
    
    /**
     * 扫描书籍（从 URI）
     */
    fun scanBooksFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanError = null) }
            
            try {
                // 保存最后扫描的 URI 到内存和持久化存储
                _lastScannedUri.value = uri
                settingsRepository.updateLastScannedFolderUri(uri.toString())
                
                val result = repository.scanAndImportBooksFromUri(uri)
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        scanResult = result
                    )
                }
                loadStatistics()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        scanError = e.message ?: "扫描失败"
                    )
                }
            }
        }
    }
    
    /**
     * 生成所有封面
     */
    fun generateAllCovers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingCovers = true) }
            
            try {
                val count = repository.generateCoversForAll()
                _uiState.update { 
                    it.copy(
                        isGeneratingCovers = false,
                        message = "已生成 $count 个封面"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isGeneratingCovers = false,
                        message = "生成封面失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 切换收藏状态
     */
    fun toggleFavorite(bookId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(bookId, isFavorite)
        }
    }
    
    /**
     * 删除书籍
     */
    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
            loadStatistics()
        }
    }
    
    /**
     * 获取书籍按分类
     */
    fun getBooksByCategory(category: String): StateFlow<List<Book>> {
        return repository.getBooksByCategoryFlow(category)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
    
    /**
     * 获取书籍按格式
     */
    fun getBooksByFormat(format: BookFormat): StateFlow<List<Book>> {
        return repository.getBooksByFormatFlow(format)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
    
    /**
     * 加载统计信息
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            val stats = repository.getStatistics()
            _statistics.value = stats
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    /**
     * 清除扫描结果
     */
    fun clearScanResult() {
        _uiState.update { it.copy(scanResult = null, scanError = null) }
    }
    
    /**
     * 执行同步操作
     * 
     * @param syncType 同步类型
     * @param options 同步选项
     */
    fun performSync(syncType: SyncType, options: SyncOptions) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncError = null) }
            
            try {
                // 保存同步选项到 DataStore
                syncOptionsRepository.updateSyncOptions(options)
                
                // 如果有保存的 URI，使用 URI 扫描；否则使用默认目录
                val scanResult = if (_lastScannedUri.value != null) {
                    // 使用保存的 URI 重新扫描
                    repository.scanAndImportBooksFromUri(_lastScannedUri.value!!)
                } else {
                    // 使用默认的同步逻辑
                    when (syncType) {
                        SyncType.FULL -> repository.executeFullSync(options)
                        SyncType.INCREMENTAL -> repository.executeIncrementalSync(options)
                        SyncType.MAINTENANCE -> repository.executeMaintenance(options)
                    }
                }
                
                // 将 ScanResult 转换为 SyncResult（如果需要）
                val syncResult = if (scanResult is takagi.ru.paysage.repository.ScanResult) {
                    takagi.ru.paysage.data.model.SyncResult(
                        newBooks = scanResult.newBooks,
                        updatedBooks = scanResult.updatedBooks,
                        deletedBooks = 0,
                        generatedThumbnails = 0,
                        duration = 0,
                        errors = emptyList()
                    )
                } else {
                    scanResult as takagi.ru.paysage.data.model.SyncResult
                }
                
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        syncResult = syncResult,
                        message = "同步完成：新增 ${syncResult.newBooks} 本，更新 ${syncResult.updatedBooks} 本"
                    )
                }
                loadStatistics()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        syncError = e.message ?: "同步失败"
                    )
                }
            }
        }
    }
    
    /**
     * 清除同步结果
     */
    fun clearSyncResult() {
        _uiState.update { it.copy(syncResult = null, syncError = null) }
    }
    
    /**
     * 显示书籍详情
     */
    fun showBookDetail(book: Book) {
        _bookDetailUiState.update { 
            it.copy(
                isVisible = true,
                selectedBook = book,
                tempTags = book.tags
            )
        }
    }
    
    /**
     * 隐藏书籍详情
     */
    fun hideBookDetail() {
        _bookDetailUiState.update { 
            takagi.ru.paysage.ui.state.BookDetailUiState()
        }
    }
    
    /**
     * 更新书籍标签
     */
    fun updateBookTags(bookId: Long, tags: List<String>) {
        viewModelScope.launch {
            repository.updateBookTags(bookId, tags)
            // 更新当前显示的书籍
            _bookDetailUiState.value.selectedBook?.let { book ->
                if (book.id == bookId) {
                    _bookDetailUiState.update { 
                        it.copy(selectedBook = book.copy(tags = tags))
                    }
                }
            }
        }
    }
    
    /**
     * 更新排序偏好
     */
    fun updateSortPreference(bookId: Long, preference: String) {
        viewModelScope.launch {
            repository.updateSortPreference(bookId, preference)
            // 更新当前显示的书籍
            _bookDetailUiState.value.selectedBook?.let { book ->
                if (book.id == bookId) {
                    _bookDetailUiState.update { 
                        it.copy(selectedBook = book.copy(sortPreference = preference))
                    }
                }
            }
        }
    }
    
    /**
     * 切换收藏状态（用于详情页）
     */
    fun toggleFavoriteFromDetail(bookId: Long) {
        viewModelScope.launch {
            _bookDetailUiState.value.selectedBook?.let { book ->
                val newFavoriteState = !book.isFavorite
                repository.toggleFavorite(bookId, newFavoriteState)
                _bookDetailUiState.update { 
                    it.copy(selectedBook = book.copy(isFavorite = newFavoriteState))
                }
            }
        }
    }
    
    /**
     * 删除书籍（用于详情页）
     */
    fun deleteBookFromDetail(bookId: Long) {
        viewModelScope.launch {
            _bookDetailUiState.value.selectedBook?.let { book ->
                repository.deleteBook(book)
                hideBookDetail()
                loadStatistics()
            }
        }
    }
    
    /**
     * 分享书籍
     */
    fun shareBook(book: Book): android.content.Intent {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = book.fileFormat.mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(java.io.File(book.filePath)))
            putExtra(android.content.Intent.EXTRA_SUBJECT, book.title)
            putExtra(android.content.Intent.EXTRA_TEXT, "分享书籍: ${book.title}")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return android.content.Intent.createChooser(intent, "分享书籍")
    }
    
    /**
     * 设置筛选模式
     */
    fun setFilterMode(mode: FilterMode, category: String? = null) {
        _filterMode.value = mode
        _selectedCategory.value = category
    }
    
    /**
     * 从路由参数解析筛选模式
     */
    fun parseFilterFromRoute(filter: String?, category: String?) {
        try {
            when (filter?.lowercase()?.trim()) {
                "favorites" -> setFilterMode(FilterMode.FAVORITES)
                "recent" -> setFilterMode(FilterMode.RECENT)
                "categories" -> {
                    if (category != null && category.isNotBlank()) {
                        // 验证分类是否存在
                        viewModelScope.launch {
                            val validCategories = categoriesWithCount.value.map { it.name }
                            if (validCategories.contains(category)) {
                                setFilterMode(FilterMode.CATEGORY, category)
                            } else {
                                // 分类不存在，回退到分类列表
                                setFilterMode(FilterMode.CATEGORIES)
                            }
                        }
                    } else {
                        setFilterMode(FilterMode.CATEGORIES)
                    }
                }
                null, "", "all" -> setFilterMode(FilterMode.ALL)
                else -> {
                    // 无效的筛选参数，回退到全部书籍
                    setFilterMode(FilterMode.ALL)
                }
            }
        } catch (e: Exception) {
            // 发生错误时回退到默认状态
            setFilterMode(FilterMode.ALL)
        }
    }
}

/**
 * 书库 UI 状态
 */
data class LibraryUiState(
    val isScanning: Boolean = false,
    val scanResult: ScanResult? = null,
    val scanError: String? = null,
    val isGeneratingCovers: Boolean = false,
    val isSyncing: Boolean = false,
    val syncResult: takagi.ru.paysage.data.model.SyncResult? = null,
    val syncError: String? = null,
    val message: String? = null
)
