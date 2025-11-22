package takagi.ru.paysage.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import takagi.ru.paysage.R
import takagi.ru.paysage.data.model.ColorScheme
import takagi.ru.paysage.data.model.ThemeMode
import takagi.ru.paysage.viewmodel.SettingsViewModel

/**
 * 外观设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    // 对话框状态
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 主题模式
            item {
                SettingsItemWithIcon(
                    icon = Icons.Filled.DarkMode,
                    title = stringResource(R.string.settings_theme_mode),
                    subtitle = when (settings.themeMode) {
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            // 配色方案
            item {
                SettingsItemWithIcon(
                    icon = Icons.Filled.Colorize,
                    title = stringResource(R.string.settings_color_scheme),
                    subtitle = when (settings.colorScheme) {
                        ColorScheme.DEFAULT -> stringResource(R.string.settings_color_default)
                        ColorScheme.OCEAN_BLUE -> stringResource(R.string.settings_color_ocean_blue)
                        ColorScheme.SUNSET_ORANGE -> stringResource(R.string.settings_color_sunset_orange)
                        ColorScheme.FOREST_GREEN -> stringResource(R.string.settings_color_forest_green)
                        ColorScheme.TECH_PURPLE -> stringResource(R.string.settings_color_tech_purple)
                        ColorScheme.BLACK_MAMBA -> stringResource(R.string.settings_color_black_mamba)
                        ColorScheme.GREY_STYLE -> stringResource(R.string.settings_color_grey_style)
                        else -> stringResource(R.string.settings_color_custom)
                    },
                    onClick = { showColorSchemeDialog = true }
                )
            }
            
            // 壁纸取色
            item {
                SettingsSwitchItemWithIcon(
                    icon = Icons.Filled.Colorize,
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(R.string.settings_dynamic_color_desc),
                    checked = settings.dynamicColorEnabled,
                    onCheckedChange = { viewModel.updateDynamicColorEnabled(it) }
                )
            }
            

        }
    }
    
    // 主题模式对话框
    if (showThemeDialog) {
        ThemeModeDialog(
            currentTheme = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                viewModel.updateThemeMode(theme)
                showThemeDialog = false
            }
        )
    }
    
    // 配色方案对话框
    if (showColorSchemeDialog) {
        ColorSchemeDialog(
            currentScheme = settings.colorScheme,
            onDismiss = { showColorSchemeDialog = false },
            onSelect = { scheme ->
                viewModel.updateColorScheme(scheme)
                showColorSchemeDialog = false
            }
        )
    }
    

}

/**
 * 带图标的设置项
 */
@Composable
private fun SettingsItemWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 带图标的设置开关项
 */
@Composable
private fun SettingsSwitchItemWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

/**
 * 主题模式对话框
 */
@Composable
private fun ThemeModeDialog(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_mode)) },
        text = {
            Column {
                ThemeMode.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = { onSelect(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (theme) {
                                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * 配色方案对话框
 */
@Composable
private fun ColorSchemeDialog(
    currentScheme: ColorScheme,
    onDismiss: () -> Unit,
    onSelect: (ColorScheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_color_scheme)) },
        text = {
            Column {
                // 默认配色
                ColorSchemeOption(
                    name = stringResource(R.string.settings_color_default),
                    primaryColor = Color(0xFF6650a4),
                    secondaryColor = Color(0xFF625b71),
                    tertiaryColor = Color(0xFF7D5260),
                    isSelected = currentScheme == ColorScheme.DEFAULT,
                    onClick = { onSelect(ColorScheme.DEFAULT) }
                )
                
                // 海洋蓝
                ColorSchemeOption(
                    name = stringResource(R.string.settings_color_ocean_blue),
                    primaryColor = Color(0xFF1565C0),
                    secondaryColor = Color(0xFF0277BD),
                    tertiaryColor = Color(0xFF26C6DA),
                    isSelected = currentScheme == ColorScheme.OCEAN_BLUE,
                    onClick = { onSelect(ColorScheme.OCEAN_BLUE) }
                )
                
                // 日落橙
                ColorSchemeOption(
                    name = stringResource(R.string.settings_color_sunset_orange),
                    primaryColor = Color(0xFFE65100),
                    secondaryColor = Color(0xFFF57C00),
                    tertiaryColor = Color(0xFFFFA726),
                    isSelected = currentScheme == ColorScheme.SUNSET_ORANGE,
                    onClick = { onSelect(ColorScheme.SUNSET_ORANGE) }
                )
                
                // 森林绿
                ColorSchemeOption(
                    name = stringResource(R.string.settings_color_forest_green),
                    primaryColor = Color(0xFF1B5E20),
                    secondaryColor = Color(0xFF2E7D32),
                    tertiaryColor = Color(0xFF388E3C),
                    isSelected = currentScheme == ColorScheme.FOREST_GREEN,
                    onClick = { onSelect(ColorScheme.FOREST_GREEN) }
                )
                
                // 科技紫
                ColorSchemeOption(
                    name = stringResource(R.string.settings_color_tech_purple),
                    primaryColor = Color(0xFF4A148C),
                    secondaryColor = Color(0xFF6A1B9A),
                    tertiaryColor = Color(0xFF8E24AA),
                    isSelected = currentScheme == ColorScheme.TECH_PURPLE,
                    onClick = { onSelect(ColorScheme.TECH_PURPLE) }
                )
                
                // 黑曼巴
                ColorSchemeOption(
                    name = stringResource(R.string.settings_color_black_mamba),
                    primaryColor = Color(0xFF552583),
                    secondaryColor = Color(0xFFFDB927),
                    tertiaryColor = Color(0xFF2A2A2A),
                    isSelected = currentScheme == ColorScheme.BLACK_MAMBA,
                    onClick = { onSelect(ColorScheme.BLACK_MAMBA) }
                )
                
                // 小黑紫
                ColorSchemeOption(
                    name = stringResource(R.string.settings_color_grey_style),
                    primaryColor = Color(0xFF424242),
                    secondaryColor = Color(0xFF616161),
                    tertiaryColor = Color(0xFF9E9E9E),
                    isSelected = currentScheme == ColorScheme.GREY_STYLE,
                    onClick = { onSelect(ColorScheme.GREY_STYLE) }
                )
            }
        },
        confirmButton = {}
    )
}

/**
 * 配色方案选项（Monica 风格）
 */
@Composable
private fun ColorSchemeOption(
    name: String,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 颜色圆圈
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(secondaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tertiaryColor)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 名称
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            // 选中标记
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



