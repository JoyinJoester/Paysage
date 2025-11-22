package takagi.ru.paysage.reader.config

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 翻页模式配置管理器
 * 
 * 负责保存和恢复翻页模式配置
 * 支持全局配置和书籍特定配置
 */
class PageFlipConfigManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PageFlipConfigManager"
        private const val DATASTORE_NAME = "page_flip_config"
        private const val KEY_GLOBAL_MODE = "global_page_flip_mode"
        private const val KEY_GLOBAL_ANIMATION_DURATION = "global_animation_duration"
        
        // DataStore 扩展
        private val Context.pageFlipConfigDataStore: DataStore<Preferences> by preferencesDataStore(
            name = DATASTORE_NAME
        )
    }
    
    private val dataStore = context.pageFlipConfigDataStore
    
    /**
     * 保存全局翻页模式
     */
    suspend fun saveGlobalMode(mode: String) {
        try {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(KEY_GLOBAL_MODE)] = mode
            }
            Log.d(TAG, "Saved global page flip mode: $mode")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving global page flip mode", e)
        }
    }
    
    /**
     * 获取全局翻页模式
     */
    suspend fun getGlobalMode(): String {
        return try {
            val preferences = dataStore.data.first()
            preferences[stringPreferencesKey(KEY_GLOBAL_MODE)] ?: "SLIDE"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting global page flip mode", e)
            "SLIDE"
        }
    }
    
    /**
     * 获取全局翻页模式 Flow
     */
    fun getGlobalModeFlow(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(KEY_GLOBAL_MODE)] ?: "SLIDE"
        }
    }
    
    /**
     * 保存书籍特定翻页模式
     */
    suspend fun saveBookMode(bookId: String, mode: String) {
        try {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("book_${bookId}_mode")] = mode
            }
            Log.d(TAG, "Saved page flip mode for book $bookId: $mode")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving page flip mode for book $bookId", e)
        }
    }
    
    /**
     * 获取书籍特定翻页模式
     * 如果没有设置，返回全局模式
     */
    suspend fun getBookMode(bookId: String): String {
        return try {
            val preferences = dataStore.data.first()
            preferences[stringPreferencesKey("book_${bookId}_mode")] ?: getGlobalMode()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page flip mode for book $bookId", e)
            getGlobalMode()
        }
    }
    
    /**
     * 获取书籍特定翻页模式 Flow
     */
    fun getBookModeFlow(bookId: String): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("book_${bookId}_mode")] 
                ?: preferences[stringPreferencesKey(KEY_GLOBAL_MODE)] 
                ?: "SLIDE"
        }
    }
    
    /**
     * 删除书籍特定翻页模式（使用全局模式）
     */
    suspend fun deleteBookMode(bookId: String) {
        try {
            dataStore.edit { preferences ->
                preferences.remove(stringPreferencesKey("book_${bookId}_mode"))
            }
            Log.d(TAG, "Deleted page flip mode for book $bookId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting page flip mode for book $bookId", e)
        }
    }
    
    /**
     * 保存全局动画时长
     */
    suspend fun saveGlobalAnimationDuration(duration: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(KEY_GLOBAL_ANIMATION_DURATION)] = duration.toString()
            }
            Log.d(TAG, "Saved global animation duration: $duration")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving global animation duration", e)
        }
    }
    
    /**
     * 获取全局动画时长
     */
    suspend fun getGlobalAnimationDuration(): Int {
        return try {
            val preferences = dataStore.data.first()
            preferences[stringPreferencesKey(KEY_GLOBAL_ANIMATION_DURATION)]?.toIntOrNull() ?: 300
        } catch (e: Exception) {
            Log.e(TAG, "Error getting global animation duration", e)
            300
        }
    }
    
    /**
     * 获取全局动画时长 Flow
     */
    fun getGlobalAnimationDurationFlow(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(KEY_GLOBAL_ANIMATION_DURATION)]?.toIntOrNull() ?: 300
        }
    }
    
    /**
     * 获取完整配置
     */
    suspend fun getConfig(): PageFlipConfig {
        return PageFlipConfig(
            globalMode = getGlobalMode(),
            animationDuration = getGlobalAnimationDuration()
        )
    }
    
    /**
     * 保存完整配置
     */
    suspend fun saveConfig(config: PageFlipConfig) {
        saveGlobalMode(config.globalMode)
        saveGlobalAnimationDuration(config.animationDuration)
    }
}

/**
 * 翻页配置数据类
 */
data class PageFlipConfig(
    val globalMode: String = "SLIDE",
    val animationDuration: Int = 300
) {
    companion object {
        /**
         * 默认配置
         */
        val Default = PageFlipConfig()
        
        /**
         * 快速翻页配置
         */
        val Fast = PageFlipConfig(
            globalMode = "SLIDE",
            animationDuration = 200
        )
        
        /**
         * 慢速翻页配置
         */
        val Slow = PageFlipConfig(
            globalMode = "SIMULATION",
            animationDuration = 400
        )
        
        /**
         * 无动画配置
         */
        val NoAnimation = PageFlipConfig(
            globalMode = "NONE",
            animationDuration = 0
        )
    }
}

/**
 * 全局翻页配置管理器
 */
object GlobalPageFlipConfigManager {
    private lateinit var instance: PageFlipConfigManager
    
    fun initialize(context: Context) {
        instance = PageFlipConfigManager(context)
    }
    
    suspend fun saveGlobalMode(mode: String) {
        if (::instance.isInitialized) {
            instance.saveGlobalMode(mode)
        }
    }
    
    suspend fun getGlobalMode(): String {
        return if (::instance.isInitialized) {
            instance.getGlobalMode()
        } else {
            "SLIDE"
        }
    }
    
    fun getGlobalModeFlow(): Flow<String> {
        return if (::instance.isInitialized) {
            instance.getGlobalModeFlow()
        } else {
            kotlinx.coroutines.flow.flowOf("SLIDE")
        }
    }
    
    suspend fun saveBookMode(bookId: String, mode: String) {
        if (::instance.isInitialized) {
            instance.saveBookMode(bookId, mode)
        }
    }
    
    suspend fun getBookMode(bookId: String): String {
        return if (::instance.isInitialized) {
            instance.getBookMode(bookId)
        } else {
            "SLIDE"
        }
    }
    
    fun getBookModeFlow(bookId: String): Flow<String> {
        return if (::instance.isInitialized) {
            instance.getBookModeFlow(bookId)
        } else {
            kotlinx.coroutines.flow.flowOf("SLIDE")
        }
    }
    
    suspend fun deleteBookMode(bookId: String) {
        if (::instance.isInitialized) {
            instance.deleteBookMode(bookId)
        }
    }
    
    suspend fun saveGlobalAnimationDuration(duration: Int) {
        if (::instance.isInitialized) {
            instance.saveGlobalAnimationDuration(duration)
        }
    }
    
    suspend fun getGlobalAnimationDuration(): Int {
        return if (::instance.isInitialized) {
            instance.getGlobalAnimationDuration()
        } else {
            300
        }
    }
    
    fun getGlobalAnimationDurationFlow(): Flow<Int> {
        return if (::instance.isInitialized) {
            instance.getGlobalAnimationDurationFlow()
        } else {
            kotlinx.coroutines.flow.flowOf(300)
        }
    }
    
    suspend fun getConfig(): PageFlipConfig {
        return if (::instance.isInitialized) {
            instance.getConfig()
        } else {
            PageFlipConfig.Default
        }
    }
    
    suspend fun saveConfig(config: PageFlipConfig) {
        if (::instance.isInitialized) {
            instance.saveConfig(config)
        }
    }
}
