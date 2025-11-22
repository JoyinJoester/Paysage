package takagi.ru.paysage.reader.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import takagi.ru.paysage.reader.ReaderConfig

/**
 * 文本页面渲染器
 * 负责将 TextPage 渲染到 Canvas 上
 */
class TextPageRenderer(
    private val config: ReaderConfig
) {
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = config.textSize.toFloat()
        color = config.textColor
        typeface = Typeface.DEFAULT
    }
    
    private val titlePaint = Paint().apply {
        isAntiAlias = true
        textSize = (config.textSize * 1.2f)
        color = config.textColor
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val selectionPaint = Paint().apply {
        color = Color(0x4000BFFF).toArgb()  // 半透明蓝色
        style = Paint.Style.FILL
    }
    
    private val highlightPaint = Paint().apply {
        color = Color(0x40FFFF00).toArgb()  // 半透明黄色
        style = Paint.Style.FILL
    }
    
    /**
     * 渲染文本页面
     */
    fun renderPage(
        canvas: Canvas,
        page: TextPage,
        selection: TextSelection? = null,
        highlights: List<SearchResult> = emptyList()
    ) {
        canvas.save()
        
        try {
            // 应用边距
            canvas.translate(
                config.paddingLeft.toFloat(),
                config.paddingTop.toFloat()
            )
            
            // 渲染每一行
            for (line in page.lines) {
                renderLine(canvas, line, page, selection, highlights)
            }
        } finally {
            canvas.restore()
        }
    }
    
    /**
     * 渲染单行文本
     */
    private fun renderLine(
        canvas: Canvas,
        line: TextLine,
        page: TextPage,
        selection: TextSelection?,
        highlights: List<SearchResult>
    ) {
        val paint = if (line.isTitle) titlePaint else textPaint
        
        // 渲染选择背景
        if (selection != null && !selection.isEmpty) {
            renderSelectionBackground(canvas, line, page, selection)
        }
        
        // 渲染搜索高亮背景
        for (highlight in highlights) {
            if (highlight.pageIndex == page.pageIndex) {
                renderHighlightBackground(canvas, line, highlight)
            }
        }
        
        // 渲染文本
        canvas.drawText(
            line.text,
            0f,
            line.baseline,
            paint
        )
    }
    
    /**
     * 渲染选择背景
     */
    private fun renderSelectionBackground(
        canvas: Canvas,
        line: TextLine,
        page: TextPage,
        selection: TextSelection
    ) {
        val linePosition = TextPosition(page.pageIndex, line.lineIndex, 0)
        
        if (!selection.contains(linePosition)) {
            return
        }
        
        // 计算选择范围
        val startChar = if (selection.start.pageIndex == page.pageIndex && 
                            selection.start.lineIndex == line.lineIndex) {
            selection.start.charIndex
        } else {
            0
        }
        
        val endChar = if (selection.end.pageIndex == page.pageIndex && 
                          selection.end.lineIndex == line.lineIndex) {
            selection.end.charIndex
        } else {
            line.text.length
        }
        
        if (startChar >= endChar) return
        
        // 测量文本宽度
        val startX = textPaint.measureText(line.text, 0, startChar)
        val endX = textPaint.measureText(line.text, 0, endChar)
        
        // 绘制选择背景
        canvas.drawRect(
            startX,
            line.y,
            endX,
            line.bottom,
            selectionPaint
        )
    }
    
    /**
     * 渲染搜索高亮背景
     */
    private fun renderHighlightBackground(
        canvas: Canvas,
        line: TextLine,
        highlight: SearchResult
    ) {
        if (highlight.position.lineIndex != line.lineIndex) {
            return
        }
        
        val startChar = highlight.position.charIndex
        val endChar = startChar + highlight.matchText.length
        
        if (startChar >= line.text.length) return
        
        // 测量文本宽度
        val startX = textPaint.measureText(line.text, 0, startChar)
        val endX = textPaint.measureText(line.text, 0, endChar.coerceAtMost(line.text.length))
        
        // 绘制高亮背景
        canvas.drawRect(
            startX,
            line.y,
            endX,
            line.bottom,
            highlightPaint
        )
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(newConfig: ReaderConfig) {
        textPaint.apply {
            textSize = newConfig.textSize.toFloat()
            color = newConfig.textColor
        }
        
        titlePaint.apply {
            textSize = (newConfig.textSize * 1.2f)
            color = newConfig.textColor
        }
    }
    
    /**
     * 获取文本在指定位置的字符索引
     */
    fun getCharIndexAt(x: Float, line: TextLine): Int {
        var currentX = 0f
        
        for (i in line.text.indices) {
            val charWidth = textPaint.measureText(line.text, i, i + 1)
            if (x < currentX + charWidth / 2) {
                return i
            }
            currentX += charWidth
        }
        
        return line.text.length
    }
    
    /**
     * 获取字符的边界矩形
     */
    fun getCharBounds(charIndex: Int, line: TextLine): Rect {
        val x = textPaint.measureText(line.text, 0, charIndex)
        val width = textPaint.measureText(line.text, charIndex, charIndex + 1)
        
        return Rect(
            x.toInt(),
            line.y.toInt(),
            (x + width).toInt(),
            line.bottom.toInt()
        )
    }
}
