package takagi.ru.paysage.ui.components.reader

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * 垂直侧边控制器
 * 
 * M3E 风格的侧边控制栏，支持：
 * - 亮度调节模式（默认）
 * - 字体大小调节模式
 * - 平滑动画切换
 */
@Composable
fun VerticalSideControl(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    
    // 控制模式: false=亮度, true=字体大小
    var isFontSizeMode by remember { mutableStateOf(false) }
    
    // 内部亮度状态，用于实时响应
    var currentBrightness by remember { mutableFloatStateOf(brightness) }
    
    // 字体大小状态（归一化到 0-1 范围用于 Slider）
    val fontSizeRange = 12..32
    val normalizedFontSize = ((fontSize - fontSizeRange.first).toFloat() / 
        (fontSizeRange.last - fontSizeRange.first).toFloat()).coerceIn(0f, 1f)
    var currentFontSize by remember { mutableFloatStateOf(normalizedFontSize) }
    
    // 同步外部值
    LaunchedEffect(brightness) {
        currentBrightness = brightness
    }
    LaunchedEffect(fontSize) {
        currentFontSize = ((fontSize - fontSizeRange.first).toFloat() / 
            (fontSizeRange.last - fontSizeRange.first).toFloat()).coerceIn(0f, 1f)
    }
    
    // 使用统一的颜色，不随模式切换变化
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val trackColor = MaterialTheme.colorScheme.primary
    
    Surface(
        modifier = modifier
            .width(48.dp)
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 垂直滑块区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // 使用旋转实现垂直滑块
                VerticalSlider(
                    value = if (isFontSizeMode) currentFontSize else currentBrightness,
                    onValueChange = { newValue ->
                        if (isFontSizeMode) {
                            currentFontSize = newValue
                            // 转换回实际字体大小
                            val actualSize = (fontSizeRange.first + 
                                (newValue * (fontSizeRange.last - fontSizeRange.first))).toInt()
                            onFontSizeChange(actualSize)
                        } else {
                            currentBrightness = newValue.coerceAtLeast(0.01f)
                            // 实时应用亮度到窗口
                            window?.let { win ->
                                val layoutParams = win.attributes
                                layoutParams.screenBrightness = currentBrightness
                                win.attributes = layoutParams
                            }
                        }
                    },
                    onValueChangeFinished = {
                        if (!isFontSizeMode) {
                            onBrightnessChange(currentBrightness)
                        }
                    },
                    valueRange = if (isFontSizeMode) 0f..1f else 0.01f..1f,
                    trackColor = trackColor,
                    modifier = Modifier.fillMaxHeight()
                )
            }
            
            // 数值显示
            AnimatedContent(
                targetState = if (isFontSizeMode) {
                    "${(fontSizeRange.first + (currentFontSize * (fontSizeRange.last - fontSizeRange.first))).toInt()}"
                } else {
                    "${(currentBrightness * 100).toInt()}%"
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith 
                    fadeOut(animationSpec = tween(200))
                },
                label = "valueDisplay"
            ) { displayValue ->
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 模式切换按钮 - 显示当前模式的图标
            FilledTonalIconButton(
                onClick = { isFontSizeMode = !isFontSizeMode },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                AnimatedContent(
                    targetState = isFontSizeMode,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith 
                        (scaleOut(targetScale = 0.8f) + fadeOut())
                    },
                    label = "iconSwitch"
                ) { fontMode ->
                    // 显示当前模式的图标：亮度模式显示太阳，字体模式显示文字
                    Icon(
                        imageVector = if (fontMode) Icons.Default.FormatSize else Icons.Default.Brightness6,
                        contentDescription = if (fontMode) "字体大小调节模式" else "亮度调节模式",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 垂直方向的 Slider
 * 通过旋转水平 Slider 实现
 */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    trackColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    // 动画数值变化
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sliderValue"
    )
    
    Box(
        modifier = modifier
            .vertical()
            .width(140.dp)
            .height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = trackColor.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * Modifier 扩展：将元素旋转为垂直方向
 */
private fun Modifier.vertical() = this
    .graphicsLayer {
        rotationZ = 270f
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(-placeable.width, 0)
        }
    }
