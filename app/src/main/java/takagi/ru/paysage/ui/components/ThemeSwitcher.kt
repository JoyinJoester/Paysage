package takagi.ru.paysage.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.ui.theme.ExpressiveAnimations

/**
 * 主题切换器
 * 用于在亮色和暗色主题之间切换
 */
@Composable
fun ThemeSwitcher(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkTheme) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = androidx.compose.animation.core.tween(ExpressiveAnimations.DURATION_MEDIUM),
        label = "theme_bg"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (isDarkTheme) 1.1f else 1f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "icon_scale"
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onThemeChange(!isDarkTheme) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = if (isDarkTheme) "暗色主题" else "亮色主题",
            modifier = Modifier.scale(iconScale),
            tint = if (isDarkTheme) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSecondaryContainer
        )
        
        Text(
            text = if (isDarkTheme) "暗色主题" else "亮色主题",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * 紧凑型主题切换开关
 */
@Composable
fun CompactThemeSwitcher(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = isDarkTheme,
        onCheckedChange = onThemeChange,
        modifier = modifier,
        thumbContent = {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}
