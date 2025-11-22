package takagi.ru.paysage.reader.canvas

import android.graphics.*
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Canvas 绘制优化器
 * 
 * 提供各种 Canvas 绘制优化策略
 * 
 * 优化策略：
 * - Paint 对象缓存
 * - Path 对象缓存
 * - 可见性检测
 * - 批量绘制
 */
object CanvasOptimizer {
    private const val TAG = "CanvasOptimizer"

    // Paint 对象缓存
    private val paintCache = ConcurrentHashMap<String, Paint>()
    
    // Path 对象缓存
    private val pathCache = ConcurrentHashMap<String, Path>()
    
    // Matrix 对象缓存
    private val matrixCache = ConcurrentHashMap<String, Matrix>()

    /**
     * 获取优化的 Paint 对象
     */
    fun getOptimizedPaint(
        color: Int = Color.BLACK,
        style: Paint.Style = Paint.Style.FILL,
        strokeWidth: Float = 0f,
        isAntiAlias: Boolean = true
    ): Paint {
        val key = "${color}_${style}_${strokeWidth}_$isAntiAlias"
        return paintCache.getOrPut(key) {
            Paint().apply {
                this.color = color
                this.style = style
                this.strokeWidth = strokeWidth
                this.isAntiAlias = isAntiAlias
                isDither = true
                isFilterBitmap = true
            }
        }
    }

    /**
     * 获取优化的文本 Paint
     */
    fun getOptimizedTextPaint(
        textSize: Float,
        color: Int = Color.BLACK,
        typeface: Typeface? = null
    ): Paint {
        val key = "text_${textSize}_${color}_${typeface?.hashCode() ?: 0}"
        return paintCache.getOrPut(key) {
            Paint().apply {
                this.textSize = textSize
                this.color = color
                this.typeface = typeface
                this.isAntiAlias = true
                isSubpixelText = true
                isLinearText = true
            }
        }
    }

    /**
     * 获取优化的 Path 对象
     */
    fun getOptimizedPath(key: String): Path {
        return pathCache.getOrPut(key) { Path() }
    }

    /**
     * 获取优化的 Matrix 对象
     */
    fun getOptimizedMatrix(key: String): Matrix {
        return matrixCache.getOrPut(key) { Matrix() }
    }

    /**
     * 优化的矩形绘制
     */
    fun drawOptimizedRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    ) {
        // 检查是否在可见区域内
        if (isRectVisible(canvas, left, top, right, bottom)) {
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }

    /**
     * 优化的圆形绘制
     */
    fun drawOptimizedCircle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        paint: Paint
    ) {
        // 检查是否在可见区域内
        if (isCircleVisible(canvas, cx, cy, radius)) {
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }

    /**
     * 优化的 Bitmap 绘制
     */
    fun drawOptimizedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        src: Rect?,
        dst: Rect,
        paint: Paint?
    ) {
        // 检查 Bitmap 是否有效
        if (bitmap.isRecycled) {
            Log.w(TAG, "Attempting to draw recycled bitmap")
            return
        }

        // 检查是否在可见区域内
        if (isRectVisible(canvas, dst.left.toFloat(), dst.top.toFloat(), 
                         dst.right.toFloat(), dst.bottom.toFloat())) {
            canvas.drawBitmap(bitmap, src, dst, paint)
        }
    }

    /**
     * 优化的文本绘制
     */
    fun drawOptimizedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        if (text.isNotEmpty()) {
            // 检查文本是否在可见区域内
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            if (isRectVisible(canvas, x + bounds.left, y + bounds.top, 
                             x + bounds.right, y + bounds.bottom)) {
                canvas.drawText(text, x, y, paint)
            }
        }
    }

    /**
     * 优化的路径绘制
     */
    fun drawOptimizedPath(
        canvas: Canvas,
        path: Path,
        paint: Paint
    ) {
        // 检查路径边界是否在可见区域内
        val bounds = RectF()
        path.computeBounds(bounds, true)
        if (isRectVisible(canvas, bounds.left, bounds.top, bounds.right, bounds.bottom)) {
            canvas.drawPath(path, paint)
        }
    }

    /**
     * 检查矩形是否在可见区域内
     */
    private fun isRectVisible(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float): Boolean {
        val clipBounds = canvas.clipBounds
        return !(right < clipBounds.left || left > clipBounds.right || 
                bottom < clipBounds.top || top > clipBounds.bottom)
    }

    /**
     * 检查圆形是否在可见区域内
     */
    private fun isCircleVisible(canvas: Canvas, cx: Float, cy: Float, radius: Float): Boolean {
        val clipBounds = canvas.clipBounds
        return !(cx + radius < clipBounds.left || cx - radius > clipBounds.right ||
                cy + radius < clipBounds.top || cy - radius > clipBounds.bottom)
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        paintCache.clear()
        pathCache.clear()
        matrixCache.clear()
        Log.d(TAG, "Canvas optimizer cache cleared")
    }

    /**
     * 获取缓存统计
     */
    fun getCacheStats(): CanvasCacheStats {
        return CanvasCacheStats(
            paintCacheSize = paintCache.size,
            pathCacheSize = pathCache.size,
            matrixCacheSize = matrixCache.size
        )
    }
}

/**
 * Canvas 缓存统计
 */
data class CanvasCacheStats(
    val paintCacheSize: Int,
    val pathCacheSize: Int,
    val matrixCacheSize: Int
) {
    val totalCacheSize: Int
        get() = paintCacheSize + pathCacheSize + matrixCacheSize

    override fun toString(): String {
        return "Canvas Cache: Paint($paintCacheSize), Path($pathCacheSize), Matrix($matrixCacheSize), Total($totalCacheSize)"
    }
}
