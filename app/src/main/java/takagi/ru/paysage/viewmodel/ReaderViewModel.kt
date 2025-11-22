package takagi.ru.paysage.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
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
    
    // 页面缓存 - 用于预加载
    // 使用 mutableStateMapOf 以便 Compose 能感知变化
    private val pageCache = mutableStateMapOf<Int, Bitmap>()
    
    // 预加载状态
    private val _preloadProgress = MutableStateFlow(0f)
    val preloadProgress: StateFlow<Float> = _preloadProgress.asStateFlow()
    
    // 预加载任务控制
    private var fullBookPreloadJob: kotlinx.coroutines.Job? = null
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
                
                // 启动全书预加载（低优先级）
                startFullBookPreload(book, progress.currentPage)
                
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
            if (pageCache.containsKey(pageNumber)) {
                _currentPageBitmap.value = pageCache[pageNumber]
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
                pageCache[pageNumber] = bitmap
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
        pageCache[pageNumber]?.let { return it }
        
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
                pageCache[pageNumber] = bitmap
                Log.d(TAG, "Preloaded page $pageNumber")
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
            val neighbors = listOf(currentPage + 1, currentPage - 1, currentPage + 2, currentPage - 2)
            for (page in neighbors) {
                if (page in 0 until book.totalPages && !pageCache.containsKey(page)) {
                    preloadPageInternal(book, page)
                }
            }
        }
    }
    
    /**
     * 启动全书预加载 (低优先级)
     */
    private fun startFullBookPreload(book: Book, currentPage: Int) {
        // 不取消之前的全书预加载，除非是 cleanup
        if (fullBookPreloadJob?.isActive == true) return
        
        fullBookPreloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalPages = book.totalPages
                val loadedPages = mutableSetOf<Int>()
                
                // 简单的顺序预加载，跳过已加载的
                // 从当前页向后
                for (page in currentPage until totalPages) {
                    if (!pageCache.containsKey(page)) {
                        preloadPageInternal(book, page)
                        loadedPages.add(page)
                        // 稍微延时，避免占满 IO
                        kotlinx.coroutines.delay(50)
                    }
                }
                // 从当前页向前
                for (page in currentPage - 1 downTo 0) {
                    if (!pageCache.containsKey(page)) {
                        preloadPageInternal(book, page)
                        loadedPages.add(page)
                        kotlinx.coroutines.delay(50)
                    }
                }
                
                _preloadProgress.value = 1f
                Log.d(TAG, "Full book preload completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Full book preload failed", e)
            }
        }
    }
    
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
        fullBookPreloadJob?.cancel()
        nearbyPreloadJob = null
        fullBookPreloadJob = null
        
        _currentBookId.value = null
        _currentPageBitmap.value?.recycle()
        _currentPageBitmap.value = null
        
        // 清理页面缓存
        pageCache.values.forEach { bitmap ->
            bitmap.recycle()
        }
        pageCache.clear()
        
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
        return pageCache[page]
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
