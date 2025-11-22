package takagi.ru.paysage.reader

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlin.math.max
import kotlin.math.min

private const val TAG = "BitmapMemoryManager"

/**
 * Bitmap 内存管理器
 * 负责内存监控、Bitmap 采样和回收
 */
class BitmapMemoryManager(private val context: Context) {
    
    // 最大内存使用比例（设备可用内存的 25%）
    private val maxMemoryRatio = 0.25f
    
    // 内存警告阈值（20%）
    private val memoryWarningRatio = 0.20f
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // 内存警告监听器
    private var memoryWarningListener: (() -> Unit)? = null
    
    /**
     * 计算合适的采样率
     * @param originalWidth 原始宽度
     * @param originalHeight 原始高度
     * @param targetWidth 目标宽度（屏幕宽度）
     * @param targetHeight 目标高度（屏幕高度）
     * @return 采样率（1, 2, 4, 8...）
     */
    fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var inSampleSize = 1
        
        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2
            
            // 计算最大的 inSampleSize 值，使得宽高都大于目标尺寸
            while ((halfHeight / inSampleSize) >= targetHeight &&
                   (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2
            }
        }
        
        Log.d(TAG, "Calculated sample size: $inSampleSize for ${originalWidth}x${originalHeight} -> ${targetWidth}x${targetHeight}")
        return max(1, inSampleSize)
    }
    
    /**
     * 检查是否需要清理缓存
     * @return true 如果内存使用超过阈值
     */
    fun shouldClearCache(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val availableMemory = memInfo.availMem
        val totalMemory = memInfo.totalMem
        val usedMemory = totalMemory - availableMemory
        val usedRatio = usedMemory.toFloat() / totalMemory.toFloat()
        
        val shouldClear = usedRatio > (1.0f - maxMemoryRatio)
        
        if (shouldClear) {
            Log.w(TAG, "Memory usage high: ${(usedRatio * 100).toInt()}%, should clear cache")
        }
        
        // 如果超过警告阈值，触发监听器
        if (usedRatio > (1.0f - memoryWarningRatio)) {
            memoryWarningListener?.invoke()
        }
        
        return shouldClear
    }
    
    /**
     * 获取可用内存（字节）
     */
    fun getAvailableMemory(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }
    
    /**
     * 获取最大允许使用的内存（字节）
     */
    fun getMaxAllowedMemory(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem * maxMemoryRatio).toLong()
    }
    
    /**
     * 安全回收 Bitmap（尝试放入池中复用）
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        try {
            if (bitmap != null && !bitmap.isRecycled) {
                // 尝试放入池中，如果失败则直接回收
                if (!GlobalBitmapPool.put(bitmap)) {
                    bitmap.recycle()
                    Log.d(TAG, "Bitmap recycled: ${bitmap.width}x${bitmap.height}")
                } else {
                    Log.d(TAG, "Bitmap returned to pool: ${bitmap.width}x${bitmap.height}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recycling bitmap", e)
        }
    }
    
    /**
     * 创建 Bitmap（使用内存池）
     */
    fun createBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        return try {
            // 先尝试从池中获取
            val bitmap = GlobalBitmapPool.getOrCreate(width, height, config)
            Log.d(TAG, "Created bitmap: ${width}x${height}")
            bitmap
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OutOfMemoryError creating bitmap ${width}x${height}")
            // 清理池和内存后重试
            GlobalBitmapPool.clearPool()
            handleLowMemory()
            try {
                Bitmap.createBitmap(width, height, config)
            } catch (e2: OutOfMemoryError) {
                Log.e(TAG, "Failed to create bitmap after cleanup")
                null
            }
        }
    }
    
    /**
     * 注册内存警告监听
     */
    fun registerMemoryWarningListener(listener: () -> Unit) {
        this.memoryWarningListener = listener
    }
    
    /**
     * 处理低内存情况
     */
    fun handleLowMemory() {
        Log.w(TAG, "Handling low memory situation")
        
        val memoryBefore = getMemoryReport()
        Log.d(TAG, "Memory before cleanup: ${memoryBefore.toMB(memoryBefore.usedMemory)}")
        
        // 触发监听器让缓存管理器清理
        memoryWarningListener?.invoke()
        
        // 建议系统进行 GC
        System.gc()
        
        // 等待一小段时间让 GC 完成
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            // Ignore
        }
        
        val memoryAfter = getMemoryReport()
        Log.d(TAG, "Memory after cleanup: ${memoryAfter.toMB(memoryAfter.usedMemory)}")
    }
    
    /**
     * 获取当前内存使用情况报告
     */
    fun getMemoryReport(): MemoryReport {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val availableMemory = memInfo.availMem
        val totalMemory = memInfo.totalMem
        val usedMemory = totalMemory - availableMemory
        val maxAllowed = getMaxAllowedMemory()
        
        return MemoryReport(
            totalMemory = totalMemory,
            availableMemory = availableMemory,
            usedMemory = usedMemory,
            maxAllowedMemory = maxAllowed,
            usageRatio = usedMemory.toFloat() / totalMemory.toFloat()
        )
    }
}

/**
 * 内存使用报告
 */
data class MemoryReport(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val maxAllowedMemory: Long,
    val usageRatio: Float
) {
    fun toMB(bytes: Long): String = "${bytes / 1024 / 1024}MB"
    
    override fun toString(): String {
        return """
            Memory Report:
            - Total: ${toMB(totalMemory)}
            - Available: ${toMB(availableMemory)}
            - Used: ${toMB(usedMemory)} (${(usageRatio * 100).toInt()}%)
            - Max Allowed: ${toMB(maxAllowedMemory)}
        """.trimIndent()
    }
}
