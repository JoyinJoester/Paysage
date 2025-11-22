package takagi.ru.paysage.reader.touch

/**
 * 触摸动作枚举
 * 
 * 定义触摸区域可以执行的动作类型
 */
enum class TouchAction {
    /** 翻到上一页 */
    PREVIOUS_PAGE,
    
    /** 翻到下一页 */
    NEXT_PAGE,
    
    /** 切换工具栏显示/隐藏 */
    TOGGLE_TOOLBAR,
    
    /** 显示菜单 */
    SHOW_MENU,
    
    /** 添加书签 */
    BOOKMARK,
    
    /** 显示快速设置 */
    QUICK_SETTINGS,
    
    /** 无动作 */
    NONE;
    
    /**
     * 获取动作的显示名称
     */
    fun getDisplayName(): String {
        return when (this) {
            PREVIOUS_PAGE -> "上一页"
            NEXT_PAGE -> "下一页"
            TOGGLE_TOOLBAR -> "显示/隐藏工具栏"
            SHOW_MENU -> "显示菜单"
            BOOKMARK -> "添加书签"
            QUICK_SETTINGS -> "快速设置"
            NONE -> "无动作"
        }
    }
}
