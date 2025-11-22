package takagi.ru.paysage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.Folder
import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.repository.FolderRepository
import takagi.ru.paysage.repository.FolderSortOption
import takagi.ru.paysage.ui.components.FolderSortOption as UiFolderSortOption

/**
 * 文件夹编辑 UI 状态
 */
data class FolderEditUiState(
    val isEditMode: Boolean = false,
    val selectedFolders: Set<Long> = emptySet(),
    val sortOption: UiFolderSortOption = UiFolderSortOption.NAME_ASC,
    val showRenameDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showSortDialog: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 文件夹管理ViewModel
 */
class FolderViewModel(
    private val context: android.content.Context,
    private val folderRepository: FolderRepository
) : ViewModel() {
    
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()
    
    private val _createFolderState = MutableStateFlow<CreateFolderState>(CreateFolderState.Idle)
    val createFolderState: StateFlow<CreateFolderState> = _createFolderState.asStateFlow()
    
    // 编辑模式 UI 状态
    private val _editUiState = MutableStateFlow(FolderEditUiState())
    val editUiState: StateFlow<FolderEditUiState> = _editUiState.asStateFlow()
    
    // 保留旧的状态以兼容现有代码
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    private val _selectedFolderIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFolderIds: StateFlow<Set<Long>> = _selectedFolderIds.asStateFlow()
    
    // 重命名状态
    private val _renameFolderState = MutableStateFlow<RenameFolderState>(RenameFolderState.Idle)
    val renameFolderState: StateFlow<RenameFolderState> = _renameFolderState.asStateFlow()
    
    // 删除状态
    private val _deleteFolderState = MutableStateFlow<DeleteFolderState>(DeleteFolderState.Idle)
    val deleteFolderState: StateFlow<DeleteFolderState> = _deleteFolderState.asStateFlow()
    
    // 排序选项
    private val _sortOption = MutableStateFlow(FolderSortOption.NAME)
    val sortOption: StateFlow<FolderSortOption> = _sortOption.asStateFlow()
    
    /**
     * 获取指定模块的路径
     */
    fun getModulePath(moduleType: ModuleType): String {
        return takagi.ru.paysage.util.FolderPathManager.getModulePath(context, moduleType)
    }
    
    /**
     * 创建文件夹（使用自动路径）
     */
    fun createFolder(
        folderName: String,
        moduleType: ModuleType
    ) {
        val parentPath = getModulePath(moduleType)
        createFolder(parentPath, folderName, moduleType)
    }
    
    /**
     * 创建文件夹（指定路径）
     */
    fun createFolder(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ) {
        // 🔍 调试日志
        android.util.Log.d("FolderDebug", "=== 创建文件夹 ===")
        android.util.Log.d("FolderDebug", "parentPath: $parentPath")
        android.util.Log.d("FolderDebug", "folderName: $folderName")
        android.util.Log.d("FolderDebug", "moduleType: $moduleType")
        
        viewModelScope.launch {
            _createFolderState.value = CreateFolderState.Creating
            try {
                val result = folderRepository.createFolder(
                    parentPath = parentPath,
                    folderName = folderName,
                    moduleType = moduleType
                )
                android.util.Log.d("FolderDebug", "创建成功: ${result.name}, id=${result.id}, moduleType=${result.moduleType}")
                _createFolderState.value = CreateFolderState.Success(result)
                refreshFolders(parentPath, moduleType)
            } catch (e: Exception) {
                android.util.Log.e("FolderDebug", "创建失败", e)
                _createFolderState.value = CreateFolderState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 刷新文件夹列表（使用自动路径）
     */
    fun refreshFolders(moduleType: ModuleType) {
        val path = getModulePath(moduleType)
        refreshFolders(path, moduleType)
    }
    
    /**
     * 刷新文件夹列表（指定路径）
     */
    fun refreshFolders(path: String, moduleType: ModuleType) {
        // 🔍 调试日志
        android.util.Log.d("FolderDebug", "=== 刷新文件夹列表 ===")
        android.util.Log.d("FolderDebug", "path: $path")
        android.util.Log.d("FolderDebug", "moduleType: $moduleType")
        
        viewModelScope.launch {
            try {
                val folders = folderRepository.getFolders(path, moduleType, _sortOption.value)
                android.util.Log.d("FolderDebug", "查询到 ${folders.size} 个文件夹:")
                folders.forEach {
                    android.util.Log.d("FolderDebug", "  - ${it.name} (id=${it.id}, moduleType=${it.moduleType}, parentPath=${it.parentPath})")
                }
                _folders.value = folders
            } catch (e: Exception) {
                // 处理错误
                android.util.Log.e("FolderDebug", "刷新失败", e)
                _folders.value = emptyList()
            }
        }
    }
    
    /**
     * 重置创建文件夹状态
     */
    fun resetCreateFolderState() {
        _createFolderState.value = CreateFolderState.Idle
    }
    
    // ========== 编辑模式管理 ==========
    
    /**
     * 进入编辑模式
     */
    fun enterEditMode() {
        _isEditMode.value = true
        _selectedFolderIds.value = emptySet()
    }
    
    /**
     * 退出编辑模式
     */
    fun exitEditMode() {
        _isEditMode.value = false
        _selectedFolderIds.value = emptySet()
    }
    
    /**
     * 切换文件夹选择状态
     */
    fun toggleFolderSelection(folderId: Long) {
        val currentSelection = _selectedFolderIds.value.toMutableSet()
        if (folderId in currentSelection) {
            currentSelection.remove(folderId)
        } else {
            currentSelection.add(folderId)
        }
        _selectedFolderIds.value = currentSelection
    }
    
    /**
     * 全选
     */
    fun selectAll() {
        _selectedFolderIds.value = _folders.value.map { it.id }.toSet()
    }
    
    /**
     * 取消全选
     */
    fun deselectAll() {
        _selectedFolderIds.value = emptySet()
    }
    
    // ========== 重命名功能 ==========
    
    /**
     * 重命名文件夹
     */
    fun renameFolder(folderId: Long, newName: String, path: String, moduleType: ModuleType) {
        viewModelScope.launch {
            _renameFolderState.value = RenameFolderState.Renaming
            try {
                val result = folderRepository.renameFolder(folderId, newName)
                _renameFolderState.value = RenameFolderState.Success(result)
                refreshFolders(path, moduleType)
            } catch (e: Exception) {
                _renameFolderState.value = RenameFolderState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 重置重命名状态
     */
    fun resetRenameFolderState() {
        _renameFolderState.value = RenameFolderState.Idle
    }
    
    // ========== 删除功能 ==========
    
    /**
     * 删除单个文件夹
     */
    fun deleteFolder(folderId: Long, path: String, moduleType: ModuleType) {
        viewModelScope.launch {
            _deleteFolderState.value = DeleteFolderState.Deleting
            try {
                folderRepository.deleteFolders(listOf(folderId))
                _deleteFolderState.value = DeleteFolderState.Success(1)
                refreshFolders(path, moduleType)
            } catch (e: Exception) {
                _deleteFolderState.value = DeleteFolderState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 批量删除文件夹
     */
    fun deleteSelectedFolders(path: String, moduleType: ModuleType) {
        val idsToDelete = _selectedFolderIds.value.toList()
        if (idsToDelete.isEmpty()) return
        
        viewModelScope.launch {
            _deleteFolderState.value = DeleteFolderState.Deleting
            try {
                folderRepository.deleteFolders(idsToDelete)
                _deleteFolderState.value = DeleteFolderState.Success(idsToDelete.size)
                _selectedFolderIds.value = emptySet()
                refreshFolders(path, moduleType)
            } catch (e: Exception) {
                _deleteFolderState.value = DeleteFolderState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 重置删除状态
     */
    fun resetDeleteFolderState() {
        _deleteFolderState.value = DeleteFolderState.Idle
    }
    
    // ========== 排序功能 ==========
    
    /**
     * 设置排序选项
     */
    fun setSortOption(option: FolderSortOption, path: String, moduleType: ModuleType) {
        _sortOption.value = option
        refreshFolders(path, moduleType)
    }
    
    /**
     * 更新文件夹顺序（拖拽排序）
     */
    fun updateFolderOrder(folders: List<Folder>) {
        viewModelScope.launch {
            try {
                folderRepository.updateFolderOrder(folders)
                _folders.value = folders
                _sortOption.value = FolderSortOption.CUSTOM
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}

/**
 * 创建文件夹状态
 */
sealed class CreateFolderState {
    object Idle : CreateFolderState()
    object Creating : CreateFolderState()
    data class Success(val folder: Folder) : CreateFolderState()
    data class Error(val message: String) : CreateFolderState()
}

/**
 * 重命名文件夹状态
 */
sealed class RenameFolderState {
    object Idle : RenameFolderState()
    object Renaming : RenameFolderState()
    data class Success(val folder: Folder) : RenameFolderState()
    data class Error(val message: String) : RenameFolderState()
}

/**
 * 删除文件夹状态
 */
sealed class DeleteFolderState {
    object Idle : DeleteFolderState()
    object Deleting : DeleteFolderState()
    data class Success(val count: Int) : DeleteFolderState()
    data class Error(val message: String) : DeleteFolderState()
}
