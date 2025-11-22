package takagi.ru.paysage.reader.touch

/**
 * 触摸区域配置
 * 
 * 定义每个触摸区域对应的动作
 * 
 * 默认配置参考 Legado 的九宫格布局：
 * - 左侧三个区域：上一页
 * - 中心区域：显示/隐藏工具栏
 * - 右侧三个区域：下一页
 * 
 * @property zoneActions 区域到动作的映射
 */
data class TouchZoneConfig(
    val zoneActions: Map<TouchZone, TouchAction> = defaultZoneActions
) {
    companion object {
        /**
         * 默认的区域动作映射
         * 
         * 参考用户提供的九宫格图片：
         * ```
         * ┌─────────┬─────────┬─────────┐
         * │  上一页  │  上一页  │  下一页  │
         * ├─────────┼─────────┼─────────┤
         * │  上一页  │显示工具栏│  下一页  │
         * ├─────────┼─────────┼─────────┤
         * │  上一页  │  下一页  │  下一页  │
         * └─────────┴─────────┴─────────┘
         * ```
         */
        private val defaultZoneActions = mapOf(
            TouchZone.TOP_LEFT to TouchAction.PREVIOUS_PAGE,
            TouchZone.TOP_CENTER to TouchAction.PREVIOUS_PAGE,
            TouchZone.TOP_RIGHT to TouchAction.NEXT_PAGE,
            TouchZone.MIDDLE_LEFT to TouchAction.PREVIOUS_PAGE,
            TouchZone.CENTER to TouchAction.TOGGLE_TOOLBAR,
            TouchZone.MIDDLE_RIGHT to TouchAction.NEXT_PAGE,
            TouchZone.BOTTOM_LEFT to TouchAction.PREVIOUS_PAGE,
            TouchZone.BOTTOM_CENTER to TouchAction.NEXT_PAGE,
            TouchZone.BOTTOM_RIGHT to TouchAction.NEXT_PAGE
        )
        
        /**
         * 创建默认配置
         */
        fun default() = TouchZoneConfig()
        
        /**
         * 创建自定义配置
         */
        fun custom(zoneActions: Map<TouchZone, TouchAction>) = TouchZoneConfig(zoneActions)
    }
    
    /**
     * 获取指定区域的动作
     */
    fun getAction(zone: TouchZone): TouchAction {
        return zoneActions[zone] ?: TouchAction.NONE
    }
    
    /**
     * 更新指定区域的动作
     */
    fun updateAction(zone: TouchZone, action: TouchAction): TouchZoneConfig {
        return copy(zoneActions = zoneActions + (zone to action))
    }
    
    /**
     * 重置为默认配置
     */
    fun reset(): TouchZoneConfig {
        return default()
    }
}
