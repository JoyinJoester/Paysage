package takagi.ru.paysage.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.Bookmark
import takagi.ru.paysage.repository.BookmarkRepository

private const val TAG = "BookmarkViewModel"

/**
 * 书签视图模型
 * 管理书签的增删查改和排序
 */
class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bookmarkRepository: BookmarkRepository = BookmarkRepository(application)
    
    // 当前书籍的所有书签
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()
    
    // 当前书籍 ID
    private var currentBookId: Long? = null
    
    /**
     * 加载指定书籍的所有书签
     */
    fun loadBookmarks(bookId: Long) {
        currentBookId = bookId
        viewModelScope.launch {
            try {
                bookmarkRepository.getBookmarksByBookIdFlow(bookId).collect { bookmarkList ->
                    _bookmarks.value = bookmarkList
                    Log.d(TAG, "Loaded ${bookmarkList.size} bookmarks for book $bookId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bookmarks", e)
            }
        }
    }
    
    /**
     * 添加书签
     */
    fun addBookmark(bookId: Long, pageNumber: Int, note: String = "") {
        viewModelScope.launch {
            try {
                val bookmark = Bookmark(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    note = note,
                    createdAt = System.currentTimeMillis()
                )
                bookmarkRepository.insertBookmark(bookmark)
                Log.d(TAG, "Bookmark added: page $pageNumber")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add bookmark", e)
            }
        }
    }
    
    /**
     * 更新书签备注
     */
    fun updateBookmarkNote(bookmarkId: Long, note: String) {
        viewModelScope.launch {
            try {
                val bookmark = _bookmarks.value.find { it.id == bookmarkId }
                if (bookmark != null) {
                    val updatedBookmark = bookmark.copy(note = note)
                    bookmarkRepository.updateBookmark(updatedBookmark)
                    Log.d(TAG, "Bookmark note updated: $bookmarkId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update bookmark note", e)
            }
        }
    }
    
    /**
     * 删除书签
     */
    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            try {
                val bookmark = _bookmarks.value.find { it.id == bookmarkId }
                if (bookmark != null) {
                    bookmarkRepository.deleteBookmark(bookmark)
                    Log.d(TAG, "Bookmark deleted: $bookmarkId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete bookmark", e)
            }
        }
    }
    
    /**
     * 删除指定页的书签
     */
    fun deleteBookmarkByPage(bookId: Long, pageNumber: Int) {
        viewModelScope.launch {
            try {
                val bookmark = _bookmarks.value.find { 
                    it.bookId == bookId && it.pageNumber == pageNumber 
                }
                if (bookmark != null) {
                    bookmarkRepository.deleteBookmark(bookmark)
                    Log.d(TAG, "Bookmark deleted: book $bookId, page $pageNumber")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete bookmark by page", e)
            }
        }
    }
    
    /**
     * 检查指定页是否有书签
     */
    fun hasBookmark(pageNumber: Int): Boolean {
        return _bookmarks.value.any { it.pageNumber == pageNumber }
    }
    
    /**
     * 获取指定页的书签
     */
    fun getBookmarkByPage(pageNumber: Int): Bookmark? {
        return _bookmarks.value.find { it.pageNumber == pageNumber }
    }
    
    /**
     * 清除当前状态
     */
    fun clear() {
        _bookmarks.value = emptyList()
        currentBookId = null
    }
}
