package takagi.ru.paysage.reader.animation

import android.graphics.Bitmap
import android.util.LruCache

/**
 * 位图预加载器
 * 
 * 使用 LruCache 缓存页面位图,提高翻页性能。
 * 
 * @param maxSize 最大缓存数量,默认 3(当前页、前一页、后一页)
 */
class BitmapPreloader(maxSize: Int = 3) {
    
    private val cache = LruCache<Int, Bitmap>(maxSize)
    
    /**
     * 预加载位图
     * 
     * @param pageIndex 页面索引
     * @param bitmap 页面位图
     */
    fun preload(pageIndex: Int, bitmap: Bitmap) {
        cache.put(pageIndex, bitmap)
    }
    
    /**
     * 获取位图
     * 
     * @param pageIndex 页面索引
     * @return 页面位图,如果不存在则返回 null
     */
    fun get(pageIndex: Int): Bitmap? {
        return cache.get(pageIndex)
    }
    
    /**
     * 移除位图
     * 
     * @param pageIndex 页面索引
     */
    fun remove(pageIndex: Int) {
        cache.remove(pageIndex)
    }
    
    /**
     * 清空缓存
     */
    fun clear() {
        cache.evictAll()
    }
    
    /**
     * 预加载相邻页面
     * 
     * 预加载当前页、前一页、后一页。
     * 
     * @param currentPage 当前页索引
     * @param pageCount 总页面数
     * @param loadBitmap 加载位图的函数
     */
    suspend fun preloadAdjacentPages(
        currentPage: Int,
        pageCount: Int,
        loadBitmap: suspend (Int) -> Bitmap?
    ) {
        // 预加载当前页
        if (get(currentPage) == null) {
            loadBitmap(currentPage)?.let { preload(currentPage, it) }
        }
        
        // 预加载前一页
        if (currentPage > 0 && get(currentPage - 1) == null) {
            loadBitmap(currentPage - 1)?.let { preload(currentPage - 1, it) }
        }
        
        // 预加载后一页
        if (currentPage < pageCount - 1 && get(currentPage + 1) == null) {
            loadBitmap(currentPage + 1)?.let { preload(currentPage + 1, it) }
        }
    }
    
    /**
     * 清理不需要的缓存
     * 
     * 只保留当前页及相邻页面的缓存。
     * 
     * @param currentPage 当前页索引
     */
    fun cleanupUnusedCache(currentPage: Int) {
        val snapshot = cache.snapshot()
        for ((pageIndex, _) in snapshot) {
            // 如果不是当前页或相邻页,则移除
            if (pageIndex < currentPage - 1 || pageIndex > currentPage + 1) {
                remove(pageIndex)
            }
        }
    }
}
