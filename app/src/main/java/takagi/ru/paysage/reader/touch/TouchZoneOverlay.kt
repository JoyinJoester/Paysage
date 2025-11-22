package takagi.ru.paysage.reader.touch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/**
 * 九宫格触摸区域覆盖层
 * 
 * 用于可视化显示触摸区域划分和对应的动作
 * 
 * @param config 触摸区域配置
 * @param modifier Modifier
 */
@Composable
fun TouchZoneOverlay(
    config: TouchZoneConfig,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurface
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // 绘制九宫格线条
        drawGridLines(width, height)
        
        // 绘制每个区域的动作文本
        drawZoneLabels(
            width = width,
            height = height,
            config = config,
            textColor = textColor
        )
    }
}

/**
 * 绘制九宫格线条
 */
private fun DrawScope.drawGridLines(width: Float, height: Float) {
    val lineColor = Color.White.copy(alpha = 0.5f)
    val lineWidth = 2f
    
    // 垂直线
    drawLine(
        color = lineColor,
        start = Offset(width / 3, 0f),
        end = Offset(width / 3, height),
        strokeWidth = lineWidth
    )
    drawLine(
        color = lineColor,
        start = Offset(width * 2 / 3, 0f),
        end = Offset(width * 2 / 3, height),
        strokeWidth = lineWidth
    )
    
    // 水平线
    drawLine(
        color = lineColor,
        start = Offset(0f, height / 3),
        end = Offset(width, height / 3),
        strokeWidth = lineWidth
    )
    drawLine(
        color = lineColor,
        start = Offset(0f, height * 2 / 3),
        end = Offset(width, height * 2 / 3),
        strokeWidth = lineWidth
    )
}

/**
 * 绘制区域标签
 */
private fun DrawScope.drawZoneLabels(
    width: Float,
    height: Float,
    config: TouchZoneConfig,
    textColor: Color
) {
    val zones = listOf(
        Triple(TouchZone.TOP_LEFT, width / 6, height / 6),
        Triple(TouchZone.TOP_CENTER, width / 2, height / 6),
        Triple(TouchZone.TOP_RIGHT, width * 5 / 6, height / 6),
        Triple(TouchZone.MIDDLE_LEFT, width / 6, height / 2),
        Triple(TouchZone.CENTER, width / 2, height / 2),
        Triple(TouchZone.MIDDLE_RIGHT, width * 5 / 6, height / 2),
        Triple(TouchZone.BOTTOM_LEFT, width / 6, height * 5 / 6),
        Triple(TouchZone.BOTTOM_CENTER, width / 2, height * 5 / 6),
        Triple(TouchZone.BOTTOM_RIGHT, width * 5 / 6, height * 5 / 6)
    )
    
    zones.forEach { (zone, x, y) ->
        val action = config.getAction(zone)
        val text = action.getDisplayName()
        
        // 使用 Android Canvas 绘制文本
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                // 添加阴影使文本更清晰
                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
            }
            
            drawText(text, x, y, paint)
        }
    }
}
