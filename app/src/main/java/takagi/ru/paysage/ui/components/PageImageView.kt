package takagi.ru.paysage.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import kotlin.math.abs

/**
 * 页面图片视图组件
 * 
 * 功能：
 * - 显示图片
 * - 双击缩放（1.0x ↔ 2.0x）
 * - 双指捏合缩放（0.5x - 3.0x）
 * - 拖动平移（仅在缩放时）
 * - 左右滑动翻页
 * - 点击切换工具栏
 */
@Composable
fun PageImageView(
    bitmap: Bitmap?,
    onTap: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        // 更新缩放
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        
        // 只在缩放时允许平移
        if (scale > 1f) {
            offset += offsetChange
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // 点击切换工具栏
                        onTap()
                    },
                    onDoubleTap = { tapOffset ->
                        // 双击缩放
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2f
                            // 以点击位置为中心缩放
                            offset = Offset(
                                x = (size.width / 2 - tapOffset.x) * scale,
                                y = (size.height / 2 - tapOffset.y) * scale
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // 滑动翻页手势
                detectDragGestures(
                    onDragEnd = {
                        // 重置偏移
                        if (scale <= 1f) {
                            offset = Offset.Zero
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    
                    if (scale <= 1f) {
                        // 未缩放时，检测翻页手势
                        val swipeThreshold = 50f
                        if (abs(dragAmount.x) > abs(dragAmount.y)) {
                            if (dragAmount.x < -swipeThreshold) {
                                onSwipeLeft()
                            } else if (dragAmount.x > swipeThreshold) {
                                onSwipeRight()
                            }
                        }
                    } else {
                        // 缩放时，允许拖动
                        offset += dragAmount
                    }
                }
            }
            .transformable(state = state),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Page",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}
