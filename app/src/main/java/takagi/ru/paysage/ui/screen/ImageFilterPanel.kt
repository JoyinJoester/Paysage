package takagi.ru.paysage.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.util.ImageFilter

/**
 * 图片过滤器设置底部面板
 */
@Composable
fun ImageFilterPanel(
    filter: ImageFilter,
    onFilterChange: (ImageFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "图片调整",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    TextButton(onClick = onReset) {
                        Text("重置")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("完成")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 亮度滑块
            FilterSlider(
                label = "亮度",
                value = filter.brightness,
                onValueChange = { onFilterChange(filter.copy(brightness = it)) },
                valueRange = -100f..100f,
                icon = Icons.Default.Brightness6
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 对比度滑块
            FilterSlider(
                label = "对比度",
                value = filter.contrast,
                onValueChange = { onFilterChange(filter.copy(contrast = it)) },
                valueRange = 0.5f..2.0f,
                icon = Icons.Default.Contrast
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 饱和度滑块
            FilterSlider(
                label = "饱和度",
                value = filter.saturation,
                onValueChange = { onFilterChange(filter.copy(saturation = it)) },
                valueRange = 0f..2f,
                icon = Icons.Default.Palette
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 色调滑块
            FilterSlider(
                label = "色调",
                value = filter.hue,
                onValueChange = { onFilterChange(filter.copy(hue = it)) },
                valueRange = -180f..180f,
                icon = Icons.Default.ColorLens
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 开关选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 灰度开关
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = filter.grayscale,
                        onCheckedChange = { onFilterChange(filter.copy(grayscale = it)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("灰度")
                }
                
                // 反色开关
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = filter.invert,
                        onCheckedChange = { onFilterChange(filter.copy(invert = it)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("反色")
                }
            }
        }
    }
}

/**
 * 过滤器滑块组件
 */
@Composable
private fun FilterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = String.format("%.1f", value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
