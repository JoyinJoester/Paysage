package takagi.ru.paysage.ui.components.reader

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.data.model.ReaderConfig

/**
 * 快速设置面板
 * 提供常用设置的快速访问
 */
@Composable
fun QuickSettingsPanel(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit,
    onMoreSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "快速设置",
                    style = MaterialTheme.typography.titleMedium
                )
                
                TextButton(onClick = onMoreSettings) {
                    Text("更多设置")
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Divider()
            
            // 亮度调节
            BrightnessControl()
            
            // 字体大小
            FontSizeControl(
                fontSize = config.textSize,
                onFontSizeChange = {
                    onConfigChange(config.copy(textSize = it))
                }
            )
            
            // 翻页模式快速切换
            // TODO: 重新实现翻页模式控制以使用 PageMode 枚举
            // PageFlipModeControl(
            //     currentMode = config.pageMode,
            //     onModeChange = {
            //         onConfigChange(config.copy(pageMode = it))
            //     }
            // )
            
            // 快捷开关
            QuickToggles(
                config = config,
                onConfigChange = onConfigChange
            )
        }
    }
}

/**
 * 亮度控制
 */
@Composable
private fun BrightnessControl() {
    var brightness by remember { mutableStateOf(0.5f) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Brightness6,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text("亮度")
            }
            Text(
                "${(brightness * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0f..1f
        )
    }
}

/**
 * 字体大小控制
 */
@Composable
private fun FontSizeControl(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text("字体大小")
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        if (fontSize > 12) onFontSizeChange(fontSize - 1)
                    },
                    enabled = fontSize > 12
                ) {
                    Icon(Icons.Default.Remove, "减小")
                }
                
                Text(
                    "$fontSize",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(32.dp)
                )
                
                IconButton(
                    onClick = { 
                        if (fontSize < 32) onFontSizeChange(fontSize + 1)
                    },
                    enabled = fontSize < 32
                ) {
                    Icon(Icons.Default.Add, "增大")
                }
            }
        }
    }
}

/**
 * 翻页模式控制
 */
@Composable
private fun PageFlipModeControl(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SwipeRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text("翻页模式")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                "SLIDE" to "滑动",
                "COVER" to "覆盖",
                "SIMULATION" to "仿真"
            )
            
            modes.forEach { (mode, name) ->
                FilterChip(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(name) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 快捷开关
 */
@Composable
private fun QuickToggles(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit
) {
    Column {
        Text(
            "快捷开关",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 音量键翻页
            QuickToggleChip(
                label = "音量键",
                icon = Icons.Default.VolumeUp,
                checked = config.volumeKeyPage,
                onCheckedChange = {
                    onConfigChange(config.copy(volumeKeyPage = it))
                }
            )
            
            // 屏幕常亮
            QuickToggleChip(
                label = "常亮",
                icon = Icons.Default.LightMode,
                checked = config.keepScreenOn,
                onCheckedChange = {
                    onConfigChange(config.copy(keepScreenOn = it))
                }
            )
            
            // 触摸区域
            // TODO: 添加触摸区域启用配置到 ReaderConfig
            // QuickToggleChip(
            //     label = "触摸区",
            //     icon = Icons.Default.TouchApp,
            //     checked = config.touchZoneEnabled,
            //     onCheckedChange = {
            //         onConfigChange(config.copy(touchZoneEnabled = it))
            //     }
            // )
        }
    }
}

/**
 * 快捷开关芯片
 */
@Composable
private fun RowScope.QuickToggleChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        },
        modifier = Modifier.weight(1f)
    )
}

/**
 * 浮动快速设置按钮
 */
@Composable
fun FloatingQuickSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Icon(
            Icons.Default.Tune,
            contentDescription = "快速设置"
        )
    }
}
