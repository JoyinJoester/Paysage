package takagi.ru.paysage.reader

import android.graphics.Bitmap
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Bitmap 内存池
 * 
 * 负责 Bitmap 对象的复用，减少内存分配和 GC 压力
 * 
 * 设计原则：
 * - 按尺寸分组管理 Bitmap
 * - 限制总内存使用量
 * - 线程安全
 * - 自动清理过期对象
 */
class BitmapPool(
    private val maxPoolSize: Long = 50 * 1024 * 1024L // 50MB
) {
    companion object {
        private const val TAG = "BitmapPool"
        private const val MAX_BITMAP_SIZE = 10 * 1024 * 1024 // 10MB 单个 Bitmap 最大尺寸
    }

    // 按尺寸分组的 Bitmap 池
    private val pools = ConcurrentHashMap<String, ConcurrentLinkedQueue<Bitmap>>()
    
    // 当前池大小
    private val currentPoolSize = AtomicLong(0)
    
    // 统计信息
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val putCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)

    /**
     * 获取指定尺寸的 Bitmap
     */
    fun get(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        val key = makeKey(width, height, config)
        val pool = pools[key]
        
        if (pool != null) {
            val bitmap = pool.poll()
            if (bitmap != null && !bitmap.isRecycled) {
                hitCount.incrementAndGet()
                currentPoolSize.addAndGet(-getBitmapSize(bitmap))
                Log.d(TAG, "Pool hit for ${width}x${height}, pool size: ${currentPoolSize.get()}")
                return bitmap
            }
        }
        
        missCount.incrementAndGet()
        Log.d(TAG, "Pool miss for ${width}x${height}")
        return null
    }

    /**
     * 将 Bitmap 放入池中
     */
    fun put(bitmap: Bitmap?): Boolean {
        if (bitmap == null || bitmap.isRecycled) {
            return false
        }

        val bitmapSize = getBitmapSize(bitmap)
        
        // 检查 Bitmap 是否过大
        if (bitmapSize > MAX_BITMAP_SIZE) {
            Log.d(TAG, "Bitmap too large for pool: ${bitmapSize}bytes")
            return false
        }

        val key = makeKey(bitmap.width, bitmap.height, bitmap.config)
        val pool = pools.getOrPut(key) { ConcurrentLinkedQueue() }

        // 检查池容量
        if (currentPoolSize.get() + bitmapSize > maxPoolSize) {
            evictOldest()
        }

        if (currentPoolSize.get() + bitmapSize <= maxPoolSize) {
            pool.offer(bitmap)
            currentPoolSize.addAndGet(bitmapSize)
            putCount.incrementAndGet()
            Log.d(TAG, "Put bitmap ${bitmap.width}x${bitmap.height} to pool, size: ${currentPoolSize.get()}")
            return true
        }

        return false
    }

    /**
     * 获取或创建指定尺寸的 Bitmap
     */
    fun getOrCreate(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        // 先尝试从池中获取
        get(width, height, config)?.let { return it }
        
        // 池中没有，创建新的
        return try {
            Bitmap.createBitmap(width, height, config)
        } catch (e: OutOfMemoryError) {
            // 内存不足时清理池并重试
            clearPool()
            Bitmap.createBitmap(width, height, config)
        }
    }

    /**
     * 清空池
     */
    fun clearPool() {
        pools.values.forEach { pool ->
            while (pool.isNotEmpty()) {
                pool.poll()?.recycle()
            }
        }
        pools.clear()
        currentPoolSize.set(0)
        Log.d(TAG, "Pool cleared")
    }

    /**
     * 移除最旧的 Bitmap
     */
    private fun evictOldest() {
        var evicted = false
        for ((key, pool) in pools) {
            val bitmap = pool.poll()
            if (bitmap != null) {
                currentPoolSize.addAndGet(-getBitmapSize(bitmap))
                bitmap.recycle()
                evictionCount.incrementAndGet()
                evicted = true
                Log.d(TAG, "Evicted bitmap from pool: $key")
                break
            }
        }
        
        if (!evicted) {
            // 如果没有可移除的，清空一个最小的池
            val smallestPool = pools.minByOrNull { it.value.size }
            smallestPool?.let { (key, pool) ->
                while (pool.isNotEmpty()) {
                    val bitmap = pool.poll()
                    if (bitmap != null) {
                        currentPoolSize.addAndGet(-getBitmapSize(bitmap))
                        bitmap.recycle()
                        evictionCount.incrementAndGet()
                    }
                }
                Log.d(TAG, "Cleared smallest pool: $key")
            }
        }
    }

    /**
     * 生成缓存键
     */
    private fun makeKey(width: Int, height: Int, config: Bitmap.Config): String {
        return "${width}x${height}_${config.name}"
    }

    /**
     * 计算 Bitmap 大小
     */
    private fun getBitmapSize(bitmap: Bitmap): Long {
        return bitmap.byteCount.toLong()
    }

    /**
     * 获取池统计信息
     */
    fun getStats(): BitmapPoolStats {
        return BitmapPoolStats(
            maxPoolSize = maxPoolSize,
            currentPoolSize = currentPoolSize.get(),
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            putCount = putCount.get(),
            evictionCount = evictionCount.get(),
            poolCount = pools.size,
            hitRate = if (hitCount.get() + missCount.get() > 0) {
                hitCount.get().toDouble() / (hitCount.get() + missCount.get())
            } else 0.0
        )
    }

    /**
     * 修剪池到指定大小
     */
    fun trimToSize(targetSize: Long) {
        while (currentPoolSize.get() > targetSize && pools.isNotEmpty()) {
            evictOldest()
        }
        Log.d(TAG, "Pool trimmed to ${currentPoolSize.get()} bytes")
    }
}

/**
 * Bitmap 池统计信息
 */
data class BitmapPoolStats(
    val maxPoolSize: Long,
    val currentPoolSize: Long,
    val hitCount: Long,
    val missCount: Long,
    val putCount: Long,
    val evictionCount: Long,
    val poolCount: Int,
    val hitRate: Double
) {
    val memoryUsageMB: Double
        get() = currentPoolSize / (1024.0 * 1024.0)
    
    val maxMemoryMB: Double
        get() = maxPoolSize / (1024.0 * 1024.0)
    
    val usageRatio: Double
        get() = currentPoolSize.toDouble() / maxPoolSize

    override fun toString(): String {
        return """
            BitmapPool Stats:
            - Memory: ${String.format("%.1f", memoryUsageMB)}MB / ${String.format("%.1f", maxMemoryMB)}MB (${String.format("%.1f%%", usageRatio * 100)})
            - Hit Rate: ${String.format("%.1f%%", hitRate * 100)} ($hitCount hits, $missCount misses)
            - Operations: $putCount puts, $evictionCount evictions
            - Pools: $poolCount different sizes
        """.trimIndent()
    }
}

/**
 * 全局 Bitmap 池实例
 */
object GlobalBitmapPool {
    private val instance = BitmapPool()

    fun get(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        return instance.get(width, height, config)
    }

    fun put(bitmap: Bitmap?): Boolean {
        return instance.put(bitmap)
    }

    fun getOrCreate(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        return instance.getOrCreate(width, height, config)
    }

    fun clearPool() {
        instance.clearPool()
    }

    fun getStats(): BitmapPoolStats {
        return instance.getStats()
    }

    fun trimToSize(targetSize: Long) {
        instance.trimToSize(targetSize)
    }
}
