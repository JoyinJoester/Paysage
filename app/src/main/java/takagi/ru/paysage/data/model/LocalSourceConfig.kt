package takagi.ru.paysage.data.model

/**
 * 本地源配置数据类
 * 存储本地源的配置信息
 */
data class LocalSourceConfig(
    /** 本地漫画文件夹路径 */
    val mangaPath: String? = null,
    
    /** 本地阅读文件夹路径 */
    val readingPath: String? = null,
    
    /** 最后更新时间戳 */
    val lastUpdated: Long = System.currentTimeMillis()
)
