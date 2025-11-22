package takagi.ru.paysage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import takagi.ru.paysage.ui.screens.ReaderUiState

/**
 * M3E 阅读器 ViewModel
 * 
 * 负责管理阅读器的状态和业务逻辑
 */
class M3EReaderViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    
    /**
     * 加载书籍
     */
    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // TODO: 实际的书籍加载逻辑将在后续任务中实现
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalPages = 100, // 临时数据
                        chapterTitle = "第一章" // 临时数据
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }
    
    /**
     * 切换工具栏显示状态
     */
    fun toggleToolbar() {
        _uiState.update {
            val newShowState = !it.showTopBar
            it.copy(
                showTopBar = newShowState,
                showBottomBar = newShowState
            )
        }
    }
    
    /**
     * 翻到下一页
     */
    fun nextPage() {
        _uiState.update {
            if (it.currentPage < it.totalPages - 1) {
                it.copy(currentPage = it.currentPage + 1)
            } else {
                it
            }
        }
    }
    
    /**
     * 翻到上一页
     */
    fun previousPage() {
        _uiState.update {
            if (it.currentPage > 0) {
                it.copy(currentPage = it.currentPage - 1)
            } else {
                it
            }
        }
    }
    
    /**
     * 跳转到指定页
     */
    fun goToPage(page: Int) {
        _uiState.update {
            it.copy(
                currentPage = page.coerceIn(0, it.totalPages - 1)
            )
        }
    }
    
    /**
     * 显示快速设置面板
     */
    fun showQuickSettings() {
        _uiState.update { it.copy(showQuickSettings = true) }
    }
    
    /**
     * 隐藏快速设置面板
     */
    fun hideQuickSettings() {
        _uiState.update { it.copy(showQuickSettings = false) }
    }
    
    /**
     * 显示阅读设置对话框
     */
    fun showReadingSettings() {
        _uiState.update { it.copy(showReadingSettings = true) }
    }
    
    /**
     * 隐藏阅读设置对话框
     */
    fun hideReadingSettings() {
        _uiState.update { it.copy(showReadingSettings = false) }
    }
    
    /**
     * 切换触摸区域覆盖层
     */
    fun toggleTouchZoneOverlay() {
        _uiState.update { it.copy(showTouchZoneOverlay = !it.showTouchZoneOverlay) }
    }
}
