# Design Document - 沉浸式状态栏

## Overview

本设计实现 Android 应用的沉浸式状态栏和导航栏效果，让内容延伸到系统栏区域，提供更现代的视觉体验。设计将与现有的主题系统集成，自动根据主题调整系统栏图标颜色，并正确处理 Window Insets 以避免内容被遮挡。

## Architecture

### 核心组件

1. **SystemBarController** - 系统栏控制器
   - 管理状态栏和导航栏的透明度
   - 控制系统栏图标颜色（深色/浅色）
   - 处理不同 Android 版本的兼容性

2. **Theme.kt 增强** - 主题系统集成
   - 在 `PaysageTheme` 中集成系统栏配置
   - 根据主题自动调整系统栏样式
   - 移除硬编码的状态栏颜色设置

3. **WindowInsetsHandler** - Insets 处理组件
   - 提供 Modifier 扩展函数处理系统栏 insets
   - 确保内容不被系统栏遮挡
   - 支持选择性应用 insets（顶部、底部、两者）

4. **ReaderImmersiveMode** - 阅读器沉浸模式
   - 在阅读器中提供完全沉浸体验
   - 支持隐藏/显示系统栏
   - 点击屏幕临时显示系统栏

## Components and Interfaces

### 1. SystemBarController

```kotlin
/**
 * 系统栏控制器
 * 管理状态栏和导航栏的外观
 */
object SystemBarController {
    /**
     * 配置透明系统栏
     * @param window 窗口对象
     * @param view 视图对象
     * @param darkIcons 是否使用深色图标（true = 深色图标用于浅色背景）
     */
    fun setupTransparentSystemBars(
        window: Window,
        view: View,
        darkIcons: Boolean
    )
    
    /**
     * 配置沉浸模式（隐藏系统栏）
     * @param window 窗口对象
     * @param view 视图对象
     * @param enabled 是否启用沉浸模式
     */
    fun setupImmersiveMode(
        window: Window,
        view: View,
        enabled: Boolean
    )
    
    /**
     * 检查设备是否支持透明导航栏
     */
    fun supportsTransparentNavigationBar(): Boolean
}
```

### 2. WindowInsetsHandler (Modifier Extensions)

```kotlin
/**
 * 应用状态栏 insets 内边距
 */
fun Modifier.statusBarsPadding(): Modifier

/**
 * 应用导航栏 insets 内边距
 */
fun Modifier.navigationBarsPadding(): Modifier

/**
 * 应用系统栏 insets 内边距（状态栏 + 导航栏）
 */
fun Modifier.systemBarsPadding(): Modifier

/**
 * 应用 IME（输入法）insets 内边距
 */
fun Modifier.imePadding(): Modifier

/**
 * 获取状态栏高度
 */
@Composable
fun rememberStatusBarHeight(): Dp

/**
 * 获取导航栏高度
 */
@Composable
fun rememberNavigationBarHeight(): Dp
```

### 3. Theme Integration

修改 `PaysageTheme` 以集成系统栏配置：

```kotlin
@Composable
fun PaysageTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    transparentSystemBars: Boolean = true, // 新增参数
    content: @Composable () -> Unit
) {
    // ... 现有逻辑 ...
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            if (transparentSystemBars) {
                // 配置透明系统栏
                SystemBarController.setupTransparentSystemBars(
                    window = window,
                    view = view,
                    darkIcons = !darkTheme
                )
            } else {
                // 使用传统的有色状态栏
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    
    // ... MaterialTheme ...
}
```

### 4. ReaderImmersiveMode

```kotlin
/**
 * 阅读器沉浸模式状态
 */
data class ImmersiveModeState(
    val isEnabled: Boolean = false,
    val isSystemBarsVisible: Boolean = true
)

/**
 * 管理阅读器沉浸模式
 */
@Composable
fun rememberImmersiveModeState(): ImmersiveModeState

/**
 * 应用沉浸模式效果
 */
@Composable
fun ImmersiveModeEffect(
    enabled: Boolean,
    onSystemBarsVisibilityChange: (Boolean) -> Unit
)
```

## Data Models

### SystemBarConfiguration

```kotlin
/**
 * 系统栏配置
 */
data class SystemBarConfiguration(
    val statusBarColor: Color = Color.Transparent,
    val navigationBarColor: Color = Color.Transparent,
    val darkIcons: Boolean = false,
    val hideSystemBars: Boolean = false
)
```

### ImmersiveModeSettings

```kotlin
/**
 * 沉浸模式设置（存储在用户设置中）
 */
data class ImmersiveModeSettings(
    val enableInReader: Boolean = false,
    val autoHideSystemBars: Boolean = false,
    val showOnTap: Boolean = true
)
```

## Implementation Details

### 1. SystemBarController 实现

使用 `WindowCompat` 和 `WindowInsetsControllerCompat` API：

```kotlin
object SystemBarController {
    fun setupTransparentSystemBars(
        window: Window,
        view: View,
        darkIcons: Boolean
    ) {
        // 设置透明颜色
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        
        // 启用边到边绘制
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 配置图标颜色
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = darkIcons
        insetsController.isAppearanceLightNavigationBars = darkIcons
    }
    
    fun setupImmersiveMode(
        window: Window,
        view: View,
        enabled: Boolean
    ) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        
        if (enabled) {
            // 隐藏系统栏
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // 显示系统栏
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    
    fun supportsTransparentNavigationBar(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
```

### 2. WindowInsets Modifier 实现

使用 Compose 的 `WindowInsets` API：

```kotlin
fun Modifier.statusBarsPadding(): Modifier = composed {
    this.windowInsetsPadding(WindowInsets.statusBars)
}

fun Modifier.navigationBarsPadding(): Modifier = composed {
    this.windowInsetsPadding(WindowInsets.navigationBars)
}

fun Modifier.systemBarsPadding(): Modifier = composed {
    this.windowInsetsPadding(WindowInsets.systemBars)
}

@Composable
fun rememberStatusBarHeight(): Dp {
    val insets = WindowInsets.statusBars.asPaddingValues()
    return insets.calculateTopPadding()
}

@Composable
fun rememberNavigationBarHeight(): Dp {
    val insets = WindowInsets.navigationBars.asPaddingValues()
    return insets.calculateBottomPadding()
}
```

### 3. 在现有组件中应用

#### MainActivity

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge() // 已存在
    setContent {
        val settings by settingsViewModel.settings.collectAsState()
        
        PaysageTheme(
            themeMode = settings.themeMode,
            transparentSystemBars = true // 启用透明系统栏
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                PaysageApp(navigationViewModel)
            }
        }
    }
}
```

#### TwoLayerNavigationScaffold

在导航 Scaffold 中应用状态栏 padding：

```kotlin
// Compact 模式
Surface(
    modifier = Modifier
        .fillMaxHeight()
        .width(56.dp)
        .statusBarsPadding(), // 添加状态栏 padding
    // ...
) {
    // 第一层导航内容
}

// Medium/Expanded 模式
PermanentNavigationDrawer(
    drawerContent = {
        PermanentDrawerSheet(
            modifier = Modifier
                .width(80.dp)
                .statusBarsPadding(), // 添加状态栏 padding
            // ...
        ) {
            // 第一层导航内容
        }
    }
)
```

#### LibraryScreen

在内容区域应用系统栏 padding：

```kotlin
Scaffold(
    topBar = {
        // TopAppBar 不需要额外 padding，因为它会自动处理
    },
    modifier = Modifier.fillMaxSize()
) { paddingValues ->
    // 内容使用 scaffold 提供的 padding
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        // ...
    )
}
```

#### ReaderScreen

在阅读器中实现沉浸模式：

```kotlin
@Composable
fun ReaderScreen(
    // ... 参数 ...
) {
    val immersiveModeEnabled by remember { mutableStateOf(false) }
    
    // 应用沉浸模式效果
    ImmersiveModeEffect(
        enabled = immersiveModeEnabled,
        onSystemBarsVisibilityChange = { visible ->
            // 处理系统栏可见性变化
        }
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 漫画内容（全屏，不应用 padding）
        ComicContent(
            modifier = Modifier.fillMaxSize()
        )
        
        // 控制栏（应用 padding 避免被遮挡）
        ReaderControls(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }
}
```

## Error Handling

### 1. 版本兼容性

```kotlin
// 在不支持透明导航栏的设备上优雅降级
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    window.navigationBarColor = Color.Transparent.toArgb()
} else {
    // Android 7.x 及以下，使用半透明黑色
    window.navigationBarColor = Color.Black.copy(alpha = 0.5f).toArgb()
}
```

### 2. 刘海屏/挖孔屏处理

```kotlin
// Android 9+ 支持刘海屏
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
}
```

### 3. 折叠屏处理

使用 `WindowMetrics` API 检测屏幕配置变化：

```kotlin
@Composable
fun rememberWindowMetrics(): WindowMetrics {
    val context = LocalContext.current
    val windowManager = context.getSystemService(WindowManager::class.java)
    return windowManager.currentWindowMetrics
}
```

## Testing Strategy

### 1. 单元测试

- 测试 `SystemBarController` 的配置逻辑
- 测试不同 Android 版本的兼容性处理
- 测试 Insets 计算的正确性

### 2. UI 测试

- 测试不同主题下系统栏图标颜色是否正确
- 测试内容是否正确应用 padding 避免遮挡
- 测试沉浸模式的启用/禁用

### 3. 设备测试

- 在不同 Android 版本设备上测试（5.0 - 14）
- 在刘海屏/挖孔屏设备上测试
- 在折叠屏设备上测试（如果可能）
- 测试横屏/竖屏切换

### 4. 视觉回归测试

- 截图对比不同主题下的系统栏外观
- 验证过渡动画的流畅性
- 检查是否有视觉闪烁或跳动

## Performance Considerations

1. **避免过度重组** - 使用 `SideEffect` 而不是 `LaunchedEffect` 来配置系统栏
2. **缓存 Insets 值** - 使用 `remember` 缓存计算的 insets 值
3. **最小化窗口配置更改** - 只在必要时更新系统栏配置

## Accessibility Considerations

1. **确保足够的对比度** - 系统栏图标与背景之间要有足够对比度
2. **避免重要内容被遮挡** - 所有可交互元素都要应用适当的 padding
3. **支持大字体** - 确保在大字体模式下内容不会被系统栏遮挡

## Migration Path

1. **阶段 1** - 实现 `SystemBarController` 和 Insets 处理
2. **阶段 2** - 更新 `PaysageTheme` 集成系统栏配置
3. **阶段 3** - 在主要界面应用 insets padding
4. **阶段 4** - 实现阅读器沉浸模式
5. **阶段 5** - 测试和优化

## Dependencies

- `androidx.core:core-ktx` - WindowCompat API
- `androidx.compose.foundation:foundation` - WindowInsets API
- 现有的主题系统和设置系统

## References

- [Android Edge-to-Edge Guide](https://developer.android.com/develop/ui/views/layout/edge-to-edge)
- [WindowInsets in Compose](https://developer.android.com/jetpack/compose/layouts/insets)
- [Material 3 Guidelines](https://m3.material.io/)
