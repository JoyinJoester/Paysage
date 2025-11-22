package takagi.ru.paysage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import takagi.ru.paysage.data.dao.HistoryDao
import takagi.ru.paysage.data.model.HistoryItem
import takagi.ru.paysage.data.model.toHistoryEntity
import takagi.ru.paysage.data.model.toHistoryItem

/**
 * 阅读历史记录Repository接口
 */
interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryItem>>
    suspend fun getHistoryById(id: Long): HistoryItem?
    suspend fun getHistoryByBookId(bookId: Long): HistoryItem?
    suspend fun addOrUpdateHistory(item: HistoryItem): Long
    suspend fun updateReadingProgress(
        id: Long, 
        progress: Float, 
        currentPage: Int,
        lastReadTime: Long = System.currentTimeMillis()
    )
    suspend fun updateLastReadTime(bookId: Long, lastReadTime: Long = System.currentTimeMillis())
    suspend fun deleteHistory(id: Long)
    suspend fun deleteHistoryByBookId(bookId: Long)
    suspend fun clearAllHistory()
    suspend fun getHistoryCount(): Int
}

/**
 * 阅读历史记录Repository实现类
 */
class HistoryRepositoryImpl(
    private val historyDao: HistoryDao
) : HistoryRepository {
    
    companion object {
        const val MAX_HISTORY_ITEMS = 500
    }
    
    override fun getAllHistory(): Flow<List<HistoryItem>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toHistoryItem() }
        }
    }
    
    override suspend fun getHistoryById(id: Long): HistoryItem? {
        return historyDao.getHistoryById(id)?.toHistoryItem()
    }
    
    override suspend fun getHistoryByBookId(bookId: Long): HistoryItem? {
        return historyDao.getHistoryByBookId(bookId)?.toHistoryItem()
    }
    
    override suspend fun addOrUpdateHistory(item: HistoryItem): Long {
        // 检查是否已存在该书籍的历史记录
        val existing = historyDao.getHistoryByBookId(item.bookId)
        
        return if (existing != null) {
            // 更新现有记录
            val updated = item.copy(id = existing.id)
            historyDao.updateHistory(updated.toHistoryEntity())
            existing.id
        } else {
            // 检查历史记录数量，如果超过限制则删除最旧的记录
            val count = historyDao.getHistoryCount()
            if (count >= MAX_HISTORY_ITEMS) {
                historyDao.deleteOldestHistory(count - MAX_HISTORY_ITEMS + 1)
            }
            
            // 插入新记录
            historyDao.insertHistory(item.toHistoryEntity())
        }
    }
    
    override suspend fun updateReadingProgress(
        id: Long, 
        progress: Float, 
        currentPage: Int,
        lastReadTime: Long
    ) {
        historyDao.updateProgress(id, progress, currentPage, lastReadTime)
    }
    
    override suspend fun updateLastReadTime(bookId: Long, lastReadTime: Long) {
        historyDao.updateLastReadTime(bookId, lastReadTime)
    }
    
    override suspend fun deleteHistory(id: Long) {
        historyDao.deleteHistoryById(id)
    }
    
    override suspend fun deleteHistoryByBookId(bookId: Long) {
        historyDao.deleteHistoryByBookId(bookId)
    }
    
    override suspend fun clearAllHistory() {
        historyDao.deleteAllHistory()
    }
    
    override suspend fun getHistoryCount(): Int {
        return historyDao.getHistoryCount()
    }
}
