package takagi.ru.paysage.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.BookSource
import takagi.ru.paysage.data.model.CategoryType

private const val TAG = "OnlineSourceRepository"

/**
 * 在线书源 Repository
 * 管理在线书源的增删改查
 */
class OnlineSourceRepository(private val context: Context) {
    
    private val database = PaysageDatabase.getDatabase(context)
    private val bookSourceDao = database.bookSourceDao()
    
    // ========== Flow 数据流 ==========
    
    /**
     * 获取所有书源
     */
    fun getAllSourcesFlow(): Flow<List<BookSource>> = bookSourceDao.getAllSourcesFlow()
    
    /**
     * 根据分类类型获取书源
     */
    fun getSourcesByCategoryFlow(categoryType: CategoryType): Flow<List<BookSource>> =
        bookSourceDao.getSourcesByCategoryFlow(categoryType)
    
    /**
     * 获取启用的书源
     */
    fun getEnabledSourcesFlow(): Flow<List<BookSource>> = bookSourceDao.getEnabledSourcesFlow()
    
    /**
     * 根据分类类型获取启用的书源
     */
    fun getEnabledSourcesByCategoryFlow(categoryType: CategoryType): Flow<List<BookSource>> =
        bookSourceDao.getEnabledSourcesByCategoryFlow(categoryType)
    
    /**
     * 根据ID获取书源（Flow）
     */
    fun getSourceByIdFlow(id: Long): Flow<BookSource?> = bookSourceDao.getSourceByIdFlow(id)
    
    // ========== 基本操作 ==========
    
    /**
     * 获取所有书源
     */
    suspend fun getAllSources(): List<BookSource> = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.getAllSources()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all sources", e)
            emptyList()
        }
    }
    
    /**
     * 根据分类类型获取书源
     */
    suspend fun getSourcesByCategory(categoryType: CategoryType): List<BookSource> =
        withContext(Dispatchers.IO) {
            try {
                bookSourceDao.getSourcesByCategory(categoryType)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting sources by category", e)
                emptyList()
            }
        }
    
    /**
     * 获取启用的书源
     */
    suspend fun getEnabledSources(): List<BookSource> = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.getEnabledSources()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting enabled sources", e)
            emptyList()
        }
    }
    
    /**
     * 根据ID获取书源
     */
    suspend fun getSourceById(id: Long): BookSource? = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.getSourceById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting source by id: $id", e)
            null
        }
    }
    
    /**
     * 添加书源
     */
    suspend fun addSource(source: BookSource): Long = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.insertSource(source)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding source: ${source.name}", e)
            -1L
        }
    }
    
    /**
     * 批量添加书源
     */
    suspend fun addSources(sources: List<BookSource>) = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.insertSources(sources)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sources", e)
        }
    }
    
    /**
     * 更新书源
     */
    suspend fun updateSource(source: BookSource) = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.updateSource(source)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating source: ${source.name}", e)
        }
    }
    
    /**
     * 删除书源
     */
    suspend fun deleteSource(source: BookSource) = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.deleteSource(source)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting source: ${source.name}", e)
        }
    }
    
    /**
     * 根据ID删除书源
     */
    suspend fun deleteSourceById(id: Long) = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.deleteSourceById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting source by id: $id", e)
        }
    }
    
    /**
     * 切换书源启用状态
     */
    suspend fun toggleSourceEnabled(id: Long, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.toggleSourceEnabled(id, isEnabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling source enabled: $id", e)
        }
    }
    
    /**
     * 切换书源启用状态（自动取反）
     */
    suspend fun toggleSourceEnabled(id: Long) = withContext(Dispatchers.IO) {
        try {
            val source = bookSourceDao.getSourceById(id)
            if (source != null) {
                bookSourceDao.toggleSourceEnabled(id, !source.isEnabled)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling source enabled: $id", e)
            throw e
        }
    }
    
    /**
     * 更新书源优先级
     */
    suspend fun updateSourcePriority(id: Long, priority: Int) = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.updateSourcePriority(id, priority)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating source priority: $id", e)
        }
    }
    
    /**
     * 更新书源最后使用时间
     */
    suspend fun updateLastUsedAt(id: Long) = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.updateLastUsedAt(id, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last used time: $id", e)
        }
    }
    
    /**
     * 更新书源统计信息
     */
    suspend fun updateSourceStats(id: Long, totalBooks: Int, successRate: Float) =
        withContext(Dispatchers.IO) {
            try {
                bookSourceDao.updateSourceStats(id, totalBooks, successRate)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating source stats: $id", e)
            }
        }
    
    /**
     * 获取书源数量
     */
    suspend fun getSourceCount(): Int = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.getSourceCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting source count", e)
            0
        }
    }
    
    /**
     * 获取启用的书源数量
     */
    suspend fun getEnabledSourceCount(): Int = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.getEnabledSourceCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting enabled source count", e)
            0
        }
    }
    
    /**
     * 清空所有书源
     */
    suspend fun deleteAllSources() = withContext(Dispatchers.IO) {
        try {
            bookSourceDao.deleteAllSources()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all sources", e)
        }
    }
    
    // ========== 高级功能 ==========
    
    /**
     * 导入书源（从JSON）
     * TODO: 实现JSON解析和导入逻辑
     */
    suspend fun importSourcesFromJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            // TODO: 解析JSON并导入书源
            ImportResult(success = 0, failed = 0, errors = listOf("功能尚未实现"))
        } catch (e: Exception) {
            Log.e(TAG, "Error importing sources from JSON", e)
            ImportResult(success = 0, failed = 0, errors = listOf(e.message ?: "未知错误"))
        }
    }
    
    /**
     * 导出书源（到JSON）
     * TODO: 实现JSON序列化和导出逻辑
     */
    suspend fun exportSourcesToJson(): String? = withContext(Dispatchers.IO) {
        try {
            // TODO: 序列化书源到JSON
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting sources to JSON", e)
            null
        }
    }
    
    /**
     * 验证书源可用性
     * TODO: 实现网络请求验证逻辑
     */
    suspend fun validateSource(source: BookSource): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 发送网络请求验证书源
                ValidationResult(isValid = false, message = "功能尚未实现")
            } catch (e: Exception) {
                Log.e(TAG, "Error validating source: ${source.name}", e)
                ValidationResult(isValid = false, message = e.message ?: "验证失败")
            }
        }
    
    /**
     * 搜索书籍（通过书源）
     * TODO: 实现网络搜索逻辑
     */
    suspend fun searchBooks(source: BookSource, query: String): SearchResult =
        withContext(Dispatchers.IO) {
            try {
                // 更新最后使用时间
                updateLastUsedAt(source.id)
                
                // TODO: 发送网络请求搜索书籍
                SearchResult(books = emptyList(), error = "功能尚未实现")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching books: ${source.name}", e)
                SearchResult(books = emptyList(), error = e.message ?: "搜索失败")
            }
        }
}

/**
 * 导入结果
 */
data class ImportResult(
    val success: Int,
    val failed: Int,
    val errors: List<String>
)

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * 搜索结果
 */
data class SearchResult(
    val books: List<OnlineBook>,
    val error: String? = null
)

/**
 * 在线书籍（简化版）
 * TODO: 根据实际需求完善字段
 */
data class OnlineBook(
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val sourceId: Long,
    val sourceUrl: String
)
