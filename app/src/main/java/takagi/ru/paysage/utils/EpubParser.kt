package takagi.ru.paysage.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "EpubParser"

/**
 * EPUB 文件解析工具类
 * EPUB 本质上是一个 ZIP 文件，包含 XML 和 HTML 内容
 */
class EpubParser(private val context: Context) {
    
    /**
     * EPUB 章节数据
     */
    data class EpubChapter(
        val title: String,
        val content: String,  // HTML 内容
        val plainText: String // 纯文本内容
    )
    
    /**
     * EPUB 元数据
     */
    data class EpubMetadata(
        val title: String,
        val author: String,
        val description: String,
        val chapterCount: Int
    )
    
    // 缓存解析结果
    private var cachedSpine: List<String>? = null
    private var cachedOpfPath: String? = null
    private var cachedFilePath: String? = null
    
    /**
     * 获取 EPUB 元数据
     */
    suspend fun getMetadata(file: File): EpubMetadata? = withContext(Dispatchers.IO) {
        try {
            ZipFile(file).use { zip ->
                val opfPath = getOpfPath(zip) ?: return@withContext null
                val opfEntry = zip.getEntry(opfPath) ?: return@withContext null
                
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().readText()
                val doc = parseXml(opfContent)
                
                val title = getElementText(doc, "dc:title") 
                    ?: getElementText(doc, "title") 
                    ?: file.nameWithoutExtension
                val author = getElementText(doc, "dc:creator") 
                    ?: getElementText(doc, "creator") 
                    ?: "未知作者"
                val description = getElementText(doc, "dc:description") 
                    ?: getElementText(doc, "description") 
                    ?: ""
                
                val spine = getSpine(doc, opfPath)
                
                EpubMetadata(
                    title = title,
                    author = author,
                    description = description,
                    chapterCount = spine.size
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting EPUB metadata", e)
            null
        }
    }
    
    /**
     * 从 URI 获取 EPUB 元数据
     */
    suspend fun getMetadataFromUri(uri: Uri): EpubMetadata? = withContext(Dispatchers.IO) {
        try {
            val tempFile = copyUriToTemp(uri) ?: return@withContext null
            val metadata = getMetadata(tempFile)
            tempFile.delete()
            metadata
        } catch (e: Exception) {
            Log.e(TAG, "Error getting EPUB metadata from URI", e)
            null
        }
    }
    
    /**
     * 获取章节数量
     */
    suspend fun getChapterCount(file: File): Int = withContext(Dispatchers.IO) {
        getMetadata(file)?.chapterCount ?: 0
    }
    
    /**
     * 从 URI 获取章节数量
     */
    suspend fun getChapterCountFromUri(uri: Uri): Int = withContext(Dispatchers.IO) {
        getMetadataFromUri(uri)?.chapterCount ?: 0
    }
    
    /**
     * 获取指定章节内容
     */
    suspend fun getChapter(file: File, chapterIndex: Int): EpubChapter? = withContext(Dispatchers.IO) {
        try {
            ZipFile(file).use { zip ->
                val opfPath = getOpfPath(zip) ?: return@withContext null
                val opfEntry = zip.getEntry(opfPath) ?: return@withContext null
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().readText()
                val doc = parseXml(opfContent)
                
                val spine = getSpine(doc, opfPath)
                
                if (chapterIndex < 0 || chapterIndex >= spine.size) {
                    return@withContext null
                }
                
                val chapterHref = spine[chapterIndex]
                val chapterEntry = zip.getEntry(chapterHref) 
                    ?: zip.entries().toList().find { it.name.endsWith(chapterHref.substringAfterLast('/')) }
                    ?: return@withContext null
                
                val htmlContent = zip.getInputStream(chapterEntry).bufferedReader().readText()
                val contentWithImages = embedImages(zip, htmlContent, chapterHref)
                val plainText = extractTextFromHtml(htmlContent)
                val title = extractTitleFromHtml(htmlContent) ?: "第${chapterIndex + 1}章"
                
                EpubChapter(
                    title = title,
                    content = contentWithImages, // 使用包含图片的 HTML
                    plainText = plainText
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chapter $chapterIndex", e)
            null
        }
    }
    
    /**
     * 从 URI 获取指定章节内容
     */
    suspend fun getChapterFromUri(uri: Uri, chapterIndex: Int): EpubChapter? = withContext(Dispatchers.IO) {
        try {
            val tempFile = copyUriToTemp(uri) ?: return@withContext null
            val chapter = getChapter(tempFile, chapterIndex)
            tempFile.delete()
            chapter
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chapter $chapterIndex from URI", e)
            null
        }
    }
    
    /**
     * 获取所有章节标题列表
     */
    suspend fun getChapterTitles(file: File): List<String> = withContext(Dispatchers.IO) {
        try {
            ZipFile(file).use { zip ->
                val opfPath = getOpfPath(zip) ?: return@withContext emptyList()
                val opfEntry = zip.getEntry(opfPath) ?: return@withContext emptyList()
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().readText()
                val doc = parseXml(opfContent)
                
                val spine = getSpine(doc, opfPath)
                
                spine.mapIndexed { index, href ->
                    try {
                        val entry = zip.getEntry(href) 
                            ?: zip.entries().toList().find { it.name.endsWith(href.substringAfterLast('/')) }
                        if (entry != null) {
                            val htmlContent = zip.getInputStream(entry).bufferedReader().readText()
                            extractTitleFromHtml(htmlContent) ?: "第${index + 1}章"
                        } else {
                            "第${index + 1}章"
                        }
                    } catch (e: Exception) {
                        "第${index + 1}章"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chapter titles", e)
            emptyList()
        }
    }
    
    /**
     * 从 URI 获取所有章节标题列表
     */
    suspend fun getChapterTitlesFromUri(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        try {
            val tempFile = copyUriToTemp(uri) ?: return@withContext emptyList()
            val titles = getChapterTitles(tempFile)
            tempFile.delete()
            titles
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chapter titles from URI", e)
            emptyList()
        }
    }
    
    /**
     * 提取封面图片
     */
    suspend fun extractCover(file: File): Bitmap? = withContext(Dispatchers.IO) {
        try {
            ZipFile(file).use { zip ->
                val opfPath = getOpfPath(zip) ?: return@withContext null
                val opfEntry = zip.getEntry(opfPath) ?: return@withContext null
                val opfContent = zip.getInputStream(opfEntry).bufferedReader().readText()
                val doc = parseXml(opfContent)
                val opfDir = opfPath.substringBeforeLast('/', "")
                
                // 策略 1: 查找 <meta name="cover" content="cover-id" />
                var coverId: String? = null
                val metaTags = doc.getElementsByTagName("meta")
                for (i in 0 until metaTags.length) {
                    val meta = metaTags.item(i) as Element
                    if (meta.getAttribute("name") == "cover") {
                        coverId = meta.getAttribute("content")
                        break
                    }
                }
                
                // 策略 2: 查找 manifest item properties="cover-image"
                if (coverId == null) {
                    val items = doc.getElementsByTagName("item")
                    for (i in 0 until items.length) {
                        val item = items.item(i) as Element
                        if (item.getAttribute("properties").contains("cover-image")) {
                            coverId = item.getAttribute("id")
                            break
                        }
                    }
                }
                
                // 如果找到了 coverId，获取对应的 href
                if (coverId != null) {
                    val items = doc.getElementsByTagName("item")
                    for (i in 0 until items.length) {
                        val item = items.item(i) as Element
                        if (item.getAttribute("id") == coverId) {
                            var href = item.getAttribute("href")
                            val mediaType = item.getAttribute("media-type")
                            if (opfDir.isNotEmpty()) {
                                href = "$opfDir/$href"
                            }
                            
                            // 如果是图片，直接返回
                            if (mediaType.startsWith("image/")) {
                                val entry = zip.getEntry(href)
                                    ?: zip.entries().toList().find { it.name.endsWith(href.substringAfterLast('/')) }
                                if (entry != null) {
                                    return@withContext BitmapFactory.decodeStream(zip.getInputStream(entry))
                                }
                            }
                            
                            // 如果是 HTML 页面（cover page），解析其中的图片
                            if (mediaType.contains("html") || mediaType.contains("xhtml")) {
                                val coverPageEntry = zip.getEntry(href)
                                    ?: zip.entries().toList().find { it.name.endsWith(href.substringAfterLast('/')) }
                                if (coverPageEntry != null) {
                                    val coverPageHtml = zip.getInputStream(coverPageEntry).bufferedReader().readText()
                                    val imgSrc = extractImageFromHtml(coverPageHtml)
                                    if (imgSrc != null) {
                                        val imgPath = resolvePath(href, imgSrc)
                                        val imgEntry = zip.getEntry(imgPath)
                                            ?: zip.entries().toList().find { it.name.endsWith(imgSrc.substringAfterLast('/')) }
                                        if (imgEntry != null) {
                                            return@withContext BitmapFactory.decodeStream(zip.getInputStream(imgEntry))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 策略 2.5: 查找 guide 中的 cover 引用
                val guideRefs = doc.getElementsByTagName("reference")
                for (i in 0 until guideRefs.length) {
                    val ref = guideRefs.item(i) as Element
                    if (ref.getAttribute("type") == "cover") {
                        var href = ref.getAttribute("href")
                        if (opfDir.isNotEmpty() && !href.startsWith("/")) {
                            href = "$opfDir/$href"
                        }
                        // 移除 fragment identifier
                        href = href.substringBefore('#')
                        
                        val coverPageEntry = zip.getEntry(href)
                            ?: zip.entries().toList().find { it.name.endsWith(href.substringAfterLast('/')) }
                        if (coverPageEntry != null) {
                            // 如果是图片
                            if (isImageFile(href)) {
                                return@withContext BitmapFactory.decodeStream(zip.getInputStream(coverPageEntry))
                            }
                            // 如果是 HTML，解析其中的图片
                            val coverPageHtml = zip.getInputStream(coverPageEntry).bufferedReader().readText()
                            val imgSrc = extractImageFromHtml(coverPageHtml)
                            if (imgSrc != null) {
                                val imgPath = resolvePath(href, imgSrc)
                                val imgEntry = zip.getEntry(imgPath)
                                    ?: zip.entries().toList().find { it.name.endsWith(imgSrc.substringAfterLast('/')) }
                                if (imgEntry != null) {
                                    return@withContext BitmapFactory.decodeStream(zip.getInputStream(imgEntry))
                                }
                            }
                        }
                    }
                }
                
                // 策略 2.6: 查找名为 cover.xhtml / cover.html 的文件
                val coverPageNames = listOf("cover.xhtml", "cover.html", "Cover.xhtml", "Cover.html")
                for (pageName in coverPageNames) {
                    val coverPagePath = if (opfDir.isNotEmpty()) "$opfDir/$pageName" else pageName
                    val coverPageEntry = zip.getEntry(coverPagePath)
                        ?: zip.entries().toList().find { it.name.endsWith(pageName) }
                    if (coverPageEntry != null) {
                        val coverPageHtml = zip.getInputStream(coverPageEntry).bufferedReader().readText()
                        val imgSrc = extractImageFromHtml(coverPageHtml)
                        if (imgSrc != null) {
                            val imgPath = resolvePath(coverPageEntry.name, imgSrc)
                            val imgEntry = zip.getEntry(imgPath)
                                ?: zip.entries().toList().find { it.name.endsWith(imgSrc.substringAfterLast('/')) }
                            if (imgEntry != null) {
                                return@withContext BitmapFactory.decodeStream(zip.getInputStream(imgEntry))
                            }
                        }
                    }
                }
                
                // 策略 3: 常见的封面图片名称模式
                val coverPatterns = listOf("cover", "Cover", "COVER", "front", "folder")
                
                for (entry in zip.entries()) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        val lowerName = entry.name.lowercase()
                        if (coverPatterns.any { lowerName.contains(it) }) {
                            val inputStream = zip.getInputStream(entry)
                            return@withContext BitmapFactory.decodeStream(inputStream)
                        }
                    }
                }
                
                // 策略 4: 查找 images 目录下的第一个大图
                // 很多 EPUB 将图片放在 OEBPS/images 或 EPUB/images 下
                val imageEntries = zip.entries().toList()
                    .filter { !it.isDirectory && isImageFile(it.name) }
                    .sortedBy { it.name } // 排序，通常封面可能是 000.jpg, 001.jpg
                
                // 优先查找含 cover 字样的
                for (entry in imageEntries) {
                    val lowerName = entry.name.lowercase()
                    if (lowerName.contains("cover") || lowerName.contains("front")) {
                         return@withContext BitmapFactory.decodeStream(zip.getInputStream(entry))
                    }
                }
                
                // 其次查找名为 0000, 0001, 01 等开头的图片（通常是第一页）
                for (entry in imageEntries) {
                    val name = entry.name.substringAfterLast('/').lowercase()
                    if (name.startsWith("000") || name.startsWith("001") || name.startsWith("cover")) {
                         return@withContext BitmapFactory.decodeStream(zip.getInputStream(entry))
                    }
                }

                // 最后返回找到的第一张图片
                if (imageEntries.isNotEmpty()) {
                    return@withContext BitmapFactory.decodeStream(zip.getInputStream(imageEntries.first()))
                }
                
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting cover", e)
            null
        }
    }
    
    /**
     * 从 URI 提取封面图片
     */
    suspend fun extractCoverFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val tempFile = copyUriToTemp(uri) ?: return@withContext null
            val cover = extractCover(tempFile)
            tempFile.delete()
            cover
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting cover from URI", e)
            null
        }
    }
    
    /**
     * 保存封面到缓存
     */
    suspend fun saveCover(file: File, bookId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val cover = extractCover(file) ?: return@withContext null
            saveCoverBitmap(cover, bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving EPUB cover", e)
            null
        }
    }
    
    /**
     * 从 URI 保存封面到缓存
     */
    suspend fun saveCoverFromUri(uri: Uri, bookId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val cover = extractCoverFromUri(uri) ?: return@withContext null
            saveCoverBitmap(cover, bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving EPUB cover from URI", e)
            null
        }
    }
    
    // ===== 私有辅助方法 =====
    
    private fun copyUriToTemp(uri: Uri): File? {
        return try {
            val tempFile = File(context.cacheDir, "temp_epub_${System.currentTimeMillis()}.epub")
            
            // 处理 tree URI 格式: content://authority/tree/{treeId}/document/{documentId}
            // 需要提取完整的 documentId（包括文件名）
            val actualUri = if (uri.toString().contains("/tree/") && uri.toString().contains("/document/")) {
                try {
                    // 从 URI 路径中提取完整的 document ID
                    val uriString = uri.toString()
                    val documentPart = uriString.substringAfter("/document/")
                    val documentId = java.net.URLDecoder.decode(documentPart, "UTF-8")
                    
                    Log.d(TAG, "Extracted documentId: $documentId")
                    
                    // 使用 buildDocumentUriUsingTree 保持权限
                    android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to build document URI, trying original", e)
                    uri
                }
            } else {
                uri
            }
            
            Log.d(TAG, "Opening URI: $actualUri (original: $uri)")
            
            context.contentResolver.openInputStream(actualUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to temp file", e)
            null
        }
    }
    
    private fun getOpfPath(zip: ZipFile): String? {
        try {
            // 读取 container.xml 以获取 OPF 文件路径
            val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
            val containerContent = zip.getInputStream(containerEntry).bufferedReader().readText()
            
            // 解析 container.xml 获取 rootfile
            val doc = parseXml(containerContent)
            val rootfiles = doc.getElementsByTagName("rootfile")
            if (rootfiles.length > 0) {
                val element = rootfiles.item(0) as Element
                return element.getAttribute("full-path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting OPF path", e)
        }
        return null
    }
    
    private fun parseXml(content: String): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(content.byteInputStream())
    }
    
    private fun getElementText(doc: org.w3c.dom.Document, tagName: String): String? {
        val elements = doc.getElementsByTagName(tagName)
        if (elements.length > 0) {
            return elements.item(0).textContent?.trim()
        }
        // Try without namespace prefix
        val localName = tagName.substringAfter(':')
        val elementsLocal = doc.getElementsByTagName(localName)
        if (elementsLocal.length > 0) {
            return elementsLocal.item(0).textContent?.trim()
        }
        return null
    }
    
    /**
     * 从 HTML 内容中提取第一个图片的 src
     */
    private fun extractImageFromHtml(html: String): String? {
        // 尝试匹配 <img src="...">
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val imgMatch = imgRegex.find(html)
        if (imgMatch != null) {
            return imgMatch.groupValues[1]
        }
        
        // 尝试匹配 SVG <image href="..."> 或 xlink:href="..."
        val svgRegex = Regex("""<image[^>]+(?:href|xlink:href)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val svgMatch = svgRegex.find(html)
        if (svgMatch != null) {
            return svgMatch.groupValues[1]
        }
        
        return null
    }
    
    private fun getSpine(doc: org.w3c.dom.Document, opfPath: String): List<String> {
        val opfDir = opfPath.substringBeforeLast('/', "")
        val prefix = if (opfDir.isNotEmpty()) "$opfDir/" else ""
        
        // 构建 id -> href 映射
        val manifest = mutableMapOf<String, String>()
        val manifestElements = doc.getElementsByTagName("item")
        for (i in 0 until manifestElements.length) {
            val element = manifestElements.item(i) as Element
            val id = element.getAttribute("id")
            val href = element.getAttribute("href")
            if (id.isNotEmpty() && href.isNotEmpty()) {
                manifest[id] = prefix + href
            }
        }
        
        // 按 spine 顺序获取章节
        val spineElements = doc.getElementsByTagName("itemref")
        val spine = mutableListOf<String>()
        for (i in 0 until spineElements.length) {
            val element = spineElements.item(i) as Element
            val idref = element.getAttribute("idref")
            manifest[idref]?.let { spine.add(it) }
        }
        
        return spine
    }
    
    private fun saveCoverBitmap(cover: Bitmap, bookId: Long): String? {
        return try {
            val coverDir = File(context.cacheDir, "covers")
            if (!coverDir.exists()) {
                coverDir.mkdirs()
            }
            
            val coverFile = File(coverDir, "cover_$bookId.jpg")
            FileOutputStream(coverFile).use { out ->
                cover.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            cover.recycle()
            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cover bitmap", e)
            null
        }
    }
    
    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
    
    private fun extractTextFromHtml(html: String): String {
        try {
            var text = html
            
            // 1. 移除 head, script, style 等不可见内容
            text = text.replace(Regex("<head[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            
            // 2. 预处理：将 HTML 中的换行符和制表符转换为空格（遵循 HTML 渲染规则）
            text = text.replace(Regex("[\\r\\n\\t]+"), " ")
            
            // 3. 处理块级元素和换行
            // </p>, </div>, </h1>..</h6> 替换为双换行（段落间距）
            text = text.replace(Regex("</(?:p|div|h\\d)[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            // <br> 替换为单换行
            text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            // <li> 替换为换行 + 项目符号
            text = text.replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "\n• ")
            
            // 4. 处理图片
            // 简单将图片替换为 [图片] 占位符，避免完全丢失
            // 后续可以考虑解析 src 属性来显示图片
            text = text.replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE), "\n[图片]\n")
            
            // 5. 移除所有剩余的 HTML 标签
            text = text.replace(Regex("<[^>]+>"), "")
            
            // 6. 解码 HTML 实体
            text = text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”")
                .replace("&lsquo;", "‘")
                .replace("&rsquo;", "’")
                .replace("&mdash;", "—")
                .replace("&middot;", "·")
            
            // 7. 后处理排版
            // 移除多余的空白行（超过3个换行符替换为2个）
            text = text.replace(Regex("\\n{3,}"), "\n\n")
            // 处理段首缩进：如果这一行看起来是正文（不是空行），添加两个全角空格
            // 注意：这里简单处理，每一段开始都缩进
            val lines = text.split("\n")
            val sb = StringBuilder()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    // 如果不是特殊符号开头（如 [图片]），添加缩进
                    if (!trimmed.startsWith("[") && !trimmed.startsWith("•")) {
                        sb.append("\u3000\u3000")
                    }
                    sb.append(trimmed).append("\n")
                } else {
                    // 保留一些段落间距，但不要太多
                   // sb.append("\n") 
                }
            }
            
            return sb.toString().trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text", e)
            return html // 降级：如果出错，返回原始内容
        }
    }
    
    private fun extractTitleFromHtml(html: String): String? {
        return try {
            // 尝试提取 <title> 标签
            val titleMatch = Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE)
                .find(html)
            if (titleMatch != null) {
                val title = titleMatch.groupValues[1].trim()
                if (title.isNotEmpty()) return title
            }
            
            // 尝试提取 <h1> 或 <h2> 标签
            val h1Match = Regex("<h[12][^>]*>([^<]*)</h[12]>", RegexOption.IGNORE_CASE)
                .find(html)
            if (h1Match != null) {
                val title = h1Match.groupValues[1].trim()
                if (title.isNotEmpty()) return title
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将 HTML 中的图片引用转换为 Base64 内嵌图片
     */
    private fun embedImages(zip: ZipFile, html: String, chapterPath: String): String {
        try {
            var modifiedHtml = html
            // 匹配 <img src="..." /> 或 <image href="..." />
            val imgRegex = Regex("<img[^>]+src=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
            val svgImgRegex = Regex("<image[^>]+href=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
            
            // 处理 img 标签
            imgRegex.findAll(html).forEach { match ->
                val src = match.groupValues[1]
                val imagePath = resolvePath(chapterPath, src)
                val base64 = getImageAsBase64(zip, imagePath)
                
                if (base64 != null) {
                    val mimeType = getMimeType(imagePath)
                    modifiedHtml = modifiedHtml.replace(src, "data:$mimeType;base64,$base64")
                }
            }
            
            // 处理 svg image 标签
            svgImgRegex.findAll(html).forEach { match ->
                val href = match.groupValues[1]
                val imagePath = resolvePath(chapterPath, href)
                val base64 = getImageAsBase64(zip, imagePath)
                
                if (base64 != null) {
                    val mimeType = getMimeType(imagePath)
                    modifiedHtml = modifiedHtml.replace(href, "data:$mimeType;base64,$base64")
                }
            }
            
            return modifiedHtml
        } catch (e: Exception) {
            Log.e(TAG, "Error embedding images", e)
            return html
        }
    }
    
    /**
     * 解析相对路径
     */
    private fun resolvePath(basePath: String, relativePath: String): String {
        // 如果是绝对路径或 URL，直接返回
        if (relativePath.startsWith("/") || relativePath.contains("://")) {
            return relativePath
        }
        
        val baseDir = basePath.substringBeforeLast('/', "")
        if (baseDir.isEmpty()) return relativePath
        
        val parts = (baseDir + "/" + relativePath).split('/')
        val result = mutableListOf<String>()
        
        for (part in parts) {
            when (part) {
                ".", "" -> {} // 忽略当前目录
                ".." -> if (result.isNotEmpty()) result.removeAt(result.lastIndex) // 返回上一级
                else -> result.add(part)
            }
        }
        
        return result.joinToString("/")
    }
    
    /**
     * 从 Zip 中获取图片并转换为 Base64
     */
    private fun getImageAsBase64(zip: ZipFile, imagePath: String): String? {
        try {
            // 尝试直接查找
            var entry = zip.getEntry(imagePath)
            
            // 如果找不到，尝试查找类似路径（处理 URL 解码问题）
            if (entry == null) {
                val decodedPath = java.net.URLDecoder.decode(imagePath, "UTF-8")
                entry = zip.getEntry(decodedPath)
            }
            
            // 还是找不到，遍历查找
            if (entry == null) {
                val fileName = imagePath.substringAfterLast('/')
                entry = zip.entries().toList().find { 
                    it.name.endsWith(fileName) && !it.isDirectory 
                }
            }
            
            if (entry == null) return null
            
            return zip.getInputStream(entry).use { input ->
                val bytes = input.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading image: $imagePath", e)
            return null
        }
    }
    
    private fun getMimeType(path: String): String {
        return when (path.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }
}
