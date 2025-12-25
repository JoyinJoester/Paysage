package takagi.ru.paysage.ui.components.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ImageLoader"

/**
 * 图片加载缓存（使用 LruCache 防止内存泄漏）
 */
object ImageCache {
    // 使用最大可用内存的 1/8 作为缓存大小
    private val maxCacheSize: Int by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        maxMemory / 8
    }
    
    private val cache: LruCache<String, Bitmap> by lazy {
        object : LruCache<String, Bitmap>(maxCacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // 每个 Bitmap 的大小（KB）
                return bitmap.byteCount / 1024
            }
        }
    }
    
    fun get(src: String): Bitmap? = cache.get(src)
    
    fun put(src: String, bitmap: Bitmap?) {
        if (bitmap != null) {
            cache.put(src, bitmap)
        }
    }
    
    fun clear() {
        cache.evictAll()
    }
}

/**
 * 缩放 Bitmap 缓存（避免每帧创建新 Bitmap）
 */
object ScaledBitmapCache {
    private val cache: LruCache<String, Bitmap> by lazy {
        object : LruCache<String, Bitmap>(20) {  // 最多缓存 20 个缩放后的图片
            override fun sizeOf(key: String, bitmap: Bitmap): Int = 1
        }
    }
    
    fun getScaled(key: String, bitmap: Bitmap, width: Int, height: Int): Bitmap {
        if (width <= 0 || height <= 0) return bitmap
        
        val cacheKey = "$key-$width-$height"
        return cache.get(cacheKey) ?: run {
            val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
            cache.put(cacheKey, scaled)
            scaled
        }
    }
    
    fun clear() {
        cache.evictAll()
    }
}

/**
 * 解码 base64 图片
 */
suspend fun decodeBase64Image(base64: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        // 提取 base64 数据部分
        val data = if (base64.startsWith("data:image")) {
            // 格式：data:image/jpeg;base64,/9j/4AAQ...
            val commaIndex = base64.indexOf(',')
            if (commaIndex > 0) base64.substring(commaIndex + 1) else base64
        } else {
            base64
        }
        
        val imageBytes = Base64.decode(data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode base64 image", e)
        null
    }
}

/**
 * Composable: 加载和缓存图片
 */
@Composable
fun rememberImageBitmap(src: String): Bitmap? {
    var bitmap by remember(src) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(src) {
        // 检查缓存
        val cached = ImageCache.get(src)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }
        
        // 解码图片
        val decoded = decodeBase64Image(src)
        bitmap = decoded
        ImageCache.put(src, decoded)
    }
    
    return bitmap
}

/**
 * DrawScope 扩展：绘制 base64 图片（使用缓存避免每帧创建新 Bitmap）
 */
fun DrawScope.drawBase64Image(
    src: String,
    topLeft: Offset,
    size: Size,
    bitmap: Bitmap?
) {
    if (bitmap != null && size.width > 0 && size.height > 0) {
        // 使用缓存获取缩放后的 Bitmap，避免每帧创建新对象
        val scaledBitmap = ScaledBitmapCache.getScaled(
            key = src,
            bitmap = bitmap,
            width = size.width.toInt(),
            height = size.height.toInt()
        )
        drawImage(
            image = scaledBitmap.asImageBitmap(),
            topLeft = topLeft
        )
    }
}

