package takagi.ru.paysage.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import takagi.ru.paysage.data.model.AppSettings
import takagi.ru.paysage.data.model.ReadingDirection
import takagi.ru.paysage.data.model.ReadingMode
import takagi.ru.paysage.data.model.ThemeMode
import takagi.ru.paysage.util.ImageFilter
import java.io.IOException

private const val TAG = "SettingsRepository"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * 设置仓库
 * 使用 DataStore 持久化应用设置
 */
class SettingsRepository(private val context: Context) {
    
    private val dataStore = context.dataStore
    
    // 设置键
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val CUSTOM_PRIMARY_COLOR = longPreferencesKey("custom_primary_color")
        val CUSTOM_SECONDARY_COLOR = longPreferencesKey("custom_secondary_color")
        val CUSTOM_TERTIARY_COLOR = longPreferencesKey("custom_tertiary_color")
        val LANGUAGE = stringPreferencesKey("language")
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val VOLUME_KEY_NAVIGATION = booleanPreferencesKey("volume_key_navigation")
        val SHOW_PROGRESS = booleanPreferencesKey("show_progress")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
        val AUTO_SCAN_ON_START = booleanPreferencesKey("auto_scan_on_start")
        val LAST_SCANNED_FOLDER_URI = stringPreferencesKey("last_scanned_folder_uri")
        val ENABLE_ANALYTICS = booleanPreferencesKey("enable_analytics")
        val LAST_VERSION = stringPreferencesKey("last_version")
        
        // 图片过滤器设置
        val FILTER_BRIGHTNESS = floatPreferencesKey("filter_brightness")
        val FILTER_CONTRAST = floatPreferencesKey("filter_contrast")
        val FILTER_SATURATION = floatPreferencesKey("filter_saturation")
        val FILTER_HUE = floatPreferencesKey("filter_hue")
        val FILTER_GRAYSCALE = booleanPreferencesKey("filter_grayscale")
        val FILTER_INVERT = booleanPreferencesKey("filter_invert")
        
        // 触摸区域设置
        val TOUCH_ZONE_ENABLED = booleanPreferencesKey("touch_zone_enabled")
        val TOUCH_ZONE_HAPTIC_FEEDBACK = booleanPreferencesKey("touch_zone_haptic_feedback")
        val TOUCH_ZONE_DEBUG_MODE = booleanPreferencesKey("touch_zone_debug_mode")
        
        // 页面过渡动画设置
        val PAGE_TRANSITION_MODE = stringPreferencesKey("page_transition_mode")
        val ANIMATION_SPEED = stringPreferencesKey("animation_speed")
        val EDGE_SENSITIVITY = stringPreferencesKey("edge_sensitivity")
        val ENABLE_TRANSITION_EFFECTS = booleanPreferencesKey("enable_transition_effects")
        val ENABLE_TRANSITION_HAPTIC = booleanPreferencesKey("enable_transition_haptic")
        
        // Legado 翻页模式设置
        val PAGE_FLIP_MODE = stringPreferencesKey("page_flip_mode")
        val PAGE_FLIP_ANIMATION_SPEED = intPreferencesKey("page_flip_animation_speed")
    }
    
    /**
     * 获取设置 Flow
     */
    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferencesToSettings(preferences)
        }
    
    /**
     * 更新主题模式
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
        Log.d(TAG, "Theme mode updated to: $themeMode")
    }
    
    /**
     * 更新配色方案
     */
    suspend fun updateColorScheme(colorScheme: takagi.ru.paysage.data.model.ColorScheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_SCHEME] = colorScheme.name
        }
        Log.d(TAG, "Color scheme updated to: $colorScheme")
    }
    
    /**
     * 更新动态颜色开关
     */
    suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] = enabled
        }
        Log.d(TAG, "Dynamic color enabled: $enabled")
    }
    
    /**
     * 更新语言
     */
    suspend fun updateLanguage(language: takagi.ru.paysage.data.model.Language) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language.name
        }
        Log.d(TAG, "Language updated to: $language")
    }
    
    /**
     * 更新阅读方向
     */
    suspend fun updateReadingDirection(direction: ReadingDirection) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.READING_DIRECTION] = direction.name
        }
        Log.d(TAG, "Reading direction updated to: $direction")
    }
    
    /**
     * 更新阅读模式
     */
    suspend fun updateReadingMode(mode: ReadingMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.READING_MODE] = mode.name
        }
        Log.d(TAG, "Reading mode updated to: $mode")
    }
    
    /**
     * 更新图片过滤器
     */
    suspend fun updateImageFilter(filter: ImageFilter) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FILTER_BRIGHTNESS] = filter.brightness
            preferences[PreferencesKeys.FILTER_CONTRAST] = filter.contrast
            preferences[PreferencesKeys.FILTER_SATURATION] = filter.saturation
            preferences[PreferencesKeys.FILTER_HUE] = filter.hue
            preferences[PreferencesKeys.FILTER_GRAYSCALE] = filter.grayscale
            preferences[PreferencesKeys.FILTER_INVERT] = filter.invert
        }
        Log.d(TAG, "Image filter updated: $filter")
    }
    
    /**
     * 更新保持屏幕常亮
     */
    suspend fun updateKeepScreenOn(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = enabled
        }
        Log.d(TAG, "Keep screen on: $enabled")
    }
    
    /**
     * 更新音量键翻页
     */
    suspend fun updateVolumeKeyNavigation(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOLUME_KEY_NAVIGATION] = enabled
        }
        Log.d(TAG, "Volume key navigation: $enabled")
    }
    
    /**
     * 更新显示进度
     */
    suspend fun updateShowProgress(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_PROGRESS] = enabled
        }
        Log.d(TAG, "Show progress: $enabled")
    }
    
    /**
     * 更新网格列数
     */
    suspend fun updateGridColumns(columns: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
        Log.d(TAG, "Grid columns: $columns")
    }
    
    /**
     * 更新库布局
     */
    suspend fun updateLibraryLayout(layout: takagi.ru.paysage.data.model.LibraryLayout) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIBRARY_LAYOUT] = layout.name
        }
        Log.d(TAG, "Library layout updated to: $layout")
    }
    
    /**
     * 更新启动自动扫描
     */
    suspend fun updateAutoScanOnStart(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCAN_ON_START] = enabled
        }
        Log.d(TAG, "Auto scan on start: $enabled")
    }
    
    /**
     * 更新分析开关
     */
    suspend fun updateEnableAnalytics(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_ANALYTICS] = enabled
        }
        Log.d(TAG, "Enable analytics: $enabled")
    }
    
    /**
     * 更新最后版本
     */
    suspend fun updateLastVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_VERSION] = version
        }
        Log.d(TAG, "Last version: $version")
    }
    
    /**
     * 更新触摸区域启用状态
     */
    suspend fun updateTouchZoneEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOUCH_ZONE_ENABLED] = enabled
        }
        Log.d(TAG, "Touch zone enabled: $enabled")
    }
    
    /**
     * 更新触摸区域触觉反馈
     */
    suspend fun updateTouchZoneHapticFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOUCH_ZONE_HAPTIC_FEEDBACK] = enabled
        }
        Log.d(TAG, "Touch zone haptic feedback: $enabled")
    }
    
    /**
     * 更新触摸区域调试模式
     */
    suspend fun updateTouchZoneDebugMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOUCH_ZONE_DEBUG_MODE] = enabled
        }
        Log.d(TAG, "Touch zone debug mode: $enabled")
    }
    
    /**
     * 清除所有设置
     */
    suspend fun clearAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d(TAG, "All settings cleared")
    }
    
    /**
     * 映射 Preferences 到 AppSettings
     */
    private fun mapPreferencesToSettings(preferences: Preferences): AppSettings {
        return AppSettings(
            themeMode = preferences[PreferencesKeys.THEME_MODE]?.let {
                try {
                    ThemeMode.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    ThemeMode.SYSTEM
                }
            } ?: ThemeMode.SYSTEM,
            
            colorScheme = preferences[PreferencesKeys.COLOR_SCHEME]?.let {
                try {
                    takagi.ru.paysage.data.model.ColorScheme.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    takagi.ru.paysage.data.model.ColorScheme.DEFAULT
                }
            } ?: takagi.ru.paysage.data.model.ColorScheme.DEFAULT,
            
            dynamicColorEnabled = preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] ?: true,
            customPrimaryColor = preferences[PreferencesKeys.CUSTOM_PRIMARY_COLOR] ?: 0xFFFF6B35,
            customSecondaryColor = preferences[PreferencesKeys.CUSTOM_SECONDARY_COLOR] ?: 0xFF6A4C9C,
            customTertiaryColor = preferences[PreferencesKeys.CUSTOM_TERTIARY_COLOR] ?: 0xFF00BFA5,
            
            language = preferences[PreferencesKeys.LANGUAGE]?.let {
                try {
                    takagi.ru.paysage.data.model.Language.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    takagi.ru.paysage.data.model.Language.SYSTEM
                }
            } ?: takagi.ru.paysage.data.model.Language.SYSTEM,
            
            readingDirection = preferences[PreferencesKeys.READING_DIRECTION]?.let {
                try {
                    ReadingDirection.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    ReadingDirection.LEFT_TO_RIGHT
                }
            } ?: ReadingDirection.LEFT_TO_RIGHT,
            
            readingMode = preferences[PreferencesKeys.READING_MODE]?.let {
                try {
                    ReadingMode.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    ReadingMode.SINGLE_PAGE
                }
            } ?: ReadingMode.SINGLE_PAGE,
            
            keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
            volumeKeyNavigation = preferences[PreferencesKeys.VOLUME_KEY_NAVIGATION] ?: true,
            
            imageFilter = ImageFilter(
                brightness = preferences[PreferencesKeys.FILTER_BRIGHTNESS] ?: 0f,
                contrast = preferences[PreferencesKeys.FILTER_CONTRAST] ?: 1f,
                saturation = preferences[PreferencesKeys.FILTER_SATURATION] ?: 1f,
                hue = preferences[PreferencesKeys.FILTER_HUE] ?: 0f,
                grayscale = preferences[PreferencesKeys.FILTER_GRAYSCALE] ?: false,
                invert = preferences[PreferencesKeys.FILTER_INVERT] ?: false
            ),
            
            showProgress = preferences[PreferencesKeys.SHOW_PROGRESS] ?: true,
            gridColumns = preferences[PreferencesKeys.GRID_COLUMNS] ?: 3,
            libraryLayout = preferences[PreferencesKeys.LIBRARY_LAYOUT]?.let {
                try {
                    takagi.ru.paysage.data.model.LibraryLayout.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY
                }
            } ?: takagi.ru.paysage.data.model.LibraryLayout.COVER_ONLY,
            autoScanOnStart = preferences[PreferencesKeys.AUTO_SCAN_ON_START] ?: false,
            lastScannedFolderUri = preferences[PreferencesKeys.LAST_SCANNED_FOLDER_URI],
            enableAnalytics = preferences[PreferencesKeys.ENABLE_ANALYTICS] ?: false,
            lastVersion = preferences[PreferencesKeys.LAST_VERSION] ?: ""
        )
    }
    
    /**
     * 保存最后扫描的文件夹URI
     */
    suspend fun updateLastScannedFolderUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferencesKeys.LAST_SCANNED_FOLDER_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.LAST_SCANNED_FOLDER_URI)
            }
        }
        Log.d(TAG, "Last scanned folder URI updated: $uri")
    }
    
    /**
     * 更新页面过渡模式
     */
    suspend fun updatePageTransitionMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAGE_TRANSITION_MODE] = mode
        }
        Log.d(TAG, "Page transition mode updated to: $mode")
    }
    
    /**
     * 更新动画速度
     */
    suspend fun updateAnimationSpeed(speed: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATION_SPEED] = speed
        }
        Log.d(TAG, "Animation speed updated to: $speed")
    }
    
    /**
     * 更新边缘灵敏度
     */
    suspend fun updateEdgeSensitivity(sensitivity: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EDGE_SENSITIVITY] = sensitivity
        }
        Log.d(TAG, "Edge sensitivity updated to: $sensitivity")
    }
    
    /**
     * 更新过渡效果开关
     */
    suspend fun updateEnableTransitionEffects(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_TRANSITION_EFFECTS] = enabled
        }
        Log.d(TAG, "Enable transition effects: $enabled")
    }
    
    /**
     * 更新过渡触觉反馈
     */
    suspend fun updateEnableTransitionHaptic(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_TRANSITION_HAPTIC] = enabled
        }
        Log.d(TAG, "Enable transition haptic: $enabled")
    }
    
    /**
     * 更新翻页模式
     */
    suspend fun updatePageFlipMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAGE_FLIP_MODE] = mode
        }
        Log.d(TAG, "Page flip mode: $mode")
    }
    
    /**
     * 更新翻页动画速度
     */
    suspend fun updatePageFlipAnimationSpeed(speed: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAGE_FLIP_ANIMATION_SPEED] = speed
        }
        Log.d(TAG, "Page flip animation speed: $speed")
    }
}
