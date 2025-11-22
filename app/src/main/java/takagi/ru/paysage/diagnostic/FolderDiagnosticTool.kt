package takagi.ru.paysage.diagnostic

import android.content.Context
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.diagnostic.model.*
import java.io.File

/**
 * 文件夹诊断工具
 * 提供统一的诊断接口
 */
class FolderDiagnosticTool(
    private val database: PaysageDatabase,
    private val context: Context
) {
    private val databaseAnalyzer = DatabaseAnalyzer(database.folderDao())
    private val fileSystemAnalyzer = FileSystemAnalyzer(context)
    private val isolationValidator = IsolationValidator(database.folderDao())
    
    /**
     * 运行完整诊断
     */
    suspend fun runFullDiagnostic(): DiagnosticReport {
        // 获取文件夹统计
        val folderCounts = databaseAnalyzer.getFolderCountByModule()
        val totalFolders = folderCounts.values.sum()
        
        // 获取唯一路径
        val uniquePaths = databaseAnalyzer.getUniqueParentPaths()
        val localPaths = uniquePaths[ModuleType.LOCAL_MANAGEMENT] ?: emptySet()
        val onlinePaths = uniquePaths[ModuleType.ONLINE_MANAGEMENT] ?: emptySet()
        val sharedPaths = localPaths.intersect(onlinePaths).size
        
        // 查找重复文件夹
        val duplicates = databaseAnalyzer.findPotentialDuplicates()
        
        // 分析路径冲突
        val pathConflicts = fileSystemAnalyzer.analyzePathConflicts(localPaths, onlinePaths)
        
        // 运行隔离检查
        val isolationReport = isolationValidator.runIsolationCheck()
        
        // 生成建议
        val recommendations = generateRecommendations(
            duplicates,
            pathConflicts,
            isolationReport
        )
        
        return DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            isolationStatus = IsolationStatus(
                isIsolated = isolationReport.isIsolated,
                violations = isolationReport.violations
            ),
            pathConflicts = pathConflicts,
            duplicateFolders = duplicates,
            statistics = FolderStatistics(
                totalFolders = totalFolders,
                localFolders = folderCounts[ModuleType.LOCAL_MANAGEMENT] ?: 0,
                onlineFolders = folderCounts[ModuleType.ONLINE_MANAGEMENT] ?: 0,
                sharedPaths = sharedPaths
            ),
            recommendations = recommendations
        )
    }

    
    /**
     * 验证数据库隔离
     */
    suspend fun verifyDatabaseIsolation(): IsolationReport {
        return isolationValidator.runIsolationCheck()
    }
    
    /**
     * 分析文件系统路径
     */
    suspend fun analyzeFileSystemPaths(): Map<String, Any> {
        val uniquePaths = databaseAnalyzer.getUniqueParentPaths()
        val localPaths = uniquePaths[ModuleType.LOCAL_MANAGEMENT] ?: emptySet()
        val onlinePaths = uniquePaths[ModuleType.ONLINE_MANAGEMENT] ?: emptySet()
        
        return mapOf(
            "localPaths" to localPaths,
            "onlinePaths" to onlinePaths,
            "sharedPaths" to localPaths.intersect(onlinePaths),
            "conflicts" to fileSystemAnalyzer.analyzePathConflicts(localPaths, onlinePaths)
        )
    }
    
    /**
     * 查找重复文件夹
     */
    suspend fun findDuplicateFolders(): List<DuplicateFolder> {
        return databaseAnalyzer.findPotentialDuplicates()
    }
    
    /**
     * 导出诊断日志
     */
    fun exportDiagnosticLogs(format: ExportFormat): File {
        val exportFile = File(context.getExternalFilesDir(null), "diagnostic_export.${format.extension}")
        
        when (format) {
            ExportFormat.JSON -> {
                // 导出为 JSON 格式
                exportFile.writeText("[]") // 简化实现
            }
            ExportFormat.CSV -> {
                // 导出为 CSV 格式
                exportFile.writeText("timestamp,operation,moduleType,result\n") // 简化实现
            }
        }
        
        return exportFile
    }
    
    /**
     * 生成建议
     */
    private fun generateRecommendations(
        duplicates: List<DuplicateFolder>,
        pathConflicts: List<PathConflict>,
        isolationReport: IsolationReport
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (duplicates.isNotEmpty()) {
            recommendations.add("发现 ${duplicates.size} 个重复文件夹，建议运行数据清理工具")
        }
        
        if (pathConflicts.isNotEmpty()) {
            recommendations.add("发现 ${pathConflicts.size} 个路径冲突，建议检查文件系统配置")
        }
        
        if (!isolationReport.isIsolated) {
            recommendations.add("检测到 ${isolationReport.violations.size} 个隔离违规，建议立即修复")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("系统运行正常，未发现问题")
        }
        
        return recommendations
    }
}

/**
 * 导出格式
 */
enum class ExportFormat(val extension: String) {
    JSON("json"),
    CSV("csv")
}
