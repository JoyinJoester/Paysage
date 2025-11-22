package takagi.ru.paysage.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.BookSource
import takagi.ru.paysage.data.model.CategoryType
import takagi.ru.paysage.repository.OnlineSourceRepository
import takagi.ru.paysage.repository.SearchResult
import takagi.ru.paysage.repository.ValidationResult

/**
 * 在线书源 ViewModel
 * 管理在线书源的状态和操作
 */
class OnlineSourceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = OnlineSourceRepository(application)
    
    // ========== 状态 ==========
    
    /**
     * 所有书源
     */
    val allSources: StateFlow<List<BookSource>> = repository.getAllSourcesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * 启用的书源
     */
    val enabledSources: StateFlow<List<BookSource>> = repository.getEnabledSourcesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * 当前选中的分类类型
     */
    private val _selectedCategoryType = MutableStateFlow(CategoryType.MANGA)
    val selectedCategoryType: StateFlow<CategoryType> = _selectedCategoryType.asStateFlow()
    
    /**
     * 根据选中的分类类型获取书源
     */
    val sourcesByCategory: StateFlow<List<BookSource>> = selectedCategoryType
        .flatMapLatest { categoryType ->
            repository.getSourcesByCategoryFlow(categoryType)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * 根据选中的分类类型获取启用的书源
     */
    val enabledSourcesByCategory: StateFlow<List<BookSource>> = selectedCategoryType
        .flatMapLatest { categoryType ->
            repository.getEnabledSourcesByCategoryFlow(categoryType)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * UI 状态
     */
    private val _uiState = MutableStateFlow(OnlineSourceUiState())
    val uiState: StateFlow<OnlineSourceUiState> = _uiState.asStateFlow()
    
    /**
     * 搜索结果
     */
    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()
    
    // ========== 操作方法 ==========
    
    /**
     * 设置选中的分类类型
     */
    fun setSelectedCategoryType(categoryType: CategoryType) {
        _selectedCategoryType.value = categoryType
    }
    
    /**
     * 切换分类类型
     */
    fun toggleCategoryType() {
        _selectedCategoryType.value = when (_selectedCategoryType.value) {
            CategoryType.MANGA -> CategoryType.NOVEL
            CategoryType.NOVEL -> CategoryType.MANGA
        }
    }
    
    /**
     * 根据分类类型获取书源
     */
    fun getSourcesByCategory(categoryType: CategoryType): StateFlow<List<BookSource>> {
        return repository.getSourcesByCategoryFlow(categoryType)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
    
    /**
     * 添加书源
     */
    fun addSource(source: BookSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val id = repository.addSource(source)
                if (id > 0) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            message = "书源添加成功"
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "书源添加失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "添加书源时发生错误: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 更新书源
     */
    fun updateSource(source: BookSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                repository.updateSource(source)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "书源更新成功"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "更新书源时发生错误: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 删除书源
     */
    fun deleteSource(source: BookSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                repository.deleteSource(source)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "书源删除成功"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "删除书源时发生错误: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 切换书源启用状态
     */
    fun toggleSourceEnabled(id: Long) {
        viewModelScope.launch {
            try {
                repository.toggleSourceEnabled(id)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "切换书源状态时发生错误: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 更新书源优先级
     */
    fun updateSourcePriority(id: Long, priority: Int) {
        viewModelScope.launch {
            try {
                repository.updateSourcePriority(id, priority)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "更新书源优先级时发生错误: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 验证书源
     */
    fun validateSource(source: BookSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, validationResult = null) }
            
            try {
                val result = repository.validateSource(source)
                _uiState.update { 
                    it.copy(
                        isValidating = false,
                        validationResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isValidating = false,
                        validationResult = ValidationResult(
                            isValid = false,
                            message = "验证失败: ${e.message}"
                        )
                    )
                }
            }
        }
    }
    
    /**
     * 搜索书籍
     */
    fun searchBooks(source: BookSource, query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            _searchResults.value = null
            
            try {
                val result = repository.searchBooks(source, query)
                _searchResults.value = result
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = result.error
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = "搜索失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 清除搜索结果
     */
    fun clearSearchResults() {
        _searchResults.value = null
    }
    
    /**
     * 导入书源
     */
    fun importSources(json: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val result = repository.importSourcesFromJson(json)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "导入完成：成功 ${result.success} 个，失败 ${result.failed} 个",
                        error = if (result.errors.isNotEmpty()) result.errors.joinToString("\n") else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "导入书源时发生错误: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 导出书源
     */
    fun exportSources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val json = repository.exportSourcesToJson()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        exportedJson = json,
                        message = if (json != null) "导出成功" else "导出失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "导出书源时发生错误: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
    
    /**
     * 清除导出的JSON
     */
    fun clearExportedJson() {
        _uiState.update { it.copy(exportedJson = null) }
    }
}

/**
 * 在线书源 UI 状态
 */
data class OnlineSourceUiState(
    val isLoading: Boolean = false,
    val isValidating: Boolean = false,
    val isSearching: Boolean = false,
    val validationResult: ValidationResult? = null,
    val exportedJson: String? = null,
    val message: String? = null,
    val error: String? = null
)
