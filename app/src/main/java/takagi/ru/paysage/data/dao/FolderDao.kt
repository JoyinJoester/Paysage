package takagi.ru.paysage.data.dao

import androidx.room.*
import takagi.ru.paysage.data.model.Folder
import takagi.ru.paysage.data.model.ModuleType

/**
 * 文件夹数据访问对象
 */
@Dao
interface FolderDao {
    /**
     * 插入文件夹
     * @return 插入的文件夹ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder): Long
    
    /**
     * 根据父路径和模块类型获取文件夹列表
     */
    @Query("SELECT * FROM folders WHERE parent_path = :path AND module_type = :moduleType ORDER BY name COLLATE NOCASE ASC")
    suspend fun getFoldersByPath(path: String, moduleType: ModuleType): List<Folder>
    
    /**
     * 根据路径删除文件夹
     */
    @Query("DELETE FROM folders WHERE path = :path")
    suspend fun deleteByPath(path: String)
    
    /**
     * 根据ID获取文件夹
     */
    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): Folder?
    
    /**
     * 获取所有文件夹
     */
    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<Folder>
    
    /**
     * 检查文件夹是否存在
     */
    @Query("SELECT COUNT(*) FROM folders WHERE path = :path")
    suspend fun folderExists(path: String): Int
    
    /**
     * 更新文件夹
     */
    @Update
    suspend fun update(folder: Folder)
    
    /**
     * 根据ID删除文件夹
     */
    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * 批量删除文件夹
     */
    @Query("DELETE FROM folders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    /**
     * 根据父路径和模块类型获取文件夹列表（按sortOrder排序）
     */
    @Query("SELECT * FROM folders WHERE parent_path = :path AND module_type = :moduleType ORDER BY sort_order ASC, name COLLATE NOCASE ASC")
    suspend fun getFoldersByPathSorted(path: String, moduleType: ModuleType): List<Folder>
    
    /**
     * 根据父路径和模块类型获取文件夹列表（按名称排序）
     */
    @Query("SELECT * FROM folders WHERE parent_path = :path AND module_type = :moduleType ORDER BY name COLLATE NOCASE ASC")
    suspend fun getFoldersByPathByName(path: String, moduleType: ModuleType): List<Folder>
    
    /**
     * 根据父路径和模块类型获取文件夹列表（按创建时间排序）
     */
    @Query("SELECT * FROM folders WHERE parent_path = :path AND module_type = :moduleType ORDER BY created_at DESC")
    suspend fun getFoldersByPathByCreatedTime(path: String, moduleType: ModuleType): List<Folder>
    
    /**
     * 根据父路径和模块类型获取文件夹列表（按修改时间排序）
     */
    @Query("SELECT * FROM folders WHERE parent_path = :path AND module_type = :moduleType ORDER BY updated_at DESC")
    suspend fun getFoldersByPathByUpdatedTime(path: String, moduleType: ModuleType): List<Folder>
    
    /**
     * 检查文件夹名称是否存在（用于重命名验证）
     */
    @Query("SELECT COUNT(*) FROM folders WHERE parent_path = :parentPath AND name = :name AND id != :excludeId")
    suspend fun folderNameExists(parentPath: String, name: String, excludeId: Long): Int
    
    // ========== 诊断相关方法 ==========
    
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
