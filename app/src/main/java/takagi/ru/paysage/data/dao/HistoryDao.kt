package takagi.ru.paysage.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.paysage.data.model.HistoryEntity

/**
 * 阅读历史记录数据访问对象
 */
@Dao
interface HistoryDao {
    
    /**
     * 获取所有阅读历史记录，按最后阅读时间倒序排列
     */
    @Query("SELECT * FROM reading_history ORDER BY last_read_time DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    /**
     * 根据ID获取单条历史记录
     */
    @Query("SELECT * FROM reading_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): HistoryEntity?
    
    /**
     * 根据书籍ID获取历史记录
     */
    @Query("SELECT * FROM reading_history WHERE book_id = :bookId")
    suspend fun getHistoryByBookId(bookId: Long): HistoryEntity?
    
    /**
     * 插入或更新历史记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: HistoryEntity): Long
    
    /**
     * 更新历史记录
     */
    @Update
    suspend fun updateHistory(entity: HistoryEntity)
    
    /**
     * 更新阅读进度和页码
     */
    @Query("""
        UPDATE reading_history 
        SET progress = :progress, 
            current_page = :currentPage,
            last_read_time = :lastReadTime
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: Long, 
        progress: Float, 
        currentPage: Int,
        lastReadTime: Long
    )
    
    /**
     * 更新最后阅读时间
     */
    @Query("UPDATE reading_history SET last_read_time = :lastReadTime WHERE book_id = :bookId")
    suspend fun updateLastReadTime(bookId: Long, lastReadTime: Long)
    
    /**
     * 删除历史记录
     */
    @Delete
    suspend fun deleteHistory(entity: HistoryEntity)
    
    /**
     * 根据ID删除历史记录
     */
    @Query("DELETE FROM reading_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
    
    /**
     * 根据书籍ID删除历史记录
     */
    @Query("DELETE FROM reading_history WHERE book_id = :bookId")
    suspend fun deleteHistoryByBookId(bookId: Long)
    
    /**
     * 删除所有历史记录
     */
    @Query("DELETE FROM reading_history")
    suspend fun deleteAllHistory()
    
    /**
     * 获取历史记录总数
     */
    @Query("SELECT COUNT(*) FROM reading_history")
    suspend fun getHistoryCount(): Int
    
    /**
     * 删除最旧的N条记录
     */
    @Query("""
        DELETE FROM reading_history 
        WHERE id IN (
            SELECT id FROM reading_history 
            ORDER BY last_read_time ASC 
            LIMIT :count
        )
    """)
    suspend fun deleteOldestHistory(count: Int)
}
