# 页面过渡动画系统 - 集成指南

## 概述

本指南说明如何将页面过渡动画系统集成到现有的 ReaderScreen 中。

## 集成步骤

### 步骤 1: 扩展 ReaderViewModel

在 `ReaderViewModel.kt` 中添加过渡动画支持：

```kotlin
class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    // 现有代码...
    
    // 添加：下一页 bitmap（用于过渡动画）
    private val _nextPageBitmap = MutableStateFlow<Bitmap?>(null)
    val nextPageBitmap: StateFlow<Bitmap?> = _nextPageBitmap.asStateFlow()
    
    // 添加：过渡设置
    private val _transitionSettings = MutableStateFlow(PageTransitionSettings.Default)
    val transitionSettings: StateFlow<PageTransitionSettings> = 
        _transitionSettings.asStateFlow()
    
    /**
     * 预加载下一页用于过渡
     */
    fun preloadNextPageForTransition() {
        viewModelScope.launch {
            val book = currentBook.value ?: return@launch
            val nextPage = uiState.value.currentPage + 1
            
            if (nextPage < book.totalPages) {
                _nextPageBitmap.value = loadSecondPage(nextPage)
            } else {
                _nextPageBitmap.value = null
            }
        }
    }
    
    /**
     * 更新过渡模式
     */
    fun updateTransitionMode(mode: TransitionMode) {
        _transitionSettings.update { it.copy(mode = mode) }
    }
    
    /**
     * 从设置加载过渡配置
     */
    fun loadTransitionSettings(settings: AppSettings) {
        _transitionSettings.value = PageTransitionSettings(
            mode = TransitionMode.fromString(settings.pageTransitionMode),
            speed = when (settings.animationSpeed) {
                "fast" -> AnimationSpeed.FAST
                "slow" -> AnimationSpeed.SLOW
                else -> AnimationSpeed.NORMAL
            },
            edgeSensitivity = when (settings.edgeSensitivity) {
                "low" -> EdgeSensitivity.LOW
                "high" -> EdgeSensitivity.HIGH
                else -> EdgeSensitivity.MEDIUM
            },
            enableVisualEffects = settings.enableTransitionEffects,
            enableHapticFeedback = settings.enableTransitionHaptic
        )
    }
}
```

### 步骤 2: 修改 ReaderScreen

在 `ReaderScreen.kt` 中集成过渡动画：

```kotlin
@Composable
fun ReaderScreen(
    bookId: Long,
    initialPage: Int = -1,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val settings by settingsViewModel.settings.collectAsState()
    val currentPage by viewModel.currentPageBitmap.collectAsState()
    val nextPage by viewModel.nextPageBitmap.collectAsState()
    val transitionSettings by viewModel.transitionSettings.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 加载过渡设置
    LaunchedEffect(settings) {
        viewModel.loadTransitionSettings(settings)
    }
    
    // 创建过渡控制器
    val controller = remember(transitionSettings) {
        PageTransitionController(
            scope = scope,
            config = transitionSettings.toConfig(),
            onPageChange = { page -> 
                viewModel.goToPage(page)
                viewModel.preloadNextPageForTransition()
            }
        )
    }
    
    val transitionState by controller.transitionState
    val animator by controller.currentAnimator
    
    // 创建手势处理器
    val gestureHandler = remember(transitionSettings) {
        PageGestureHandler(
            config = transitionSettings.toConfig(),
            onTransitionStart = { direction ->
                val currentPageIndex = viewModel.uiState.value.currentPage
                val targetPage = when (direction) {
                    TransitionDirection.FORWARD -> currentPageIndex + 1
                    TransitionDirection.BACKWARD -> currentPageIndex - 1
                }
                controller.startTransition(currentPageIndex, targetPage, direction)
            },
            onTransitionUpdate = { progress ->
                controller.updateTransition(progress)
            },
            onTransitionEnd = { complete ->
                if (complete) {
                    controller.completeTransition()
                } else {
                    controller.cancelTransition()
                }
            }
        )
    }
    
    // 预加载下一页
    LaunchedEffect(currentPage) {
        viewModel.preloadNextPageForTransition()
    }
    
    Scaffold(
        // ... 现有的 topBar 和 bottomBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (uiState.isUiVisible) paddingValues else PaddingValues(0.dp))
        ) {
            // 使用 PageTransitionContainer 替代原来的 PageView
            PageTransitionContainer(
                currentPageBitmap = currentPage,
                nextPageBitmap = nextPage,
                transitionState = transitionState,
                animator = animator,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                gestureHandler.handleDragStart(offset, size)
                            },
                            onDrag = { change, _ ->
                                gestureHandler.handleDrag(change.position, size)
                            },
                            onDragEnd = {
                                gestureHandler.handleDragEnd(
                                    Velocity.Zero,
                                    transitionState.progress
                                )
                            },
                            onDragCancel = {
                                gestureHandler.cancelDrag()
                            }
                        )
                    }
            )
        }
    }
}
```

### 步骤 3: 添加设置界面

创建一个设置界面让用户配置过渡动画：

```kotlin
@Composable
fun PageTransitionSettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("翻页动画设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 过渡模式选择
            item {
                Text(
                    "翻页模式",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            item {
                val modes = listOf(
                    "slide" to "滑动",
                    "overlay" to "覆盖",
                    "sidebyside" to "并排",
                    "fade" to "淡入淡出",
                    "curl" to "卷曲"
                )
                
                modes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settingsViewModel.updatePageTransitionMode(value)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.pageTransitionMode == value,
                            onClick = {
                                settingsViewModel.updatePageTransitionMode(value)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
            
            // 动画速度
            item {
                Text(
                    "动画速度",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            item {
                val speeds = listOf(
                    "fast" to "快速",
                    "normal" to "正常",
                    "slow" to "慢速"
                )
                
                speeds.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settingsViewModel.updateAnimationSpeed(value)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.animationSpeed == value,
                            onClick = {
                                settingsViewModel.updateAnimationSpeed(value)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
            
            // 边缘灵敏度
            item {
                Text(
                    "边缘灵敏度",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            item {
                val sensitivities = listOf(
                    "low" to "低（20%）",
                    "medium" to "中（40%）",
                    "high" to "高（全屏）"
                )
                
                sensitivities.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settingsViewModel.updateEdgeSensitivity(value)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.edgeSensitivity == value,
                            onClick = {
                                settingsViewModel.updateEdgeSensitivity(value)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
            
            // 视觉效果开关
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用视觉效果（阴影）")
                    Switch(
                        checked = settings.enableTransitionEffects,
                        onCheckedChange = {
                            settingsViewModel.updateEnableTransitionEffects(it)
                        }
                    )
                }
            }
            
            // 触觉反馈开关
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用触觉反馈")
                    Switch(
                        checked = settings.enableTransitionHaptic,
                        onCheckedChange = {
                            settingsViewModel.updateEnableTransitionHaptic(it)
                        }
                    )
                }
            }
        }
    }
}
```

## 简化集成（最小改动）

如果你想要最小的改动，可以只添加基础的滑动动画：

### 最小集成示例

```kotlin
// 在 ReaderScreen 中，只需替换 PageView 为 PageTransitionContainer

// 原来的代码：
PageView(
    bitmap = pageBitmap!!,
    scale = scale,
    offset = offset,
    // ... 其他参数
)

// 替换为：
val controller = remember {
    PageTransitionController(
        scope = rememberCoroutineScope(),
        config = TransitionConfig(mode = TransitionMode.Slide()),
        onPageChange = { page -> viewModel.goToPage(page) }
    )
}

PageTransitionContainer(
    currentPageBitmap = pageBitmap,
    nextPageBitmap = null, // 暂时不预加载
    transitionState = controller.transitionState.value,
    animator = controller.currentAnimator.value,
    modifier = Modifier.fillMaxSize()
)
```

## 注意事项

1. **性能考虑**
   - 确保预加载不会占用过多内存
   - 在低端设备上考虑禁用复杂动画（如 Curl）

2. **手势冲突**
   - 需要协调缩放手势和翻页手势
   - 建议在缩放状态下禁用翻页手势

3. **双页模式**
   - 双页模式需要特殊处理
   - 建议在双页模式下使用 SideBySide 动画

4. **内存管理**
   - 过渡完成后立即释放旧页面
   - 监控内存使用，必要时降级动画

## 测试建议

1. 测试所有5种过渡模式
2. 测试不同的动画速度
3. 测试边缘灵敏度设置
4. 测试在不同设备上的性能
5. 测试与现有功能的兼容性（缩放、过滤器等）

## 故障排除

### 问题：动画卡顿
**解决方案**：
- 检查是否使用了 GPU 加速属性
- 降低动画复杂度
- 切换到更简单的模式（Fade）

### 问题：内存不足
**解决方案**：
- 减少预加载页面数量
- 及时释放不可见页面
- 降低图片质量

### 问题：手势冲突
**解决方案**：
- 在缩放时禁用翻页手势
- 调整边缘灵敏度
- 使用更明确的手势区分

## 总结

页面过渡动画系统已经准备就绪，可以通过以上步骤集成到 ReaderScreen 中。建议从最小集成开始，逐步添加更多功能。

核心组件都已实现并经过测试，可以安全使用。
