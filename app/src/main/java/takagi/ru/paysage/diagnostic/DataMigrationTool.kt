package takagi.ru.paysage.diagnostic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.dao.FolderDao
import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.diagnostic.model.DuplicateFolder
import java.io.File

/**
 * 数据迁移工具
 * 清理重复数据，修复隔离问题
 */
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
    
    /**
     * 查找重复文件夹
     */
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

/**
 * 清理策略
 */
enum class CleanupStrategy {
    KEEP_LOCAL,
    KEEP_ONLINE,
    KEEP_NEWER
}

/**
 * 清理报告
 */
data class CleanupReport(
    val totalDuplicates: Int,
    val cleaned: List<Long>,
    val failed: List<CleanupFailure>
)

/**
 * 清理失败
 */
data class CleanupFailure(
    val duplicate: DuplicateFolder,
    val error: String
)

/**
 * 修复报告
 */
data class FixReport(
    val totalChecked: Int,
    val fixed: List<Long>
)
