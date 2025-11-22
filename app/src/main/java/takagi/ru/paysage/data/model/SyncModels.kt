package takagi.ru.paysage.data.model

/**
 * 同步选项数据类
 * 用于配置书库同步的各种选项
 */
data class SyncOptions(
    // 维护选项
    val removeDeletedFiles: Boolean = false,
    val updateModifiedFiles: Boolean = false,
    val generateMissingThumbnails: Boolean = false,
    
    // 扫描选项
    val scanSubfolders: Boolean = true,
    val skipHiddenFolders: Boolean = true,
    val parallelSync: Boolean = false
)

/**
 * 同步类型枚举
 * 定义三种不同的同步操作模式
 */
enum class SyncType {
    MAINTENANCE,    // 仅执行维护操作
    INCREMENTAL,    // 增量同步（仅新增和修改）
    FULL            // 完整同步（重新扫描所有文件）
}

/**
 * 同步结果数据类
 * 记录同步操作的执行结果
 */
data class SyncResult(
    val newBooks: Int = 0,
    val updatedBooks: Int = 0,
    val deletedBooks: Int = 0,
    val generatedThumbnails: Int = 0,
    val duration: Long = 0,
    val errors: List<String> = emptyList()
)
