package takagi.ru.paysage.reader

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import takagi.ru.paysage.util.ImageFilter

private const val TAG = "PageCacheManager"

/**
 * 页面缓存管理器
 * 使用双层 LRU 缓存：原始图片缓存 + 过滤图片缓存
 */
class PageCacheManager(
    private val maxRawCacheSize: Int = 10,
    private val maxFilterCacheSize: Int = 5,
    private val memoryManager: BitmapMemoryManager
) {
    // 原始图片缓存 (key -> Bitmap)
    private val rawCache = object : LruCache<String, Bitmap>(maxRawCacheSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted) {
                Log.d(TAG, "Raw cache evicted: $key")
                memoryManager.recycleBitmap(oldValue)
            }
        }
        
        override fun sizeOf(key: String, value: Bitmap): Int {
            return 1  // 按数量计算，不按字节
        }
    }
    
    // 过滤后图片缓存 (key -> Bitmap)
    private val filterCache = object : LruCache<String, Bitmap>(maxFilterCacheSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted) {
                Log.d(TAG, "Filter cache evicted: $key")
                memoryManager.recycleBitmap(oldValue)
            }
        }
        
        override fun sizeOf(key: String, value: Bitmap): Int {
            return 1
        }
    }
    
    // 缓存统计
    private var cacheHits = 0
    private var cacheMisses = 0
    
    /**
     * 获取原始页面
     */
    fun getRawPage(bookId: Long, pageIndex: Int): Bitmap? {
        val key = makeRawKey(bookId, pageIndex)
        val bitmap = rawCache.get(key)
        
        if (bitmap != null) {
            cacheHits++
            Log.d(TAG, "Raw cache hit: $key")
        } else {
            cacheMisses++
            Log.d(TAG, "Raw cache miss: $key")
        }
        
        return bitmap
    }
    
    /**
     * 缓存原始页面
     */
    fun putRawPage(bookId: Long, pageIndex: Int, bitmap: Bitmap) {
        val key = makeRawKey(bookId, pageIndex)
        rawCache.put(key, bitmap)
        Log.d(TAG, "Raw cache put: $key (${bitmap.width}x${bitmap.height})")
    }
    
    /**
     * 获取过滤后的页面
     */
    fun getFilteredPage(bookId: Long, pageIndex: Int, filter: ImageFilter): Bitmap? {
        val key = makeFilterKey(bookId, pageIndex, filter)
        val bitmap = filterCache.get(key)
        
        if (bitmap != null) {
            cacheHits++
            Log.d(TAG, "Filter cache hit: $key")
        } else {
            cacheMisses++
            Log.d(TAG, "Filter cache miss: $key")
        }
        
        return bitmap
    }
    
    /**
     * 缓存过滤后的页面
     */
    fun putFilteredPage(bookId: Long, pageIndex: Int, filter: ImageFilter, bitmap: Bitmap) {
        val key = makeFilterKey(bookId, pageIndex, filter)
        filterCache.put(key, bitmap)
        Log.d(TAG, "Filter cache put: $key")
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        Log.i(TAG, "Clearing all caches")
        
        // 手动回收所有 Bitmap
        rawCache.snapshot().values.forEach { memoryManager.recycleBitmap(it) }
        filterCache.snapshot().values.forEach { memoryManager.recycleBitmap(it) }
        
        rawCache.evictAll()
        filterCache.evictAll()
        
        // 重置统计
        cacheHits = 0
        cacheMisses = 0
    }
    
    /**
     * 清空过滤缓存（当过滤器参数改变时）
     */
    fun clearFilterCache() {
        Log.i(TAG, "Clearing filter cache")
        filterCache.snapshot().values.forEach { memoryManager.recycleBitmap(it) }
        filterCache.evictAll()
    }
    
    /**
     * 获取缓存命中率
     */
    fun getCacheHitRate(): Float {
        val total = cacheHits + cacheMisses
        return if (total == 0) 0f else cacheHits.toFloat() / total.toFloat()
    }
    
    /**
     * 获取当前缓存内存使用量（字节）
     */
    fun getMemoryUsage(): Long {
        var totalBytes = 0L
        
        rawCache.snapshot().values.forEach { bitmap ->
            totalBytes += bitmap.byteCount.toLong()
        }
        
        filterCache.snapshot().values.forEach { bitmap ->
            totalBytes += bitmap.byteCount.toLong()
        }
        
        return totalBytes
    }
    
    /**
     * 移除指定书籍的所有缓存
     */
    fun removeBook(bookId: Long) {
        Log.i(TAG, "Removing caches for book: $bookId")
        
        // 移除原始缓存
        val rawKeys = rawCache.snapshot().keys.filter { it.startsWith("${bookId}_") }
        rawKeys.forEach { key ->
            rawCache.remove(key)?.let { memoryManager.recycleBitmap(it) }
        }
        
        // 移除过滤缓存
        val filterKeys = filterCache.snapshot().keys.filter { it.startsWith("${bookId}_") }
        filterKeys.forEach { key ->
            filterCache.remove(key)?.let { memoryManager.recycleBitmap(it) }
        }
    }
    
    /**
     * 修剪原始缓存（保留指定比例）
     */
    fun trimRawCache(keepRatio: Float) {
        val currentSize = rawCache.size()
        val targetSize = (currentSize * keepRatio).toInt()
        
        Log.i(TAG, "Trimming raw cache from $currentSize to $targetSize")
        
        rawCache.trimToSize(targetSize)
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            rawCacheSize = rawCache.size(),
            filterCacheSize = filterCache.size(),
            maxRawCacheSize = maxRawCacheSize,
            maxFilterCacheSize = maxFilterCacheSize,
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            hitRate = getCacheHitRate(),
            memoryUsage = getMemoryUsage()
        )
    }
    
    // 私有辅助方法
    
    private fun makeRawKey(bookId: Long, pageIndex: Int): String {
        return "${bookId}_${pageIndex}"
    }
    
    private fun makeFilterKey(bookId: Long, pageIndex: Int, filter: ImageFilter): String {
        return "${bookId}_${pageIndex}_${filter.hashCode()}"
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val rawCacheSize: Int,
    val filterCacheSize: Int,
    val maxRawCacheSize: Int,
    val maxFilterCacheSize: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val hitRate: Float,
    val memoryUsage: Long
) {
    override fun toString(): String {
        return """
            Cache Stats:
            - Raw: $rawCacheSize/$maxRawCacheSize
            - Filter: $filterCacheSize/$maxFilterCacheSize
            - Hits: $cacheHits, Misses: $cacheMisses
            - Hit Rate: ${(hitRate * 100).toInt()}%
            - Memory: ${memoryUsage / 1024 / 1024}MB
        """.trimIndent()
    }
}
