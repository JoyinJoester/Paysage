package takagi.ru.paysage.reader.text

import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import takagi.ru.paysage.reader.ReaderConfig

/**
 * 文本排版引擎
 * 参考 Legado 的排版思想，使用 Android StaticLayout 实现
 */
class TextLayoutEngine(
    private val config: ReaderConfig
) {
    
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = config.textSize.toFloat()
        color = config.textColor
        typeface = Typeface.DEFAULT
    }
    
    /**
     * 对文本内容进行排版，生成页面列表
     */
    fun layoutText(
        content: TextContent,
        width: Int,
        height: Int
    ): List<TextPage> {
        if (content.text.isEmpty()) {
            return emptyList()
        }
        
        val availableWidth = width - config.paddingLeft - config.paddingRight
        val availableHeight = height - config.paddingTop - config.paddingBottom
        
        if (availableWidth <= 0 || availableHeight <= 0) {
            return emptyList()
        }
        
        // 使用 StaticLayout 进行文本排版
        val layout = createStaticLayout(content.text, availableWidth)
        
        // 将排版结果分页
        return paginateLayout(layout, availableHeight, content)
    }
    
    /**
     * 创建 StaticLayout
     */
    private fun createStaticLayout(text: String, width: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(config.lineSpacing, 1f)
            .setIncludePad(false)
            .build()
    }
    
    /**
     * 将 StaticLayout 分页
     */
    private fun paginateLayout(
        layout: StaticLayout,
        pageHeight: Int,
        content: TextContent
    ): List<TextPage> {
        val pages = mutableListOf<TextPage>()
        var currentPageLines = mutableListOf<TextLine>()
        var currentPageHeight = 0f
        var lineIndexInPage = 0
        
        for (i in 0 until layout.lineCount) {
            val lineTop = layout.getLineTop(i).toFloat()
            val lineBottom = layout.getLineBottom(i).toFloat()
            val lineHeight = lineBottom - lineTop
            val lineBaseline = layout.getLineBaseline(i).toFloat()
            
            // 检查是否需要开始新页
            if (currentPageHeight + lineHeight > pageHeight && currentPageLines.isNotEmpty()) {
                // 保存当前页
                pages.add(createTextPage(
                    pageIndex = pages.size,
                    lines = currentPageLines.toList(),
                    content = content
                ))
                
                // 开始新页
                currentPageLines.clear()
                currentPageHeight = 0f
                lineIndexInPage = 0
            }
            
            // 获取行文本
            val lineStart = layout.getLineStart(i)
            val lineEnd = layout.getLineEnd(i)
            val lineText = content.text.substring(lineStart, lineEnd)
            
            // 检查是否为段落开始/结束
            val isParagraphStart = lineStart == 0 || 
                content.text.getOrNull(lineStart - 1) == '\n'
            val isParagraphEnd = lineEnd >= content.text.length || 
                content.text.getOrNull(lineEnd) == '\n'
            
            // 创建文本行
            val textLine = TextLine(
                text = lineText,
                y = currentPageHeight,
                baseline = currentPageHeight + (lineBaseline - lineTop),
                height = lineHeight,
                lineIndex = lineIndexInPage,
                isTitle = false,  // TODO: 检测标题
                isParagraphStart = isParagraphStart,
                isParagraphEnd = isParagraphEnd
            )
            
            currentPageLines.add(textLine)
            currentPageHeight += lineHeight
            lineIndexInPage++
        }
        
        // 添加最后一页
        if (currentPageLines.isNotEmpty()) {
            pages.add(createTextPage(
                pageIndex = pages.size,
                lines = currentPageLines.toList(),
                content = content
            ))
        }
        
        // 更新总页数
        return pages.mapIndexed { index, page ->
            page.copy(totalPages = pages.size)
        }
    }
    
    /**
     * 创建 TextPage
     */
    private fun createTextPage(
        pageIndex: Int,
        lines: List<TextLine>,
        content: TextContent
    ): TextPage {
        return TextPage(
            pageIndex = pageIndex,
            lines = lines,
            chapterIndex = content.chapterIndex,
            chapterTitle = content.chapterTitle
        )
    }
    
    /**
     * 测量文本宽度
     */
    fun measureTextWidth(text: String): Float {
        return textPaint.measureText(text)
    }
    
    /**
     * 获取字体度量信息
     */
    fun getFontMetrics(): Paint.FontMetrics {
        return textPaint.fontMetrics
    }
}
