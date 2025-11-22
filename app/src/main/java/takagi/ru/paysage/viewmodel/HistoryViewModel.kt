package takagi.ru.paysage.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.HistoryItem
import takagi.ru.paysage.repository.HistoryRepository

/**
 * 阅读历史记录ViewModel
 */
class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "HistoryViewModel"
    }
    
    // 历史记录列表
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // 选中的历史记录项（用于操作菜单）
    private val _selectedItem = MutableStateFlow<HistoryItem?>(null)
    val selectedItem: StateFlow<HistoryItem?> = _selectedItem.asStateFlow()
    
    // 显示清空确认对话框
    private val _showClearConfirmDialog = MutableStateFlow(false)
    val showClearConfirmDialog: StateFlow<Boolean> = _showClearConfirmDialog.asStateFlow()
    
    init {
        loadHistory()
    }
    
    /**
     * 加载阅读历史记录
     */
    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllHistory()
                    .catch { e ->
                        Log.e(TAG, "Failed to load reading history", e)
                        _error.value = "加载阅读记录失败: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { items ->
                        _historyItems.value = items
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load reading history", e)
                _error.value = "加载阅读记录失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 添加或更新阅读历史
     */
    fun addOrUpdateHistory(item: HistoryItem) {
        viewModelScope.launch {
            try {
                repository.addOrUpdateHistory(item)
                Log.d(TAG, "Added/Updated reading history for book: ${item.bookId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add/update reading history", e)
                _error.value = "保存阅读记录失败: ${e.message}"
            }
        }
    }
    
    /**
     * 删除单条历史记录
     */
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteHistory(id)
                Log.d(TAG, "Deleted history item: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete history item", e)
                _error.value = "删除失败: ${e.message}"
            }
        }
    }
    
    /**
     * 清空所有历史记录
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllHistory()
                Log.d(TAG, "Cleared all reading history")
                _showClearConfirmDialog.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear history", e)
                _error.value = "清空失败: ${e.message}"
            }
        }
    }
    
    /**
     * 更新阅读进度
     */
    fun updateReadingProgress(
        id: Long, 
        progress: Float, 
        currentPage: Int
    ) {
        viewModelScope.launch {
            try {
                repository.updateReadingProgress(
                    id = id,
                    progress = progress,
                    currentPage = currentPage,
                    lastReadTime = System.currentTimeMillis()
                )
                Log.d(TAG, "Updated reading progress: $progress")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update reading progress", e)
            }
        }
    }
    
    /**
     * 选择历史记录项
     */
    fun selectItem(item: HistoryItem?) {
        _selectedItem.value = item
    }
    
    /**
     * 显示清空确认对话框
     */
    fun showClearConfirmDialog() {
        _showClearConfirmDialog.value = true
    }
    
    /**
     * 隐藏清空确认对话框
     */
    fun hideClearConfirmDialog() {
        _showClearConfirmDialog.value = false
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }
}
