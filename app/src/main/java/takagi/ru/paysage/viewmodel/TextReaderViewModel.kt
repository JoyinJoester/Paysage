package takagi.ru.paysage.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.utils.EpubParser
import java.io.File

private const val TAG = "TextReaderViewModel"

/**
 * 文本阅读器 ViewModel
 * 用于管理 EPUB 等文本格式的阅读状态
 * 支持章节缓存和懒加载
 */
class TextReaderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val epubParser = EpubParser(application)
    private val bookRepository = takagi.ru.paysage.repository.BookRepository(application)
    
    // UI 状态
    private val _uiState = MutableStateFlow(TextReaderUiState())
    val uiState: StateFlow<TextReaderUiState> = _uiState.asStateFlow()
    
    // 当前章节内容（保留兼容性）
    private val _currentChapterContent = MutableStateFlow<EpubParser.EpubChapter?>(null)
    val currentChapterContent: StateFlow<EpubParser.EpubChapter?> = _currentChapterContent.asStateFlow()
    
    // 所有已加载的章节内容缓存
    private val _allChapterContents = mutableStateMapOf<Int, EpubParser.EpubChapter>()
    val allChapterContents: Map<Int, EpubParser.EpubChapter?> get() = _allChapterContents
    
    // 章节标题列表
    private val _chapterTitles = MutableStateFlow<List<String>>(emptyList())
    val chapterTitles: StateFlow<List<String>> = _chapterTitles.asStateFlow()
    
    // 正在加载的章节索引集合
    private val loadingChapters = mutableSetOf<Int>()
    
    // 当前文件路径
    private var currentFilePath: String? = null
    private var currentBookId: Long? = null
    
    /**
     * 打开书籍
     */
    fun openBook(bookId: Long, filePath: String) {
        Log.d(TAG, "openBook called with bookId=$bookId, filePath=$filePath")
        
        if (currentFilePath == filePath && _uiState.value.totalChapters > 0) {
            // 已经加载过同一本书
            return
        }
        
        // 清除旧的缓存
        _allChapterContents.clear()
        loadingChapters.clear()
        
        currentBookId = bookId
        currentFilePath = filePath
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val isUri = filePath.startsWith("content://")
                
                // 获取元数据
                val metadata = if (isUri) {
                    epubParser.getMetadataFromUri(Uri.parse(filePath))
                } else {
                    epubParser.getMetadata(File(filePath))
                }
                
                if (metadata == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "无法解析EPUB文件"
                        )
                    }
                    return@launch
                }
                
                // 获取章节标题
                val titles = if (isUri) {
                    epubParser.getChapterTitlesFromUri(Uri.parse(filePath))
                } else {
                    epubParser.getChapterTitles(File(filePath))
                }
                
                _chapterTitles.value = titles
                
                // 获取保存的进度
                var savedChapter = 0
                if (bookId > 0) {
                    val book = withContext(Dispatchers.IO) { bookRepository.getBookById(bookId) }
                    if (book != null) {
                        savedChapter = book.currentPage
                        if (savedChapter >= metadata.chapterCount) {
                            savedChapter = 0
                        }
                        Log.d(TAG, "Restoring progress: chapter $savedChapter for bookId $bookId")
                    }
                }
                
                // 更新状态
                _uiState.update { 
                    it.copy(
                        bookTitle = metadata.title,
                        author = metadata.author,
                        totalChapters = metadata.chapterCount,
                        currentChapter = savedChapter,
                        isLoading = false
                    )
                }
                
                // 预加载当前章节及相邻章节
                val startLoad = (savedChapter - 1).coerceAtLeast(0)
                val endLoad = (savedChapter + 1).coerceAtMost(metadata.chapterCount - 1)
                for (i in startLoad..endLoad) {
                    ensureChapterLoaded(i)
                }
                
                Log.d(TAG, "Book opened: ${metadata.title}, ${metadata.chapterCount} chapters")
                
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
     * 确保章节已加载（懒加载）
     */
    fun ensureChapterLoaded(chapterIndex: Int) {
        // 已经加载过或正在加载
        if (_allChapterContents.containsKey(chapterIndex) || loadingChapters.contains(chapterIndex)) {
            return
        }
        
        val filePath = currentFilePath ?: return
        val total = _uiState.value.totalChapters
        
        if (chapterIndex !in 0 until total) return
        
        loadingChapters.add(chapterIndex)
        
        viewModelScope.launch {
            try {
                val isUri = filePath.startsWith("content://")
                
                val chapter = withContext(Dispatchers.IO) {
                    if (isUri) {
                        epubParser.getChapterFromUri(Uri.parse(filePath), chapterIndex)
                    } else {
                        epubParser.getChapter(File(filePath), chapterIndex)
                    }
                }
                
                if (chapter != null) {
                    _allChapterContents[chapterIndex] = chapter
                    
                    // 同时更新当前章节（兼容旧代码）
                    if (chapterIndex == _uiState.value.currentChapter) {
                        _currentChapterContent.value = chapter
                    }
                    
                    Log.d(TAG, "Chapter $chapterIndex loaded: ${chapter.title}")
                } else {
                    Log.w(TAG, "Failed to load chapter $chapterIndex")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chapter $chapterIndex", e)
            } finally {
                loadingChapters.remove(chapterIndex)
            }
        }
    }
    
    /**
     * 加载指定章节
     */
    private suspend fun loadChapter(chapterIndex: Int, startFromEnd: Boolean = false) {
        ensureChapterLoaded(chapterIndex)
        _uiState.update { 
            it.copy(
                currentChapter = chapterIndex,
                startFromEnd = startFromEnd, // 标记是否从末尾开始
                currentPage = 1, // 重置页码，具体页码由 UI 加载完成后回调决定
                totalPages = 1
            ) 
        }
    }
    
    /**
     * 下一章
     */
    fun nextChapter() {
        val current = _uiState.value.currentChapter
        val total = _uiState.value.totalChapters
        
        if (current < total - 1) {
            viewModelScope.launch {
                loadChapter(current + 1, startFromEnd = false)
            }
        }
    }
    
    /**
     * 上一章
     */
    fun previousChapter() {
        val current = _uiState.value.currentChapter
        
        if (current > 0) {
            viewModelScope.launch {
                loadChapter(current - 1, startFromEnd = true) // 只有这里是从末尾开始
            }
        }
    }
    
    /**
     * 更新实时阅读进度（滑动翻页时调用）
     * 只更新状态和数据库，不触发内容加载
     */
    fun updateRealtimeProgress(chapterIndex: Int, currentPageInBook: Int, totalPagesInBook: Int, isFinished: Boolean = false) {
        val currentState = _uiState.value
        
        // 如果章节变了，更新章节索引
        if (chapterIndex != currentState.currentChapter) {
            _uiState.update { 
                it.copy(currentChapter = chapterIndex) 
            }
            // 预加载新章节的相邻章节
            ensureChapterLoaded(chapterIndex + 1)
        }
        
        // 更新页码信息
        _uiState.update { 
            it.copy(
                currentPage = 1, // 这里简化处理，暂不追踪章节内精确页码，只追踪章节
                totalPages = 1
            )
        }
        
        // 持久化保存
        val bookId = currentBookId
        if (bookId != null && bookId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                // 保存当前章节作为进度
                bookRepository.updateReadingProgress(bookId, chapterIndex, isFinished)
            }
        }
    }

    /**
     * 更新当前页码进度（旧方法，保留兼容性）
     */
    fun updatePageProgress(current: Int, total: Int) {
        _uiState.update { 
            it.copy(
                currentPage = current,
                totalPages = total
            )
        }
    }
    
    /**
     * 保存阅读进度（退出时或暂停时调用）
     */
    fun saveProgress() {
        val bookId = currentBookId ?: return
        val state = _uiState.value
        
        viewModelScope.launch(Dispatchers.IO) {
            // 获取当前书籍
            val book = bookRepository.getBookById(bookId)
            if (book != null) {
                // 更新书籍信息：当前章节，总页数（总章节数）
                val updatedBook = book.copy(
                    currentPage = state.currentChapter, // 这里用 currentPage 存章节索引
                    totalPages = state.totalChapters,   // 这里用 totalPages 存总章节数
                    lastReadAt = System.currentTimeMillis()
                )
                bookRepository.updateBook(updatedBook)
                Log.d(TAG, "Progress saved: Chapter ${state.currentChapter}, BookId $bookId")
            }
        }
    }

    /**
     * 跳转到指定章节
     */
    fun goToChapter(chapterIndex: Int) {
        val total = _uiState.value.totalChapters
        
        if (chapterIndex in 0 until total) {
            _uiState.update { it.copy(currentChapter = chapterIndex, startFromEnd = false) }
            ensureChapterLoaded(chapterIndex)
            
            // 同时预加载相邻章节
            if (chapterIndex > 0) ensureChapterLoaded(chapterIndex - 1)
            if (chapterIndex < total - 1) ensureChapterLoaded(chapterIndex + 1)
        }
    }
    
    /**
     * 更新阅读配置
     */
    fun updateConfig(config: ReaderConfig) {
        _uiState.update { it.copy(config = config) }
    }
    
    override fun onCleared() {
        saveProgress() // 退出时保存
        super.onCleared()
        currentFilePath = null
        currentBookId = null
        _allChapterContents.clear()
        loadingChapters.clear()
    }
}

/**
 * 文本阅读器 UI 状态
 */
data class TextReaderUiState(
    val bookTitle: String = "",
    val author: String = "",
    val currentChapter: Int = 0,
    val totalChapters: Int = 0,
    val currentPage: Int = 1, // 当前章内页码
    val totalPages: Int = 1,  // 当前章内总页数
    val startFromEnd: Boolean = false, // 是否从章节末尾开始（用于从下一章返回上一章时）
    val isLoading: Boolean = false,
    val error: String? = null,
    val config: ReaderConfig = ReaderConfig()
)
