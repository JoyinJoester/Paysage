package takagi.ru.paysage.diagnostic

import takagi.ru.paysage.data.dao.FolderDao
import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.diagnostic.model.IsolationReport
import takagi.ru.paysage.diagnostic.model.IsolationViolation
import takagi.ru.paysage.diagnostic.model.ValidationResult
import takagi.ru.paysage.diagnostic.model.ViolationType

/**
 * 隔离验证器
 * 验证模块间的数据隔离，检测违规操作
 */
class IsolationValidator(
    private val folderDao: FolderDao
) {
    /**
     * 验证查询是否包含 module_type 过滤
     */
    fun validateQuery(query: String): ValidationResult {
        val hasModuleTypeFilter = query.contains("module_type", ignoreCase = true)
        return ValidationResult(
            isValid = hasModuleTypeFilter,
            message = if (hasModuleTypeFilter) 
                "Query includes module_type filter" 
            else 
                "Query missing module_type filter"
        )
    }
    
    /**
     * 验证文件夹操作的模块类型一致性
     */
    suspend fun validateFolderOperation(
        folderId: Long,
        expectedModuleType: ModuleType
    ): ValidationResult {
        val folder = folderDao.getFolderById(folderId)
        return if (folder == null) {
            ValidationResult(false, "Folder not found")
        } else if (folder.moduleType != expectedModuleType) {
            ValidationResult(
                false, 
                "Module type mismatch: expected $expectedModuleType, got ${folder.moduleType}"
            )
        } else {
            ValidationResult(true, "Module type matches")
        }
    }
    
    /**
     * 运行完整的隔离检查
     */
    suspend fun runIsolationCheck(): IsolationReport {
        val violations = mutableListOf<IsolationViolation>()
        
        // 检查是否有相同路径的文件夹
        val allFolders = folderDao.getAllFolders()
        val pathGroups = allFolders.groupBy { it.path }
        
        pathGroups.forEach { (path, folders) ->
            if (folders.size > 1) {
                val moduleTypes = folders.map { it.moduleType }.toSet()
                if (moduleTypes.size > 1) {
                    violations.add(IsolationViolation(
                        type = ViolationType.SHARED_PATH,
                        description = "Path $path is used by multiple module types",
                        affectedFolders = folders
                    ))
                }
            }
        }
        
        return IsolationReport(
            isIsolated = violations.isEmpty(),
            violations = violations,
            timestamp = System.currentTimeMillis()
        )
    }
}
