package takagi.ru.paysage.diagnostic.model

import takagi.ru.paysage.data.model.Folder

/**
 * 诊断报告
 */
data class DiagnosticReport(
    val timestamp: Long,
    val isolationStatus: IsolationStatus,
    val pathConflicts: List<PathConflict>,
    val duplicateFolders: List<DuplicateFolder>,
    val statistics: FolderStatistics,
    val recommendations: List<String>
)

/**
 * 隔离状态
 */
data class IsolationStatus(
    val isIsolated: Boolean,
    val violations: List<IsolationViolation>
)

/**
 * 路径冲突
 */
data class PathConflict(
    val path: String,
    val localFolders: List<Folder>,
    val onlineFolders: List<Folder>
)

/**
 * 重复文件夹
 */
data class DuplicateFolder(
    val name: String,
    val localFolder: Folder?,
    val onlineFolder: Folder?,
    val similarity: Float
)

/**
 * 文件夹统计
 */
data class FolderStatistics(
    val totalFolders: Int,
    val localFolders: Int,
    val onlineFolders: Int,
    val sharedPaths: Int
)


/**
 * 隔离违规
 */
data class IsolationViolation(
    val type: ViolationType,
    val description: String,
    val affectedFolders: List<Folder>
)

/**
 * 违规类型
 */
enum class ViolationType {
    SHARED_PATH,
    DUPLICATE_NAME,
    MISSING_MODULE_TYPE,
    INCORRECT_MODULE_TYPE
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * 隔离报告
 */
data class IsolationReport(
    val isIsolated: Boolean,
    val violations: List<IsolationViolation>,
    val timestamp: Long
)

/**
 * 路径权限
 */
data class PathPermissions(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean
)
