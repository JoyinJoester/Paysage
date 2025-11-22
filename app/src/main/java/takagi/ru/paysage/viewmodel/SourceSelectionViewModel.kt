package takagi.ru.paysage.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import takagi.ru.paysage.repository.SourceSelectionRepository
import takagi.ru.paysage.util.UriUtils

/**
 * 源选择 ViewModel
 * 管理源选择页面的状态和业务逻辑
 */
class SourceSelectionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = SourceSelectionRepository(application)
    private val context = application.applicationContext
    
    // 本地漫画路径状态（原始 URI）
    private val _selectedLocalMangaPath = MutableStateFlow<String?>(null)
    // 本地漫画路径（可读格式）
    val selectedLocalMangaPath: StateFlow<String?> = _selectedLocalMangaPath.map { uri ->
        UriUtils.getReadablePath(context, uri)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    // 本地阅读路径状态（原始 URI）
    private val _selectedLocalReadingPath = MutableStateFlow<String?>(null)
    // 本地阅读路径（可读格式）
    val selectedLocalReadingPath: StateFlow<String?> = _selectedLocalReadingPath.map { uri ->
        UriUtils.getReadablePath(context, uri)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // 从 DataStore 加载保存的路径
        loadSavedPaths()
    }
    
    /**
     * 加载保存的路径
     */
    private fun loadSavedPaths() {
        // 使用单独的协程收集每个 Flow
        viewModelScope.launch {
            repository.getLocalMangaPath().collect { path ->
                _selectedLocalMangaPath.value = path
            }
        }
        
        viewModelScope.launch {
            repository.getLocalReadingPath().collect { path ->
                _selectedLocalReadingPath.value = path
            }
        }
    }
    
    /**
     * 更新本地漫画路径
     */
    fun updateLocalMangaPath(path: String) {
        // 直接保存路径，不进行文件系统验证（因为可能是 content:// URI）
        _selectedLocalMangaPath.value = path
        viewModelScope.launch {
            repository.saveLocalMangaPath(path)
        }
        _error.value = null
    }
    
    /**
     * 更新本地阅读路径
     */
    fun updateLocalReadingPath(path: String) {
        // 直接保存路径，不进行文件系统验证（因为可能是 content:// URI）
        _selectedLocalReadingPath.value = path
        viewModelScope.launch {
            repository.saveLocalReadingPath(path)
        }
        _error.value = null
    }
    
    /**
     * 清除本地漫画路径
     */
    fun clearLocalMangaPath() {
        _selectedLocalMangaPath.value = null
        viewModelScope.launch {
            repository.clearLocalMangaPath()
        }
    }
    
    /**
     * 清除本地阅读路径
     */
    fun clearLocalReadingPath() {
        _selectedLocalReadingPath.value = null
        viewModelScope.launch {
            repository.clearLocalReadingPath()
        }
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _error.value = null
    }
}
