package takagi.ru.paysage.diagnostic

import android.content.Context
import android.util.Log
import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.diagnostic.model.LogEntry
import takagi.ru.paysage.diagnostic.model.FolderOperation
import takagi.ru.paysage.diagnostic.model.OperationResult
import takagi.ru.paysage.diagnostic.model.OperationPatternReport
import takagi.ru.paysage.diagnostic.model.SuspiciousPattern
import takagi.ru.paysage.diagnostic.model.PatternType
import java.io.File

/**
 * 诊断日志系统
 * 记录所有文件夹操作，便于问题追踪
 */
class DiagnosticLogger(
    private val context: Context
) {
    private val logFile = File(context.filesDir, "folder_diagnostic.log")
    
    /**
     * 记录文件夹操作
     */
    fun logFolderOperation(
        operation: FolderOperation,
        moduleType: ModuleType,
        folderId: Long?,
        folderName: String?,
        parentPath: String?,
        result: OperationResult,
        stackTrace: String? = null
    ) {
        val logEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            operation = operation,
            moduleType = moduleType,
            folderId = folderId,
            folderName = folderName,
            parentPath = parentPath,
            result = result,
            stackTrace = stackTrace
        )
        
        writeLog(logEntry)
    }
    
    /**
     * 获取操作历史
     */
    fun getOperationHistory(
        moduleType: ModuleType? = null,
        operation: FolderOperation? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<LogEntry> {
        return readLogs()
            .filter { entry ->
                (moduleType == null || entry.moduleType == moduleType) &&
                (operation == null || entry.operation == operation) &&
                (startTime == null || entry.timestamp >= startTime) &&
                (endTime == null || entry.timestamp <= endTime)
            }
    }

    
    /**
     * 分析操作模式
     */
    fun analyzeOperationPatterns(): OperationPatternReport {
        val logs = readLogs()
        val recentLogs = logs.filter { 
            it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000 
        }
        
        return OperationPatternReport(
            totalOperations = recentLogs.size,
            operationsByType = recentLogs.groupBy { it.operation }.mapValues { it.value.size },
            operationsByModule = recentLogs.groupBy { it.moduleType }.mapValues { it.value.size },
            suspiciousPatterns = detectSuspiciousPatterns(recentLogs)
        )
    }
    
    /**
     * 检测可疑模式
     */
    private fun detectSuspiciousPatterns(logs: List<LogEntry>): List<SuspiciousPattern> {
        val patterns = mutableListOf<SuspiciousPattern>()
        
        // 检测短时间内相同名称的文件夹在不同模块创建
        val createOps = logs.filter { it.operation == FolderOperation.CREATE }
        val timeWindow = 5000L // 5秒
        
        createOps.forEachIndexed { index, entry ->
            val similarOps = createOps.drop(index + 1).filter { other ->
                other.folderName == entry.folderName &&
                other.moduleType != entry.moduleType &&
                Math.abs(other.timestamp - entry.timestamp) < timeWindow
            }
            
            if (similarOps.isNotEmpty()) {
                patterns.add(SuspiciousPattern(
                    type = PatternType.DUPLICATE_CREATION,
                    description = "Folder '${entry.folderName}' created in both modules within ${timeWindow}ms",
                    relatedLogs = listOf(entry) + similarOps
                ))
            }
        }
        
        return patterns
    }
    
    /**
     * 写入日志
     */
    private fun writeLog(entry: LogEntry) {
        try {
            logFile.appendText("${entry.toJson()}\n")
        } catch (e: Exception) {
            Log.e("DiagnosticLogger", "Failed to write log", e)
        }
    }
    
    /**
     * 读取日志
     */
    private fun readLogs(): List<LogEntry> {
        return try {
            if (logFile.exists()) {
                logFile.readLines()
                    .mapNotNull { line ->
                        try {
                            LogEntry.fromJson(line)
                        } catch (e: Exception) {
                            null
                        }
                    }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DiagnosticLogger", "Failed to read logs", e)
            emptyList()
        }
    }
}
