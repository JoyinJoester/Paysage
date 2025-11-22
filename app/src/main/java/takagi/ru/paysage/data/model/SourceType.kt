package takagi.ru.paysage.data.model

/**
 * 内容源类型枚举
 * 定义不同类型的内容源
 */
enum class SourceType {
    /** 本地漫画 */
    LOCAL_MANGA,
    
    /** 本地阅读（小说等） */
    LOCAL_READING,
    
    /** 在线漫画源 */
    MANGA_SOURCE,
    
    /** 在线阅读源（小说源等） */
    READING_SOURCE
}
