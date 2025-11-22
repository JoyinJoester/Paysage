package takagi.ru.paysage.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import takagi.ru.paysage.data.model.SyncOptions
import java.io.IOException

private const val TAG = "SyncOptionsRepository"
private val Context.syncOptionsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_options")

/**
 * 同步选项仓库
 * 使用 DataStore 持久化同步选项配置
 */
class SyncOptionsRepository(private val context: Context) {
    
    private val dataStore = context.syncOptionsDataStore
    
    // 同步选项键
    private object Keys {
        val REMOVE_DELETED = booleanPreferencesKey("remove_deleted_files")
        val UPDATE_MODIFIED = booleanPreferencesKey("update_modified_files")
        val GENERATE_THUMBNAILS = booleanPreferencesKey("generate_thumbnails")
        val SCAN_SUBFOLDERS = booleanPreferencesKey("scan_subfolders")
        val SKIP_HIDDEN = booleanPreferencesKey("skip_hidden_folders")
        val PARALLEL_SYNC = booleanPreferencesKey("parallel_sync")
    }
    
    /**
     * 获取同步选项 Flow
     */
    val syncOptionsFlow: Flow<SyncOptions> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading sync options", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SyncOptions(
                removeDeletedFiles = preferences[Keys.REMOVE_DELETED] ?: false,
                updateModifiedFiles = preferences[Keys.UPDATE_MODIFIED] ?: false,
                generateMissingThumbnails = preferences[Keys.GENERATE_THUMBNAILS] ?: false,
                scanSubfolders = preferences[Keys.SCAN_SUBFOLDERS] ?: true,
                skipHiddenFolders = preferences[Keys.SKIP_HIDDEN] ?: true,
                parallelSync = preferences[Keys.PARALLEL_SYNC] ?: false
            )
        }
    
    /**
     * 更新同步选项
     */
    suspend fun updateSyncOptions(options: SyncOptions) {
        dataStore.edit { preferences ->
            preferences[Keys.REMOVE_DELETED] = options.removeDeletedFiles
            preferences[Keys.UPDATE_MODIFIED] = options.updateModifiedFiles
            preferences[Keys.GENERATE_THUMBNAILS] = options.generateMissingThumbnails
            preferences[Keys.SCAN_SUBFOLDERS] = options.scanSubfolders
            preferences[Keys.SKIP_HIDDEN] = options.skipHiddenFolders
            preferences[Keys.PARALLEL_SYNC] = options.parallelSync
        }
        Log.d(TAG, "Sync options updated: $options")
    }
    
    /**
     * 清除同步选项（恢复默认值）
     */
    suspend fun clearSyncOptions() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d(TAG, "Sync options cleared")
    }
}
