package takagi.ru.paysage.diagnostic.model

import takagi.ru.paysage.data.model.ModuleType
import org.json.JSONObject

/**
 * 日志条目
 */
data class LogEntry(
    val timestamp: Long,
    val operation: FolderOperation,
    val moduleType: ModuleType,
    val folderId: Long?,
    val folderName: String?,
    val parentPath: String?,
    val result: OperationResult,
    val stackTrace: String?
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("timestamp", timestamp)
        json.put("operation", operation.name)
        json.put("moduleType", moduleType.name)
        json.put("folderId", folderId)
        json.put("folderName", folderName)
        json.put("parentPath", parentPath)
        json.put("result", result.name)
        json.put("stackTrace", stackTrace)
        return json.toString()
    }
    
    companion object {
        fun fromJson(json: String): LogEntry {
            val jsonObject = JSONObject(json)
            return LogEntry(
                timestamp = jsonObject.getLong("timestamp"),
                operation = FolderOperation.valueOf(jsonObject.getString("operation")),
                moduleType = ModuleType.valueOf(jsonObject.getString("moduleType")),
                folderId = if (jsonObject.isNull("folderId")) null else jsonObject.getLong("folderId"),
                folderName = if (jsonObject.isNull("folderName")) null else jsonObject.getString("folderName"),
                parentPath = if (jsonObject.isNull("parentPath")) null else jsonObject.getString("parentPath"),
                result = OperationResult.valueOf(jsonObject.getString("result")),
                stackTrace = if (jsonObject.isNull("stackTrace")) null else jsonObject.getString("stackTrace")
            )
        }
    }
}

/**
 * 文件夹操作类型
 */
enum class FolderOperation {
    CREATE, UPDATE, DELETE, QUERY, RENAME
}

/**
 * 操作结果
 */
enum class OperationResult {
    SUCCESS, FAILURE, PARTIAL
}

/**
 * 操作模式报告
 */
data class OperationPatternReport(
    val totalOperations: Int,
    val operationsByType: Map<FolderOperation, Int>,
    val operationsByModule: Map<ModuleType, Int>,
    val suspiciousPatterns: List<SuspiciousPattern>
)

/**
 * 可疑模式
 */
data class SuspiciousPattern(
    val type: PatternType,
    val description: String,
    val relatedLogs: List<LogEntry>
)

/**
 * 模式类型
 */
enum class PatternType {
    DUPLICATE_CREATION,
    RAPID_DELETION,
    CROSS_MODULE_ACCESS
}
