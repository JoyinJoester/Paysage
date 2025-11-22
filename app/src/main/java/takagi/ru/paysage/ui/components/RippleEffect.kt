package takagi.ru.paysage.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 涟漪效果数据类
 */
data class Ripple(
    val position: Offset,
    val startTime: Long
)

/**
 * 涟漪效果组件
 * 为导航项添加点击涟漪动画
 */
@Composable
fun RippleEffect(
    ripples: List<Ripple>,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    modifier: Modifier = Modifier
) {
    val currentTime by produceState(0L) {
        while (true) {
            value = System.currentTimeMillis()
            delay(16) // ~60 FPS
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        ripples.forEach { ripple ->
            val elapsed = currentTime - ripple.startTime
            if (elapsed < 600) { // 动画持续 600ms
                val progress = elapsed / 600f
                val radius = size.minDimension * progress * 0.5f
                val alpha = (1f - progress) * 0.3f
                
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = ripple.position
                )
            }
        }
    }
}

/**
 * 记住涟漪状态
 */
@Composable
fun rememberRippleState(): MutableState<List<Ripple>> {
    return remember { mutableStateOf(emptyList()) }
}

/**
 * 添加涟漪
 */
fun MutableState<List<Ripple>>.addRipple(position: Offset) {
    value = value + Ripple(position, System.currentTimeMillis())
}

/**
 * 清理过期的涟漪
 */
fun MutableState<List<Ripple>>.cleanupRipples() {
    val currentTime = System.currentTimeMillis()
    value = value.filter { currentTime - it.startTime < 600 }
}
