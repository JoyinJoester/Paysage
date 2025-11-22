package takagi.ru.paysage.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.ReadingProgress
import takagi.ru.paysage.data.model.ReadingMode
import takagi.ru.paysage.data.model.PageTurnDirection

/**
 * 阅读进度 Repository
 */
class ReadingProgressRepository(context: Context) {
    
    private val database = PaysageDatabase.getDatabase(context)
    private val progressDao = database.readingProgressDao()
    
    fun getProgressFlow(bookId: Long): Flow<ReadingProgress?> = 
        progressDao.getProgressFlow(bookId)
    
    suspend fun getProgress(bookId: Long): ReadingProgress? = 
        progressDao.getProgress(bookId)
    
    suspend fun saveProgress(progress: ReadingProgress) = 
        progressDao.insertProgress(progress)
    
    suspend fun updateCurrentPage(bookId: Long, page: Int, totalPages: Int) {
        val progress = page.toFloat() / totalPages.toFloat()
        progressDao.updateCurrentPage(bookId, page, progress, System.currentTimeMillis())
    }
    
    suspend fun updateViewState(bookId: Long, zoom: Float, scrollX: Float, scrollY: Float) {
        progressDao.updateViewState(bookId, zoom, scrollX, scrollY)
    }
    
    suspend fun updateReadingMode(bookId: Long, mode: ReadingMode) {
        val progress = getProgress(bookId)
        if (progress != null) {
            progressDao.updateProgress(progress.copy(readingMode = mode))
        }
    }
    
    suspend fun updatePageTurnDirection(bookId: Long, direction: PageTurnDirection) {
        val progress = getProgress(bookId)
        if (progress != null) {
            progressDao.updateProgress(progress.copy(pageTurnDirection = direction))
        }
    }
    
    suspend fun deleteProgress(bookId: Long) = 
        progressDao.deleteProgressByBookId(bookId)
    
    /**
     * 创建新的阅读进度
     */
    suspend fun createProgress(bookId: Long, totalPages: Int): ReadingProgress {
        val progress = ReadingProgress(
            bookId = bookId,
            totalPages = totalPages
        )
        saveProgress(progress)
        return progress
    }
    
    /**
     * 获取或创建阅读进度
     */
    suspend fun getOrCreateProgress(bookId: Long, totalPages: Int): ReadingProgress {
        return getProgress(bookId) ?: createProgress(bookId, totalPages)
    }
}
