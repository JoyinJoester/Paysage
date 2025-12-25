package takagi.ru.paysage.ui.components.reader

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.ui.book.read.entities.TextPage
import takagi.ru.paysage.ui.book.read.entities.column.ImageColumn
import takagi.ru.paysage.ui.book.read.entities.column.TextColumn

/**
 * Canvas 渲染的文本页面（参考 Legado）
 * 使用 Canvas 绘制 TextLine，支持图片加载
 */
@Composable
fun TextPageCanvas(
    page: TextPage,
    config: ReaderConfig,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(config.bgColor)
    
    // Paint 对象
    val textPaint = remember(config.textSize, config.textColor) {
        Paint().apply {
            textSize = config.textSize * android.content.res.Resources.getSystem().displayMetrics.scaledDensity
            color = config.textColor
            isAntiAlias = true
        }
    }
    
    val titlePaint = remember(config.textSize, config.textColor) {
        Paint().apply {
            textSize = config.textSize * 1.2f * android.content.res.Resources.getSystem().displayMetrics.scaledDensity
            color = config.textColor
            isFakeBoldText = true
            isAntiAlias = true
        }
    }
    
    // 预加载所有图片
    val images = remember(page) {
        page.lines
            .filter { it.isImage }
            .mapNotNull { line ->
                val imageColumn = line.columns.firstOrNull() as? ImageColumn
                imageColumn?.src
            }
    }
    
    // 加载图片
    val loadedImages = images.associateWith { src ->
        rememberImageBitmap(src)
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // 绘制每一行
        for (line in page.lines) {
            when {
                line.isImage -> {
                    // 绘制图片
                    val imageColumn = line.columns.firstOrNull() as? ImageColumn
                    if (imageColumn != null) {
                        val bitmap = loadedImages[imageColumn.src]
                        
                        if (bitmap != null) {
                            // 计算保持宽高比的尺寸
                            val maxWidth = imageColumn.end - imageColumn.start
                            val maxHeight = line.lineBottom - line.lineTop
                            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            
                            var drawWidth = maxWidth
                            var drawHeight = maxHeight
                            
                            // 根据宽高比调整
                            if (drawWidth / drawHeight > aspectRatio) {
                                // 太宽，以高度为准
                                drawWidth = drawHeight * aspectRatio
                            } else {
                                // 太高，以宽度为准
                                drawHeight = drawWidth / aspectRatio
                            }
                            
                            // 居中显示
                            val offsetX = imageColumn.start + (maxWidth - drawWidth) / 2f
                            val offsetY = line.lineTop + (maxHeight - drawHeight) / 2f
                            
                            // 绘制实际图片
                            drawBase64Image(
                                src = imageColumn.src,
                                topLeft = Offset(offsetX, offsetY),
                                size = Size(drawWidth, drawHeight),
                                bitmap = bitmap
                            )
                        } else {
                            // 加载中：显示占位符
                            drawRect(
                                color = Color.LightGray,
                                topLeft = Offset(imageColumn.start, line.lineTop),
                                size = Size(
                                    imageColumn.end - imageColumn.start,
                                    line.lineBottom - line.lineTop
                                ),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }
                else -> {
                    // 绘制文本
                    val paint = if (line.isTitle) titlePaint else textPaint
                    for (column in line.columns) {
                        if (column is TextColumn) {
                            drawContext.canvas.nativeCanvas.drawText(
                                column.charData,
                                column.start,
                                line.lineBase,
                                paint
                            )
                        }
                    }
                }
            }
        }
    }
}
