package takagi.ru.paysage.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipFile

private const val TAG = "FileParser"

/**
 * 文件解析工具类
 * 用于解析各种格式的漫画文件和 PDF
 */
class FileParser(private val context: Context) {
    
    /**
     * 获取文件的总页数
     */
    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        when (extension) {
            "pdf" -> getPdfPageCount(file)
            "cbz", "zip" -> getArchivePageCount(file)
            "cbr", "rar" -> getArchivePageCount(file)
            "cbt", "tar" -> getArchivePageCount(file)
            "cb7", "7z" -> getArchivePageCount(file)
            else -> 0
        }
    }
    
    /**
     * 提取指定页面的图像
     */
    suspend fun extractPage(file: File, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        when (extension) {
            "pdf" -> extractPdfPage(file, pageIndex)
            "cbz", "zip" -> extractArchivePage(file, pageIndex)
            "cbr", "rar" -> extractArchivePage(file, pageIndex)
            "cbt", "tar" -> extractArchivePage(file, pageIndex)
            "cb7", "7z" -> extractArchivePage(file, pageIndex)
            else -> null
        }
    }
    
    /**
     * 提取指定页面的图像（带采样选项）
     */
    suspend fun extractPageWithOptions(file: File, pageIndex: Int, options: BitmapFactory.Options): Bitmap? = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        when (extension) {
            "pdf" -> extractPdfPage(file, pageIndex)  // PDF 使用自己的缩放
            "cbz", "zip" -> extractZipPage(file, pageIndex, options)
            "cbr", "rar" -> extractRarPage(file, pageIndex)  // RAR 暂不支持采样
            "cbt", "tar" -> extractTarPage(file, pageIndex)  // TAR 暂不支持采样
            "cb7", "7z" -> extract7ZPage(file, pageIndex)  // 7Z 暂不支持采样
            else -> null
        }
    }
    
    /**
     * 从 URI 提取指定页面的图像
     */
    suspend fun extractPageFromUri(uri: Uri, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameFromUri(uri) ?: return@withContext null
            val extension = fileName.substringAfterLast('.', "").lowercase()
            
            when (extension) {
                "pdf" -> extractPdfPageFromUri(uri, pageIndex)
                "cbz", "zip" -> extractArchivePageFromUri(uri, pageIndex)
                "cbr", "rar" -> extractArchivePageFromUri(uri, pageIndex)
                "cbt", "tar" -> extractArchivePageFromUri(uri, pageIndex)
                "cb7", "7z" -> extractArchivePageFromUri(uri, pageIndex)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting page from URI", e)
            null
        }
    }
    
    /**
     * 提取封面图片（第一页）
     */
    suspend fun extractCover(file: File): Bitmap? {
        return extractPage(file, 0)
    }
    
    /**
     * 保存封面到缓存目录
     */
    suspend fun saveCover(file: File, bookId: Long): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating cover for book ID: $bookId, file: ${file.name}")
            val cover = extractCover(file) ?: run {
                Log.w(TAG, "Failed to extract cover from file: ${file.name}")
                return@withContext null
            }
            
            val coverDir = File(context.cacheDir, "covers")
            if (!coverDir.exists()) {
                coverDir.mkdirs()
                Log.d(TAG, "Created covers directory: ${coverDir.absolutePath}")
            }
            
            val coverFile = File(coverDir, "cover_$bookId.jpg")
            FileOutputStream(coverFile).use { out ->
                cover.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            cover.recycle()
            Log.d(TAG, "Cover saved successfully: ${coverFile.absolutePath}")
            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cover for book ID: $bookId", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取 URI 文件的总页数
     */
    suspend fun getPageCountFromUri(uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameFromUri(uri) ?: return@withContext 0
            val extension = fileName.substringAfterLast('.', "").lowercase()
            
            when (extension) {
                "pdf" -> getPdfPageCountFromUri(uri)
                "cbz", "zip" -> getArchivePageCountFromUri(uri)
                "cbr", "rar" -> getArchivePageCountFromUri(uri)
                "cbt", "tar" -> getArchivePageCountFromUri(uri)
                "cb7", "7z" -> getArchivePageCountFromUri(uri)
                else -> 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page count from URI: $uri", e)
            0
        }
    }
    
    /**
     * 从 URI 提取封面并保存
     */
    suspend fun saveCoverFromUri(uri: Uri, bookId: Long): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating cover from URI for book ID: $bookId")
            val cover = extractCoverFromUri(uri) ?: run {
                Log.w(TAG, "Failed to extract cover from URI: $uri")
                return@withContext null
            }
            
            val coverDir = File(context.cacheDir, "covers")
            if (!coverDir.exists()) {
                coverDir.mkdirs()
            }
            
            val coverFile = File(coverDir, "cover_$bookId.jpg")
            FileOutputStream(coverFile).use { out ->
                cover.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            cover.recycle()
            Log.d(TAG, "Cover saved successfully from URI: ${coverFile.absolutePath}")
            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cover from URI for book ID: $bookId", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从 URI 提取封面
     */
    private suspend fun extractCoverFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameFromUri(uri) ?: return@withContext null
            val extension = fileName.substringAfterLast('.', "").lowercase()
            
            when (extension) {
                "pdf" -> extractPdfPageFromUri(uri, 0)
                "cbz", "zip" -> extractArchivePageFromUri(uri, 0)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting cover from URI", e)
            null
        }
    }
    
    /**
     * 获取 URI 的文件名
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    uri.lastPathSegment
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
    
    /**
     * 从 URI 获取 PDF 页数
     */
    private fun getPdfPageCountFromUri(uri: Uri): Int {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF page count from URI", e)
            0
        }
    }
    
    /**
     * 从 URI 提取 PDF 页面
     */
    private fun extractPdfPageFromUri(uri: Uri, pageIndex: Int): Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex >= renderer.pageCount) return null
                    
                    renderer.openPage(pageIndex).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width,
                            page.height,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PDF page from URI", e)
            null
        }
    }
    
    /**
     * 从 URI 获取压缩包页数
     */
    private fun getArchivePageCountFromUri(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = File.createTempFile("temp_archive", ".zip", context.cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                val count = getArchivePageCount(tempFile)
                tempFile.delete()
                count
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting archive page count from URI", e)
            0
        }
    }
    
    /**
     * 从 URI 提取压缩包页面
     */
    private fun extractArchivePageFromUri(uri: Uri, pageIndex: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = File.createTempFile("temp_archive", ".zip", context.cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                val bitmap = extractArchivePage(tempFile, pageIndex)
                tempFile.delete()
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting archive page from URI", e)
            null
        }
    }
    
    // PDF 相关方法
    private fun getPdfPageCount(file: File): Int {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun extractPdfPage(file: File, pageIndex: Int): Bitmap? {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return null
            }
            
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // 压缩文件相关方法
    private fun getArchivePageCount(file: File): Int {
        return try {
            when (file.extension.lowercase()) {
                "cbz", "zip" -> getZipPageCount(file)
                "cbr", "rar" -> getRarPageCount(file)
                "cb7", "7z" -> get7ZPageCount(file)
                "cbt", "tar" -> getTarPageCount(file)
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun getZipPageCount(file: File): Int {
        return try {
            ZipFile(file).use { zip ->
                zip.entries().toList()
                    .filter { entry ->
                        !entry.isDirectory && isImageFile(entry.name)
                    }
                    .size
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun getRarPageCount(file: File): Int {
        return try {
            Archive(file).use { archive ->
                var count = 0
                var fileHeader: FileHeader? = archive.nextFileHeader()
                while (fileHeader != null) {
                    if (!fileHeader.isDirectory && isImageFile(fileHeader.fileName)) {
                        count++
                    }
                    fileHeader = archive.nextFileHeader()
                }
                count
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun get7ZPageCount(file: File): Int {
        return try {
            SevenZFile(file).use { sevenZFile ->
                var count = 0
                var entry = sevenZFile.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        count++
                    }
                    entry = sevenZFile.nextEntry
                }
                count
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun getTarPageCount(file: File): Int {
        return try {
            FileInputStream(file).use { fis ->
                TarArchiveInputStream(fis).use { tarInput ->
                    var count = 0
                    generateSequence { tarInput.nextEntry }
                        .filterNot { it.isDirectory }
                        .filter { isImageFile(it.name) }
                        .forEach { count++ }
                    count
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    private fun extractArchivePage(file: File, pageIndex: Int): Bitmap? {
        return try {
            when (file.extension.lowercase()) {
                "cbz", "zip" -> extractZipPage(file, pageIndex)
                "cbr", "rar" -> extractRarPage(file, pageIndex)
                "cb7", "7z" -> extract7ZPage(file, pageIndex)
                "cbt", "tar" -> extractTarPage(file, pageIndex)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun extractZipPage(file: File, pageIndex: Int, options: BitmapFactory.Options? = null): Bitmap? {
        return try {
            ZipFile(file).use { zip ->
                val imageEntries = zip.entries().toList()
                    .filter { entry ->
                        !entry.isDirectory && isImageFile(entry.name)
                    }
                    .sortedBy { it.name }
                
                if (pageIndex >= imageEntries.size) return null
                
                val entry = imageEntries[pageIndex]
                val inputStream = zip.getInputStream(entry)
                val bitmap = if (options != null) {
                    BitmapFactory.decodeStream(inputStream, null, options)
                } else {
                    BitmapFactory.decodeStream(inputStream)
                }
                inputStream.close()
                
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun extractRarPage(file: File, pageIndex: Int): Bitmap? {
        return try {
            Archive(file).use { archive ->
                val imageHeaders = mutableListOf<FileHeader>()
                var fileHeader: FileHeader? = archive.nextFileHeader()
                while (fileHeader != null) {
                    if (!fileHeader.isDirectory && isImageFile(fileHeader.fileName)) {
                        imageHeaders.add(fileHeader)
                    }
                    fileHeader = archive.nextFileHeader()
                }
                
                imageHeaders.sortBy { it.fileName }
                
                if (pageIndex >= imageHeaders.size) return null
                
                // 重新打开 archive 以提取指定文件
                Archive(file).use { extractArchive ->
                    val targetHeader = imageHeaders[pageIndex]
                    var currentHeader: FileHeader? = extractArchive.nextFileHeader()
                    while (currentHeader != null) {
                        if (currentHeader.fileName == targetHeader.fileName) {
                            val outputStream = ByteArrayOutputStream()
                            extractArchive.extractFile(currentHeader, outputStream)
                            val bytes = outputStream.toByteArray()
                            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        currentHeader = extractArchive.nextFileHeader()
                    }
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun extract7ZPage(file: File, pageIndex: Int): Bitmap? {
        return try {
            SevenZFile(file).use { sevenZFile ->
                val imageEntries = mutableListOf<org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry>()
                var entry = sevenZFile.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        imageEntries.add(entry)
                    }
                    entry = sevenZFile.nextEntry
                }
                
                imageEntries.sortBy { it.name }
                
                if (pageIndex >= imageEntries.size) return null
                
                // 重新打开文件以提取指定页面
                SevenZFile(file).use { extractFile ->
                    val targetEntry = imageEntries[pageIndex]
                    var currentEntry = extractFile.nextEntry
                    while (currentEntry != null) {
                        if (currentEntry.name == targetEntry.name) {
                            val content = ByteArray(currentEntry.size.toInt())
                            extractFile.read(content)
                            return BitmapFactory.decodeByteArray(content, 0, content.size)
                        }
                        currentEntry = extractFile.nextEntry
                    }
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun extractTarPage(file: File, pageIndex: Int): Bitmap? {
        return try {
            // 首先获取所有图片条目名称
            val imageEntries = FileInputStream(file).use { fis ->
                TarArchiveInputStream(fis).use { tarInput ->
                    generateSequence { tarInput.nextEntry }
                        .filterNot { it.isDirectory }
                        .filter { isImageFile(it.name) }
                        .map { it.name }
                        .toList()
                        .sorted()
                }
            }
            
            if (pageIndex >= imageEntries.size) return null
            
            // 重新打开文件以提取指定页面
            val targetName = imageEntries[pageIndex]
            FileInputStream(file).use { fis2 ->
                TarArchiveInputStream(fis2).use { tarInput2 ->
                    generateSequence { tarInput2.nextEntry }
                        .firstOrNull { it.name == targetName }
                        ?.let {
                            val outputStream = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (tarInput2.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            val bytes = outputStream.toByteArray()
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
    
    /**
     * 获取文件所有页面的列表（用于预览）
     */
    suspend fun getAllPages(file: File): List<String> = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        when (extension) {
            "cbz", "zip" -> getZipPageList(file)
            else -> emptyList()
        }
    }
    
    private fun getZipPageList(file: File): List<String> {
        return try {
            ZipFile(file).use { zip ->
                zip.entries().toList()
                    .filter { entry ->
                        !entry.isDirectory && isImageFile(entry.name)
                    }
                    .sortedBy { it.name }
                    .map { it.name }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
