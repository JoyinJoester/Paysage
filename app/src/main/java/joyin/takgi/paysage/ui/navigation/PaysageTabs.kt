package joyin.takgi.paysage.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R

enum class PaysageTab(
    val index: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Home(0, R.string.tab_home, Icons.Default.Home),
    Esim(1, R.string.tab_esim, Icons.Default.Phone),
    Settings(2, R.string.tab_settings, Icons.Default.Settings);

    companion object {
        fun fromIndex(index: Int): PaysageTab = entries.firstOrNull { it.index == index } ?: Home
    }
}

@Composable
fun PaysageBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = NavigationBarDefaults.windowInsets,
        tonalElevation = 3.dp
    ) {
        PaysageTab.entries.forEach { tab ->
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = selectedTab == tab.index,
                onClick = { onTabSelected(tab.index) },
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
