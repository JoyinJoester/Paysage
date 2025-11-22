package takagi.ru.paysage.ui.components.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.reader.touch.TouchZone
import takagi.ru.paysage.reader.touch.detectTouchZone

/**
 * 阅读器内容组件 - 无动画即时翻页
 */
@Composable
fun ReaderContent(
    currentPageBitmap: Bitmap?,
    nextPageBitmap: Bitmap?,
    previousPageBitmap: Bitmap?,
    config: ReaderConfig,
    onTap: (zone: TouchZone) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val zone = detectTouchZone(offset, IntSize(size.width, size.height))
                    onTap(zone)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragOffsetX = 0f
                    },
                    onDragEnd = {
                        val threshold = size.width * 0.3f
                        when {
                            dragOffsetX < -threshold -> {
                                onSwipeLeft()
                            }
                            dragOffsetX > threshold -> {
                                onSwipeRight()
                            }
                        }
                        dragOffsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        dragOffsetX += dragAmount.x
                        change.consume()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            currentPageBitmap?.let { bitmap ->
                drawIntoCanvas { canvas ->
                    val imageBitmap = bitmap.asImageBitmap()
                    val scale = minOf(
                        size.width / imageBitmap.width,
                        size.height / imageBitmap.height
                    )
                    val scaledWidth = imageBitmap.width * scale
                    val scaledHeight = imageBitmap.height * scale
                    val dx = (size.width - scaledWidth) / 2f
                    val dy = (size.height - scaledHeight) / 2f
                    
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(dx, dy)
                    canvas.nativeCanvas.scale(scale, scale)
                    canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
                    canvas.nativeCanvas.restore()
                }
            }
        }
    }
}
