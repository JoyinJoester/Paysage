# 阅读器翻页性能优化设计文档

## 概述

本文档描述了阅读器翻页性能优化的技术设计方案。通过引入多级缓存、智能预加载、内存管理和异步处理等技术，将翻页延迟从当前的 500-1000ms 降低到 50-100ms，实现流畅的阅读体验。

## 架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      ReaderScreen (UI)                       │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐│
│  │  PageView      │  │  DualPageView  │  │  FilterPanel   ││
│  └────────────────┘  └────────────────┘  └────────────────┘│
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                     ReaderViewModel                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              PageCacheManager                         │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │  │
│  │  │ Raw Cache    │  │ Filter Cache │  │ Preloader  │ │  │
│  │  │ (LRU 10)     │  │ (LRU 5)      │  │            │ │  │
│  │  └──────────────┘  └──────────────┘  └────────────┘ │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              BitmapMemoryManager                      │  │
│  │  - Memory monitoring                                  │  │
│  │  - Bitmap recycling                                   │  │
│  │  - Sample size calculation                            │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                      FileParser                              │
│  - Decode images from archives                              │
│  - Apply sample size                                        │
│  - IO operations                                            │
└─────────────────────────────────────────────────────────────┘
```

### 数据流

```
用户翻页请求
    │
    ▼
检查原始缓存 ──命中──> 检查过滤缓存 ──命中──> 立即显示 (50ms)
    │                      │
    未命中                 未命中
    │                      │
    ▼                      ▼
后台解码图片            应用过滤器
    │                      │
    ▼                      ▼
存入原始缓存            存入过滤缓存
    │                      │
    └──────────┬───────────┘
               ▼
           显示页面 (100ms)
               │
               ▼
         触发预加载
```

## 组件和接口

### 1. PageCacheManager

页面缓存管理器，负责管理原始图片和过滤后图片的缓存。

```kotlin
/**
 * 页面缓存管理器
 * 使用双层 LRU 缓存：原始图片缓存 + 过滤图片缓存
 */
class PageCacheManager(
    private val maxRawCacheSize: Int = 10,
    private val maxFilterCacheSize: Int = 5
) {
    // 原始图片缓存 (bookId_pageIndex -> Bitmap)
    private val rawCache = LruCache<String, Bitmap>(maxRawCacheSize)
    
    // 过滤后图片缓存 (bookId_pageIndex_filterHash -> Bitmap)
    private val filterCache = LruCache<String, Bitmap>(maxFilterCacheSize)
    
    // 缓存统计
    private var cacheHits = 0
    private var cacheMisses = 0
    
    /**
     * 获取原始页面
     */
    fun getRawPage(bookId: Long, pageIndex: Int): Bitmap?
    
    /**
     * 缓存原始页面
     */
    fun putRawPage(bookId: Long, pageIndex: Int, bitmap: Bitmap)
    
    /**
     * 获取过滤后的页面
     */
    fun getFilteredPage(bookId: Long, pageIndex: Int, filter: ImageFilter): Bitmap?
    
    /**
     * 缓存过滤后的页面
     */
    fun putFilteredPage(bookId: Long, pageIndex: Int, filter: ImageFilter, bitmap: Bitmap)
    
    /**
     * 清空所有缓存
     */
    fun clearAll()
    
    /**
     * 清空过滤缓存（当过滤器参数改变时）
     */
    fun clearFilterCache()
    
    /**
     * 获取缓存命中率
     */
    fun getCacheHitRate(): Float
    
    /**
     * 获取当前缓存内存使用量（字节）
     */
    fun getMemoryUsage(): Long
    
    /**
     * 移除指定书籍的所有缓存
     */
    fun removeBook(bookId: Long)
}
```

### 2. PagePreloader

页面预加载器，在后台预加载相邻页面。

```kotlin
/**
 * 页面预加载器
 * 在后台低优先级协程中预加载相邻页面
 */
class PagePreloader(
    private val fileParser: FileParser,
    private val cacheManager: PageCacheManager,
    private val memoryManager: BitmapMemoryManager,
    private val scope: CoroutineScope
) {
    // 当前预加载任务
    private var currentJobs = mutableListOf<Job>()
    
    /**
     * 预加载相邻页面
     * @param book 当前书籍
     * @param currentPage 当前页码
     * @param readingMode 阅读模式（单页/双页）
     * @param forward 是否向前翻页
     */
    fun preloadAdjacentPages(
        book: Book,
        currentPage: Int,
        readingMode: ReadingMode,
        forward: Boolean = true
    )
    
    /**
     * 取消所有预加载任务
     */
    fun cancelAll()
    
    /**
     * 预加载指定页面
     */
    private suspend fun preloadPage(book: Book, pageIndex: Int)
}
```

### 3. BitmapMemoryManager

Bitmap 内存管理器，负责内存监控和 Bitmap 采样。

```kotlin
/**
 * Bitmap 内存管理器
 * 负责内存监控、Bitmap 采样和回收
 */
class BitmapMemoryManager(private val context: Context) {
    // 最大内存使用比例（设备可用内存的 25%）
    private val maxMemoryRatio = 0.25f
    
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
    ): Int
    
    /**
     * 检查是否需要清理缓存
     * @return true 如果内存使用超过阈值
     */
    fun shouldClearCache(): Boolean
    
    /**
     * 获取可用内存（字节）
     */
    fun getAvailableMemory(): Long
    
    /**
     * 获取最大允许使用的内存（字节）
     */
    fun getMaxAllowedMemory(): Long
    
    /**
     * 安全回收 Bitmap
     */
    fun recycleBitmap(bitmap: Bitmap?)
    
    /**
     * 注册内存警告监听
     */
    fun registerMemoryWarningListener(listener: () -> Unit)
}
```

### 4. 增强的 ReaderViewModel

```kotlin
class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    
    // 新增组件
    private val cacheManager = PageCacheManager()
    private val memoryManager = BitmapMemoryManager(application)
    private val preloader = PagePreloader(fileParser, cacheManager, memoryManager, viewModelScope)
    
    // 性能监控
    private val performanceMonitor = PerformanceMonitor()
    
    /**
     * 加载页面（优化版）
     */
    private suspend fun loadPageOptimized(book: Book, pageNumber: Int) {
        val startTime = System.currentTimeMillis()
        
        try {
            _uiState.update { it.copy(isLoadingPage = true) }
            
            // 1. 检查原始缓存
            var bitmap = cacheManager.getRawPage(book.id, pageNumber)
            
            if (bitmap == null) {
                // 2. 缓存未命中，从文件解码
                bitmap = withContext(Dispatchers.IO) {
                    val options = BitmapFactory.Options().apply {
                        // 计算采样率
                        inJustDecodeBounds = true
                        // 先获取图片尺寸...
                        inJustDecodeBounds = false
                        inSampleSize = memoryManager.calculateSampleSize(
                            outWidth, outHeight,
                            screenWidth, screenHeight
                        )
                    }
                    
                    if (book.filePath.startsWith("content://")) {
                        fileParser.extractPageFromUri(Uri.parse(book.filePath), pageNumber)
                    } else {
                        fileParser.extractPage(File(book.filePath), pageNumber)
                    }
                }
                
                // 3. 存入原始缓存
                bitmap?.let { cacheManager.putRawPage(book.id, pageNumber, it) }
            }
            
            // 4. 应用过滤器
            val currentFilter = settingsViewModel.settings.value.imageFilter
            val finalBitmap = if (currentFilter.isActive()) {
                // 检查过滤缓存
                var filtered = cacheManager.getFilteredPage(book.id, pageNumber, currentFilter)
                if (filtered == null && bitmap != null) {
                    // 在后台应用过滤器
                    filtered = withContext(Dispatchers.Default) {
                        ImageFilterUtil.applyFilter(bitmap, currentFilter)
                    }
                    cacheManager.putFilteredPage(book.id, pageNumber, currentFilter, filtered)
                }
                filtered
            } else {
                bitmap
            }
            
            // 5. 更新 UI
            _currentPageBitmap.value = finalBitmap
            _uiState.update { it.copy(isLoadingPage = false, currentPage = pageNumber) }
            
            // 6. 更新进度和历史
            updateProgress(book, pageNumber)
            
            // 7. 触发预加载
            val readingMode = settingsViewModel.settings.value.readingMode
            preloader.preloadAdjacentPages(book, pageNumber, readingMode, forward = true)
            
            // 8. 性能监控
            val loadTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordPageLoad(loadTime)
            if (loadTime > 200) {
                Log.w(TAG, "Slow page load: ${loadTime}ms for page $pageNumber")
            }
            
            // 9. 检查内存
            if (memoryManager.shouldClearCache()) {
                cacheManager.clearFilterCache()
            }
            
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoadingPage = false,
                    error = "加载页面失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 快速翻页（跳过中间页）
     */
    fun fastPageTurn(targetPage: Int) {
        // 取消当前预加载
        preloader.cancelAll()
        // 直接跳转
        goToPage(targetPage)
    }
}
```

### 5. PerformanceMonitor

性能监控器，用于收集和分析性能指标。

```kotlin
/**
 * 性能监控器
 */
class PerformanceMonitor {
    private val pageLoadTimes = mutableListOf<Long>()
    private var totalCacheHits = 0
    private var totalCacheRequests = 0
    
    /**
     * 记录页面加载时间
     */
    fun recordPageLoad(timeMs: Long) {
        pageLoadTimes.add(timeMs)
        if (pageLoadTimes.size > 100) {
            pageLoadTimes.removeAt(0)
        }
    }
    
    /**
     * 记录缓存命中
     */
    fun recordCacheHit(hit: Boolean) {
        totalCacheRequests++
        if (hit) totalCacheHits++
    }
    
    /**
     * 获取平均加载时间
     */
    fun getAverageLoadTime(): Long {
        return if (pageLoadTimes.isEmpty()) 0
        else pageLoadTimes.average().toLong()
    }
    
    /**
     * 获取缓存命中率
     */
    fun getCacheHitRate(): Float {
        return if (totalCacheRequests == 0) 0f
        else totalCacheHits.toFloat() / totalCacheRequests
    }
    
    /**
     * 获取性能报告
     */
    fun getReport(): PerformanceReport {
        return PerformanceReport(
            averageLoadTime = getAverageLoadTime(),
            cacheHitRate = getCacheHitRate(),
            totalPageLoads = pageLoadTimes.size
        )
    }
}

data class PerformanceReport(
    val averageLoadTime: Long,
    val cacheHitRate: Float,
    val totalPageLoads: Int
)
```

## 数据模型

### CacheKey

```kotlin
/**
 * 缓存键
 */
data class CacheKey(
    val bookId: Long,
    val pageIndex: Int,
    val filterHash: Int = 0  // 过滤器参数的哈希值
) {
    fun toRawKey(): String = "${bookId}_${pageIndex}"
    fun toFilterKey(): String = "${bookId}_${pageIndex}_${filterHash}"
}
```

### ImageFilter 扩展

```kotlin
/**
 * 为 ImageFilter 添加哈希计算
 */
fun ImageFilter.hashCode(): Int {
    var result = brightness.hashCode()
    result = 31 * result + contrast.hashCode()
    result = 31 * result + saturation.hashCode()
    result = 31 * result + (if (grayscale) 1 else 0)
    result = 31 * result + (if (invert) 1 else 0)
    return result
}

fun ImageFilter.isActive(): Boolean {
    return brightness != 0f || 
           contrast != 1f || 
           saturation != 1f || 
           grayscale || 
           invert
}
```

## 错误处理

### 内存不足处理

```kotlin
// 在 BitmapMemoryManager 中
fun handleLowMemory() {
    // 1. 清空过滤缓存
    cacheManager.clearFilterCache()
    
    // 2. 如果还不够，清空一半原始缓存
    if (shouldClearCache()) {
        cacheManager.trimRawCache(0.5f)
    }
    
    // 3. 强制 GC
    System.gc()
}
```

### 解码失败处理

```kotlin
// 在 loadPageOptimized 中
catch (e: OutOfMemoryError) {
    Log.e(TAG, "OOM while loading page", e)
    memoryManager.handleLowMemory()
    // 重试一次，使用更大的采样率
    retryWithHigherSampleSize(book, pageNumber)
}
```

### 缓存一致性

```kotlin
// 当过滤器参数改变时
fun onFilterChanged(newFilter: ImageFilter) {
    // 清空过滤缓存
    cacheManager.clearFilterCache()
    
    // 重新应用过滤器到当前页
    val currentPage = _uiState.value.currentPage
    reloadCurrentPage()
}
```

## 测试策略

### 单元测试

1. **PageCacheManager 测试**
   - 测试 LRU 淘汰策略
   - 测试缓存命中率计算
   - 测试内存使用量计算
   - 测试并发访问安全性

2. **BitmapMemoryManager 测试**
   - 测试采样率计算
   - 测试内存阈值检查
   - 测试 Bitmap 回收

3. **PagePreloader 测试**
   - 测试预加载任务调度
   - 测试任务取消
   - 测试单页/双页模式预加载

### 集成测试

1. **端到端翻页测试**
   - 测试首次加载性能
   - 测试缓存命中后的性能
   - 测试快速连续翻页
   - 测试内存压力下的表现

2. **不同文件格式测试**
   - ZIP/CBZ 格式
   - RAR/CBR 格式
   - PDF 格式
   - 不同图片尺寸

### 性能测试

1. **基准测试**
   - 首次加载时间 < 200ms
   - 缓存命中时间 < 50ms
   - 预加载不影响当前页显示
   - 内存使用 < 设备可用内存的 25%

2. **压力测试**
   - 连续翻页 100 次不崩溃
   - 大图片（4000x6000）加载正常
   - 低内存设备（2GB RAM）正常运行

## 性能优化清单

### 已实现优化

- [x] 双层 LRU 缓存（原始 + 过滤）
- [x] 智能预加载
- [x] Bitmap 采样
- [x] 异步解码
- [x] 内存监控

### 待实现优化

- [ ] Bitmap 对象池（复用 Bitmap 内存）
- [ ] 硬件加速过滤器（RenderScript）
- [ ] 磁盘缓存（持久化缓存）
- [ ] 增量解码（渐进式显示）
- [ ] WebP 格式支持（更小的文件）

## 配置参数

```kotlin
object ReaderConfig {
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
}
```

## 日志和调试

### 性能日志

```kotlin
// 在 ReaderViewModel 中
private fun logPerformance() {
    val report = performanceMonitor.getReport()
    Log.d(TAG, """
        Performance Report:
        - Average load time: ${report.averageLoadTime}ms
        - Cache hit rate: ${report.cacheHitRate * 100}%
        - Total page loads: ${report.totalPageLoads}
        - Memory usage: ${cacheManager.getMemoryUsage() / 1024 / 1024}MB
    """.trimIndent())
}
```

### 调试模式

```kotlin
// 在 ReaderScreen 中添加调试信息覆盖层
if (BuildConfig.DEBUG && settings.showPerformanceOverlay) {
    PerformanceOverlay(
        averageLoadTime = performanceMonitor.getAverageLoadTime(),
        cacheHitRate = cacheManager.getCacheHitRate(),
        memoryUsage = cacheManager.getMemoryUsage()
    )
}
```

## 兼容性考虑

### Android 版本

- 最低支持 Android 7.0 (API 24)
- LruCache 在所有版本可用
- BitmapFactory.Options 在所有版本可用

### 设备类型

- 低端设备：减小缓存大小，增大采样率
- 高端设备：增大缓存大小，使用原始分辨率
- 平板设备：双页模式优化

### 内存配置

```kotlin
// 根据设备内存动态调整
fun getOptimalCacheSize(context: Context): Int {
    val memoryClass = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .memoryClass
    
    return when {
        memoryClass < 128 -> 5  // 低端设备
        memoryClass < 256 -> 10  // 中端设备
        else -> 15  // 高端设备
    }
}
```
