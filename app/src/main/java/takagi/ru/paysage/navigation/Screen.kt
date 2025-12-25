package takagi.ru.paysage.navigation

/**
 * 导航路由
 */
sealed class Screen(val route: String) {
    object Library : Screen("library?filter={filter}&category={category}") {
        fun createRoute(filter: String? = null, category: String? = null): String {
            val params = mutableListOf<String>()
            filter?.let { params.add("filter=$it") }
            category?.let { params.add("category=$it") }
            return if (params.isEmpty()) "library" else "library?${params.joinToString("&")}"
        }
    }
    object Reader : Screen("reader/{bookId}?page={page}") {
        fun createRoute(bookId: Long, page: Int = -1): String {
            return if (page >= 0) {
                "reader/$bookId?page=$page"
            } else {
                "reader/$bookId"
            }
        }
    }
    object TextReader : Screen("text_reader/{bookId}/{filePath}") {
        fun createRoute(bookId: Long, filePath: String): String {
            // 对文件路径进行 URL 编码
            val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            return "text_reader/$bookId/$encodedPath"
        }
    }
    object Settings : Screen("settings?section={section}") {
        fun createRoute(section: String? = null): String {
            return if (section != null) {
                "settings?section=$section"
            } else {
                "settings"
            }
        }
    }
    object Bookmarks : Screen("bookmarks/{bookId}/{bookTitle}") {
        fun createRoute(bookId: Long, bookTitle: String) = "bookmarks/$bookId/${bookTitle.replace("/", "_")}"
    }
    object BookDetail : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }
}
