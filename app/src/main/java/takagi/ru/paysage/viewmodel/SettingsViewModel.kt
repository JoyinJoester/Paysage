package takagi.ru.paysage.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.AppSettings
import takagi.ru.paysage.data.model.ReadingDirection
import takagi.ru.paysage.data.model.ReadingMode
import takagi.ru.paysage.data.model.ThemeMode
import takagi.ru.paysage.repository.SettingsRepository
import takagi.ru.paysage.util.ImageFilter
import java.io.File

private const val TAG = "SettingsViewModel"

/**
 * 设置视图模型
 * 管理应用设置的状态和业务逻辑
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository = SettingsRepository(application)
    
    // 设置状态
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    // 缓存大小状态
    private val _cacheSize = MutableStateFlow("计算中...")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()
    
    // Activity 重建事件
    private val _recreateActivityEvent = MutableSharedFlow<Unit>(replay = 0)
    val recreateActivityEvent: SharedFlow<Unit> = _recreateActivityEvent.asSharedFlow()
    
    init {
        loadSettings()
        calculateCacheSize()
    }
    
    /**
     * 加载设置
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _settings.value = settings
                Log.d(TAG, "Settings loaded: $settings")
            }
        }
    }
    
    /**
     * 更新主题模式
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                settingsRepository.updateThemeMode(themeMode)
                Log.d(TAG, "Theme mode updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update theme mode", e)
            }
        }
    }
    
    /**
     * 更新配色方案
     */
    fun updateColorScheme(colorScheme: takagi.ru.paysage.data.model.ColorScheme) {
        viewModelScope.launch {
            try {
                settingsRepository.updateColorScheme(colorScheme)
                Log.d(TAG, "Color scheme updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update color scheme", e)
            }
        }
    }
    
    /**
     * 更新动态颜色开关
     */
    fun updateDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateDynamicColorEnabled(enabled)
                Log.d(TAG, "Dynamic color enabled updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update dynamic color enabled", e)
            }
        }
    }
    
    /**
     * 更新语言
     */
    fun updateLanguage(language: takagi.ru.paysage.data.model.Language) {
        viewModelScope.launch {
            try {
                settingsRepository.updateLanguage(language)
                _recreateActivityEvent.emit(Unit) // 触发 Activity 重建以应用新语言
                Log.d(TAG, "Language updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update language", e)
            }
        }
    }
    
    /**
     * 更新阅读方向
     */
    fun updateReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch {
            try {
                settingsRepository.updateReadingDirection(direction)
                Log.d(TAG, "Reading direction updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update reading direction", e)
            }
        }
    }
    
    /**
     * 更新阅读模式
     */
    fun updateReadingMode(mode: ReadingMode) {
        viewModelScope.launch {
            try {
                settingsRepository.updateReadingMode(mode)
                Log.d(TAG, "Reading mode updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update reading mode", e)
            }
        }
    }
    
    /**
     * 更新图片过滤器
     */
    fun updateImageFilter(filter: ImageFilter) {
        viewModelScope.launch {
            try {
                settingsRepository.updateImageFilter(filter)
                Log.d(TAG, "Image filter updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update image filter", e)
            }
        }
    }
    
    /**
     * 更新保持屏幕常亮
     */
    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateKeepScreenOn(enabled)
                Log.d(TAG, "Keep screen on updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update keep screen on", e)
            }
        }
    }
    
    /**
     * 更新音量键翻页
     */
    fun updateVolumeKeyNavigation(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateVolumeKeyNavigation(enabled)
                Log.d(TAG, "Volume key navigation updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update volume key navigation", e)
            }
        }
    }
    
    /**
     * 更新显示进度
     */
    fun updateShowProgress(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateShowProgress(enabled)
                Log.d(TAG, "Show progress updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update show progress", e)
            }
        }
    }
    
    /**
     * 更新网格列数
     */
    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateGridColumns(columns)
                Log.d(TAG, "Grid columns updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update grid columns", e)
            }
        }
    }
    
    /**
     * 更新库布局
     */
    fun updateLibraryLayout(layout: takagi.ru.paysage.data.model.LibraryLayout) {
        viewModelScope.launch {
            try {
                settingsRepository.updateLibraryLayout(layout)
                Log.d(TAG, "Library layout updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update library layout", e)
            }
        }
    }
    
    /**
     * 更新启动自动扫描
     */
    fun updateAutoScanOnStart(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateAutoScanOnStart(enabled)
                Log.d(TAG, "Auto scan on start updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update auto scan on start", e)
            }
        }
    }
    
    /**
     * 更新分析开关
     */
    fun updateEnableAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateEnableAnalytics(enabled)
                Log.d(TAG, "Enable analytics updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update enable analytics", e)
            }
        }
    }
    
    /**
     * 计算缓存大小
     */
    fun calculateCacheSize() {
        viewModelScope.launch {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                val size = calculateDirectorySize(cacheDir)
                _cacheSize.value = formatFileSize(size)
                Log.d(TAG, "Cache size calculated: ${_cacheSize.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to calculate cache size", e)
                _cacheSize.value = "未知"
            }
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                deleteDirectory(cacheDir)
                cacheDir.mkdirs() // 重新创建缓存目录
                calculateCacheSize()
                Log.d(TAG, "Cache cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
            }
        }
    }
    
    /**
     * 重置所有设置
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.clearAllSettings()
                Log.d(TAG, "All settings reset successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset settings", e)
            }
        }
    }
    
    /**
     * 计算目录大小
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size: Long = 0
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }
    
    /**
     * 删除目录
     */
    private fun deleteDirectory(directory: File): Boolean {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                deleteDirectory(file)
            }
        }
        return directory.delete()
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
}
