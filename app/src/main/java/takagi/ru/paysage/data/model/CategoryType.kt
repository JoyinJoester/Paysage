package takagi.ru.paysage.data.model

/**
 * 书籍分类类型
 * 用于区分漫画和小说两大类内容
 */
enum class CategoryType {
    /**
     * 漫画分类
     * 包括CBZ、CBR、CBT、CB7等漫画格式
     */
    MANGA,
    
    /**
     * 阅读分类（小说）
     * 包括PDF、EPUB、TXT等文本格式
     */
    NOVEL;
    
    companion object {
        /**
         * 从字符串解析分类类型
         */
        fun fromString(value: String?): CategoryType {
            return when (value?.lowercase()) {
                "manga" -> MANGA
                "novel" -> NOVEL
                else -> MANGA // 默认为漫画
            }
        }
        
        /**
         * 根据文件格式推断分类类型
         */
        fun fromBookFormat(format: BookFormat): CategoryType {
            return when (format) {
                BookFormat.CBZ, BookFormat.CBR, BookFormat.CBT, 
                BookFormat.CB7, BookFormat.ZIP, BookFormat.RAR,
                BookFormat.TAR, BookFormat.SEVEN_ZIP -> MANGA
                BookFormat.PDF -> NOVEL
            }
        }
    }
}

/**
 * 书库显示模式
 * 用于区分本地功能和在线功能
 */
enum class DisplayMode {
    /**
     * 本地功能模式
     * 显示本地存储的书籍
     */
    LOCAL,
    
    /**
     * 在线功能模式
     * 显示通过网络书源获取的书籍
     */
    ONLINE;
    
    companion object {
        /**
         * 从字符串解析显示模式
         */
        fun fromString(value: String?): DisplayMode {
            return when (value?.lowercase()) {
                "local" -> LOCAL
                "online" -> ONLINE
                else -> LOCAL // 默认为本地
            }
        }
    }
}
