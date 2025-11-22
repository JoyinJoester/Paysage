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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import takagi.ru.paysage.reader.touch.TouchAction
import takagi.ru.paysage.reader.touch.TouchZone

/**
 * 触摸区域配置管理器
 * 
 * 负责保存和恢复触摸区域配置
 * 使用 DataStore 进行持久化存储
 */
class TouchZoneConfigManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TouchZoneConfigManager"
        private const val DATASTORE_NAME = "touch_zone_config"
        private const val KEY_TOUCH_CONFIG = "touch_zone_config"
        
        // DataStore 扩展
        private val Context.touchConfigDataStore: DataStore<Preferences> by preferencesDataStore(
            name = DATASTORE_NAME
        )
        
        // JSON 序列化器
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }
    
    private val dataStore = context.touchConfigDataStore
    
    /**
     * 保存触摸区域配置
     */
    suspend fun saveConfig(config: TouchZoneConfigData) {
        try {
            val configJson = json.encodeToString(config)
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(KEY_TOUCH_CONFIG)] = configJson
            }
            Log.d(TAG, "Saved touch zone config")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving touch zone config", e)
        }
    }
    
    /**
     * 获取触摸区域配置
     */
    suspend fun getConfig(): TouchZoneConfigData {
        return try {
            val preferences = dataStore.data.first()
            val configJson = preferences[stringPreferencesKey(KEY_TOUCH_CONFIG)]
            
            if (configJson != null) {
                json.decodeFromString<TouchZoneConfigData>(configJson)
            } else {
                TouchZoneConfigData.Default
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting touch zone config", e)
            TouchZoneConfigData.Default
        }
    }
    
    /**
     * 获取触摸区域配置 Flow
     */
    fun getConfigFlow(): Flow<TouchZoneConfigData> {
        return dataStore.data.map { preferences ->
            try {
                val configJson = preferences[stringPreferencesKey(KEY_TOUCH_CONFIG)]
                if (configJson != null) {
                    json.decodeFromString<TouchZoneConfigData>(configJson)
                } else {
                    TouchZoneConfigData.Default
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing touch zone config", e)
                TouchZoneConfigData.Default
            }
        }
    }
    
    /**
     * 重置为默认配置
     */
    suspend fun resetToDefault() {
        saveConfig(TouchZoneConfigData.Default)
    }
    
    /**
     * 导出配置为 JSON 字符串
     */
    suspend fun exportConfig(): String {
        val config = getConfig()
        return json.encodeToString(config)
    }
    
    /**
     * 从 JSON 字符串导入配置
     */
    suspend fun importConfig(configJson: String): Boolean {
        return try {
            val config = json.decodeFromString<TouchZoneConfigData>(configJson)
            saveConfig(config)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing touch zone config", e)
            false
        }
    }
}

/**
 * 触摸区域配置数据
 */
@Serializable
data class TouchZoneConfigData(
    val zoneActions: Map<String, String>,
    val enableTouchZones: Boolean = true,
    val showTouchZoneOverlay: Boolean = false
) {
    companion object {
        /**
         * 默认配置
         */
        val Default = TouchZoneConfigData(
            zoneActions = mapOf(
                TouchZone.TOP_LEFT.name to TouchAction.PREVIOUS_PAGE.name,
                TouchZone.TOP_CENTER.name to TouchAction.TOGGLE_TOOLBAR.name,
                TouchZone.TOP_RIGHT.name to TouchAction.NEXT_PAGE.name,
                TouchZone.MIDDLE_LEFT.name to TouchAction.PREVIOUS_PAGE.name,
                TouchZone.CENTER.name to TouchAction.TOGGLE_TOOLBAR.name,
                TouchZone.MIDDLE_RIGHT.name to TouchAction.NEXT_PAGE.name,
                TouchZone.BOTTOM_LEFT.name to TouchAction.PREVIOUS_PAGE.name,
                TouchZone.BOTTOM_CENTER.name to TouchAction.TOGGLE_TOOLBAR.name,
                TouchZone.BOTTOM_RIGHT.name to TouchAction.NEXT_PAGE.name
            ),
            enableTouchZones = true,
            showTouchZoneOverlay = false
        )
        
        /**
         * 左右翻页配置
         */
        val LeftRightFlip = TouchZoneConfigData(
            zoneActions = mapOf(
                TouchZone.TOP_LEFT.name to TouchAction.PREVIOUS_PAGE.name,
                TouchZone.TOP_CENTER.name to TouchAction.TOGGLE_TOOLBAR.name,
                TouchZone.TOP_RIGHT.name to TouchAction.NEXT_PAGE.name,
                TouchZone.MIDDLE_LEFT.name to TouchAction.PREVIOUS_PAGE.name,
                TouchZone.CENTER.name to TouchAction.NONE.name,
                TouchZone.MIDDLE_RIGHT.name to TouchAction.NEXT_PAGE.name,
                TouchZone.BOTTOM_LEFT.name to TouchAction.PREVIOUS_PAGE.name,
                TouchZone.BOTTOM_CENTER.name to TouchAction.TOGGLE_TOOLBAR.name,
                TouchZone.BOTTOM_RIGHT.name to TouchAction.NEXT_PAGE.name
            ),
            enableTouchZones = true,
            showTouchZoneOverlay = false
        )
        
        /**
         * 简单配置（只有中心区域）
         */
        val Simple = TouchZoneConfigData(
            zoneActions = mapOf(
                TouchZone.TOP_LEFT.name to TouchAction.NONE.name,
                TouchZone.TOP_CENTER.name to TouchAction.NONE.name,
                TouchZone.TOP_RIGHT.name to TouchAction.NONE.name,
                TouchZone.MIDDLE_LEFT.name to TouchAction.PREVIOUS_PAGE.name,
                TouchZone.CENTER.name to TouchAction.TOGGLE_TOOLBAR.name,
                TouchZone.MIDDLE_RIGHT.name to TouchAction.NEXT_PAGE.name,
                TouchZone.BOTTOM_LEFT.name to TouchAction.NONE.name,
                TouchZone.BOTTOM_CENTER.name to TouchAction.NONE.name,
                TouchZone.BOTTOM_RIGHT.name to TouchAction.NONE.name
            ),
            enableTouchZones = true,
            showTouchZoneOverlay = false
        )
    }
    
    /**
     * 获取指定区域的动作
     */
    fun getAction(zone: TouchZone): TouchAction {
        val actionName = zoneActions[zone.name] ?: TouchAction.NONE.name
        return try {
            TouchAction.valueOf(actionName)
        } catch (e: Exception) {
            TouchAction.NONE
        }
    }
    
    /**
     * 设置指定区域的动作
     */
    fun setAction(zone: TouchZone, action: TouchAction): TouchZoneConfigData {
        val newActions = zoneActions.toMutableMap()
        newActions[zone.name] = action.name
        return copy(zoneActions = newActions)
    }
}

/**
 * 全局触摸区域配置管理器
 */
object GlobalTouchZoneConfigManager {
    private lateinit var instance: TouchZoneConfigManager
    
    fun initialize(context: Context) {
        instance = TouchZoneConfigManager(context)
    }
    
    suspend fun saveConfig(config: TouchZoneConfigData) {
        if (::instance.isInitialized) {
            instance.saveConfig(config)
        }
    }
    
    suspend fun getConfig(): TouchZoneConfigData {
        return if (::instance.isInitialized) {
            instance.getConfig()
        } else {
            TouchZoneConfigData.Default
        }
    }
    
    fun getConfigFlow(): Flow<TouchZoneConfigData> {
        return if (::instance.isInitialized) {
            instance.getConfigFlow()
        } else {
            kotlinx.coroutines.flow.flowOf(TouchZoneConfigData.Default)
        }
    }
    
    suspend fun resetToDefault() {
        if (::instance.isInitialized) {
            instance.resetToDefault()
        }
    }
    
    suspend fun exportConfig(): String {
        return if (::instance.isInitialized) {
            instance.exportConfig()
        } else {
            ""
        }
    }
    
    suspend fun importConfig(configJson: String): Boolean {
        return if (::instance.isInitialized) {
            instance.importConfig(configJson)
        } else {
            false
        }
    }
}
