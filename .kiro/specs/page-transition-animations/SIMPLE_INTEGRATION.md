# 简单集成指南 - ReaderScreen

## 快速集成步骤

由于 ReaderScreen 的复杂性，我创建了一个辅助类来简化集成。

### 方法 1: 使用辅助类（推荐）

在 ReaderScreen.kt 中添加以下代码：

```kotlin
import takagi.ru.paysage.reader.transition.*

@Composable
fun ReaderScreen(
    // ... 现有参数
) {
    // ... 现有代码
    
    val book by viewModel.currentBook.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // 添加：创建过渡状态
    val transitionState = rememberReaderTransitionState(
        scope = coroutineScope,
        settings = settings,
        currentPage = uiState.currentPage,
        totalPages = book?.totalPages ?: 0,
        onPageChange = { page -> viewModel.goToPage(page) }
    )
    
    // 添加：预加载下一页
    var nextPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(uiState.currentPage, pageBitmap) {
        book?.let { currentBook ->
            val nextPage = uiState.currentPage + 1
            if (nextPage < currentBook.totalPages) {
                nextPageBitmap = viewModel.loadSecondPage(nextPage)
            } else {
                nextPageBitmap = null
            }
        }
    }
    
    // 在 Scaffold 的 content 中，替换原来的 PageView：
    
    // 原来的代码：
    // PageView(
    //     bitmap = pageBitmap!!,
    //     ...
    // )
    
    // 替换为：
    PageViewWithTransition(
        currentBitmap = pageBitmap,
        nextBitmap = nextPageBitmap,
        transitionState = transitionState,
        scale = scale,
        touchZoneConfig = uiState.touchZoneConfig,
        readingDirection = settings.readingDirection,
        onTouchZone = { zone ->
            viewModel.handleTouchZone(
                zone,
                settings.readingDirection,
                isDoublePageMode = false
            )
        },
        onScaleChange = { scale = it },
        modifier = Modifier.fillMaxSize()
    )
}
```

### 方法 2: 最小集成（仅基础功能）

如果你只想要基础的滑动动画，可以使用更简单的方式：

```kotlin
import takagi.ru.paysage.reader.transition.*

// 在 ReaderScreen 中添加：
val controller = remember {
    PageTransitionController(
        scope = rememberCoroutineScope(),
        config = TransitionConfig(
            mode = TransitionMode.Slide(),
            duration = 300
        ),
        onPageChange = { page -> viewModel.goToPage(page) }
    )
}

// 使用 PageTransitionContainer 替代 Image：
PageTransitionContainer(
    currentPageBitmap = pageBitmap,
    nextPageBitmap = null, // 暂时不预加载
    transitionState = controller.transitionState.value,
    animator = controller.currentAnimator.value,
    modifier = Modifier.fillMaxSize()
)
```

## 配置选项

用户可以在设置中配置过渡动画：

1. **过渡模式**
   - slide（滑动）- 默认
   - overlay（覆盖）
   - sidebyside（并排）
   - fade（淡入淡出）
   - curl（卷曲）

2. **动画速度**
   - fast（200ms）
   - normal（300ms）- 默认
   - slow（500ms）

3. **边缘灵敏度**
   - low（20%）
   - medium（40%）- 默认
   - high（100%）

4. **视觉效果**
   - 启用/禁用阴影效果

5. **触觉反馈**
   - 启用/禁用触觉反馈

## 注意事项

### 1. 性能考虑

- 预加载会占用额外内存，建议只预加载1-2页
- 在低端设备上，复杂动画（如 Curl）可能影响性能
- 可以根据设备性能自动降级动画

### 2. 手势冲突

- 缩放时自动禁用翻页手势（已在辅助类中处理）
- 触摸区域检测与拖动手势协调工作

### 3. 双页模式

- 双页模式暂时使用原有的 DualPageView
- 未来可以为双页模式添加专门的过渡动画

## 测试建议

1. 测试不同的过渡模式
2. 测试不同的动画速度
3. 测试边缘灵敏度设置
4. 测试缩放时的行为
5. 测试在不同设备上的性能

## 故障排除

### 问题：动画不流畅
**解决方案**：
- 检查是否预加载了下一页
- 尝试更简单的动画模式（Slide 或 Fade）
- 降低动画速度

### 问题：手势不响应
**解决方案**：
- 检查边缘灵敏度设置
- 确保没有在缩放状态下尝试翻页
- 检查触摸区域配置

### 问题：内存占用高
**解决方案**：
- 减少预加载页面数量
- 及时释放不可见页面
- 使用更简单的动画模式

## 完整示例

查看 `DEMO_EXAMPLE.kt` 获取完整的演示代码。

## 下一步

1. 集成基础功能
2. 测试不同场景
3. 根据需要调整配置
4. 添加设置界面（可选）

所有核心组件已经准备就绪，可以立即开始集成！
