package takagi.ru.paysage.reader.progress

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 阅读进度管理器
 * 
 * 负责保存和恢复阅读进度
 * 使用 DataStore 进行持久化存储
 */
class ReadingProgressManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ReadingProgressManager"
        private const val DATASTORE_NAME = "reading_progress"
        
        // DataStore 扩展
        private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
            name = DATASTORE_NAME
        )
    }
    
    private val dataStore = context.progressDataStore
    
    /**
     * 保存阅读进度
     */
    suspend fun saveProgress(bookId: String, progress: ReadingProgress) {
        try {
            dataStore.edit { preferences ->
                val prefix = "book_${bookId}_"
                preferences[stringPreferencesKey("${prefix}chapter_id")] = progress.chapterId
                preferences[intPreferencesKey("${prefix}page_index")] = progress.pageIndex
                preferences[intPreferencesKey("${prefix}total_pages")] = progress.totalPages
                preferences[longPreferencesKey("${prefix}timestamp")] = progress.timestamp
                preferences[intPreferencesKey("${prefix}scroll_offset")] = progress.scrollOffset
            }
            Log.d(TAG, "Saved progress for book $bookId: page ${progress.pageIndex}/${progress.totalPages}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving progress for book $bookId", e)
        }
    }
    
    /**
     * 获取阅读进度
     */
    suspend fun getProgress(bookId: String): ReadingProgress? {
        return try {
            val preferences = dataStore.data.first()
            val prefix = "book_${bookId}_"
            
            val chapterId = preferences[stringPreferencesKey("${prefix}chapter_id")]
            val pageIndex = preferences[intPreferencesKey("${prefix}page_index")]
            val totalPages = preferences[intPreferencesKey("${prefix}total_pages")]
            val timestamp = preferences[longPreferencesKey("${prefix}timestamp")]
            val scrollOffset = preferences[intPreferencesKey("${prefix}scroll_offset")]
            
            if (chapterId != null && pageIndex != null && totalPages != null && timestamp != null) {
                ReadingProgress(
                    chapterId = chapterId,
                    pageIndex = pageIndex,
                    totalPages = totalPages,
                    timestamp = timestamp,
                    scrollOffset = scrollOffset ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting progress for book $bookId", e)
            null
        }
    }
    
    /**
     * 获取阅读进度 Flow
     */
    fun getProgressFlow(bookId: String): Flow<ReadingProgress?> {
        return dataStore.data.map { preferences ->
            val prefix = "book_${bookId}_"
            
            val chapterId = preferences[stringPreferencesKey("${prefix}chapter_id")]
            val pageIndex = preferences[intPreferencesKey("${prefix}page_index")]
            val totalPages = preferences[intPreferencesKey("${prefix}total_pages")]
            val timestamp = preferences[longPreferencesKey("${prefix}timestamp")]
            val scrollOffset = preferences[intPreferencesKey("${prefix}scroll_offset")]
            
            if (chapterId != null && pageIndex != null && totalPages != null && timestamp != null) {
                ReadingProgress(
                    chapterId = chapterId,
                    pageIndex = pageIndex,
                    totalPages = totalPages,
                    timestamp = timestamp,
                    scrollOffset = scrollOffset ?: 0
                )
            } else {
                null
            }
        }
    }
    
    /**
     * 删除阅读进度
     */
    suspend fun deleteProgress(bookId: String) {
        try {
            dataStore.edit { preferences ->
                val prefix = "book_${bookId}_"
                preferences.remove(stringPreferencesKey("${prefix}chapter_id"))
                preferences.remove(intPreferencesKey("${prefix}page_index"))
                preferences.remove(intPreferencesKey("${prefix}total_pages"))
                preferences.remove(longPreferencesKey("${prefix}timestamp"))
                preferences.remove(intPreferencesKey("${prefix}scroll_offset"))
            }
            Log.d(TAG, "Deleted progress for book $bookId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting progress for book $bookId", e)
        }
    }
    
    /**
     * 清除所有阅读进度
     */
    suspend fun clearAllProgress() {
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
            Log.d(TAG, "Cleared all reading progress")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all progress", e)
        }
    }
}

/**
 * 阅读进度数据类
 */
data class ReadingProgress(
    val chapterId: String,
    val pageIndex: Int,
    val totalPages: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val scrollOffset: Int = 0
) {
    /**
     * 计算阅读进度百分比
     */
    val progressPercentage: Float
        get() = if (totalPages > 0) {
            (pageIndex.toFloat() / totalPages.toFloat()) * 100f
        } else {
            0f
        }
    
    /**
     * 是否已完成阅读
     */
    val isCompleted: Boolean
        get() = pageIndex >= totalPages - 1
}

/**
 * 全局阅读进度管理器
 */
object GlobalReadingProgressManager {
    private lateinit var instance: ReadingProgressManager
    
    fun initialize(context: Context) {
        instance = ReadingProgressManager(context)
    }
    
    suspend fun saveProgress(bookId: String, progress: ReadingProgress) {
        if (::instance.isInitialized) {
            instance.saveProgress(bookId, progress)
        }
    }
    
    suspend fun getProgress(bookId: String): ReadingProgress? {
        return if (::instance.isInitialized) {
            instance.getProgress(bookId)
        } else {
            null
        }
    }
    
    fun getProgressFlow(bookId: String): Flow<ReadingProgress?> {
        return if (::instance.isInitialized) {
            instance.getProgressFlow(bookId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }
    
    suspend fun deleteProgress(bookId: String) {
        if (::instance.isInitialized) {
            instance.deleteProgress(bookId)
        }
    }
    
    suspend fun clearAllProgress() {
        if (::instance.isInitialized) {
            instance.clearAllProgress()
        }
    }
}
