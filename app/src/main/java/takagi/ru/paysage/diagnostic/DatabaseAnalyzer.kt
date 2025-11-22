package takagi.ru.paysage.diagnostic

import takagi.ru.paysage.data.dao.FolderDao
import takagi.ru.paysage.data.model.ModuleType
import takagi.ru.paysage.diagnostic.model.DuplicateFolder

/**
 * 数据库分析器
 * 分析数据库中的文件夹记录，检测隔离问题
 */
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
    
    /**
     * 计算两个文件夹的相似度
     */
    private fun calculateSimilarity(folder1: takagi.ru.paysage.data.model.Folder, folder2: takagi.ru.paysage.data.model.Folder): Float {
        var score = 0f
        if (folder1.name == folder2.name) score += 0.4f
        if (folder1.parentPath == folder2.parentPath) score += 0.4f
        if (folder1.path == folder2.path) score += 0.2f
        return score
    }
}
