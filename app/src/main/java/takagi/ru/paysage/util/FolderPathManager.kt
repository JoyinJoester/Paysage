package takagi.ru.paysage.util

import android.content.Context
import takagi.ru.paysage.data.model.ModuleType
import java.io.File

/**
 * 文件夹路径管理器
 * 负责为本地和在线管理提供独立的路径
 */
object FolderPathManager {
    
    /**
     * 获取指定模块的根路径
     */
    fun getModulePath(context: Context, moduleType: ModuleType): String {
        val baseDir = context.getExternalFilesDir(null)?.absolutePath ?: ""
        return when (moduleType) {
            ModuleType.LOCAL_MANAGEMENT -> "$baseDir/Local"
            ModuleType.ONLINE_MANAGEMENT -> "$baseDir/Online"
        }
    }
    
    /**
     * 初始化文件夹结构
     * 在应用启动时调用，确保目录存在
     */
    fun initializeFolderStructure(context: Context) {
        val baseDir = context.getExternalFilesDir(null)
        
        // 创建本地管理目录
        val localDir = File(baseDir, "Local")
        if (!localDir.exists()) {
            localDir.mkdirs()
            android.util.Log.d("FolderPathManager", "Created Local directory: ${localDir.absolutePath}")
        }
        
        // 创建在线管理目录
        val onlineDir = File(baseDir, "Online")
        if (!onlineDir.exists()) {
            onlineDir.mkdirs()
            android.util.Log.d("FolderPathManager", "Created Online directory: ${onlineDir.absolutePath}")
        }
    }
}
