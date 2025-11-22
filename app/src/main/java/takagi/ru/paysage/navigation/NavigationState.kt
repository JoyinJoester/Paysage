package takagi.ru.paysage.navigation

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 导航状态数据类
 * 管理两层导航抽屉的状态
 */
data class NavigationState(
    val selectedPrimaryItem: PrimaryNavItem = PrimaryNavItem.LocalLibrary,
    val isSecondaryDrawerOpen: Boolean = false,
    val selectedSecondaryItem: String? = null,
    val showSourceSelection: Boolean = false
)

/**
 * 第一层导航项枚举
 * 定义主要的导航菜单项
 */
enum class PrimaryNavItem(
    val icon: ImageVector,
    val labelRes: Int,
    val contentDescriptionRes: Int,
    val hasSecondaryMenu: Boolean = false
) {
    LocalLibrary(
        icon = Icons.Default.Folder,
        labelRes = takagi.ru.paysage.R.string.nav_local_library,
        contentDescriptionRes = takagi.ru.paysage.R.string.nav_open_local_library,
        hasSecondaryMenu = true
    ),
    OnlineLibrary(
        icon = Icons.Default.Cloud,
        labelRes = takagi.ru.paysage.R.string.nav_online_library,
        contentDescriptionRes = takagi.ru.paysage.R.string.nav_open_online_library,
        hasSecondaryMenu = true
    ),
    Settings(
        icon = Icons.Default.Settings,
        labelRes = takagi.ru.paysage.R.string.nav_settings,
        contentDescriptionRes = takagi.ru.paysage.R.string.nav_open_settings
    ),
    About(
        icon = Icons.Default.Info,
        labelRes = takagi.ru.paysage.R.string.nav_about,
        contentDescriptionRes = takagi.ru.paysage.R.string.nav_open_about
    );
    
    // 为了向后兼容，保留Library别名
    companion object {
        @Deprecated("Use LocalLibrary instead", ReplaceWith("LocalLibrary"))
        val Library = LocalLibrary
    }
}

/**
 * 第二层导航项数据类
 * 定义第二层菜单的选项
 */
data class SecondaryNavItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val route: String? = null,
    val action: (() -> Unit)? = null
)

/**
 * 本地书库菜单项配置
 */
object LocalLibraryNavItems {
    fun getItems(context: android.content.Context) = listOf(
        SecondaryNavItem(
            id = "local_manga",
            icon = Icons.Default.Book,
            label = context.getString(takagi.ru.paysage.R.string.source_local_manga),
            route = "library?category=manga"
        ),
        SecondaryNavItem(
            id = "local_reading",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = context.getString(takagi.ru.paysage.R.string.source_local_reading),
            route = "library?category=novel"
        )
    )
}

/**
 * 在线书库菜单项配置
 */
object OnlineLibraryNavItems {
    fun getItems(context: android.content.Context) = listOf(
        SecondaryNavItem(
            id = "manga_sources",
            icon = Icons.Default.CloudQueue,
            label = context.getString(takagi.ru.paysage.R.string.manga_sources),
            route = "online?category=manga"
        ),
        SecondaryNavItem(
            id = "novel_sources",
            icon = Icons.Default.CloudQueue,
            label = context.getString(takagi.ru.paysage.R.string.novel_sources),
            route = "online?category=novel"
        )
    )
}

/**
 * 书库菜单项配置（向后兼容）
 * @deprecated Use LocalLibraryNavItems instead
 */
@Deprecated("Use LocalLibraryNavItems instead", ReplaceWith("LocalLibraryNavItems"))
object LibraryNavItems {
    fun getItems(context: android.content.Context) = listOf(
        SecondaryNavItem(
            id = "all_books",
            icon = Icons.Default.Book,
            label = context.getString(takagi.ru.paysage.R.string.library_all_books),
            route = "library"
        ),
        SecondaryNavItem(
            id = "favorites",
            icon = Icons.Default.Favorite,
            label = context.getString(takagi.ru.paysage.R.string.library_favorites),
            route = "library?filter=favorites"
        ),
        SecondaryNavItem(
            id = "recent",
            icon = Icons.Default.History,
            label = context.getString(takagi.ru.paysage.R.string.library_recent),
            route = "library?filter=recent"
        ),
        SecondaryNavItem(
            id = "categories",
            icon = Icons.Default.Category,
            label = context.getString(takagi.ru.paysage.R.string.library_categories),
            route = "library?filter=categories"
        )
    )
}

/**
 * 设置菜单项配置
 * 设置直接在第二层抽屉中显示，不需要菜单项
 */
object SettingsNavItems {
    fun getItems(context: android.content.Context) = emptyList<SecondaryNavItem>()
}

/**
 * 关于菜单项配置
 */
object AboutNavItems {
    fun getItems(
        context: android.content.Context,
        onVersionClick: () -> Unit,
        onLicenseClick: () -> Unit,
        onGithubClick: () -> Unit
    ) = listOf(
        SecondaryNavItem(
            id = "version",
            icon = Icons.Default.AppSettingsAlt,
            label = context.getString(takagi.ru.paysage.R.string.about_version),
            action = onVersionClick
        ),
        SecondaryNavItem(
            id = "license",
            icon = Icons.Default.Description,
            label = context.getString(takagi.ru.paysage.R.string.about_license),
            action = onLicenseClick
        ),
        SecondaryNavItem(
            id = "github",
            icon = Icons.Default.Code,
            label = context.getString(takagi.ru.paysage.R.string.about_github),
            action = onGithubClick
        )
    )
}

/**
 * 保存导航状态到 Bundle
 */
fun saveNavigationState(state: NavigationState): Bundle {
    return Bundle().apply {
        putString("selectedPrimaryItem", state.selectedPrimaryItem.name)
        putBoolean("isSecondaryDrawerOpen", state.isSecondaryDrawerOpen)
        putString("selectedSecondaryItem", state.selectedSecondaryItem)
        putBoolean("showSourceSelection", state.showSourceSelection)
    }
}

/**
 * 从 Bundle 恢复导航状态
 */
fun restoreNavigationState(bundle: Bundle?): NavigationState {
    return bundle?.let {
        try {
            val itemName = it.getString("selectedPrimaryItem") ?: PrimaryNavItem.LocalLibrary.name
            // 处理旧的Library值，映射到LocalLibrary
            val mappedName = if (itemName == "Library") "LocalLibrary" else itemName
            
            NavigationState(
                selectedPrimaryItem = PrimaryNavItem.valueOf(mappedName),
                isSecondaryDrawerOpen = it.getBoolean("isSecondaryDrawerOpen", false),
                selectedSecondaryItem = it.getString("selectedSecondaryItem"),
                showSourceSelection = it.getBoolean("showSourceSelection", false)
            )
        } catch (e: IllegalArgumentException) {
            // 如果枚举值无效，返回默认状态
            NavigationState()
        }
    } ?: NavigationState()
}

/**
 * NavigationState 的 Saver，用于 rememberSaveable
 */
val NavigationStateSaver = Saver<NavigationState, Bundle>(
    save = { saveNavigationState(it) },
    restore = { restoreNavigationState(it) }
)
