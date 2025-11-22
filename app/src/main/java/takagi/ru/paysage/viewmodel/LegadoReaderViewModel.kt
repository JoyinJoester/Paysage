package takagi.ru.paysage.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.Bookmark
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.repository.BookRepository
import takagi.ru.paysage.repository.BookmarkRepository
import takagi.ru.paysage.ui.state.LegadoReaderUiState
import takagi.ru.paysage.ui.state.SearchResult

/**
 * Legado阅读器ViewModel
 * 管理阅读器状态和业务逻辑
 */
class LegadoReaderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bookRepository = BookRepository(application)
    private val bookmarkRepository = BookmarkRepository(application)
    
    // UI状态
    private val _uiState = MutableStateFlow(LegadoReaderUiState())
    val uiState: StateFlow<LegadoReaderUiState> = _uiState.asStateFlow()
    
    /**
     * 加载书籍
     */
    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
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
                
                // TODO: 加载章节列表
                // TODO: 加载阅读进度
                // TODO: 加载阅读配置
                
                _uiState.update {
                    it.copy(
                        book = book,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 页面变化回调
     */
    fun onPageChange(chapter: Int, page: Int) {
        _uiState.update {
            it.copy(
                currentChapter = chapter,
                currentPage = page
            )
        }
        
        // TODO: 自动保存阅读进度
    }
    
    /**
     * 切换菜单显示
     */
    fun onMenuToggle() {
        _uiState.update {
            val showMenu = !it.showMenu
            it.copy(
                showMenu = showMenu,
                showTopBar = showMenu,
                showBottomBar = showMenu
            )
        }
    }
    
    /**
     * 文本选择回调
     */
    fun onTextSelected(text: String) {
        _uiState.update {
            it.copy(
                selectedText = text,
                showTextActionMenu = true
            )
        }
    }
    
    /**
     * 更新阅读配置
     */
    fun updateConfig(config: ReaderConfig) {
        _uiState.update { it.copy(config = config) }
        // TODO: 保存配置到Repository
        // TODO: 通知ReadView更新
    }
    
    /**
     * 显示/隐藏对话框
     */
    fun showReadStyleDialog(show: Boolean) {
        _uiState.update { it.copy(showReadStyleDialog = show) }
    }
    
    fun showBgTextConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showBgTextConfigDialog = show) }
    }
    
    fun showMoreConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showMoreConfigDialog = show) }
    }
    
    fun showTipConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showTipConfigDialog = show) }
    }
    
    fun showPaddingConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showPaddingConfigDialog = show) }
    }
    
    fun showClickActionConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showClickActionConfigDialog = show) }
    }
    
    fun showBookmarkDialog(show: Boolean) {
        _uiState.update { it.copy(showBookmarkDialog = show) }
        if (show) {
            loadBookmarks()
        }
    }
    
    fun showSearchDialog(show: Boolean) {
        _uiState.update { it.copy(showSearchDialog = show) }
    }
    
    fun showAutoReadDialog(show: Boolean) {
        _uiState.update { it.copy(showAutoReadDialog = show) }
    }
    
    fun showReadAloudDialog(show: Boolean) {
        _uiState.update { it.copy(showReadAloudDialog = show) }
    }
    
    fun showContentEditDialog(show: Boolean) {
        _uiState.update { it.copy(showContentEditDialog = show) }
    }
    
    fun showReplaceRuleDialog(show: Boolean) {
        _uiState.update { it.copy(showReplaceRuleDialog = show) }
    }
    
    /**
     * 加载书签列表
     */
    private fun loadBookmarks() {
        val bookId = _uiState.value.book?.id ?: return
        viewModelScope.launch {
            bookmarkRepository.getBookmarksByBookIdFlow(bookId).collect { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }
    
    /**
     * 添加书签
     */
    fun addBookmark() {
        val state = _uiState.value
        val book = state.book ?: return
        
        viewModelScope.launch {
            try {
                val bookmark = Bookmark(
                    bookId = book.id,
                    pageNumber = state.currentPage,
                    title = "${state.chapterTitle} - 第${state.currentPage}页"
                )
                bookmarkRepository.insertBookmark(bookmark)
                _uiState.update { it.copy(message = "书签已添加") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "添加书签失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 删除书签
     */
    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            try {
                bookmarkRepository.deleteBookmarkById(bookmarkId)
                _uiState.update { it.copy(message = "书签已删除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除书签失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 跳转到书签
     */
    fun gotoBookmark(bookmark: Bookmark) {
        // TODO: 实现跳转逻辑
        _uiState.update {
            it.copy(
                currentPage = bookmark.pageNumber,
                showBookmarkDialog = false
            )
        }
    }
    
    /**
     * 搜索文本
     */
    fun searchText(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // TODO: 实现搜索逻辑
    }
    
    /**
     * 切换自动阅读
     */
    fun toggleAutoRead() {
        _uiState.update { it.copy(isAutoReading = !it.isAutoReading) }
        // TODO: 启动/停止自动翻页
    }
    
    /**
     * 切换朗读
     */
    fun toggleReadAloud() {
        _uiState.update { it.copy(isReadingAloud = !it.isReadingAloud) }
        // TODO: 启动/停止TTS朗读
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
