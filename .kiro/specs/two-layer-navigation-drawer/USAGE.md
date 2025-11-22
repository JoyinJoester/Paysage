# 两层导航抽屉 - 使用指南

## 📚 目录

1. [基础使用](#基础使用)
2. [响应式布局](#响应式布局)
3. [增强组件](#增强组件)
4. [自定义配置](#自定义配置)
5. [最佳实践](#最佳实践)

## 基础使用

### 集成导航系统

导航系统已经完全集成到 `MainActivity` 中。基本使用方式：

```kotlin
@Composable
fun PaysageApp(navigationViewModel: NavigationViewModel) {
    val navController = rememberNavController()
    val navigationState by navigationViewModel.navigationState.collectAsState()
    
    TwoLayerNavigationScaffold(
        navigationState = navigationState,
        onPrimaryItemClick = { item ->
            navigationViewModel.selectPrimaryItem(item)
        },
        onSecondaryItemClick = { item ->
            navigationViewModel.selectSecondaryItem(item)
            // 导航逻辑
        },
        onDrawerStateChange = { isOpen ->
            navigationViewModel.toggleSecondaryDrawer(isOpen)
        }
    ) { windowSizeClass, onOpenDrawer ->
        // 您的内容
        NavHost(navController, startDestination) {
            // 路由定义
        }
    }
}
```

### 添加新的导航项

#### 第一层导航（主菜单）

在 `NavigationState.kt` 中的 `PrimaryNavItem` 枚举添加新项：

```kotlin
enum class PrimaryNavItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String
) {
    Library(...),
    Settings(...),
    About(...),
    // 添加新项
    NewItem(
        icon = Icons.Default.YourIcon,
        label = "新功能",
        contentDescription = "打开新功能菜单"
    )
}
```

#### 第二层导航（子菜单）

创建新的菜单项配置对象：

```kotlin
object NewItemNavItems {
    val items = listOf(
        SecondaryNavItem(
            id = "item1",
            icon = Icons.Default.Icon1,
            label = "选项 1",
            route = "route1"
        ),
        SecondaryNavItem(
            id = "item2",
            icon = Icons.Default.Icon2,
            label = "选项 2",
            route = "route2"
        )
    )
}
```

然后在 `SecondaryDrawerContent.kt` 中添加对应的内容函数：

```kotlin
@Composable
fun NewItemDrawerContent(
    onItemClick: (SecondaryNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NewItemNavItems.items.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                icon = { Icon(item.icon, contentDescription = null) },
                selected = false,
                onClick = { onItemClick(item) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
```

## 响应式布局

系统自动适配三种屏幕尺寸：

### Compact（手机，< 600dp）
- 第一层和第二层合并在一个抽屉中
- 从左边缘滑动或点击菜单按钮打开
- 全屏抽屉显示

### Medium（小平板，600-839dp）
- 第一层固定显示（80dp 宽）
- 第二层模态显示（280dp 宽）
- 标准的两层导航体验

### Expanded（大平板/桌面，≥ 840dp）
- 第一层和第二层都固定显示
- 第二层宽度为 320dp
- 无遮罩层，永久可见

### 检测当前窗口尺寸

```kotlin
val windowSizeClass = rememberWindowSizeClass()

when (windowSizeClass) {
    WindowSizeClass.Compact -> {
        // 手机布局
    }
    WindowSizeClass.Medium -> {
        // 平板布局
    }
    WindowSizeClass.Expanded -> {
        // 大屏布局
    }
}
```

## 增强组件

### 1. 导航菜单按钮

在 Compact 模式下使用，打开导航抽屉：

```kotlin
NavigationMenuButton(
    onClick = { onOpenDrawer() }
)
```

### 2. 抽屉头部

显示应用名称和图标的漂亮头部：

```kotlin
DrawerHeader()
```

### 3. 导航徽章

显示未读数量或通知：

```kotlin
NavigationBadge(count = 5)
```

### 4. 抽屉搜索栏

在抽屉中搜索菜单项：

```kotlin
var searchQuery by remember { mutableStateOf("") }
var searchExpanded by remember { mutableStateOf(false) }

DrawerSearchBar(
    query = searchQuery,
    onQueryChange = { searchQuery = it },
    expanded = searchExpanded,
    onExpandedChange = { searchExpanded = it }
)

// 过滤菜单项
val filteredItems = searchMenuItems(searchQuery, allItems)
```

### 5. 主题切换器

快速切换亮色/暗色主题：

```kotlin
// 完整版
ThemeSwitcher(
    isDarkTheme = isDark,
    onThemeChange = { isDark = it }
)

// 紧凑版（开关）
CompactThemeSwitcher(
    isDarkTheme = isDark,
    onThemeChange = { isDark = it }
)
```

### 6. 导航历史

跟踪用户的导航路径：

```kotlin
val navigationHistory = rememberNavigationHistory()

// 添加记录
navigationHistory.push(route)

// 返回上一页
val previousRoute = navigationHistory.pop()

// 检查是否可以返回
if (navigationHistory.canGoBack()) {
    // 显示返回按钮
}
```

### 7. 涟漪效果

为导航项添加点击涟漪动画：

```kotlin
val ripples = rememberRippleState()

Box {
    // 您的内容
    
    RippleEffect(
        ripples = ripples.value,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    )
}

// 添加涟漪
ripples.addRipple(Offset(x, y))
```

## 自定义配置

### 修改导航栏宽度

在 `PrimaryNavigationRail.kt` 中：

```kotlin
NavigationRail(
    modifier = modifier
        .fillMaxHeight()
        .width(100.dp), // 修改宽度
    // ...
)
```

### 修改抽屉宽度

在 `TwoLayerNavigationScaffold.kt` 中：

```kotlin
// Medium 布局
ModalDrawerSheet(
    modifier = Modifier.width(320.dp), // 修改宽度
    // ...
)

// Expanded 布局
Surface(
    modifier = Modifier.width(360.dp), // 修改宽度
    // ...
)
```

### 自定义动画时长

在 `ExpressiveAnimations` 中修改：

```kotlin
object ExpressiveAnimations {
    const val DURATION_SHORT = 150  // 修改为更快
    const val DURATION_MEDIUM = 250
    const val DURATION_LONG = 400
}
```

### 自定义颜色

导航系统使用 Material 3 主题颜色：

- `surfaceContainer` - 第一层背景
- `surface` - 第二层背景
- `primaryContainer` - 选中项背景
- `onPrimaryContainer` - 选中项图标/文字
- `onSurfaceVariant` - 未选中项图标/文字

在 `Color.kt` 中修改这些颜色。

## 最佳实践

### 1. 导航项命名

- 使用清晰、简洁的标签
- 提供有意义的 contentDescription
- ID 使用 snake_case 格式

### 2. 菜单结构

- 第一层：3-5 个主要功能
- 第二层：每个主功能下 3-7 个子选项
- 避免过深的层级

### 3. 响应式设计

- 在 Compact 模式下简化菜单
- 在 Expanded 模式下充分利用空间
- 测试所有屏幕尺寸

### 4. 性能优化

- 使用 `remember` 缓存静态数据
- 避免在导航回调中执行耗时操作
- 使用 `key()` 为列表项提供稳定的键

### 5. 可访问性

- 为所有图标提供 contentDescription
- 确保触摸目标至少 48dp
- 支持 TalkBack 屏幕阅读器
- 测试高对比度模式

### 6. 状态管理

- 使用 ViewModel 管理导航状态
- 利用 SavedStateHandle 持久化状态
- 在配置变更时保持状态

### 7. 测试

- 编写单元测试验证状态逻辑
- 编写 UI 测试验证交互
- 测试不同屏幕尺寸
- 测试可访问性功能

## 示例代码

### 完整的导航设置示例

```kotlin
@Composable
fun MyApp() {
    val navigationViewModel: NavigationViewModel = viewModel()
    val navController = rememberNavController()
    val navigationState by navigationViewModel.navigationState.collectAsState()
    
    TwoLayerNavigationScaffold(
        navigationState = navigationState,
        onPrimaryItemClick = { item ->
            navigationViewModel.selectPrimaryItem(item)
        },
        onSecondaryItemClick = { item ->
            navigationViewModel.selectSecondaryItem(item)
            item.route?.let { route ->
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
            item.action?.invoke()
        },
        onDrawerStateChange = { isOpen ->
            navigationViewModel.toggleSecondaryDrawer(isOpen)
        },
        onVersionClick = {
            // 显示版本对话框
        },
        onLicenseClick = {
            // 显示许可证
        },
        onGithubClick = {
            // 打开 GitHub
        }
    ) { windowSizeClass, onOpenDrawer ->
        Scaffold(
            topBar = {
                if (windowSizeClass == WindowSizeClass.Compact) {
                    TopAppBar(
                        title = { Text("我的应用") },
                        navigationIcon = {
                            NavigationMenuButton(onClick = onOpenDrawer)
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") { HomeScreen() }
                composable("settings") { SettingsScreen() }
                // 更多路由...
            }
        }
    }
}
```

## 故障排除

### 抽屉不打开

检查：
1. `navigationState.isSecondaryDrawerOpen` 是否正确更新
2. `onDrawerStateChange` 回调是否正确调用
3. 是否有其他组件拦截了触摸事件

### 导航不工作

检查：
1. 路由是否正确定义
2. `NavHost` 是否包含对应的 `composable`
3. 导航回调是否正确实现

### 动画卡顿

优化：
1. 减少重组次数
2. 使用 `remember` 缓存数据
3. 避免在动画中执行耗时操作
4. 检查是否有内存泄漏

### 状态丢失

确保：
1. 使用 `SavedStateHandle` 保存状态
2. 使用 `rememberSaveable` 保存 UI 状态
3. 正确处理配置变更

## 更多资源

- [Material 3 设计指南](https://m3.material.io/)
- [Jetpack Compose 文档](https://developer.android.com/jetpack/compose)
- [导航组件文档](https://developer.android.com/guide/navigation)
- [可访问性指南](https://developer.android.com/guide/topics/ui/accessibility)

---

**版本**: 1.0  
**最后更新**: 2025-10-27
