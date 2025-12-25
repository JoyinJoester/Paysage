package takagi.ru.paysage.ui.components.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import takagi.ru.paysage.data.model.PageMode
import takagi.ru.paysage.data.model.ReaderConfig

/**
 * 阅读设置对话框
 * 参考 Legado 的设置界面，提供完整的阅读配置选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsDialog(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("文字", "背景", "布局", "翻页")
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column {
                // 标题栏
                TopAppBar(
                    title = { Text("阅读设置") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "关闭")
                        }
                    }
                )
                
                // 标签页
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                // 内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> TextSettingsTab(config, onConfigChange)
                        1 -> BackgroundSettingsTab(config, onConfigChange)
                        2 -> LayoutSettingsTab(config, onConfigChange)
                        3 -> PageFlipSettingsTab(config, onConfigChange)
                    }
                }
            }
        }
    }
}

/**
 * 文字设置标签页
 */
@Composable
private fun TextSettingsTab(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 字体大小
        SettingItem(
            title = "字体大小",
            value = "${config.textSize}",
            icon = Icons.Default.FormatSize
        ) {
            Slider(
                value = config.textSize.toFloat(),
                onValueChange = { 
                    onConfigChange(config.copy(textSize = it.toInt()))
                },
                valueRange = 12f..32f,
                steps = 19
            )
        }
        
        // 行间距
        SettingItem(
            title = "行间距",
            value = "${"%.1f".format(config.lineSpacing)}",
            icon = Icons.Default.FormatLineSpacing
        ) {
            Slider(
                value = config.lineSpacing,
                onValueChange = { 
                    onConfigChange(config.copy(lineSpacing = it))
                },
                valueRange = 1.0f..3.0f,
                steps = 19
            )
        }
        
        // 段落间距
        SettingItem(
            title = "段落间距",
            value = "${config.paragraphSpacing} dp",
            icon = Icons.Default.FormatIndentIncrease
        ) {
            Slider(
                value = config.paragraphSpacing.toFloat(),
                onValueChange = { 
                    onConfigChange(config.copy(paragraphSpacing = it.toInt()))
                },
                valueRange = 0f..20f,
                steps = 19
            )
        }
        
        // 文字颜色
        ColorPickerItem(
            title = "文字颜色",
            color = Color(config.textColor),
            onColorChange = { 
                onConfigChange(config.copy(textColor = it.toArgb()))
            }
        )
    }
}

/**
 * 背景设置标签页
 */
@Composable
private fun BackgroundSettingsTab(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 背景颜色
        ColorPickerItem(
            title = "背景颜色",
            color = Color(config.bgColor),
            onColorChange = { 
                onConfigChange(config.copy(bgColor = it.toArgb()))
            }
        )
        
        // 预设主题
        Text(
            "预设主题",
            style = MaterialTheme.typography.titleMedium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemePresetButton(
                name = "默认",
                textColor = Color.Black,
                bgColor = Color.White,
                onClick = {
                    onConfigChange(config.copy(
                        textColor = Color.Black.toArgb(),
                        bgColor = Color.White.toArgb()
                    ))
                }
            )
            
            ThemePresetButton(
                name = "护眼",
                textColor = Color(0xFF3E3E3E),
                bgColor = Color(0xFFCCE8CF),
                onClick = {
                    onConfigChange(config.copy(
                        textColor = Color(0xFF3E3E3E).toArgb(),
                        bgColor = Color(0xFFCCE8CF).toArgb()
                    ))
                }
            )
            
            ThemePresetButton(
                name = "夜间",
                textColor = Color(0xFFB0B0B0),
                bgColor = Color(0xFF1E1E1E),
                onClick = {
                    onConfigChange(config.copy(
                        textColor = Color(0xFFB0B0B0).toArgb(),
                        bgColor = Color(0xFF1E1E1E).toArgb()
                    ))
                }
            )
        }
    }
}

/**
 * 布局设置标签页
 */
@Composable
private fun LayoutSettingsTab(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 上边距
        SettingItem(
            title = "上边距",
            value = "${config.paddingTop}",
            icon = Icons.Default.VerticalAlignTop
        ) {
            Slider(
                value = config.paddingTop.toFloat(),
                onValueChange = { 
                    onConfigChange(config.copy(paddingTop = it.toInt()))
                },
                valueRange = 0f..100f,
                steps = 19
            )
        }
        
        // 下边距
        SettingItem(
            title = "下边距",
            value = "${config.paddingBottom}",
            icon = Icons.Default.VerticalAlignBottom
        ) {
            Slider(
                value = config.paddingBottom.toFloat(),
                onValueChange = { 
                    onConfigChange(config.copy(paddingBottom = it.toInt()))
                },
                valueRange = 0f..100f,
                steps = 19
            )
        }
        
        // 左边距
        SettingItem(
            title = "左边距",
            value = "${config.paddingLeft}",
            icon = Icons.Default.FormatAlignLeft
        ) {
            Slider(
                value = config.paddingLeft.toFloat(),
                onValueChange = { 
                    onConfigChange(config.copy(paddingLeft = it.toInt()))
                },
                valueRange = 0f..100f,
                steps = 19
            )
        }
        
        // 右边距
        SettingItem(
            title = "右边距",
            value = "${config.paddingRight}",
            icon = Icons.Default.FormatAlignRight
        ) {
            Slider(
                value = config.paddingRight.toFloat(),
                onValueChange = { 
                    onConfigChange(config.copy(paddingRight = it.toInt()))
                },
                valueRange = 0f..100f,
                steps = 19
            )
        }
    }
}

/**
 * 翻页设置标签页
 */
@Composable
private fun PageFlipSettingsTab(
    config: ReaderConfig,
    onConfigChange: (ReaderConfig) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "翻页模式",
            style = MaterialTheme.typography.titleMedium
        )
        
        // 翻页模式选择
        val pageFlipModes = listOf(
            PageMode.SLIDE to "滑动",
            PageMode.COVER to "覆盖",
            PageMode.SIMULATION to "仿真",
            PageMode.SCROLL to "滚动",
            PageMode.NONE to "无动画"
        )
        
        pageFlipModes.forEach { (mode, name) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name)
                RadioButton(
                    selected = config.pageMode == mode,
                    onClick = {
                        onConfigChange(config.copy(pageMode = mode))
                    }
                )
            }
        }
        
        Divider()
        
        // 音量键翻页
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("音量键翻页")
            Switch(
                checked = config.volumeKeyPage,
                onCheckedChange = {
                    onConfigChange(config.copy(volumeKeyPage = it))
                }
            )
        }
        
        // 屏幕常亮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("屏幕常亮")
            Switch(
                checked = config.keepScreenOn,
                onCheckedChange = {
                    onConfigChange(config.copy(keepScreenOn = it))
                }
            )
        }
    }
}

/**
 * 设置项组件
 */
@Composable
private fun SettingItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
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
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(title)
            }
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

/**
 * 颜色选择器项
 */
@Composable
private fun ColorPickerItem(
    title: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 简化的颜色选择器 - 预设颜色
            val colors = listOf(
                Color.Black, Color.DarkGray, Color.Gray,
                Color.White, Color(0xFFCCE8CF), Color(0xFFFFF8DC)
            )
            
            colors.forEach { presetColor ->
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = presetColor,
                    border = if (color == presetColor) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                    },
                    onClick = { onColorChange(presetColor) }
                ) {}
            }
        }
    }
}

/**
 * 主题预设按钮
 */
@Composable
private fun ThemePresetButton(
    name: String,
    textColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(80.dp, 60.dp),
        shape = MaterialTheme.shapes.small,
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                name,
                color = textColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
