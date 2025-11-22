package takagi.ru.paysage.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 窗口尺寸类别
 * 用于响应式布局适配
 */
enum class WindowSizeClass {
    /**
     * 紧凑型 - 手机
     * 宽度 < 600dp
     */
    Compact,
    
    /**
     * 中等型 - 小平板
     * 宽度 600-839dp
     */
    Medium,
    
    /**
     * 扩展型 - 大平板/桌面
     * 宽度 >= 840dp
     */
    Expanded
}

/**
 * 窗口尺寸断点
 */
object WindowSizeBreakpoints {
    val COMPACT_MAX: Dp = 600.dp
    val MEDIUM_MAX: Dp = 840.dp
}

/**
 * 记住当前窗口尺寸类别
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    return remember(screenWidth) {
        when {
            screenWidth < WindowSizeBreakpoints.COMPACT_MAX -> WindowSizeClass.Compact
            screenWidth < WindowSizeBreakpoints.MEDIUM_MAX -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        }
    }
}

/**
 * 窗口尺寸信息
 */
data class WindowSize(
    val width: Dp,
    val height: Dp,
    val sizeClass: WindowSizeClass
)

/**
 * 记住当前窗口尺寸信息
 */
@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    val sizeClass = rememberWindowSizeClass()
    
    return remember(width, height, sizeClass) {
        WindowSize(width, height, sizeClass)
    }
}
