package takagi.ru.paysage.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 文件夹数据模型
 * 用于管理本地和在线模块的文件夹结构
 */
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["module_type"], name = "idx_folders_module_type"),
        Index(value = ["parent_path"], name = "idx_folders_parent_path"),
        Index(value = ["sort_order"], name = "idx_folders_sort_order"),
        Index(value = ["path", "module_type"], name = "idx_folders_path_module", unique = true)
    ]
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "path")
    val path: String,
    
    @ColumnInfo(name = "parent_path")
    val parentPath: String,
    
    @ColumnInfo(name = "module_type")
    val moduleType: ModuleType,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = createdAt,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)

/**
 * 模块类型枚举
 */
enum class ModuleType {
    LOCAL_MANAGEMENT,    // 本地管理
    ONLINE_MANAGEMENT    // 在线管理
}
