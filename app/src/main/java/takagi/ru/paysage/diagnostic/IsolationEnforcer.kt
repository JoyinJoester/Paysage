package takagi.ru.paysage.diagnostic

import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.diagnostic.model.FolderOperation
import takagi.ru.paysage.diagnostic.model.OperationResult
import takagi.ru.paysage.diagnostic.model.ValidationResult

/**
 * 隔离强制器
 * 在运行时强制执行模块隔离规则
 */
class IsolationEnforcer(
    private val validator: IsolationValidator,
    private val logger: DiagnosticLogger
) {
    // 路径规则配置
    private val localPathPrefixes = listOf(
        "/storage/emulated/0/Paysage/Local",
        "/sdcard/Paysage/Local"
    )
    
    private val onlinePathPrefixes = listOf(
        "/storage/emulated/0/Paysage/Online",
        "/sdcard/Paysage/Online"
    )
    
    /**
     * 拦截并验证文件夹创建操作
     */
    suspend fun enforceFolderCreation(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ): EnforcementResult {
        // 验证路径是否属于正确的模块
        val pathValidation = validatePathForModule(parentPath, moduleType)
        if (!pathValidation.isValid) {
            logger.logFolderOperation(
                operation = FolderOperation.CREATE,
                moduleType = moduleType,
                folderId = null,
                folderName = folderName,
                parentPath = parentPath,
                result = OperationResult.FAILURE,
                stackTrace = Thread.currentThread().stackTrace.joinToString("\n")
            )
            return EnforcementResult.Denied(pathValidation.message)
        }
        
        return EnforcementResult.Allowed
    }
    
    /**
     * 验证路径是否属于指定模块
     */
    private fun validatePathForModule(
        path: String,
        moduleType: ModuleType
    ): ValidationResult {
        return when (moduleType) {
            ModuleType.LOCAL_MANAGEMENT -> {
                if (localPathPrefixes.any { path.startsWith(it) }) {
                    ValidationResult(true, "Path belongs to local module")
                } else {
                    ValidationResult(false, "Path does not belong to local module. Expected path to start with: ${localPathPrefixes.joinToString()}")
                }
            }
            ModuleType.ONLINE_MANAGEMENT -> {
                if (onlinePathPrefixes.any { path.startsWith(it) }) {
                    ValidationResult(true, "Path belongs to online module")
                } else {
                    ValidationResult(false, "Path does not belong to online module. Expected path to start with: ${onlinePathPrefixes.joinToString()}")
                }
            }
        }
    }
}

/**
 * 强制结果
 */
sealed class EnforcementResult {
    object Allowed : EnforcementResult()
    data class Denied(val reason: String) : EnforcementResult()
}
