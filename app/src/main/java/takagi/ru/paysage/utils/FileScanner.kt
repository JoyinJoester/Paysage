package takagi.ru.paysage.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import takagi.ru.paysage.data.model.BookFormat
import java.io.File

/**
 * 文件扫描器
 * 用于扫描本地存储中的漫画和 PDF 文件
 */
class FileScanner(private val context: Context) {
    
    companion object {
        private val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "cbz", "cbr", "cbt", "cb7", "zip", "rar", "tar", "7z"
        )
    }
    
    /**
     * 扫描指定目录
     */
    suspend fun scanDirectory(directory: File): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFile>()
        scanRecursive(directory, results)
        results
    }
    
    /**
     * 扫描 URI 目录（用于 SAF - Storage Access Framework）
     */
    suspend fun scanDirectoryUri(uri: Uri): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFile>()
        val documentFile = DocumentFile.fromTreeUri(context, uri)
        if (documentFile != null && documentFile.isDirectory) {
            scanDocumentFileRecursive(documentFile, results)
        }
        results
    }
    
    /**
     * 扫描默认目录（Downloads, Documents）
     */
    suspend fun scanDefaultDirectories(): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFile>()
        
        // 扫描 Downloads
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) {
            scanRecursive(downloadsDir, results)
        }
        
        // 扫描 Documents
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir.exists()) {
            scanRecursive(documentsDir, results)
        }
        
        // 扫描应用私有目录
        val appDir = context.getExternalFilesDir(null)
        if (appDir != null && appDir.exists()) {
            scanRecursive(appDir, results)
        }
        
        results
    }
    
    private fun scanRecursive(directory: File, results: MutableList<ScannedFile>) {
        try {
            val files = directory.listFiles() ?: return
            
            for (file in files) {
                if (file.isDirectory) {
                    scanRecursive(file, results)
                } else if (file.isFile && isSupportedFile(file)) {
                    val scannedFile = createScannedFile(file)
                    if (scannedFile != null) {
                        results.add(scannedFile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun isSupportedFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }
    
    private fun createScannedFile(file: File): ScannedFile? {
        return try {
            val format = BookFormat.fromFileName(file.name) ?: return null
            ScannedFile(
                path = file.absolutePath,
                name = file.nameWithoutExtension,
                size = file.length(),
                format = format,
                lastModified = file.lastModified()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 递归扫描 DocumentFile
     */
    private fun scanDocumentFileRecursive(documentFile: DocumentFile, results: MutableList<ScannedFile>) {
        try {
            val files = documentFile.listFiles()
            
            for (file in files) {
                if (file.isDirectory) {
                    scanDocumentFileRecursive(file, results)
                } else if (file.isFile && isSupportedDocumentFile(file)) {
                    val scannedFile = createScannedFileFromDocument(file)
                    if (scannedFile != null) {
                        results.add(scannedFile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 检查 DocumentFile 是否为支持的文件类型
     */
    private fun isSupportedDocumentFile(file: DocumentFile): Boolean {
        val name = file.name ?: return false
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }
    
    /**
     * 从 DocumentFile 创建 ScannedFile
     */
    private fun createScannedFileFromDocument(file: DocumentFile): ScannedFile? {
        return try {
            val name = file.name ?: return null
            val format = BookFormat.fromFileName(name) ?: return null
            val uri = file.uri.toString()
            
            ScannedFile(
                path = uri, // 使用 URI 作为路径
                name = name.substringBeforeLast('.'),
                size = file.length(),
                format = format,
                lastModified = file.lastModified()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * 扫描到的文件信息
 */
data class ScannedFile(
    val path: String,
    val name: String,
    val size: Long,
    val format: BookFormat,
    val lastModified: Long
)
