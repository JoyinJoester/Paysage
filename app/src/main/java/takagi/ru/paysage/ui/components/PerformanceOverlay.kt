package takagi.ru.paysage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.reader.CacheStats
import takagi.ru.paysage.reader.MemoryReport
import takagi.ru.paysage.reader.PerformanceReport

/**
 * 性能监控覆盖层（调试用）
 */
@Composable
fun PerformanceOverlay(
    performanceReport: PerformanceReport,
    cacheStats: CacheStats,
    memoryReport: MemoryReport,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "性能监控",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            
            // 加载性能
            Text(
                text = "平均加载: ${performanceReport.averageLoadTime}ms",
                style = MaterialTheme.typography.bodySmall,
                color = if (performanceReport.averageLoadTime > 200) Color.Red else Color.Green
            )
            
            Text(
                text = "最小/最大: ${performanceReport.minLoadTime}ms / ${performanceReport.maxLoadTime}ms",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            
            // 缓存统计
            Text(
                text = "缓存命中率: ${(cacheStats.hitRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = if (cacheStats.hitRate > 0.5f) Color.Green else Color.Yellow
            )
            
            Text(
                text = "原始缓存: ${cacheStats.rawCacheSize}/${cacheStats.maxRawCacheSize}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            
            Text(
                text = "过滤缓存: ${cacheStats.filterCacheSize}/${cacheStats.maxFilterCacheSize}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            
            // 内存使用
            Text(
                text = "缓存内存: ${cacheStats.memoryUsage / 1024 / 1024}MB",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            
            Text(
                text = "系统内存: ${memoryReport.toMB(memoryReport.usedMemory)} / ${memoryReport.toMB(memoryReport.totalMemory)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (memoryReport.usageRatio > 0.8f) Color.Red else Color.White
            )
        }
    }
}

/**
 * 简化版性能覆盖层
 */
@Composable
fun SimplePerformanceOverlay(
    averageLoadTime: Long,
    cacheHitRate: Float,
    memoryUsage: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${averageLoadTime}ms",
                style = MaterialTheme.typography.labelSmall,
                color = if (averageLoadTime > 200) Color.Red else Color.Green
            )
            
            Text(
                text = "${(cacheHitRate * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (cacheHitRate > 0.5f) Color.Green else Color.Yellow
            )
            
            Text(
                text = "${memoryUsage / 1024 / 1024}MB",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}
