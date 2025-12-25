package takagi.ru.paysage.ui.book.read.provider

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import takagi.ru.paysage.data.model.ReaderConfig
import takagi.ru.paysage.ui.book.read.entities.TextChapterPages
import takagi.ru.paysage.ui.book.read.entities.TextLine
import takagi.ru.paysage.ui.book.read.entities.TextPage
import takagi.ru.paysage.ui.book.read.entities.column.ImageColumn
import takagi.ru.paysage.ui.book.read.entities.column.TextColumn

private const val TAG = "TextChapterLayouter"

/**
 * 文本章节分页器（参考 Legado）
 * 使用 StaticLayout 进行精确文本测量
 */
class TextChapterLayouter(
    private val visibleWidth: Int,
    private val visibleHeight: Int,
    private val config: ReaderConfig
) {
    
    // Paint 对象
    private val textPaint = TextPaint().apply {
        textSize = config.textSize * resources.displayMetrics.scaledDensity
        color = config.textColor
        isAntiAlias = true
    }
    
    private val titlePaint = TextPaint().apply {
        textSize = config.textSize * 1.2f * resources.displayMetrics.scaledDensity
        color = config.textColor
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    // 边距（像素）
    private val paddingLeft = (config.paddingLeft * resources.displayMetrics.density).toInt()
    private val paddingTop = (config.paddingTop * resources.displayMetrics.density).toInt()
    private val paddingRight = (config.paddingRight * resources.displayMetrics.density).toInt()
    private val paddingBottom = (config.paddingBottom * resources.displayMetrics.density).toInt()
    
    // 内容区域尺寸
    private val contentWidth = visibleWidth - paddingLeft - paddingRight
    private val contentHeight =visibleHeight - paddingTop - paddingBottom
    
    // 当前 Y 坐标
    private var durY = 0f
    
    // 当前页面
    private var currentPage = TextPage()
    
    // 所有页面
    private val pages = mutableListOf<TextPage>()
    
    // 字符偏移计数
    private var charOffset = 0
    
    /**
     * 布局章节内容
     */
    fun layoutChapter(
        htmlContent: String,
        chapterIndex: Int,
        chapterTitle: String
    ): TextChapterPages {
        // 初始化
        pages.clear()
        durY = 0f
        charOffset = 0
        currentPage = TextPage(
            index = 0,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle
        )
        
        Log.d(TAG, "开始布局章节: $chapterTitle, 内容长度=${htmlContent.length}")
        
        // 解析内容
        val contentBlocks = ContentParser.parseHtmlContent(htmlContent)
        
        // 排版标题
        layoutTitle(chapterTitle)
        
        // 排版内容块
        for (block in contentBlocks) {
            when (block) {
                is TextBlock -> layoutTextBlock(block.text)
                is ImageBlock -> layoutImageBlock(block.src)
            }
        }
        
        // 添加最后一页
        if (currentPage.lines.isNotEmpty()) {
            currentPage.height = durY + paddingTop + paddingBottom
            pages.add(currentPage)
        }
        
        Log.d(TAG, "布局完成: ${pages.size} 页")
        
        return TextChapterPages(
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            pages = pages,
            totalChars = charOffset
        )
    }
    
    /**
     * 排版标题
     */
    private fun layoutTitle(title: String) {
        if (title.isBlank()) return
        
        // 使用 StaticLayout 测量标题
        val layout = StaticLayout.Builder
            .obtain(title, 0, title.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        
        for (lineIndex in 0 until layout.lineCount) {
            val lineStart = layout.getLineStart(lineIndex)
            val lineEnd = layout.getLineEnd(lineIndex)
            val lineText = title.substring(lineStart, lineEnd)
            
            val lineTop = layout.getLineTop(lineIndex).toFloat()
            val lineBottom = layout.getLineBottom(lineIndex).toFloat()
            val lineHeight = lineBottom - lineTop
            
            // 检查是否需要换页
            checkNewPage(lineHeight)
            
            val textLine = TextLine(
                text = lineText,
                lineTop = durY + paddingTop,
                lineBottom = durY + lineHeight + paddingTop,
                lineBase = durY + paddingTop + layout.getLineBaseline(lineIndex),
                lineStart = paddingLeft.toFloat(),
                lineEnd = (paddingLeft + contentWidth).toFloat(),
                isTitle = true,
                charOffset = charOffset
            )
            
            textLine.addColumn(
                TextColumn(
                    start = paddingLeft.toFloat(),
                    end = (paddingLeft + contentWidth).toFloat(),
                    charData = lineText
                )
            )
            
            currentPage.addLine(textLine)
            durY += lineHeight
            charOffset += lineText.length
        }
        
        // 标题后增加间距
        durY += titlePaint.textSize * 0.5f
    }
    
    /**
     * 排版文本块
     */
    private fun layoutTextBlock(text: String) {
        if (text.isBlank()) return
        
        // 按段落分割
        val paragraphs = text.split("\n")
        
        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) continue
            
            // 使用 StaticLayout 测量段落
            val layout = StaticLayout.Builder
                .obtain(paragraph, 0, paragraph.length, textPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing((config.lineSpacing - 1f) * textPaint.textSize, 1f)
                .setIncludePad(false)
                .build()
            
            for (lineIndex in 0 until layout.lineCount) {
                val lineStart = layout.getLineStart(lineIndex)
                val lineEnd = layout.getLineEnd(lineIndex)
                val lineText = paragraph.substring(lineStart, lineEnd)
                
                // 获取该行在 layout 中的实际高度
                val lineTop = layout.getLineTop(lineIndex).toFloat()
                val lineBottom = layout.getLineBottom(lineIndex).toFloat()
                val lineBaseline = layout.getLineBaseline(lineIndex).toFloat()
                val lineHeight = lineBottom - lineTop
                
                // 检查是否需要换页
                checkNewPage(lineHeight)
                
                val textLine = TextLine(
                    text = lineText,
                    lineTop = durY + paddingTop,
                    lineBottom = durY + lineHeight + paddingTop,
                    lineBase = durY + paddingTop + (lineBaseline - lineTop),  // 修正：相对于行顶的偏移
                    lineStart = paddingLeft.toFloat(),
                    lineEnd = (paddingLeft + contentWidth).toFloat(),
                    isParagraphEnd = (lineIndex == layout.lineCount - 1),
                    charOffset = charOffset
                )
                
                textLine.addColumn(
                    TextColumn(
                        start = paddingLeft.toFloat(),
                        end = (paddingLeft + contentWidth).toFloat(),
                        charData = lineText
                    )
                )
                
                currentPage.addLine(textLine)
                durY += lineHeight
                charOffset += lineText.length
            }
            
            // 段落间增加间距
            durY += textPaint.textSize * 0.2f
        }
    }
    
    /**
     * 排版图片块
     * 保持图片宽高比，最大宽度为内容宽度
     */
    private fun layoutImageBlock(src: String) {
        // 增大默认图片高度（如果无法获取真实尺寸）
        val defaultImageHeight = contentHeight * 0.6f  // 从 30% 增加到 60%
        val maxImageWidth = contentWidth.toFloat()
        val maxImageHeight = contentHeight * 0.8f  // 最大占屏幕 80%（从 60% 增加）
        
        // 这里简化处理，使用默认尺寸
        // TODO: 可以从 ImageCache 获取真实尺寸
        var imageWidth = maxImageWidth
        var imageHeight = defaultImageHeight
        
        // 确保不超过最大高度
        if (imageHeight > maxImageHeight) {
            val scale = maxImageHeight / imageHeight
            imageHeight = maxImageHeight
            imageWidth *= scale
        }
        
        // 检查是否需要换页
        checkNewPage(imageHeight)
        
        // 居中显示
        val imageLeft = paddingLeft + (contentWidth - imageWidth) / 2f
        val imageRight = imageLeft + imageWidth
        
        val textLine = TextLine(
            text = " ",
            lineTop = durY + paddingTop,
            lineBottom = durY + imageHeight + paddingTop,
            lineStart = paddingLeft.toFloat(),
            lineEnd = (paddingLeft + contentWidth).toFloat(),
            isImage = true,
            charOffset = charOffset
        )
        
        textLine.addColumn(
            ImageColumn(
                start = imageLeft,
                end = imageRight,
                src = src
            )
        )
        
        currentPage.addLine(textLine)
        durY += imageHeight + textPaint.textSize * 0.5f  // 图片后增加间距
        charOffset += 1  // 图片占一个字符位
    }
    
    /**
     * 检查是否需要新建页面
     */
    private fun checkNewPage(nextLineHeight: Float) {
        if (durY + nextLineHeight > contentHeight && currentPage.lines.isNotEmpty()) {
            // 保存当前页
            currentPage.height = durY + paddingTop + paddingBottom
            pages.add(currentPage)
            
            // 创建新页
            currentPage = TextPage(
                index = pages.size,
                chapterIndex = currentPage.chapterIndex,
                chapterTitle = currentPage.chapterTitle,
                charOffset = charOffset
            )
            durY = 0f
        }
    }
    
    companion object {
        // 用于访问 Resources
        private val resources = android.content.res.Resources.getSystem()
    }
}
