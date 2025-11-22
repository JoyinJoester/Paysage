package takagi.ru.paysage.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.Bookmark

/**
 * 书签 Repository
 */
class BookmarkRepository(context: Context) {
    
    private val database = PaysageDatabase.getDatabase(context)
    private val bookmarkDao = database.bookmarkDao()
    
    fun getBookmarksByBookIdFlow(bookId: Long): Flow<List<Bookmark>> = 
        bookmarkDao.getBookmarksByBookIdFlow(bookId)
    
    suspend fun getBookmarksByBookId(bookId: Long): List<Bookmark> = 
        bookmarkDao.getBookmarksByBookId(bookId)
    
    suspend fun getBookmarkById(bookmarkId: Long): Bookmark? = 
        bookmarkDao.getBookmarkById(bookmarkId)
    
    suspend fun getBookmarkByPage(bookId: Long, pageNumber: Int): Bookmark? = 
        bookmarkDao.getBookmarkByPage(bookId, pageNumber)
    
    suspend fun insertBookmark(bookmark: Bookmark): Long = 
        bookmarkDao.insertBookmark(bookmark)
    
    suspend fun updateBookmark(bookmark: Bookmark) = 
        bookmarkDao.updateBookmark(bookmark)
    
    suspend fun deleteBookmark(bookmark: Bookmark) = 
        bookmarkDao.deleteBookmark(bookmark)
    
    suspend fun deleteBookmarkById(bookmarkId: Long) = 
        bookmarkDao.deleteBookmarkById(bookmarkId)
    
    suspend fun deleteAllBookmarksForBook(bookId: Long) = 
        bookmarkDao.deleteAllBookmarksForBook(bookId)
    
    suspend fun getBookmarkCount(bookId: Long): Int = 
        bookmarkDao.getBookmarkCount(bookId)
    
    /**
     * 切换书签（添加或删除）
     */
    suspend fun toggleBookmark(bookId: Long, pageNumber: Int, title: String? = null): Boolean {
        val existing = getBookmarkByPage(bookId, pageNumber)
        return if (existing != null) {
            deleteBookmark(existing)
            false // 已删除
        } else {
            val bookmark = Bookmark(
                bookId = bookId,
                pageNumber = pageNumber,
                title = title ?: "第 $pageNumber 页"
            )
            insertBookmark(bookmark)
            true // 已添加
        }
    }
    
    /**
     * 检查某页是否有书签
     */
    suspend fun hasBookmark(bookId: Long, pageNumber: Int): Boolean {
        return getBookmarkByPage(bookId, pageNumber) != null
    }
}
