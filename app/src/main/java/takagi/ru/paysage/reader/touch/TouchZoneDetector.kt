package takagi.ru.paysage.reader.touch

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/**
 * 触摸区域检测器
 * 
 * 将屏幕划分为九个区域，检测用户点击的区域并触发对应的动作
 * 
 * @param modifier Modifier
 * @param config 触摸区域配置
 * @param onZoneTapped 区域点击回调
 * @param showOverlay 是否显示九宫格覆盖层
 * @param content 内容组件
 */
@Composable
fun TouchZoneDetector(
    modifier: Modifier = Modifier,
    config: TouchZoneConfig,
    onZoneTapped: (TouchZone, TouchAction) -> Unit,
    showOverlay: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(config) {
                detectTapGestures { offset ->
                    val zone = detectTouchZone(offset, size)
                    val action = config.getAction(zone)
                    onZoneTapped(zone, action)
                }
            }
    ) {
        content()
        
        // 显示九宫格覆盖层（用于调试和配置）
        if (showOverlay) {
            TouchZoneOverlay(config = config)
        }
    }
}

/**
 * 检测触摸点所在的区域
 * 
 * 将屏幕划分为 3x3 网格：
 * - 列：左(0-1/3)、中(1/3-2/3)、右(2/3-1)
 * - 行：上(0-1/3)、中(1/3-2/3)、下(2/3-1)
 * 
 * @param offset 触摸点坐标
 * @param size 屏幕尺寸
 * @return 触摸区域
 */
fun detectTouchZone(offset: Offset, size: IntSize): TouchZone {
    val x = offset.x
    val y = offset.y
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    
    // 确定列（0=左, 1=中, 2=右）
    val col = when {
        x < width / 3 -> 0
        x < width * 2 / 3 -> 1
        else -> 2
    }
    
    // 确定行（0=上, 1=中, 2=下）
    val row = when {
        y < height / 3 -> 0
        y < height * 2 / 3 -> 1
        else -> 2
    }
    
    // 根据行列计算区域
    return when (row * 3 + col) {
        0 -> TouchZone.TOP_LEFT
        1 -> TouchZone.TOP_CENTER
        2 -> TouchZone.TOP_RIGHT
        3 -> TouchZone.MIDDLE_LEFT
        4 -> TouchZone.CENTER
        5 -> TouchZone.MIDDLE_RIGHT
        6 -> TouchZone.BOTTOM_LEFT
        7 -> TouchZone.BOTTOM_CENTER
        8 -> TouchZone.BOTTOM_RIGHT
        else -> TouchZone.CENTER // 默认返回中心
    }
}
