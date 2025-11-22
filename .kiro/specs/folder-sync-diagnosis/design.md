# 设计文档 - 文件夹同步问题诊断与修复

## 概述

本设计文档详细描述了诊断和修复本地管理与在线管理之间文件夹同步问题的技术方案。通过系统化的诊断流程、根因分析和针对性修复，确保两个模块的数据完全隔离。

### 问题分析

根据代码审查，发现以下潜在问题点：

1. **数据库层面**：虽然使用 `module_type` 字段区分，但所有数据存储在同一张 `folders` 表中
2. **文件系统层面**：`parentPath` 可能在两个模块间重叠，导致物理文件夹共享
3. **应用层面**：UI 调用时可能传递错误的 `moduleType` 参数
4. **查询层面**：某些查询可能缺少 `module_type` 过滤条件

### 设计目标

1. 快速定位同步问题的根本原因
2. 提供可视化诊断工具便于问题分析
3. 实施彻底的修复方案确保数据隔离
4. 建立预防机制避免问题再次发生

## 架构

### 整体架构

```
诊断层 (Diagnostic Layer)
    ├── FolderDiagnosticTool.kt (诊断工具)
    ├── IsolationValidator.kt (隔离验证器)
    └── DiagnosticLogger.kt (诊断日志)

分析层 (Analysis Layer)
    ├── DatabaseAnalyzer.kt (数据库分析)
    ├── FileSystemAnalyzer.kt (文件系统分析)
    └── CallStackTracer.kt (调用栈追踪)

修复层 (Fix Layer)
    ├── DataMigrationTool.kt (数据迁移工具)
    ├── IsolationEnforcer.kt (隔离强制器)
    └── ValidationInterceptor.kt (验证拦截器)

监控层 (Monitoring Layer)
    ├── HealthCheckService.kt (健康检查服务)
    └── MetricsCollector.kt (指标收集器)
```

### 数据流

```
用户操作 → UI Layer → ViewModel → Repository → DAO → Database
                ↓           ↓           ↓         ↓        ↓
            Validator   Validator   Validator  Logger  Constraint
```

## 组件设计

### 1. 诊断工具核心组件

#### FolderDiagnosticTool

诊断工具主类，提供统一的诊断接口。

```kotlin
class FolderDiagnosticTool(
    private val database: PaysageDatabase,
    private val context: Context
) {
    /**
     * 运行完整诊断
     */
    suspend fun runFullDiagnostic(): DiagnosticReport
    
    /**
     * 验证数据库隔离
     */
    suspend fun verifyDatabaseIsolation(): IsolationReport
    
    /**
     * 分析文件系统路径
     */
    suspend fun analyzeFileSystemPaths(): PathAnalysisReport
    
    /**
     * 查找重复文件夹
     */
    suspend fun findDuplicateFolders(): List<DuplicateFolder>
    
    /**
     * 导出诊断日志
     */
    suspend fun exportDiagnosticLogs(format: ExportFormat): File
}
```


#### DiagnosticReport 数据模型

```kotlin
data class DiagnosticReport(
    val timestamp: Long,
    val isolationStatus: IsolationStatus,
    val pathConflicts: List<PathConflict>,
    val duplicateFolders: List<DuplicateFolder>,
    val statistics: FolderStatistics,
    val recommendations: List<String>
)

data class IsolationStatus(
    val isIsolated: Boolean,
    val violations: List<IsolationViolation>
)

data class PathConflict(
    val path: String,
    val localFolders: List<Folder>,
    val onlineFolders: List<Folder>
)

data class DuplicateFolder(
    val name: String,
    val localFolder: Folder?,
    val onlineFolder: Folder?,
    val similarity: Float
)

data class FolderStatistics(
    val totalFolders: Int,
    val localFolders: Int,
    val onlineFolders: Int,
    val sharedPaths: Int
)
```

### 2. 数据库分析器

#### DatabaseAnalyzer

分析数据库中的文件夹记录，检测隔离问题。

```kotlin
class DatabaseAnalyzer(
    private val folderDao: FolderDao
) {
    /**
     * 统计各模块的文件夹数量
     */
    suspend fun getFolderCountByModule(): Map<ModuleType, Int> {
        return mapOf(
            ModuleType.LOCAL_MANAGEMENT to folderDao.countByModuleType(ModuleType.LOCAL_MANAGEMENT),
            ModuleType.ONLINE_MANAGEMENT to folderDao.countByModuleType(ModuleType.ONLINE_MANAGEMENT)
        )
    }
    
    /**
     * 查找具有相同名称和路径的文件夹
     */
    suspend fun findPotentialDuplicates(): List<DuplicateFolder> {
        val allFolders = folderDao.getAllFolders()
        val localFolders = allFolders.filter { it.moduleType == ModuleType.LOCAL_MANAGEMENT }
        val onlineFolders = allFolders.filter { it.moduleType == ModuleType.ONLINE_MANAGEMENT }
        
        return localFolders.mapNotNull { local ->
            val online = onlineFolders.find { 
                it.name == local.name && it.parentPath == local.parentPath 
            }
            if (online != null) {
                DuplicateFolder(
                    name = local.name,
                    localFolder = local,
                    onlineFolder = online,
                    similarity = calculateSimilarity(local, online)
                )
            } else null
        }
    }
    
    /**
     * 获取所有唯一的父路径
     */
    suspend fun getUniqueParentPaths(): Map<ModuleType, Set<String>> {
        return mapOf(
            ModuleType.LOCAL_MANAGEMENT to folderDao.getUniqueParentPaths(ModuleType.LOCAL_MANAGEMENT).toSet(),
            ModuleType.ONLINE_MANAGEMENT to folderDao.getUniqueParentPaths(ModuleType.ONLINE_MANAGEMENT).toSet()
        )
    }
    
    private fun calculateSimilarity(folder1: Folder, folder2: Folder): Float {
        var score = 0f
        if (folder1.name == folder2.name) score += 0.4f
        if (folder1.parentPath == folder2.parentPath) score += 0.4f
        if (folder1.path == folder2.path) score += 0.2f
        return score
    }
}
```

### 3. 文件系统分析器

#### FileSystemAnalyzer

分析文件系统路径，检测路径重叠和冲突。

```kotlin
class FileSystemAnalyzer(
    private val context: Context
) {
    /**
     * 分析路径冲突
     */
    fun analyzePathConflicts(
        localPaths: Set<String>,
        onlinePaths: Set<String>
    ): List<PathConflict> {
        val conflicts = mutableListOf<PathConflict>()
        val commonPaths = localPaths.intersect(onlinePaths)
        
        for (path in commonPaths) {
            conflicts.add(PathConflict(
                path = path,
                localFolders = emptyList(), // 需要从数据库查询
                onlineFolders = emptyList()
            ))
        }
        
        return conflicts
    }
    
    /**
     * 验证路径是否存在
     */
    fun verifyPathExists(path: String): Boolean {
        return File(path).exists()
    }
    
    /**
     * 检查路径权限
     */
    fun checkPathPermissions(path: String): PathPermissions {
        val file = File(path)
        return PathPermissions(
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute()
        )
    }
    
    /**
     * 检测符号链接
     */
    fun detectSymbolicLinks(path: String): Boolean {
        return try {
            val file = File(path)
            file.canonicalPath != file.absolutePath
        } catch (e: Exception) {
            false
        }
    }
}

data class PathPermissions(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean
)
```

### 4. 隔离验证器

#### IsolationValidator

验证模块间的数据隔离，检测违规操作。

```kotlin
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

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

data class IsolationReport(
    val isIsolated: Boolean,
    val violations: List<IsolationViolation>,
    val timestamp: Long
)

data class IsolationViolation(
    val type: ViolationType,
    val description: String,
    val affectedFolders: List<Folder>
)

enum class ViolationType {
    SHARED_PATH,
    DUPLICATE_NAME,
    MISSING_MODULE_TYPE,
    INCORRECT_MODULE_TYPE
}
```


### 5. 诊断日志系统

#### DiagnosticLogger

记录所有文件夹操作，便于问题追踪。

```kotlin
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
    
    private fun writeLog(entry: LogEntry) {
        try {
            logFile.appendText("${entry.toJson()}\n")
        } catch (e: Exception) {
            Log.e("DiagnosticLogger", "Failed to write log", e)
        }
    }
    
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
        // 简化的 JSON 序列化
        return """{"timestamp":$timestamp,"operation":"$operation","moduleType":"$moduleType","folderId":$folderId,"folderName":"$folderName","parentPath":"$parentPath","result":"$result"}"""
    }
    
    companion object {
        fun fromJson(json: String): LogEntry {
            // 简化的 JSON 反序列化
            // 实际实现应使用 Gson 或 kotlinx.serialization
            throw NotImplementedError("Use proper JSON library")
        }
    }
}

enum class FolderOperation {
    CREATE, UPDATE, DELETE, QUERY, RENAME
}

enum class OperationResult {
    SUCCESS, FAILURE, PARTIAL
}

data class OperationPatternReport(
    val totalOperations: Int,
    val operationsByType: Map<FolderOperation, Int>,
    val operationsByModule: Map<ModuleType, Int>,
    val suspiciousPatterns: List<SuspiciousPattern>
)

data class SuspiciousPattern(
    val type: PatternType,
    val description: String,
    val relatedLogs: List<LogEntry>
)

enum class PatternType {
    DUPLICATE_CREATION,
    RAPID_DELETION,
    CROSS_MODULE_ACCESS
}
```

### 6. 数据迁移工具

#### DataMigrationTool

清理重复数据，修复隔离问题。

```kotlin
class DataMigrationTool(
    private val database: PaysageDatabase,
    private val folderDao: FolderDao
) {
    /**
     * 清理重复文件夹
     */
    suspend fun cleanupDuplicateFolders(
        strategy: CleanupStrategy
    ): CleanupReport = withContext(Dispatchers.IO) {
        val duplicates = findDuplicates()
        val cleaned = mutableListOf<Long>()
        val failed = mutableListOf<CleanupFailure>()
        
        duplicates.forEach { duplicate ->
            try {
                when (strategy) {
                    CleanupStrategy.KEEP_LOCAL -> {
                        duplicate.onlineFolder?.let { 
                            folderDao.deleteById(it.id)
                            cleaned.add(it.id)
                        }
                    }
                    CleanupStrategy.KEEP_ONLINE -> {
                        duplicate.localFolder?.let { 
                            folderDao.deleteById(it.id)
                            cleaned.add(it.id)
                        }
                    }
                    CleanupStrategy.KEEP_NEWER -> {
                        val toDelete = if (duplicate.localFolder!!.createdAt > duplicate.onlineFolder!!.createdAt) {
                            duplicate.onlineFolder
                        } else {
                            duplicate.localFolder
                        }
                        folderDao.deleteById(toDelete.id)
                        cleaned.add(toDelete.id)
                    }
                }
            } catch (e: Exception) {
                failed.add(CleanupFailure(
                    duplicate = duplicate,
                    error = e.message ?: "Unknown error"
                ))
            }
        }
        
        CleanupReport(
            totalDuplicates = duplicates.size,
            cleaned = cleaned,
            failed = failed
        )
    }
    
    /**
     * 修复模块类型
     */
    suspend fun fixModuleTypes(
        pathToModuleMapping: Map<String, ModuleType>
    ): FixReport = withContext(Dispatchers.IO) {
        val allFolders = folderDao.getAllFolders()
        val fixed = mutableListOf<Long>()
        
        allFolders.forEach { folder ->
            val expectedModule = pathToModuleMapping[folder.parentPath]
            if (expectedModule != null && expectedModule != folder.moduleType) {
                val updated = folder.copy(
                    moduleType = expectedModule,
                    updatedAt = System.currentTimeMillis()
                )
                folderDao.update(updated)
                fixed.add(folder.id)
            }
        }
        
        FixReport(
            totalChecked = allFolders.size,
            fixed = fixed
        )
    }
    
    private suspend fun findDuplicates(): List<DuplicateFolder> {
        val allFolders = folderDao.getAllFolders()
        val localFolders = allFolders.filter { it.moduleType == ModuleType.LOCAL_MANAGEMENT }
        val onlineFolders = allFolders.filter { it.moduleType == ModuleType.ONLINE_MANAGEMENT }
        
        return localFolders.mapNotNull { local ->
            val online = onlineFolders.find { 
                it.name == local.name && it.parentPath == local.parentPath 
            }
            if (online != null) {
                DuplicateFolder(
                    name = local.name,
                    localFolder = local,
                    onlineFolder = online,
                    similarity = 1.0f
                )
            } else null
        }
    }
}

enum class CleanupStrategy {
    KEEP_LOCAL,
    KEEP_ONLINE,
    KEEP_NEWER
}

data class CleanupReport(
    val totalDuplicates: Int,
    val cleaned: List<Long>,
    val failed: List<CleanupFailure>
)

data class CleanupFailure(
    val duplicate: DuplicateFolder,
    val error: String
)

data class FixReport(
    val totalChecked: Int,
    val fixed: List<Long>
)
```


### 7. 隔离强制器

#### IsolationEnforcer

在运行时强制执行模块隔离规则。

```kotlin
class IsolationEnforcer(
    private val validator: IsolationValidator,
    private val logger: DiagnosticLogger
) {
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
        // 定义模块路径规则
        val localPaths = listOf("/storage/emulated/0/Paysage/Local")
        val onlinePaths = listOf("/storage/emulated/0/Paysage/Online")
        
        return when (moduleType) {
            ModuleType.LOCAL_MANAGEMENT -> {
                if (localPaths.any { path.startsWith(it) }) {
                    ValidationResult(true, "Path belongs to local module")
                } else {
                    ValidationResult(false, "Path does not belong to local module")
                }
            }
            ModuleType.ONLINE_MANAGEMENT -> {
                if (onlinePaths.any { path.startsWith(it) }) {
                    ValidationResult(true, "Path belongs to online module")
                } else {
                    ValidationResult(false, "Path does not belong to online module")
                }
            }
        }
    }
}

sealed class EnforcementResult {
    object Allowed : EnforcementResult()
    data class Denied(val reason: String) : EnforcementResult()
}
```

### 8. DAO 扩展

需要在 `FolderDao` 中添加诊断相关的查询方法。

```kotlin
@Dao
interface FolderDao {
    // 现有方法...
    
    /**
     * 按模块类型统计文件夹数量
     */
    @Query("SELECT COUNT(*) FROM folders WHERE module_type = :moduleType")
    suspend fun countByModuleType(moduleType: ModuleType): Int
    
    /**
     * 获取指定模块的所有唯一父路径
     */
    @Query("SELECT DISTINCT parent_path FROM folders WHERE module_type = :moduleType")
    suspend fun getUniqueParentPaths(moduleType: ModuleType): List<String>
    
    /**
     * 查找具有相同名称和父路径但不同模块类型的文件夹
     */
    @Query("""
        SELECT f1.* FROM folders f1
        INNER JOIN folders f2 ON f1.name = f2.name 
            AND f1.parent_path = f2.parent_path 
            AND f1.module_type != f2.module_type
        WHERE f1.id < f2.id
    """)
    suspend fun findCrossModuleDuplicates(): List<Folder>
    
    /**
     * 获取指定时间范围内创建的文件夹
     */
    @Query("SELECT * FROM folders WHERE created_at BETWEEN :startTime AND :endTime ORDER BY created_at ASC")
    suspend fun getFoldersCreatedBetween(startTime: Long, endTime: Long): List<Folder>
    
    /**
     * 获取所有共享相同路径的文件夹组
     */
    @Query("""
        SELECT * FROM folders 
        WHERE path IN (
            SELECT path FROM folders 
            GROUP BY path 
            HAVING COUNT(DISTINCT module_type) > 1
        )
        ORDER BY path, module_type
    """)
    suspend fun getFoldersWithSharedPaths(): List<Folder>
}
```

## UI 设计

### 诊断工具界面

#### DiagnosticScreen

```kotlin
@Composable
fun DiagnosticScreen(
    viewModel: DiagnosticViewModel = viewModel()
) {
    val diagnosticReport by viewModel.diagnosticReport.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件夹同步诊断") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 统计卡片
            StatisticsCard(
                localCount = diagnosticReport?.statistics?.localFolders ?: 0,
                onlineCount = diagnosticReport?.statistics?.onlineFolders ?: 0,
                sharedPaths = diagnosticReport?.statistics?.sharedPaths ?: 0
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 隔离状态
            IsolationStatusCard(
                status = diagnosticReport?.isolationStatus
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 重复文件夹列表
            if (diagnosticReport?.duplicateFolders?.isNotEmpty() == true) {
                Text(
                    text = "发现重复文件夹",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(diagnosticReport!!.duplicateFolders) { duplicate ->
                        DuplicateFolderItem(duplicate)
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.runDiagnostic() },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("运行诊断")
                    }
                }
                
                OutlinedButton(
                    onClick = { viewModel.exportReport() },
                    enabled = diagnosticReport != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("导出报告")
                }
            }
        }
    }
}

@Composable
fun StatisticsCard(
    localCount: Int,
    onlineCount: Int,
    sharedPaths: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "文件夹统计",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("本地", localCount)
                StatItem("在线", onlineCount)
                StatItem("共享路径", sharedPaths, isWarning = sharedPaths > 0)
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: Int,
    isWarning: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun IsolationStatusCard(
    status: IsolationStatus?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status?.isIsolated == true)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (status?.isIsolated == true) 
                    Icons.Default.CheckCircle 
                else 
                    Icons.Default.Warning,
                contentDescription = null,
                tint = if (status?.isIsolated == true)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (status?.isIsolated == true) "隔离正常" else "发现隔离问题",
                    style = MaterialTheme.typography.titleMedium
                )
                if (status?.violations?.isNotEmpty() == true) {
                    Text(
                        text = "${status.violations.size} 个违规项",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
```

### DiagnosticViewModel

```kotlin
class DiagnosticViewModel(
    private val diagnosticTool: FolderDiagnosticTool
) : ViewModel() {
    
    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    fun runDiagnostic() {
        viewModelScope.launch {
            _isRunning.value = true
            try {
                val report = diagnosticTool.runFullDiagnostic()
                _diagnosticReport.value = report
            } catch (e: Exception) {
                // 处理错误
            } finally {
                _isRunning.value = false
            }
        }
    }
    
    fun exportReport() {
        viewModelScope.launch {
            try {
                diagnosticTool.exportDiagnosticLogs(ExportFormat.JSON)
                // 显示成功消息
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}

enum class ExportFormat {
    JSON, CSV
}
```


## 修复方案

### 方案 1: 强制路径隔离

**问题**: 本地和在线管理可能使用相同的 `parentPath`

**解决方案**:
1. 定义明确的路径规则：
   - 本地管理: `/storage/emulated/0/Paysage/Local/*`
   - 在线管理: `/storage/emulated/0/Paysage/Online/*`

2. 在 `FolderRepository.createFolder()` 中添加路径验证：
```kotlin
private fun validatePathForModule(path: String, moduleType: ModuleType): Boolean {
    return when (moduleType) {
        ModuleType.LOCAL_MANAGEMENT -> path.startsWith("/storage/emulated/0/Paysage/Local")
        ModuleType.ONLINE_MANAGEMENT -> path.startsWith("/storage/emulated/0/Paysage/Online")
    }
}
```

3. 在创建文件夹前强制验证：
```kotlin
override suspend fun createFolder(
    parentPath: String,
    folderName: String,
    moduleType: ModuleType
): Folder = withContext(Dispatchers.IO) {
    // 验证路径
    if (!validatePathForModule(parentPath, moduleType)) {
        throw FolderCreationException(
            "Path $parentPath is not valid for module type $moduleType"
        )
    }
    // ... 继续创建逻辑
}
```

### 方案 2: 数据库约束增强

**问题**: 数据库层面缺少约束防止重复

**解决方案**:
1. 添加唯一约束：
```kotlin
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["module_type"], name = "idx_folders_module_type"),
        Index(value = ["parent_path"], name = "idx_folders_parent_path"),
        Index(value = ["sort_order"], name = "idx_folders_sort_order"),
        // 新增：防止同一路径在同一模块下重复
        Index(
            value = ["path", "module_type"], 
            name = "idx_folders_path_module",
            unique = true
        )
    ]
)
```

2. 添加数据库迁移：
```kotlin
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 先清理重复数据
        database.execSQL("""
            DELETE FROM folders 
            WHERE id NOT IN (
                SELECT MIN(id) 
                FROM folders 
                GROUP BY path, module_type
            )
        """)
        
        // 添加唯一约束
        database.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_folders_path_module 
            ON folders(path, module_type)
        """)
    }
}
```

### 方案 3: Repository 层拦截

**问题**: 缺少运行时验证机制

**解决方案**:
1. 在 `FolderRepositoryImpl` 中集成 `IsolationEnforcer`：
```kotlin
class FolderRepositoryImpl(
    private val context: Context,
    private val database: PaysageDatabase,
    private val isolationEnforcer: IsolationEnforcer,
    private val diagnosticLogger: DiagnosticLogger
) : FolderRepository {
    
    override suspend fun createFolder(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ): Folder = withContext(Dispatchers.IO) {
        // 强制隔离检查
        val enforcementResult = isolationEnforcer.enforceFolderCreation(
            parentPath, folderName, moduleType
        )
        
        if (enforcementResult is EnforcementResult.Denied) {
            throw FolderCreationException(enforcementResult.reason)
        }
        
        // 记录操作
        diagnosticLogger.logFolderOperation(
            operation = FolderOperation.CREATE,
            moduleType = moduleType,
            folderId = null,
            folderName = folderName,
            parentPath = parentPath,
            result = OperationResult.SUCCESS
        )
        
        // ... 继续原有逻辑
    }
}
```

### 方案 4: UI 层参数验证

**问题**: UI 调用时可能传递错误的 `moduleType`

**解决方案**:
1. 在 `FolderManagementScreen` 中明确传递 `moduleType`：
```kotlin
@Composable
fun FolderManagementScreen(
    moduleType: ModuleType,  // 明确要求传递
    parentPath: String,
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    // 确保所有操作都使用正确的 moduleType
    LaunchedEffect(parentPath, moduleType) {
        viewModel.refreshFolders(parentPath, moduleType)
    }
    
    // 创建文件夹时传递 moduleType
    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { folderName ->
                viewModel.createFolder(parentPath, folderName, moduleType)
                showCreateDialog = false
            }
        )
    }
}
```

2. 在导航时确保传递正确的 `moduleType`：
```kotlin
// 本地管理导航
navController.navigate("folder_management/local/$path")

// 在线管理导航
navController.navigate("folder_management/online/$path")

// 导航处理
composable("folder_management/{type}/{path}") { backStackEntry ->
    val type = backStackEntry.arguments?.getString("type")
    val moduleType = when (type) {
        "local" -> ModuleType.LOCAL_MANAGEMENT
        "online" -> ModuleType.ONLINE_MANAGEMENT
        else -> throw IllegalArgumentException("Invalid module type")
    }
    
    FolderManagementScreen(
        moduleType = moduleType,
        parentPath = backStackEntry.arguments?.getString("path") ?: "",
        onNavigateBack = { navController.popBackStack() }
    )
}
```

## 测试策略

### 单元测试

1. **DatabaseAnalyzer 测试**:
```kotlin
@Test
fun `findPotentialDuplicates should detect cross-module duplicates`() = runTest {
    // 准备测试数据
    val localFolder = Folder(
        id = 1,
        name = "TestFolder",
        path = "/test/path",
        parentPath = "/test",
        moduleType = ModuleType.LOCAL_MANAGEMENT,
        createdAt = System.currentTimeMillis()
    )
    
    val onlineFolder = localFolder.copy(
        id = 2,
        moduleType = ModuleType.ONLINE_MANAGEMENT
    )
    
    folderDao.insert(localFolder)
    folderDao.insert(onlineFolder)
    
    // 执行测试
    val analyzer = DatabaseAnalyzer(folderDao)
    val duplicates = analyzer.findPotentialDuplicates()
    
    // 验证结果
    assertEquals(1, duplicates.size)
    assertEquals("TestFolder", duplicates[0].name)
}
```

2. **IsolationValidator 测试**:
```kotlin
@Test
fun `validateFolderOperation should detect module type mismatch`() = runTest {
    val folder = Folder(
        id = 1,
        name = "Test",
        path = "/test",
        parentPath = "/",
        moduleType = ModuleType.LOCAL_MANAGEMENT,
        createdAt = System.currentTimeMillis()
    )
    folderDao.insert(folder)
    
    val validator = IsolationValidator(folderDao)
    val result = validator.validateFolderOperation(
        folderId = 1,
        expectedModuleType = ModuleType.ONLINE_MANAGEMENT
    )
    
    assertFalse(result.isValid)
    assertTrue(result.message.contains("mismatch"))
}
```

### 集成测试

1. **完整诊断流程测试**:
```kotlin
@Test
fun `full diagnostic should detect all isolation issues`() = runTest {
    // 创建测试数据
    setupTestData()
    
    // 运行诊断
    val diagnosticTool = FolderDiagnosticTool(database, context)
    val report = diagnosticTool.runFullDiagnostic()
    
    // 验证报告
    assertFalse(report.isolationStatus.isIsolated)
    assertTrue(report.duplicateFolders.isNotEmpty())
    assertTrue(report.pathConflicts.isNotEmpty())
}
```

2. **修复流程测试**:
```kotlin
@Test
fun `cleanup should remove duplicates correctly`() = runTest {
    // 创建重复数据
    createDuplicateFolders()
    
    // 执行清理
    val migrationTool = DataMigrationTool(database, folderDao)
    val report = migrationTool.cleanupDuplicateFolders(CleanupStrategy.KEEP_LOCAL)
    
    // 验证结果
    assertEquals(1, report.cleaned.size)
    assertEquals(0, report.failed.size)
    
    // 验证数据库状态
    val remaining = folderDao.getAllFolders()
    assertEquals(1, remaining.size)
    assertEquals(ModuleType.LOCAL_MANAGEMENT, remaining[0].moduleType)
}
```

## 性能考虑

1. **诊断操作优化**:
   - 使用索引加速查询
   - 批量操作减少数据库访问
   - 异步执行避免阻塞 UI

2. **日志系统优化**:
   - 使用缓冲写入减少 I/O
   - 定期清理旧日志
   - 可配置的日志级别

3. **内存管理**:
   - 分页加载大量数据
   - 及时释放不需要的对象
   - 使用 Flow 进行流式处理

## 安全考虑

1. **权限检查**:
   - 验证文件系统访问权限
   - 确保只有授权用户可以访问诊断工具

2. **数据保护**:
   - 导出的日志不包含敏感信息
   - 诊断工具仅在 debug 模式可用

3. **操作审计**:
   - 记录所有修复操作
   - 提供回滚机制

## 部署计划

1. **阶段 1: 诊断工具开发** (1-2 天)
   - 实现核心诊断组件
   - 添加 DAO 扩展方法
   - 创建诊断 UI

2. **阶段 2: 问题分析** (1 天)
   - 在测试环境运行诊断
   - 分析诊断报告
   - 确定根本原因

3. **阶段 3: 修复实施** (2-3 天)
   - 实施路径隔离
   - 添加数据库约束
   - 集成隔离强制器

4. **阶段 4: 数据清理** (1 天)
   - 运行数据迁移工具
   - 清理重复记录
   - 验证数据完整性

5. **阶段 5: 测试验证** (1-2 天)
   - 执行单元测试
   - 执行集成测试
   - 用户验收测试

6. **阶段 6: 监控部署** (1 天)
   - 部署到生产环境
   - 启用监控
   - 观察运行状态

---

**设计版本**: 1.0  
**创建日期**: 2025-10-28  
**作者**: Kiro AI Assistant
