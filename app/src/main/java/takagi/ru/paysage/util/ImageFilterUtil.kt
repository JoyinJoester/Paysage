package takagi.ru.paysage.util

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * 图片过滤器配置
 */
@Stable
data class ImageFilter(
    val brightness: Float = 0f,      // 亮度 (-100 to 100)
    val contrast: Float = 1f,        // 对比度 (0.5 to 2.0)
    val saturation: Float = 1f,      // 饱和度 (0.0 to 2.0)
    val hue: Float = 0f,             // 色调 (-180 to 180)
    val grayscale: Boolean = false,  // 灰度模式
    val invert: Boolean = false      // 反色模式
) {
    companion object {
        val Default = ImageFilter()
    }
    
    /**
     * 检查是否使用了任何过滤器
     */
    fun isActive(): Boolean {
        return brightness != 0f || 
               contrast != 1f || 
               saturation != 1f || 
               hue != 0f || 
               grayscale || 
               invert
    }
    
    /**
     * 计算过滤器参数的哈希值，用于缓存键
     */
    override fun hashCode(): Int {
        var result = brightness.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + hue.hashCode()
        result = 31 * result + (if (grayscale) 1 else 0)
        result = 31 * result + (if (invert) 1 else 0)
        return result
    }
}

/**
 * 图片过滤器工具类
 */
object ImageFilterUtil {
    
    /**
     * 应用过滤器到 Bitmap
     */
    fun applyFilter(bitmap: Bitmap, filter: ImageFilter): Bitmap {
        if (!filter.isActive()) {
            return bitmap
        }
        
        val width = bitmap.width
        val height = bitmap.height
        val filteredBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(filteredBitmap)
        
        val paint = Paint().apply {
            colorFilter = createColorMatrixFilter(filter)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return filteredBitmap
    }
    
    /**
     * 应用过滤器到 ImageBitmap
     */
    fun applyFilter(imageBitmap: ImageBitmap, filter: ImageFilter): ImageBitmap {
        val androidBitmap = imageBitmap.asAndroidBitmap()
        val filteredBitmap = applyFilter(androidBitmap, filter)
        return filteredBitmap.asImageBitmap()
    }
    
    /**
     * 创建颜色矩阵过滤器
     */
    private fun createColorMatrixFilter(filter: ImageFilter): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        
        // 应用亮度
        if (filter.brightness != 0f) {
            val brightnessMatrix = ColorMatrix().apply {
                set(floatArrayOf(
                    1f, 0f, 0f, 0f, filter.brightness,
                    0f, 1f, 0f, 0f, filter.brightness,
                    0f, 0f, 1f, 0f, filter.brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            colorMatrix.postConcat(brightnessMatrix)
        }
        
        // 应用对比度
        if (filter.contrast != 1f) {
            val contrastMatrix = ColorMatrix().apply {
                val translate = (1f - filter.contrast) / 2f * 255f
                set(floatArrayOf(
                    filter.contrast, 0f, 0f, 0f, translate,
                    0f, filter.contrast, 0f, 0f, translate,
                    0f, 0f, filter.contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            colorMatrix.postConcat(contrastMatrix)
        }
        
        // 应用饱和度
        if (filter.saturation != 1f) {
            val saturationMatrix = ColorMatrix().apply {
                setSaturation(filter.saturation)
            }
            colorMatrix.postConcat(saturationMatrix)
        }
        
        // 应用色调
        if (filter.hue != 0f) {
            val hueMatrix = ColorMatrix().apply {
                setRotate(0, filter.hue) // 红色通道
                setRotate(1, filter.hue) // 绿色通道
                setRotate(2, filter.hue) // 蓝色通道
            }
            colorMatrix.postConcat(hueMatrix)
        }
        
        // 应用灰度
        if (filter.grayscale) {
            val grayscaleMatrix = ColorMatrix().apply {
                setSaturation(0f)
            }
            colorMatrix.postConcat(grayscaleMatrix)
        }
        
        // 应用反色
        if (filter.invert) {
            val invertMatrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(invertMatrix)
        }
        
        return ColorMatrixColorFilter(colorMatrix)
    }
    
    /**
     * 重置所有过滤器
     */
    fun resetFilter(): ImageFilter {
        return ImageFilter.Default
    }
}
