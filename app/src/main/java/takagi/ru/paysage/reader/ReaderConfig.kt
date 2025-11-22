package takagi.ru.paysage.reader

import android.app.ActivityManager
import android.content.Context

/**
 * 阅读器配置
 */
data class ReaderConfig(
    // 文本显示设置
    val textSize: Int = 18,
    val textColor: Int = 0xFF000000.toInt(),
    val bgColor: Int = 0xFFFFFFFF.toInt(),
    val lineSpacing: Float = 1.5f,
    val paragraphSpacing: Float = 1.0f,
    val paddingLeft: Int = 24,
    val paddingRight: Int = 24,
    val paddingTop: Int = 32,
    val paddingBottom: Int = 32,
    
    // 翻页设置
    val pageFlipMode: String = "SLIDE",
    val volumeKeyNavigation: Boolean = true,
    val keepScreenOn: Boolean = true,
    val touchZoneEnabled: Boolean = true
) {
    companion object {
        // 缓存配置
        const val RAW_CACHE_SIZE = 10  // 原始图片缓存数量
        const val FILTER_CACHE_SIZE = 5  // 过滤图片缓存数量
        
        // 预加载配置
        const val PRELOAD_AHEAD_PAGES = 2  // 向前预加载页数
        const val PRELOAD_BEHIND_PAGES = 1  // 向后预加载页数
        const val PRELOAD_AHEAD_DUAL_PAGES = 4  // 双页模式向前预加载
        
        // 内存配置
        const val MAX_MEMORY_RATIO = 0.25f  // 最大内存使用比例
        const val MEMORY_WARNING_RATIO = 0.20f  // 内存警告阈值
        
        // 性能配置
        const val TARGET_LOAD_TIME_MS = 100L  // 目标加载时间
        const val SLOW_LOAD_THRESHOLD_MS = 200L  // 慢加载阈值
        
        // 采样配置
        const val MAX_SCALE_FACTOR = 2.0f  // 最大缩放倍数
        const val MIN_SAMPLE_SIZE = 1  // 最小采样率
        
        /**
         * 根据设备内存动态调整缓存大小
         */
        fun getOptimalCacheSize(context: Context): CacheSizeConfig {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryClass = activityManager.memoryClass
            
            return when {
                memoryClass < 128 -> CacheSizeConfig(
                    rawCacheSize = 5,
                    filterCacheSize = 3
                )
                memoryClass < 256 -> CacheSizeConfig(
                    rawCacheSize = 10,
                    filterCacheSize = 5
                )
                else -> CacheSizeConfig(
                    rawCacheSize = 15,
                    filterCacheSize = 8
                )
            }
        }
        
        /**
         * 创建默认配置
         */
        fun default(): ReaderConfig {
            return ReaderConfig()
        }
        
        /**
         * 从 AppSettings 创建 ReaderConfig
         */
        fun fromAppSettings(settings: takagi.ru.paysage.data.model.AppSettings): ReaderConfig {
            return ReaderConfig(
                textSize = settings.textSize,
                textColor = settings.textColor,
                bgColor = settings.bgColor,
                lineSpacing = settings.lineSpacing,
                paragraphSpacing = settings.paragraphSpacing,
                paddingLeft = settings.paddingLeft,
                paddingRight = settings.paddingRight,
                paddingTop = settings.paddingTop,
                paddingBottom = settings.paddingBottom,
                volumeKeyNavigation = settings.volumeKeyNavigation,
                keepScreenOn = settings.keepScreenOn
            )
        }
    }
}

/**
 * 缓存大小配置
 */
data class CacheSizeConfig(
    val rawCacheSize: Int,
    val filterCacheSize: Int
)
