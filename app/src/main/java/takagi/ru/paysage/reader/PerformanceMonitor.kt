package takagi.ru.paysage.reader

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

private const val TAG = "PerformanceMonitor"
private const val MAX_HISTORY_SIZE = 100
private const val SLOW_LOAD_THRESHOLD_MS = 200L

/**
 * 性能监控器
 * 收集和分析阅读器性能指标
 */
class PerformanceMonitor(private val context: Context? = null) {
    
    // 页面加载时间历史（保留最近 100 次）
    private val pageLoadTimes = ConcurrentLinkedQueue<Long>()
    
    // 帧率历史
    private val fpsHistory = ConcurrentLinkedQueue<Int>()
    
    // 渲染时间历史
    private val renderTimeHistory = ConcurrentLinkedQueue<Long>()
    
    // 缓存统计
    private var totalCacheHits = 0
    private var totalCacheRequests = 0
    
    private val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    
    /**
     * 记录页面加载时间
     */
    fun recordPageLoad(timeMs: Long) {
        pageLoadTimes.offer(timeMs)
        
        // 保持队列大小
        while (pageLoadTimes.size > MAX_HISTORY_SIZE) {
            pageLoadTimes.poll()
        }
        
        // 如果加载时间过长，记录警告
        if (timeMs > SLOW_LOAD_THRESHOLD_MS) {
            Log.w(TAG, "Slow page load detected: ${timeMs}ms (threshold: ${SLOW_LOAD_THRESHOLD_MS}ms)")
        }
    }
    
    /**
     * 记录缓存命中
     */
    fun recordCacheHit(hit: Boolean) {
        totalCacheRequests++
        if (hit) {
            totalCacheHits++
        }
    }
    
    /**
     * 获取平均加载时间
     */
    fun getAverageLoadTime(): Long {
        return if (pageLoadTimes.isEmpty()) {
            0L
        } else {
            val sum = pageLoadTimes.sum()
            sum / pageLoadTimes.size
        }
    }
    
    /**
     * 获取最小加载时间
     */
    fun getMinLoadTime(): Long {
        return pageLoadTimes.minOrNull() ?: 0L
    }
    
    /**
     * 获取最大加载时间
     */
    fun getMaxLoadTime(): Long {
        return pageLoadTimes.maxOrNull() ?: 0L
    }
    
    /**
     * 获取缓存命中率
     */
    fun getCacheHitRate(): Float {
        return if (totalCacheRequests == 0) {
            0f
        } else {
            totalCacheHits.toFloat() / totalCacheRequests.toFloat()
        }
    }
    
    /**
     * 获取性能报告
     */
    fun getReport(): PerformanceReport {
        return PerformanceReport(
            averageLoadTime = getAverageLoadTime(),
            minLoadTime = getMinLoadTime(),
            maxLoadTime = getMaxLoadTime(),
            cacheHitRate = getCacheHitRate(),
            totalPageLoads = pageLoadTimes.size,
            totalCacheRequests = totalCacheRequests,
            totalCacheHits = totalCacheHits
        )
    }
    
    /**
     * 记录帧率
     */
    fun recordFps(fps: Int) {
        fpsHistory.offer(fps)
        while (fpsHistory.size > MAX_HISTORY_SIZE) {
            fpsHistory.poll()
        }
    }
    
    /**
     * 记录渲染时间
     */
    fun recordRenderTime(timeMs: Long) {
        renderTimeHistory.offer(timeMs)
        while (renderTimeHistory.size > MAX_HISTORY_SIZE) {
            renderTimeHistory.poll()
        }
    }
    
    /**
     * 获取平均帧率
     */
    fun getAverageFps(): Int {
        return if (fpsHistory.isEmpty()) {
            60 // 默认假设 60fps
        } else {
            val sum = fpsHistory.sum()
            sum / fpsHistory.size
        }
    }
    
    /**
     * 获取平均渲染时间
     */
    fun getAverageRenderTime(): Long {
        return if (renderTimeHistory.isEmpty()) {
            0L
        } else {
            val sum = renderTimeHistory.sum()
            sum / renderTimeHistory.size
        }
    }
    
    /**
     * 获取内存使用率
     */
    fun getMemoryUsage(): Float {
        return if (activityManager != null) {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val usedMemory = memInfo.totalMem - memInfo.availMem
            usedMemory.toFloat() / memInfo.totalMem.toFloat()
        } else {
            0.5f // 默认假设 50% 使用率
        }
    }
    
    /**
     * 获取 CPU 使用率（简化版本）
     */
    fun getCpuUsage(): Float {
        // 简化实现：基于渲染时间估算
        val avgRenderTime = getAverageRenderTime()
        return when {
            avgRenderTime > 33 -> 0.8f // 超过 33ms 认为 CPU 使用率高
            avgRenderTime > 16 -> 0.5f // 16-33ms 认为中等
            else -> 0.3f // 低于 16ms 认为低
        }
    }
    
    /**
     * 重置统计数据
     */
    fun reset() {
        pageLoadTimes.clear()
        fpsHistory.clear()
        renderTimeHistory.clear()
        totalCacheHits = 0
        totalCacheRequests = 0
        Log.d(TAG, "Performance statistics reset")
    }
    
    /**
     * 打印性能报告到日志
     */
    fun logReport() {
        val report = getReport()
        Log.i(TAG, """
            Performance Report:
            - Average load time: ${report.averageLoadTime}ms
            - Min/Max load time: ${report.minLoadTime}ms / ${report.maxLoadTime}ms
            - Cache hit rate: ${(report.cacheHitRate * 100).toInt()}%
            - Total page loads: ${report.totalPageLoads}
            - Cache hits/requests: ${report.totalCacheHits}/${report.totalCacheRequests}
        """.trimIndent())
    }
}

/**
 * 性能报告
 */
data class PerformanceReport(
    val averageLoadTime: Long,
    val minLoadTime: Long,
    val maxLoadTime: Long,
    val cacheHitRate: Float,
    val totalPageLoads: Int,
    val totalCacheRequests: Int,
    val totalCacheHits: Int
) {
    override fun toString(): String {
        return """
            Performance Report:
            - Avg: ${averageLoadTime}ms (Min: ${minLoadTime}ms, Max: ${maxLoadTime}ms)
            - Cache: ${(cacheHitRate * 100).toInt()}% (${totalCacheHits}/${totalCacheRequests})
            - Loads: $totalPageLoads
        """.trimIndent()
    }
}
