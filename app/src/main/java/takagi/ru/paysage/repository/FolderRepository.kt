package takagi.ru.paysage.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.PaysageDatabase
import takagi.ru.paysage.data.model.Folder
import takagi.ru.paysage.data.model.ModuleType
import java.io.File

/**
 * 排序选项
 */
enum class FolderSortOption {
    NAME,           // 按名称排序
    CREATED_TIME,   // 按创建时间排序
    UPDATED_TIME,   // 按修改时间排序
    CUSTOM          // 自定义排序
}

/**
 * 文件夹Repository接口
 */
interface FolderRepository {
    suspend fun createFolder(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ): Folder
    
    suspend fun getFolders(
        path: String,
        moduleType: ModuleType
    ): List<Folder>
    
    suspend fun getFolders(
        path: String,
        moduleType: ModuleType,
        sortOption: FolderSortOption
    ): List<Folder>
    
    suspend fun deleteFolder(
        folderPath: String,
        moduleType: ModuleType
    ): Boolean
    
    suspend fun renameFolder(
        folderId: Long,
        newName: String
    ): Folder
    
    suspend fun deleteFolders(
        folderIds: List<Long>
    ): List<Long>
    
    suspend fun updateFolderOrder(
        folders: List<Folder>
    )
}

/**
 * 文件夹Repository实现
 */
class FolderRepositoryImpl(
    private val context: Context,
    private val database: PaysageDatabase
) : FolderRepository {
    
    // 路径验证规则
    // 注意：本地和在线管理应该使用不同的子目录来区分
    private val localPathPrefixes = listOf(
        "/storage/emulated/0/Android/data/takagi.ru.paysage/files/Local",
        "/storage/emulated/0/Android/data/takagi.ru.paysage/files",  // 临时兼容：如果没有子目录，允许根目录用于本地
        "/storage/emulated/0/Paysage/Local",
        "/sdcard/Paysage/Local",
        "/sdcard/Android/data/takagi.ru.paysage/files/Local",
        "/sdcard/Android/data/takagi.ru.paysage/files"  // 临时兼容
    )
    
    private val onlinePathPrefixes = listOf(
        "/storage/emulated/0/Android/data/takagi.ru.paysage/files/Online",
        "/storage/emulated/0/Paysage/Online",
        "/sdcard/Paysage/Online",
        "/sdcard/Android/data/takagi.ru.paysage/files/Online"
    )
    
    override suspend fun createFolder(
        parentPath: String,
        folderName: String,
        moduleType: ModuleType
    ): Folder = withContext(Dispatchers.IO) {
        // 验证输入
        if (parentPath.isBlank()) {
            throw FolderCreationException("Parent path is empty")
        }
        
        if (folderName.isBlank()) {
            throw FolderCreationException("Folder name is empty")
        }
        
        // 【关键修复】验证路径是否属于正确的模块
        if (!validatePathForModule(parentPath, moduleType)) {
            val expectedPaths = when (moduleType) {
                ModuleType.LOCAL_MANAGEMENT -> localPathPrefixes
                ModuleType.ONLINE_MANAGEMENT -> onlinePathPrefixes
            }
            throw FolderCreationException(
                "Path '$parentPath' is not valid for module type $moduleType. " +
                "Expected path to start with: ${expectedPaths.joinToString(" or ")}"
            )
        }
        
        // 验证父路径存在
        val parentDir = File(parentPath)
        if (!parentDir.exists()) {
            throw FolderCreationException("Parent directory does not exist: $parentPath")
        }
        
        if (!parentDir.canWrite()) {
            throw FolderCreationException("No write permission for parent directory: $parentPath")
        }
        
        val folderPath = File(parentPath, folderName)
        
        // 检查文件夹是否已存在
        if (folderPath.exists()) {
            throw FolderAlreadyExistsException(folderName)
        }
        
        // 创建文件夹
        val created = folderPath.mkdirs()
        if (!created && !folderPath.exists()) {
            throw FolderCreationException("Failed to create folder: $folderName at $parentPath")
        }
        
        // 保存到数据库
        val folder = Folder(
            id = 0,
            name = folderName,
            path = folderPath.absolutePath,
            parentPath = parentPath,
            moduleType = moduleType,
            createdAt = System.currentTimeMillis()
        )
        
        try {
            val folderId = database.folderDao().insert(folder)
            folder.copy(id = folderId)
        } catch (e: Exception) {
            // 如果数据库插入失败，删除已创建的文件夹
            folderPath.deleteRecursively()
            throw FolderCreationException("Failed to save folder to database: ${e.message}")
        }
    }
    
    override suspend fun getFolders(
        path: String,
        moduleType: ModuleType
    ): List<Folder> = withContext(Dispatchers.IO) {
        database.folderDao().getFoldersByPath(path, moduleType)
    }
    
    override suspend fun getFolders(
        path: String,
        moduleType: ModuleType,
        sortOption: FolderSortOption
    ): List<Folder> = withContext(Dispatchers.IO) {
        when (sortOption) {
            FolderSortOption.NAME -> database.folderDao().getFoldersByPathByName(path, moduleType)
            FolderSortOption.CREATED_TIME -> database.folderDao().getFoldersByPathByCreatedTime(path, moduleType)
            FolderSortOption.UPDATED_TIME -> database.folderDao().getFoldersByPathByUpdatedTime(path, moduleType)
            FolderSortOption.CUSTOM -> database.folderDao().getFoldersByPathSorted(path, moduleType)
        }
    }
    
    override suspend fun deleteFolder(
        folderPath: String,
        moduleType: ModuleType
    ): Boolean = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        val deleted = folder.deleteRecursively()
        if (deleted) {
            database.folderDao().deleteByPath(folderPath)
        }
        deleted
    }
    
    override suspend fun renameFolder(
        folderId: Long,
        newName: String
    ): Folder = withContext(Dispatchers.IO) {
        // 验证输入
        if (newName.isBlank()) {
            throw FolderRenameException("Folder name is empty")
        }
        
        // 验证名称是否包含非法字符
        if (newName.containsIllegalChars()) {
            throw FolderRenameException("Folder name contains illegal characters")
        }
        
        // 获取原文件夹信息
        val folder = database.folderDao().getFolderById(folderId)
            ?: throw FolderRenameException("Folder not found")
        
        // 检查新名称是否已存在
        val nameExists = database.folderDao().folderNameExists(
            folder.parentPath,
            newName,
            folderId
        ) > 0
        
        if (nameExists) {
            throw FolderAlreadyExistsException(newName)
        }
        
        // 重命名文件系统中的文件夹
        val oldFile = File(folder.path)
        val newFile = File(folder.parentPath, newName)
        
        if (!oldFile.exists()) {
            throw FolderRenameException("Folder does not exist: ${folder.path}")
        }
        
        val renamed = oldFile.renameTo(newFile)
        if (!renamed) {
            throw FolderRenameException("Failed to rename folder in file system")
        }
        
        // 更新数据库
        val updatedFolder = folder.copy(
            name = newName,
            path = newFile.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        
        try {
            database.folderDao().update(updatedFolder)
            updatedFolder
        } catch (e: Exception) {
            // 如果数据库更新失败，回滚文件系统更改
            newFile.renameTo(oldFile)
            throw FolderRenameException("Failed to update folder in database: ${e.message}")
        }
    }
    
    override suspend fun deleteFolders(
        folderIds: List<Long>
    ): List<Long> = withContext(Dispatchers.IO) {
        val successfullyDeleted = mutableListOf<Long>()
        val failedDeletes = mutableListOf<Long>()
        
        for (folderId in folderIds) {
            try {
                val folder = database.folderDao().getFolderById(folderId)
                if (folder != null) {
                    val file = File(folder.path)
                    val deleted = file.deleteRecursively()
                    
                    if (deleted) {
                        database.folderDao().deleteById(folderId)
                        successfullyDeleted.add(folderId)
                    } else {
                        failedDeletes.add(folderId)
                    }
                }
            } catch (e: Exception) {
                failedDeletes.add(folderId)
            }
        }
        
        if (failedDeletes.isNotEmpty()) {
            throw FolderBatchDeleteException(
                "Failed to delete ${failedDeletes.size} folders",
                failedDeletes
            )
        }
        
        successfullyDeleted
    }
    
    override suspend fun updateFolderOrder(
        folders: List<Folder>
    ) = withContext(Dispatchers.IO) {
        folders.forEachIndexed { index, folder ->
            val updatedFolder = folder.copy(
                sortOrder = index,
                updatedAt = System.currentTimeMillis()
            )
            database.folderDao().update(updatedFolder)
        }
    }
    
    /**
     * 验证路径是否属于指定模块
     */
    private fun validatePathForModule(path: String, moduleType: ModuleType): Boolean {
        return when (moduleType) {
            ModuleType.LOCAL_MANAGEMENT -> localPathPrefixes.any { path.startsWith(it) }
            ModuleType.ONLINE_MANAGEMENT -> onlinePathPrefixes.any { path.startsWith(it) }
        }
    }
}

/**
 * 文件夹已存在异常
 */
class FolderAlreadyExistsException(folderName: String) : 
    Exception("Folder already exists: $folderName")

/**
 * 文件夹创建异常
 */
class FolderCreationException(message: String) : Exception(message)

/**
 * 文件夹重命名异常
 */
class FolderRenameException(message: String) : Exception(message)

/**
 * 文件夹批量删除异常
 */
class FolderBatchDeleteException(
    message: String,
    val failedIds: List<Long>
) : Exception(message)

/**
 * 检查字符串是否包含非法字符
 */
private fun String.containsIllegalChars(): Boolean {
    val illegalChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    return any { it in illegalChars }
}
