package takagi.ru.paysage.ui.components.reader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import takagi.ru.paysage.data.model.ReaderConfig

/**
 * WebView 章节渲染组件
 * 使用 CSS column 实现真实分页
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChapterWebView(
    htmlContent: String,
    config: ReaderConfig,
    onPageChange: (current: Int, total: Int) -> Unit,
    onRequestNextChapter: () -> Unit,
    onRequestPrevChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    builtInZoomControls = false
                   displayZoomControls = false
                    setSupportZoom(false)
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = true
                
                // JavaScript 接口
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onPageParams(current: Int, total: Int) {
                        onPageChange(current, total)
                    }
                    
                    @JavascriptInterface
                    fun onNextChapter() {
                        onRequestNextChapter()
                    }
                    
                    @JavascriptInterface
                    fun onPrevChapter() {
                        onRequestPrevChapter()
                    }
                }, "Android")
                
                webView = this
            }
        },
        update = { view ->
            val fullHtml = generateColumnHtml(htmlContent, config)
            view.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
        }
    )
}

/**
 * 生成使用 CSS column 分页的 HTML
 */
private fun generateColumnHtml(content: String, config: ReaderConfig): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                html, body {
                    height: 100%;
                    overflow: hidden;
                    background-color: ${String.format("#%06X", 0xFFFFFF and config.bgColor)};
                }
                #content {
                    height: 100vh;
                    width: 100vw;
                    column-width: 100vw;
                    column-gap: 0;
                    column-fill: auto;
                    overflow-x: scroll;
                    overflow-y: hidden;
                    padding: ${config.paddingTop}px ${config.paddingRight}px ${config.paddingBottom}px ${config.paddingLeft}px;
                    color: ${String.format("#%06X", 0xFFFFFF and config.textColor)};
                    font-size: ${config.textSize}px;
                    line-height: ${config.lineSpacing};
                    text-align: justify;
                    scroll-snap-type: x mandatory;
                    -webkit-column-break-inside: avoid;
                    break-inside: avoid;
                }
                #content > * {
                    scroll-snap-align: start;
                }
                img {
                    max-width: 100%;
                    height: auto;
                    display: block;
                    margin: 8px auto;
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                p {
                    margin: 0.5em 0;
                    text-indent: 2em;
                    orphans: 2;
                    widows: 2;
                }
            </style>
        </head>
        <body>
            <div id="content">${content}</div>
            <script>
                var totalPages = 1;
                var currentPage = 1;
                var pageWidth = 0;
                
                window.onload = function() {
                    calculatePages();
                    setupScrollListener();
                };
                
                function calculatePages() {
                    var content = document.getElementById('content');
                    pageWidth = window.innerWidth;
                    var scrollWidth = content.scrollWidth;
                    totalPages = Math.max(1, Math.ceil(scrollWidth / pageWidth));
                    updateAndroid();
                }
                
                function setupScrollListener() {
                    var content = document.getElementById('content');
                    content.addEventListener('scroll', function() {
                        var newPage = Math.round(content.scrollLeft / pageWidth) + 1;
                        if (newPage !== currentPage) {
                            currentPage = newPage;
                            updateAndroid();
                        }
                    });
                }
                
                function updateAndroid() {
                    if (window.Android) {
                        window.Android.onPageParams(currentPage, totalPages);
                    }
                }
                
                function nextPage() {
                    var content = document.getElementById('content');
                    if (currentPage < totalPages) {
                        content.scrollTo({
                            left: currentPage * pageWidth,
                            behavior: 'smooth'
                        });
                        return true;
                    } else {
                        if (window.Android) {
                            window.Android.onNextChapter();
                        }
                        return false;
                    }
                }
                
                function prevPage() {
                    var content = document.getElementById('content');
                    if (currentPage > 1) {
                        content.scrollTo({
                            left: (currentPage - 2) * pageWidth,
                            behavior: 'smooth'
                        });
                        return true;
                    } else {
                        if (window.Android) {
                            window.Android.onPrevChapter();
                        }
                        return false;
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
