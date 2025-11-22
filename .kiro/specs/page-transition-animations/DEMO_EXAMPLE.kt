package takagi.ru.paysage.demo

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import takagi.ru.paysage.reader.transition.*

/**
 * 页面过渡动画演示
 * 
 * 这是一个完整的示例，展示如何使用页面过渡动画系统
 */
@Composable
fun PageTransitionDemo(
    pages: List<Bitmap>,
    initialPage: Int = 0
) {
    var currentPageIndex by remember { mutableStateOf(initialPage) }
    val scope = rememberCoroutineScope()
    
    // 当前页和下一页的 bitmap
    val currentPage = remember(currentPageIndex) {
        pages.getOrNull(currentPageIndex)
    }
    
    val nextPage = remember(currentPageIndex) {
        pages.getOrNull(currentPageIndex + 1)
    }
    
    // 创建过渡控制器
    val controller = remember {
        PageTransitionController(
            scope = scope,
            config = TransitionConfig(
                mode = TransitionMode.Slide(),
                duration = 300,
                edgeSensitivity = EdgeSensitivity.MEDIUM,
                threshold = 0.3f
            ),
            onPageChange = { newPage ->
                if (newPage in pages.indices) {
                    currentPageIndex = newPage
                }
            }
        )
    }
    
    val transitionState by controller.transitionState
    val animator by controller.currentAnimator
    
    // 创建手势处理器
    val gestureHandler = remember(controller) {
        PageGestureHandler(
            config = controller.config,
            onTransitionStart = { direction ->
                val targetPage = when (direction) {
                    TransitionDirection.FORWARD -> currentPageIndex + 1
                    TransitionDirection.BACKWARD -> currentPageIndex - 1
                }
                
                if (targetPage in pages.indices) {
                    controller.startTransition(
                        from = currentPageIndex,
                        to = targetPage,
                        direction = direction
                    )
                }
            },
            onTransitionUpdate = { progress ->
                controller.updateTransition(progress)
            },
            onTransitionEnd = { complete ->
                if (complete) {
                    controller.completeTransition(animated = true)
                } else {
                    controller.cancelTransition(animated = true)
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("页面过渡演示 (${currentPageIndex + 1}/${pages.size})") }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 模式切换按钮
                    Button(onClick = { 
                        controller.updateMode(TransitionMode.Slide()) 
                    }) {
                        Text("滑动")
                    }
                    
                    Button(onClick = { 
                        controller.updateMode(TransitionMode.Overlay) 
                    }) {
                        Text("覆盖")
                    }
                    
                    Button(onClick = { 
                        controller.updateMode(TransitionMode.Fade) 
                    }) {
                        Text("淡入淡出")
                    }
                    
                    Button(onClick = { 
                        controller.updateMode(TransitionMode.Curl) 
                    }) {
                        Text("卷曲")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 页面过渡容器
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
                                    androidx.compose.ui.unit.Velocity.Zero,
                                    transitionState.progress
                                )
                            },
                            onDragCancel = {
                                gestureHandler.cancelDrag()
                            }
                        )
                    }
            )
            
            // 导航按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 上一页按钮
                Button(
                    onClick = {
                        if (currentPageIndex > 0) {
                            scope.launch {
                                controller.startTransition(
                                    from = currentPageIndex,
                                    to = currentPageIndex - 1,
                                    direction = TransitionDirection.BACKWARD
                                )
                                controller.completeTransition(animated = true)
                            }
                        }
                    },
                    enabled = currentPageIndex > 0 && !transitionState.isActive
                ) {
                    Text("上一页")
                }
                
                // 下一页按钮
                Button(
                    onClick = {
                        if (currentPageIndex < pages.size - 1) {
                            scope.launch {
                                controller.startTransition(
                                    from = currentPageIndex,
                                    to = currentPageIndex + 1,
                                    direction = TransitionDirection.FORWARD
                                )
                                controller.completeTransition(animated = true)
                            }
                        }
                    },
                    enabled = currentPageIndex < pages.size - 1 && !transitionState.isActive
                ) {
                    Text("下一页")
                }
            }
        }
    }
}

/**
 * 简化版演示 - 最小集成示例
 */
@Composable
fun MinimalTransitionDemo(
    currentPage: Bitmap?,
    nextPage: Bitmap?,
    onPageChange: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val controller = remember {
        PageTransitionController(
            scope = scope,
            config = TransitionConfig(mode = TransitionMode.Slide()),
            onPageChange = onPageChange
        )
    }
    
    val transitionState by controller.transitionState
    val animator by controller.currentAnimator
    
    PageTransitionContainer(
        currentPageBitmap = currentPage,
        nextPageBitmap = nextPage,
        transitionState = transitionState,
        animator = animator,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 高级演示 - 带性能监控
 */
@Composable
fun AdvancedTransitionDemo(
    pages: List<Bitmap>,
    settings: PageTransitionSettings
) {
    var currentPageIndex by remember { mutableStateOf(0) }
    var frameCount by remember { mutableStateOf(0) }
    var lastFrameTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var fps by remember { mutableStateOf(0f) }
    
    val scope = rememberCoroutineScope()
    
    val controller = remember(settings) {
        PageTransitionController(
            scope = scope,
            config = settings.toConfig(),
            onPageChange = { newPage ->
                currentPageIndex = newPage
            }
        )
    }
    
    // 性能监控
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val currentTime = System.currentTimeMillis()
            val elapsed = (currentTime - lastFrameTime) / 1000f
            fps = frameCount / elapsed
            frameCount = 0
            lastFrameTime = currentTime
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        PageTransitionContainer(
            currentPageBitmap = pages.getOrNull(currentPageIndex),
            nextPageBitmap = pages.getOrNull(currentPageIndex + 1),
            transitionState = controller.transitionState.value,
            animator = controller.currentAnimator.value,
            modifier = Modifier.fillMaxSize()
        )
        
        // FPS 显示
        Text(
            text = "FPS: ${fps.toInt()}",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
    
    // 每帧更新计数
    LaunchedEffect(controller.transitionState.value.progress) {
        frameCount++
    }
}
