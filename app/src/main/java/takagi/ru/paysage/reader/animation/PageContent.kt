package takagi.ru.paysage.reader.animation

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * 页面内容组件
 * 
 * 渲染单个页面的内容,包括位图和阴影效果。
 * 
 * @param bitmap 页面位图
 * @param shadowAlpha 阴影透明度(0f 到 1f)
 * @param modifier Modifier
 */
@Composable
fun PageContent(
    bitmap: Bitmap,
    shadowAlpha: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 绘制页面图片
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val imageBitmap = bitmap.asImageBitmap()
                
                // 计算缩放比例(保持宽高比)
                val scale = minOf(
                    size.width / imageBitmap.width,
                    size.height / imageBitmap.height
                )
                
                val scaledWidth = imageBitmap.width * scale
                val scaledHeight = imageBitmap.height * scale
                
                // 居中对齐
                val dx = (size.width - scaledWidth) / 2f
                val dy = (size.height - scaledHeight) / 2f
                
                // 绘制位图
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(dx, dy)
                canvas.nativeCanvas.scale(scale, scale)
                canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
                canvas.nativeCanvas.restore()
            }
        }
        
        // 绘制阴影效果
        if (shadowAlpha > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(0f, 0f),
                    size = size
                )
            }
        }
    }
}
