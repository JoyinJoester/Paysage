package takagi.ru.paysage.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.paysage.ui.components.ExpressiveNavigationRailItem
import takagi.ru.paysage.util.statusBarsPadding

/**
 * 第一层导航栏组件
 * 显示主要功能的图标菜单
 */
@Composable
fun PrimaryNavigationRail(
    selectedItem: PrimaryNavItem,
    onItemClick: (PrimaryNavItem) -> Unit,
    modifier: Modifier = Modifier,
    onFolderPickerClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
    ) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            // 状态栏占位
            Spacer(Modifier.statusBarsPadding())
            
            // 顶部间距
            Spacer(Modifier.height(16.dp))
            
            // 文件夹选择按钮
            Surface(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .size(56.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onFolderPickerClick
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "选择文件夹",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier
                    .width(48.dp)
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(8.dp))
            
            // 遍历所有第一层导航项
            PrimaryNavItem.values().forEach { item ->
                ExpressiveNavigationRailItem(
                    selected = selectedItem == item,
                    onClick = { onItemClick(item) },
                    icon = item.icon,
                    label = context.getString(item.labelRes),
                    contentDescription = context.getString(item.contentDescriptionRes)
                )
            }
            
            // 底部弹性空间
            Spacer(Modifier.weight(1f))
        }
    }
}
