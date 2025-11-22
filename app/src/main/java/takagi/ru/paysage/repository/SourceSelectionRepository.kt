package takagi.ru.paysage.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 源选择数据仓库
 * 使用 DataStore 持久化本地源配置
 */
class SourceSelectionRepository(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "source_selection_prefs"
        )
        
        private val LOCAL_MANGA_PATH_KEY = stringPreferencesKey("local_manga_path")
        private val LOCAL_READING_PATH_KEY = stringPreferencesKey("local_reading_path")
    }
    
    /**
     * 保存本地漫画路径
     */
    suspend fun saveLocalMangaPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_MANGA_PATH_KEY] = path
        }
    }
    
    /**
     * 保存本地阅读路径
     */
    suspend fun saveLocalReadingPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_READING_PATH_KEY] = path
        }
    }
    
    /**
     * 获取本地漫画路径
     */
    fun getLocalMangaPath(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[LOCAL_MANGA_PATH_KEY]
        }
    }
    
    /**
     * 获取本地阅读路径
     */
    fun getLocalReadingPath(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[LOCAL_READING_PATH_KEY]
        }
    }
    
    /**
     * 清除本地漫画路径
     */
    suspend fun clearLocalMangaPath() {
        context.dataStore.edit { preferences ->
            preferences.remove(LOCAL_MANGA_PATH_KEY)
        }
    }
    
    /**
     * 清除本地阅读路径
     */
    suspend fun clearLocalReadingPath() {
        context.dataStore.edit { preferences ->
            preferences.remove(LOCAL_READING_PATH_KEY)
        }
    }
}
