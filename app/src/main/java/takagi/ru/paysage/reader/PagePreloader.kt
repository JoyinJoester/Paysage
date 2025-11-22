package takagi.ru.paysage.reader

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import takagi.ru.paysage.data.model.Book
import takagi.ru.paysage.data.model.ReadingMode
import takagi.ru.paysage.utils.FileParser
import java.io.File

private const val TAG = "PagePreloader"

/**
 * 页面预加载器
 * 在后台低优先级协程中预加载相邻页面
 */
class PagePreloader(
    private val fileParser: FileParser,
    private val cacheManager: PageCacheManager,
    private val memoryManager: BitmapMemoryManager,
    private val scope: CoroutineScope
) {
    
    // 当前预加载任务
    private val currentJobs = mutableListOf<Job>()
    
    // 预加载配置
    private val preloadAheadPages = 2  // 向前预加载页数
    private val preloadBehindPages = 1  // 向后预加载页数
    private val preloadAheadDualPages = 4  // 双页模式向前预加载
    
    /**
     * 预加载相邻页面
     * @param book 当前书籍
     * @param currentPage 当前页码
     * @param readingMode 阅读模式（单页/双页）
     * @param forward 是否向前翻页
     */
    fun preloadAdjacentPages(
        book: Book,
        currentPage: Int,
        readingMode: ReadingMode,
        forward: Boolean = true
    ) {
        // 取消之前的预加载任务
        cancelAll()
        
        val pagesToPreload = mutableListOf<Int>()
        
        // 根据阅读模式确定预加载页面
        if (readingMode == ReadingMode.DOUBLE_PAGE) {
            // 双页模式：预加载后续 4 页（2 组双页）
            for (i in 1..preloadAheadDualPages) {
                val pageIndex = currentPage + i
                if (pageIndex < book.totalPages) {
                    pagesToPreload.add(pageIndex)
                }
            }
        } else {
            // 单页模式：向前预加载 2 页
            for (i in 1..preloadAheadPages) {
                val pageIndex = currentPage + i
                if (pageIndex < book.totalPages) {
                    pagesToPreload.add(pageIndex)
                }
            }
            
            // 向后预加载 1 页
            for (i in 1..preloadBehindPages) {
                val pageIndex = currentPage - i
                if (pageIndex >= 0) {
                    pagesToPreload.add(pageIndex)
                }
            }
        }
        
        Log.d(TAG, "Preloading pages: $pagesToPreload for current page $currentPage")
        
        // 启动预加载任务
        pagesToPreload.forEach { pageIndex ->
            val job = scope.launch(Dispatchers.IO) {
                preloadPage(book, pageIndex)
            }
            currentJobs.add(job)
        }
    }
    
    /**
     * 取消所有预加载任务
     */
    fun cancelAll() {
        if (currentJobs.isNotEmpty()) {
            Log.d(TAG, "Cancelling ${currentJobs.size} preload jobs")
            currentJobs.forEach { it.cancel() }
            currentJobs.clear()
        }
    }
    
    /**
     * 预加载指定页面
     */
    private suspend fun preloadPage(book: Book, pageIndex: Int) {
        try {
            // 检查是否已在缓存中
            val cached = cacheManager.getRawPage(book.id, pageIndex)
            if (cached != null) {
                Log.d(TAG, "Page $pageIndex already cached, skipping preload")
                return
            }
            
            // 检查内存是否充足
            if (memoryManager.shouldClearCache()) {
                Log.w(TAG, "Memory low, skipping preload for page $pageIndex")
                return
            }
            
            Log.d(TAG, "Preloading page $pageIndex")
            
            // 解码图片
            val bitmap = if (book.filePath.startsWith("content://")) {
                val uri = Uri.parse(book.filePath)
                fileParser.extractPageFromUri(uri, pageIndex)
            } else {
                val file = File(book.filePath)
                fileParser.extractPage(file, pageIndex)
            }
            
            // 存入缓存
            if (bitmap != null) {
                cacheManager.putRawPage(book.id, pageIndex, bitmap)
                Log.d(TAG, "Successfully preloaded page $pageIndex")
            } else {
                Log.w(TAG, "Failed to preload page $pageIndex: bitmap is null")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading page $pageIndex", e)
        }
    }
}
