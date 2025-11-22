# 设计文档 - 两层导航抽屉

## 概述

本设计文档描述了 Paysage 应用的两层导航抽屉系统的实现方案。该系统采用 Material 3 Expressive 设计语言，提供直观、流畅且富有表现力的导航体验。

第一层（PrimaryLayer）是一个固定的图标导航栏，第二层（SecondaryLayer）是一个可展开的详细菜单抽屉。两层协同工作，为用户提供清晰的导航层次结构。

## 架构

### 组件层次结构

```
MainActivity
    └─ PaysageApp
        └─ TwoLayerNavigationScaffold
            ├─ PrimaryNavigationRail (第一层)
            │   ├─ NavigationRailItem (书库)
            │   ├─ NavigationRailItem (设置)
            │   └─ NavigationRailItem (关于)
            │
            ├─ ModalNavigationDrawer (第二层容器)
            │   └─ SecondaryDrawerContent
            │       ├─ LibraryDrawerContent (书库菜单)
            │       ├─ SettingsDrawerContent (设置菜单)
            │       └─ AboutDrawerContent (关于菜单)
            │
            └─ Content Area (主内容区)
                └─ NavHost (页面导航)
```

### 数据流

```
用户点击第一层图标
    ↓
更新 NavigationState.selectedPrimaryItem
    ↓
触发 SecondaryLayer 内容切换
    ↓
展开 SecondaryLayer (如果未展开)
    ↓
用户点击第二层选项
    ↓
导航到目标页面
    ↓
关闭 SecondaryLayer
```

## 组件和接口

### 1. NavigationState (导航状态管理)

```kotlin
/**
 * 导航状态数据类
 */
data class NavigationState(
    val selectedPrimaryItem: PrimaryNavItem = PrimaryNavItem.Library,
    val isSecondaryDrawerOpen: Boolean = false,
    val selectedSecondaryItem: String? = null
)

/**
 * 第一层导航项枚举
 */
enum class PrimaryNavItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String
) {
    Library(
        icon = Icons.Default.LibraryBooks,
        label = "书库",
        contentDescription = "打开书库菜单"
    ),
    Settings(
        icon = Icons.Default.Settings,
        label = "设置",
        contentDescription = "打开设置菜单"
    ),
    About(
        icon = Icons.Default.Info,
        label = "关于",
        contentDescription = "打开关于信息"
    )
}

/**
 * 第二层导航项数据类
 */
data class SecondaryNavItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val route: String? = null,
    val action: (() -> Unit)? = null
)
```

### 2. TwoLayerNavigationScaffold (主容器组件)

```kotlin
@Composable
fun TwoLayerNavigationScaffold(
    navigationState: NavigationState,
    onPrimaryItemClick: (PrimaryNavItem) -> Unit,
    onSecondaryItemClick: (SecondaryNavItem) -> Unit,
    onDrawerStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(
        initialValue = if (navigationState.isSecondaryDrawerOpen) 
            DrawerValue.Open else DrawerValue.Closed
    )
    
    // 响应式布局检测
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isExpanded = configuration.screenWidthDp >= 840
    
    // 根据屏幕尺寸决定布局模式
    if (isCompact) {
        CompactLayout(...)
    } else {
        MediumExpandedLayout(...)
    }
}
```

### 3. PrimaryNavigationRail (第一层导航栏)

```kotlin
@Composable
fun PrimaryNavigationRail(
    selectedItem: PrimaryNavItem,
    onItemClick: (PrimaryNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(Modifier.height(16.dp))
        
        PrimaryNavItem.values().forEach { item ->
            ExpressiveNavigationRailItem(
                selected = selectedItem == item,
                onClick = { onItemClick(item) },
                icon = item.icon,
                label = item.label,
                contentDescription = item.contentDescription
            )
        }
    }
}
```

### 4. SecondaryDrawerContent (第二层抽屉内容)

```kotlin
@Composable
fun SecondaryDrawerContent(
    selectedPrimaryItem: PrimaryNavItem,
    onItemClick: (SecondaryNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp)
    ) {
        // 标题
        Text(
            text = selectedPrimaryItem.label,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // 根据选中的第一层项显示不同内容
        when (selectedPrimaryItem) {
            PrimaryNavItem.Library -> LibraryDrawerContent(onItemClick)
            PrimaryNavItem.Settings -> SettingsDrawerContent(onItemClick)
            PrimaryNavItem.About -> AboutDrawerContent(onItemClick)
        }
    }
}
```

### 5. ExpressiveNavigationRailItem (增强的导航项)

```kotlin
@Composable
fun ExpressiveNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 动画效果
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = ExpressiveAnimations.bouncySpring,
        label = "nav_item_scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            Color.Transparent,
        animationSpec = tween(ExpressiveAnimations.DURATION_MEDIUM),
        label = "nav_item_bg"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(ExpressiveAnimations.DURATION_MEDIUM),
        label = "nav_item_icon"
    )
    
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = iconColor
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = iconColor,
            unselectedIconColor = iconColor,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = backgroundColor
        ),
        interactionSource = interactionSource
    )
}
```

## 数据模型

### 导航配置

```kotlin
/**
 * 书库菜单项配置
 */
object LibraryNavItems {
    val items = listOf(
        SecondaryNavItem(
            id = "all_books",
            icon = Icons.Default.Book,
            label = "全部书籍",
            route = Screen.Library.route
        ),
        SecondaryNavItem(
            id = "favorites",
            icon = Icons.Default.Favorite,
            label = "收藏",
            route = Screen.Library.route + "?filter=favorites"
        ),
        SecondaryNavItem(
            id = "recent",
            icon = Icons.Default.History,
            label = "最近阅读",
            route = Screen.Library.route + "?filter=recent"
        ),
        SecondaryNavItem(
            id = "categories",
            icon = Icons.Default.Category,
            label = "分类",
            route = Screen.Library.route + "?filter=categories"
        )
    )
}

/**
 * 设置菜单项配置
 */
object SettingsNavItems {
    val items = listOf(
        SecondaryNavItem(
            id = "theme",
            icon = Icons.Default.Palette,
            label = "主题设置",
            route = Screen.Settings.route + "?section=theme"
        ),
        SecondaryNavItem(
            id = "reading",
            icon = Icons.Default.MenuBook,
            label = "阅读设置",
            route = Screen.Settings.route + "?section=reading"
        ),
        SecondaryNavItem(
            id = "cache",
            icon = Icons.Default.Storage,
            label = "缓存管理",
            route = Screen.Settings.route + "?section=cache"
        ),
        SecondaryNavItem(
            id = "about_app",
            icon = Icons.Default.Info,
            label = "关于应用",
            route = Screen.Settings.route + "?section=about"
        )
    )
}

/**
 * 关于菜单项配置
 */
object AboutNavItems {
    val items = listOf(
        SecondaryNavItem(
            id = "version",
            icon = Icons.Default.AppSettingsAlt,
            label = "版本信息",
            action = { /* 显示版本对话框 */ }
        ),
        SecondaryNavItem(
            id = "license",
            icon = Icons.Default.Description,
            label = "开源许可",
            action = { /* 显示许可对话框 */ }
        ),
        SecondaryNavItem(
            id = "github",
            icon = Icons.Default.Code,
            label = "GitHub",
            action = { /* 打开 GitHub 链接 */ }
        )
    )
}
```

## 错误处理

### 导航错误

```kotlin
sealed class NavigationError {
    object InvalidRoute : NavigationError()
    object DrawerStateMismatch : NavigationError()
    data class NavigationFailed(val message: String) : NavigationError()
}

fun handleNavigationError(error: NavigationError) {
    when (error) {
        is NavigationError.InvalidRoute -> {
            // 记录错误并导航到默认页面
            Log.e("Navigation", "Invalid route detected")
            // 导航到书库页面
        }
        is NavigationError.DrawerStateMismatch -> {
            // 重置抽屉状态
            Log.w("Navigation", "Drawer state mismatch, resetting")
        }
        is NavigationError.NavigationFailed -> {
            // 显示错误提示
            Log.e("Navigation", "Navigation failed: ${error.message}")
        }
    }
}
```

### 状态恢复

```kotlin
/**
 * 保存导航状态
 */
fun saveNavigationState(state: NavigationState): Bundle {
    return Bundle().apply {
        putString("selectedPrimaryItem", state.selectedPrimaryItem.name)
        putBoolean("isSecondaryDrawerOpen", state.isSecondaryDrawerOpen)
        putString("selectedSecondaryItem", state.selectedSecondaryItem)
    }
}

/**
 * 恢复导航状态
 */
fun restoreNavigationState(bundle: Bundle?): NavigationState {
    return bundle?.let {
        NavigationState(
            selectedPrimaryItem = PrimaryNavItem.valueOf(
                it.getString("selectedPrimaryItem") ?: PrimaryNavItem.Library.name
            ),
            isSecondaryDrawerOpen = it.getBoolean("isSecondaryDrawerOpen", false),
            selectedSecondaryItem = it.getString("selectedSecondaryItem")
        )
    } ?: NavigationState()
}
```

## 测试策略

### 单元测试

```kotlin
class NavigationStateTest {
    @Test
    fun `初始状态应该选中书库`() {
        val state = NavigationState()
        assertEquals(PrimaryNavItem.Library, state.selectedPrimaryItem)
        assertFalse(state.isSecondaryDrawerOpen)
    }
    
    @Test
    fun `切换第一层项应该更新状态`() {
        val state = NavigationState()
        val newState = state.copy(selectedPrimaryItem = PrimaryNavItem.Settings)
        assertEquals(PrimaryNavItem.Settings, newState.selectedPrimaryItem)
    }
}
```

### UI 测试

```kotlin
class TwoLayerNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `点击第一层图标应该展开第二层`() {
        composeTestRule.setContent {
            TwoLayerNavigationScaffold(...)
        }
        
        // 点击设置图标
        composeTestRule.onNodeWithContentDescription("打开设置菜单").performClick()
        
        // 验证第二层已展开
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }
    
    @Test
    fun `点击第二层选项应该导航并关闭抽屉`() {
        composeTestRule.setContent {
            TwoLayerNavigationScaffold(...)
        }
        
        // 展开设置菜单
        composeTestRule.onNodeWithContentDescription("打开设置菜单").performClick()
        
        // 点击主题设置
        composeTestRule.onNodeWithText("主题设置").performClick()
        
        // 验证导航成功且抽屉关闭
        // ...
    }
}
```

### 集成测试

```kotlin
@Test
fun `完整导航流程测试`() {
    // 1. 启动应用，默认在书库页面
    // 2. 点击设置图标，展开设置菜单
    // 3. 点击主题设置，导航到设置页面
    // 4. 验证页面内容正确
    // 5. 点击返回，回到书库页面
}
```

## 性能优化

### 1. 懒加载第二层内容

```kotlin
@Composable
fun SecondaryDrawerContent(
    selectedPrimaryItem: PrimaryNavItem,
    onItemClick: (SecondaryNavItem) -> Unit
) {
    // 只渲染当前选中的菜单内容
    when (selectedPrimaryItem) {
        PrimaryNavItem.Library -> {
            key("library") {
                LibraryDrawerContent(onItemClick)
            }
        }
        PrimaryNavItem.Settings -> {
            key("settings") {
                SettingsDrawerContent(onItemClick)
            }
        }
        PrimaryNavItem.About -> {
            key("about") {
                AboutDrawerContent(onItemClick)
            }
        }
    }
}
```

### 2. 动画性能优化

```kotlin
// 使用 remember 缓存动画规格
val scaleAnimation = remember {
    spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

// 避免不必要的重组
@Composable
fun OptimizedNavigationItem(...) {
    val scale by remember(isPressed) {
        animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = scaleAnimation
        )
    }
}
```

### 3. 状态提升和记忆化

```kotlin
@Composable
fun TwoLayerNavigationScaffold(...) {
    // 使用 rememberSaveable 保存状态
    var navigationState by rememberSaveable(
        stateSaver = NavigationStateSaver
    ) {
        mutableStateOf(NavigationState())
    }
    
    // 记忆化回调函数
    val onPrimaryItemClick = remember {
        { item: PrimaryNavItem ->
            navigationState = navigationState.copy(
                selectedPrimaryItem = item,
                isSecondaryDrawerOpen = true
            )
        }
    }
}
```

## 可访问性

### 1. 语义标签

```kotlin
@Composable
fun AccessibleNavigationItem(...) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(...) },
        modifier = Modifier.semantics {
            contentDescription = contentDesc
            role = Role.Button
            stateDescription = if (selected) "已选中" else "未选中"
        }
    )
}
```

### 2. 触摸目标尺寸

```kotlin
// 确保最小触摸目标为 48dp
NavigationRailItem(
    modifier = Modifier
        .size(width = 80.dp, height = 56.dp)
        .minimumInteractiveComponentSize()
)
```

### 3. 高对比度支持

```kotlin
@Composable
fun HighContrastNavigationItem(...) {
    val isHighContrast = LocalAccessibilityManager.current?.isHighContrastEnabled ?: false
    
    val iconColor = if (isHighContrast) {
        if (selected) Color.White else Color.Black
    } else {
        if (selected) 
            MaterialTheme.colorScheme.onPrimaryContainer 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant
    }
}
```

## 响应式设计

### 断点定义

```kotlin
object NavigationBreakpoints {
    const val COMPACT_MAX = 600  // 手机
    const val MEDIUM_MAX = 840   // 小平板
    // >= 840: 大平板/桌面
}

enum class WindowSizeClass {
    Compact,   // < 600dp
    Medium,    // 600-839dp
    Expanded   // >= 840dp
}
```

### 布局适配

```kotlin
@Composable
fun AdaptiveNavigationLayout(
    windowSizeClass: WindowSizeClass,
    ...
) {
    when (windowSizeClass) {
        WindowSizeClass.Compact -> {
            // 手机：隐藏第一层，使用顶部菜单按钮
            CompactNavigationLayout(...)
        }
        WindowSizeClass.Medium -> {
            // 小平板：显示第一层，第二层模态
            MediumNavigationLayout(...)
        }
        WindowSizeClass.Expanded -> {
            // 大平板：两层都固定显示
            ExpandedNavigationLayout(...)
        }
    }
}
```

## 动画细节

### 第二层展开/收起动画

```kotlin
val drawerWidth by animateDpAsState(
    targetValue = if (isOpen) 280.dp else 0.dp,
    animationSpec = tween(
        durationMillis = ExpressiveAnimations.DURATION_MEDIUM,
        easing = ExpressiveAnimations.EmphasizedEasing
    )
)

val scrimAlpha by animateFloatAsState(
    targetValue = if (isOpen) 0.32f else 0f,
    animationSpec = tween(
        durationMillis = ExpressiveAnimations.DURATION_MEDIUM
    )
)
```

### 菜单项切换动画

```kotlin
AnimatedContent(
    targetState = selectedPrimaryItem,
    transitionSpec = {
        fadeIn(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_SHORT,
                easing = ExpressiveAnimations.EmphasizedDecelerateEasing
            )
        ) with fadeOut(
            animationSpec = tween(
                durationMillis = ExpressiveAnimations.DURATION_SHORT,
                easing = ExpressiveAnimations.EmphasizedAccelerateEasing
            )
        )
    }
) { primaryItem ->
    SecondaryDrawerContent(primaryItem, onItemClick)
}
```

## 主题集成

### 颜色使用

```kotlin
// 第一层背景
containerColor = MaterialTheme.colorScheme.surfaceContainer

// 第二层背景
containerColor = MaterialTheme.colorScheme.surface

// 选中项背景
indicatorColor = MaterialTheme.colorScheme.primaryContainer

// 选中项图标
iconColor = MaterialTheme.colorScheme.onPrimaryContainer

// 未选中项图标
iconColor = MaterialTheme.colorScheme.onSurfaceVariant

// 遮罩层
scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
```

### 形状使用

```kotlin
// 导航项指示器形状
indicatorShape = MaterialTheme.shapes.large  // 24dp 圆角

// 第二层抽屉形状
drawerShape = RoundedCornerShape(
    topEnd = 16.dp,
    bottomEnd = 16.dp
)
```

## 实现优先级

### 高优先级
1. 基础两层导航结构
2. 第一层图标导航栏
3. 第二层模态抽屉
4. 基本动画效果
5. 导航状态管理

### 中优先级
1. 响应式布局适配
2. 手势支持
3. 状态持久化
4. 完整的动画效果

### 低优先级
1. 高级动画效果
2. 性能优化
3. 可访问性增强
4. 单元测试和 UI 测试

---

**版本**: 1.0  
**创建日期**: 2025-10-27  
**设计系统**: Material 3 Expressive
