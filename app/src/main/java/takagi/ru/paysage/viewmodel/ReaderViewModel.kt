package takagi.ru.paysage.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.model.*
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.repository.BookRepository
import takagi.ru.paysage.repository.HistoryRepositoryImpl
import takagi.ru.paysage.repository.ReadingProgressRepository
import takagi.ru.paysage.utils.FileParser
import java.io.File
import kotlin.math.abs

private const val TAG = "ReaderViewModel"
private const val MAX_CACHE_SIZE = 10 // 最多缓存10页，避免内存溢出

/**
 * 阅读器 ViewModel - 简化版
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bookRepository = BookRepository(application)
    private val progressRepository = ReadingProgressRepository(application)
    private val historyRepository = HistoryRepositoryImpl(
        PaysageDatabase.getDatabase(application).historyDao()
    )
    private val fileParser = FileParser(application)
    
    // 当前书籍 ID
    private val _currentBookId = MutableStateFlow<Long?>(null)
    
    // UI 状态
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    
    // 当前页面图片
    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()
    
    // 页面缓存 - 使用 LruCache 限制内存使用
    private val pageCache = object : LruCache<Int, Bitmap>(MAX_CACHE_SIZE) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) {
                try {
                    oldValue.recycle()
                    Log.d(TAG, "Recycled evicted page $key")
                } catch (e: Exception) {
                    Log.e(TAG, "Error recycling bitmap for page $key", e)
                }
            }
        }
        
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return 1 // 按页数计算，不按字节
        }
    }
    
    // 预加载状态
    private val _preloadProgress = MutableStateFlow(0f)
    val preloadProgress: StateFlow<Float> = _preloadProgress.asStateFlow()
    
    // 预加载任务控制 - 仅保留相邻页预加载
    private var nearbyPreloadJob: kotlinx.coroutines.Job? = null
    
    /**
     * 打开书籍
     */
    fun openBook(bookId: Long) {
        viewModelScope.launch {
            _currentBookId.value = bookId
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val book = bookRepository.getBookById(bookId)
                if (book == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "书籍不存在"
                        )
                    }
                    return@launch
                }
                
                // 获取或创建阅读进度
                val progress = progressRepository.getOrCreateProgress(bookId, book.totalPages)
                
                // 更新 UI 状态
                _uiState.update { 
                    it.copy(
                        bookTitle = book.title,
                        totalPages = book.totalPages,
                        currentPage = progress.currentPage,
                        isLoading = false
                    )
                }
                
                // 优先加载当前页和相邻页
                loadPage(book, progress.currentPage)
                preloadNearbyPages(book, progress.currentPage)
                
                // 注意：已禁用全书预加载，避免内存溢出
                // startFullBookPreload(book, progress.currentPage)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error opening book", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "打开书籍失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 加载指定页面 (高优先级)
     */
    private suspend fun loadPage(book: Book, pageNumber: Int) {
        try {
            // 如果缓存中有，直接使用
            val cachedBitmap = pageCache.get(pageNumber)
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                _currentPageBitmap.value = cachedBitmap
                _uiState.update { it.copy(currentPage = pageNumber) }
                
                // 更新进度
                updateProgress(book, pageNumber)
                return
            }

            // 从文件加载页面
            val bitmap = withContext(Dispatchers.IO) {
                if (book.filePath.startsWith("content://")) {
                    val uri = Uri.parse(book.filePath)
                    fileParser.extractPageFromUri(uri, pageNumber)
                } else {
                    val file = File(book.filePath)
                    fileParser.extractPage(file, pageNumber)
                }
            }
            
            // 更新 UI
            _currentPageBitmap.value = bitmap
            if (bitmap != null) {
                pageCache.put(pageNumber, bitmap)
            }
            _uiState.update { it.copy(currentPage = pageNumber) }
            
            // 更新进度
            updateProgress(book, pageNumber)
            
            Log.d(TAG, "Page $pageNumber loaded successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading page $pageNumber", e)
            _uiState.update { 
                it.copy(error = "加载页面失败: ${e.message}")
            }
        }
    }

    private suspend fun updateProgress(book: Book, pageNumber: Int) {
        progressRepository.updateCurrentPage(book.id, pageNumber, book.totalPages)
        bookRepository.updateReadingProgress(book.id, pageNumber)
        updateReadingHistory(book, pageNumber)
    }
    
    /**
     * 更新阅读历史
     */
    private suspend fun updateReadingHistory(book: Book, currentPage: Int) {
        try {
            val progress = currentPage.toFloat() / book.totalPages.toFloat()
            val historyItem = HistoryItem(
                bookId = book.id,
                title = book.title,
                author = book.author ?: "",
                thumbnailPath = book.coverPath,
                fileType = book.fileFormat.name,
                fileSize = book.fileSize,
                filePath = book.filePath,
                lastReadTime = System.currentTimeMillis(),
                progress = progress,
                currentPage = currentPage,
                totalPages = book.totalPages
            )
            historyRepository.addOrUpdateHistory(historyItem)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update reading history", e)
        }
    }
    
    /**
     * 下一页
     */
    fun nextPage() {
        viewModelScope.launch {
            val bookId = _currentBookId.value ?: return@launch
            val book = bookRepository.getBookById(bookId) ?: return@launch
            val currentPage = _uiState.value.currentPage
            
            if (currentPage < book.totalPages - 1) {
                val nextPage = currentPage + 1
                loadPage(book, nextPage)
                preloadNearbyPages(book, nextPage)
            }
        }
    }
    
    /**
     * 上一页
     */
    fun previousPage() {
        viewModelScope.launch {
            val bookId = _currentBookId.value ?: return@launch
            val book = bookRepository.getBookById(bookId) ?: return@launch
            val currentPage = _uiState.value.currentPage
            
            if (currentPage > 0) {
                val prevPage = currentPage - 1
                loadPage(book, prevPage)
                preloadNearbyPages(book, prevPage)
            }
        }
    }
    
    /**
     * 跳转到指定页
     */
    fun goToPage(pageNumber: Int) {
        viewModelScope.launch {
            val bookId = _currentBookId.value ?: return@launch
            val book = bookRepository.getBookById(bookId) ?: return@launch
            
            if (pageNumber in 0 until book.totalPages) {
                loadPage(book, pageNumber)
                preloadNearbyPages(book, pageNumber)
            }
        }
    }
    
    /**
     * 切换工具栏显示/隐藏
     */
    fun toggleToolbar() {
        _uiState.update { it.copy(isToolbarVisible = !it.isToolbarVisible) }
    }
    
    /**
     * 更新阅读器配置
     */
    fun updateConfig(config: ReaderConfig) {
        _uiState.update { it.copy(readerConfig = config) }
        // TODO: 保存配置到持久化存储
    }
    
    /**
     * 预加载指定页面 (公开接口)
     */
    fun preloadPage(pageNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookId = _currentBookId.value ?: return@launch
            val book = bookRepository.getBookById(bookId) ?: return@launch
            preloadPageInternal(book, pageNumber)
        }
    }

    /**
     * 预加载指定页面
     */
    private suspend fun preloadPageInternal(book: Book, pageNumber: Int): Bitmap? {
        if (pageNumber < 0 || pageNumber >= book.totalPages) return null
        
        // 如果已经在缓存中，直接返回
        val cachedBitmap = pageCache.get(pageNumber)
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            return cachedBitmap
        }
        
        return try {
            val bitmap = withContext(Dispatchers.IO) {
                if (book.filePath.startsWith("content://")) {
                    val uri = Uri.parse(book.filePath)
                    fileParser.extractPageFromUri(uri, pageNumber)
                } else {
                    val file = File(book.filePath)
                    fileParser.extractPage(file, pageNumber)
                }
            }
            
            if (bitmap != null) {
                pageCache.put(pageNumber, bitmap)
                Log.d(TAG, "Preloaded page $pageNumber, cache size: ${pageCache.size()}/$MAX_CACHE_SIZE")
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload page $pageNumber", e)
            null
        }
    }

    /**
     * 预加载相邻页面 (中优先级)
     * 确保当前页的前后页尽快加载
     */
    private fun preloadNearbyPages(book: Book, currentPage: Int) {
        nearbyPreloadJob?.cancel()
        nearbyPreloadJob = viewModelScope.launch(Dispatchers.IO) {
            // 只预加载前后各2页
            val neighbors = listOf(currentPage + 1, currentPage - 1, currentPage + 2, currentPage - 2)
            for (page in neighbors) {
                if (page in 0 until book.totalPages) {
                    val cached = pageCache.get(page)
                    if (cached == null || cached.isRecycled) {
                        preloadPageInternal(book, page)
                    }
                }
            }
        }
    }
    
    // 已禁用全书预加载功能，避免内存溢出
    // 如需恢复，请取消注释下面的代码
    /*
    private fun startFullBookPreload(book: Book, currentPage: Int) {
        // 全书预加载会导致大量内存消耗，已禁用
    }
    */
    
    /**
     * 获取下一页 Bitmap（用于翻页动画）
     */
    fun getNextPageBitmap(): Bitmap? {
        val currentPage = _uiState.value.currentPage
        val totalPages = _uiState.value.totalPages
        
        if (currentPage >= totalPages - 1) return null
        
        val nextPage = currentPage + 1
        return pageCache[nextPage]
    }
    
    /**
     * 获取上一页 Bitmap（用于翻页动画）
     */
    fun getPreviousPageBitmap(): Bitmap? {
        val currentPage = _uiState.value.currentPage
        
        if (currentPage <= 0) return null
        
        val previousPage = currentPage - 1
        return pageCache[previousPage]
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 取消预加载任务
        nearbyPreloadJob?.cancel()
        nearbyPreloadJob = null
        
        _currentBookId.value = null
        
        // 不回收 currentPageBitmap，因为它可能来自缓存
        _currentPageBitmap.value = null
        
        // 清理页面缓存 - LruCache 的 evictAll 会触发 entryRemoved 回调
        try {
            pageCache.evictAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing page cache", e)
        }
        
        _preloadProgress.value = 0f
        _uiState.value = ReaderUiState()
        Log.d(TAG, "ReaderViewModel cleaned up")
    }
    
    /**
     * 获取指定页面的位图
     * 
     * 用于懒加载模式。
     * 
     * @param page 页面索引
     * @return 页面位图，如果未缓存则返回 null
     */
    fun getPageBitmap(page: Int): Bitmap? {
        val bitmap = pageCache.get(page)
        return if (bitmap != null && !bitmap.isRecycled) bitmap else null
    }
    
    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}

/**
 * 阅读器 UI 状态 - 简化版
 */
data class ReaderUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isLoading: Boolean = false,
    val isToolbarVisible: Boolean = true,
    val error: String? = null,
    val bookTitle: String = "",
    val scale: Float = 1f,
    val offset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val readerConfig: ReaderConfig = ReaderConfig()
)
