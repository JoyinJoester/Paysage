package takagi.ru.paysage.reader.touch

/**
 * 触摸区域枚举
 * 
 * 将屏幕划分为九个区域（3x3 网格）
 * 
 * 布局：
 * ```
 * ┌─────────┬─────────┬─────────┐
 * │ TOP_LEFT│TOP_CENTER│TOP_RIGHT│
 * ├─────────┼─────────┼─────────┤
 * │MIDDLE_LEFT│ CENTER │MIDDLE_RIGHT│
 * ├─────────┼─────────┼─────────┤
 * │BOTTOM_LEFT│BOTTOM_CENTER│BOTTOM_RIGHT│
 * └─────────┴─────────┴─────────┘
 * ```
 */
enum class TouchZone {
    /** 左上角 */
    TOP_LEFT,
    
    /** 上中 */
    TOP_CENTER,
    
    /** 右上角 */
    TOP_RIGHT,
    
    /** 左中 */
    MIDDLE_LEFT,
    
    /** 中心 */
    CENTER,
    
    /** 右中 */
    MIDDLE_RIGHT,
    
    /** 左下角 */
    BOTTOM_LEFT,
    
    /** 下中 */
    BOTTOM_CENTER,
    
    /** 右下角 */
    BOTTOM_RIGHT
}
