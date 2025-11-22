package takagi.ru.paysage.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.paysage.data.model.BookSource
import takagi.ru.paysage.data.model.CategoryType

/**
 * 书源数据访问对象
 */
@Dao
interface BookSourceDao {
    
    /**
     * 获取所有书源
     */
    @Query("SELECT * FROM book_sources ORDER BY priority DESC, addedAt DESC")
    fun getAllSourcesFlow(): Flow<List<BookSource>>
    
    /**
     * 获取所有书源（挂起函数）
     */
    @Query("SELECT * FROM book_sources ORDER BY priority DESC, addedAt DESC")
    suspend fun getAllSources(): List<BookSource>
    
    /**
     * 根据分类类型获取书源
     */
    @Query("SELECT * FROM book_sources WHERE categoryType = :categoryType ORDER BY priority DESC, addedAt DESC")
    fun getSourcesByCategoryFlow(categoryType: CategoryType): Flow<List<BookSource>>
    
    /**
     * 根据分类类型获取书源（挂起函数）
     */
    @Query("SELECT * FROM book_sources WHERE categoryType = :categoryType ORDER BY priority DESC, addedAt DESC")
    suspend fun getSourcesByCategory(categoryType: CategoryType): List<BookSource>
    
    /**
     * 获取启用的书源
     */
    @Query("SELECT * FROM book_sources WHERE isEnabled = 1 ORDER BY priority DESC, addedAt DESC")
    fun getEnabledSourcesFlow(): Flow<List<BookSource>>
    
    /**
     * 获取启用的书源（挂起函数）
     */
    @Query("SELECT * FROM book_sources WHERE isEnabled = 1 ORDER BY priority DESC, addedAt DESC")
    suspend fun getEnabledSources(): List<BookSource>
    
    /**
     * 根据分类类型获取启用的书源
     */
    @Query("""
        SELECT * FROM book_sources 
        WHERE categoryType = :categoryType AND isEnabled = 1 
        ORDER BY priority DESC, addedAt DESC
    """)
    fun getEnabledSourcesByCategoryFlow(categoryType: CategoryType): Flow<List<BookSource>>
    
    /**
     * 根据ID获取书源
     */
    @Query("SELECT * FROM book_sources WHERE id = :id")
    suspend fun getSourceById(id: Long): BookSource?
    
    /**
     * 根据ID获取书源（Flow）
     */
    @Query("SELECT * FROM book_sources WHERE id = :id")
    fun getSourceByIdFlow(id: Long): Flow<BookSource?>
    
    /**
     * 插入书源
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: BookSource): Long
    
    /**
     * 插入多个书源
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<BookSource>)
    
    /**
     * 更新书源
     */
    @Update
    suspend fun updateSource(source: BookSource)
    
    /**
     * 删除书源
     */
    @Delete
    suspend fun deleteSource(source: BookSource)
    
    /**
     * 根据ID删除书源
     */
    @Query("DELETE FROM book_sources WHERE id = :id")
    suspend fun deleteSourceById(id: Long)
    
    /**
     * 切换书源启用状态
     */
    @Query("UPDATE book_sources SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleSourceEnabled(id: Long, isEnabled: Boolean)
    
    /**
     * 更新书源优先级
     */
    @Query("UPDATE book_sources SET priority = :priority WHERE id = :id")
    suspend fun updateSourcePriority(id: Long, priority: Int)
    
    /**
     * 更新书源最后使用时间
     */
    @Query("UPDATE book_sources SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsedAt(id: Long, timestamp: Long)
    
    /**
     * 更新书源统计信息
     */
    @Query("""
        UPDATE book_sources 
        SET totalBooks = :totalBooks, successRate = :successRate 
        WHERE id = :id
    """)
    suspend fun updateSourceStats(id: Long, totalBooks: Int, successRate: Float)
    
    /**
     * 获取书源数量
     */
    @Query("SELECT COUNT(*) FROM book_sources")
    suspend fun getSourceCount(): Int
    
    /**
     * 获取启用的书源数量
     */
    @Query("SELECT COUNT(*) FROM book_sources WHERE isEnabled = 1")
    suspend fun getEnabledSourceCount(): Int
    
    /**
     * 清空所有书源
     */
    @Query("DELETE FROM book_sources")
    suspend fun deleteAllSources()
}
