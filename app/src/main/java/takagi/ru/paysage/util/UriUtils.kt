package takagi.ru.paysage.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * URI 工具类
 * 用于处理 Android 的 content:// URI
 */
object UriUtils {
    
    /**
     * 将 content:// URI 转换为可读的路径字符串
     * 例如: content://...tree/primary%3ATest -> 内部存储/Test
     */
    fun getReadablePath(context: Context, uri: String?): String? {
        if (uri == null) return null
        
        return try {
            val parsedUri = Uri.parse(uri)
            getReadablePathFromUri(context, parsedUri)
        } catch (e: Exception) {
            // 如果解析失败，返回原始 URI
            uri
        }
    }
    
    /**
     * 从 Uri 对象获取可读路径
     */
    private fun getReadablePathFromUri(context: Context, uri: Uri): String {
        // 处理 DocumentsContract tree URI
        if (DocumentsContract.isTreeUri(uri)) {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            return parseDocumentId(docId)
        }
        
        // 处理 DocumentsContract document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            return parseDocumentId(docId)
        }
        
        // 如果无法解析，返回 URI 字符串
        return uri.toString()
    }
    
    /**
     * 解析 document ID 为可读路径
     * 例如: "primary:Test" -> "内部存储/Test"
     *      "1234-5678:folder" -> "SD卡/folder"
     */
    private fun parseDocumentId(docId: String): String {
        val parts = docId.split(":", limit = 2)
        
        if (parts.size != 2) {
            return docId
        }
        
        val storageType = parts[0]
        val path = parts[1]
        
        // 解码 URL 编码的路径
        val decodedPath = Uri.decode(path)
        
        return when {
            storageType == "primary" -> {
                // 主存储（内部存储）
                if (decodedPath.isEmpty()) {
                    "内部存储"
                } else {
                    "内部存储/$decodedPath"
                }
            }
            storageType == "home" -> {
                // Home 目录
                if (decodedPath.isEmpty()) {
                    "主目录"
                } else {
                    "主目录/$decodedPath"
                }
            }
            storageType.matches(Regex("[0-9A-F]{4}-[0-9A-F]{4}")) -> {
                // SD 卡（格式如 1234-5678）
                if (decodedPath.isEmpty()) {
                    "SD卡 ($storageType)"
                } else {
                    "SD卡/$decodedPath"
                }
            }
            else -> {
                // 其他存储类型
                if (decodedPath.isEmpty()) {
                    storageType
                } else {
                    "$storageType/$decodedPath"
                }
            }
        }
    }
    
    /**
     * 获取简短的路径名称（只显示最后一个文件夹名）
     * 例如: "内部存储/Download/Manga" -> "Manga"
     */
    fun getShortPath(fullPath: String?): String? {
        if (fullPath == null) return null
        
        val parts = fullPath.split("/")
        return parts.lastOrNull()?.takeIf { it.isNotEmpty() } ?: fullPath
    }
}
