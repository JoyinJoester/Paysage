package takagi.ru.paysage.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import takagi.ru.paysage.ui.components.ExpressiveIconButton

/**
 * 导航菜单按钮
 * 用于 Compact 布局中打开导航抽屉
 */
@Composable
fun NavigationMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "打开导航菜单"
) {
    ExpressiveIconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = contentDescription
        )
    }
}
