package takagi.ru.paysage.reader.animation

import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 性能监控器
 * 
 * 监控动画性能,包括帧率、帧时间等。
 */
class PerformanceMonitor {
    
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var totalFrameTime = 0L
    
    /**
     * 当前帧率
     */
    var currentFps by mutableStateOf(0f)
        private set
    
    /**
     * 平均帧时间(毫秒)
     */
    var averageFrameTime by mutableStateOf(0f)
        private set
    
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val currentTime = frameTimeNanos / 1_000_000  // 转换为毫秒
            
            if (lastFrameTime > 0) {
                val frameTime = currentTime - lastFrameTime
                totalFrameTime += frameTime
                frameCount++
                
                // 每秒更新一次统计
                if (frameCount >= 60) {
                    averageFrameTime = totalFrameTime.toFloat() / frameCount
                    currentFps = 1000f / averageFrameTime
                    
                    // 重置计数器
                    frameCount = 0
                    totalFrameTime = 0
                }
            }
            
            lastFrameTime = currentTime
            
            // 继续监听下一帧
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    
    /**
     * 开始监控
     */
    fun start() {
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }
    
    /**
     * 停止监控
     */
    fun stop() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
    
    /**
     * 重置统计
     */
    fun reset() {
        frameCount = 0
        lastFrameTime = 0L
        totalFrameTime = 0L
        currentFps = 0f
        averageFrameTime = 0f
    }
}

/**
 * 性能监控 Composable
 * 
 * 在 Compose 中使用性能监控器。
 * 
 * @param enabled 是否启用监控
 * @param onUpdate 性能数据更新回调
 */
@Composable
fun rememberPerformanceMonitor(
    enabled: Boolean = false,
    onUpdate: (fps: Float, frameTime: Float) -> Unit = { _, _ -> }
): PerformanceMonitor {
    val monitor = remember { PerformanceMonitor() }
    
    DisposableEffect(enabled) {
        if (enabled) {
            monitor.start()
        }
        
        onDispose {
            if (enabled) {
                monitor.stop()
            }
        }
    }
    
    // 监听性能数据变化
    DisposableEffect(monitor.currentFps, monitor.averageFrameTime) {
        if (enabled) {
            onUpdate(monitor.currentFps, monitor.averageFrameTime)
        }
        onDispose { }
    }
    
    return monitor
}
