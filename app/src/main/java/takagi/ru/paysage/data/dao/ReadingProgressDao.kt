package takagi.ru.paysage.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.paysage.data.model.ReadingProgress

/**
 * 阅读进度 DAO
 */
@Dao
interface ReadingProgressDao {
    
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun getProgressFlow(bookId: Long): Flow<ReadingProgress?>
    
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun getProgress(bookId: Long): ReadingProgress?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ReadingProgress)
    
    @Update
    suspend fun updateProgress(progress: ReadingProgress)
    
    @Delete
    suspend fun deleteProgress(progress: ReadingProgress)
    
    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteProgressByBookId(bookId: Long)
    
    @Query("UPDATE reading_progress SET currentPage = :page, progress = :progress, updatedAt = :timestamp WHERE bookId = :bookId")
    suspend fun updateCurrentPage(bookId: Long, page: Int, progress: Float, timestamp: Long)
    
    @Query("UPDATE reading_progress SET zoomLevel = :zoom, scrollX = :x, scrollY = :y WHERE bookId = :bookId")
    suspend fun updateViewState(bookId: Long, zoom: Float, x: Float, y: Float)
}
