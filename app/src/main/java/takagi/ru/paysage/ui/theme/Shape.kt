package takagi.ru.paysage.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shapes
 * 使用更圆润、更有表现力的形状
 */
val Shapes = Shapes(
    // 特小 - 用于芯片、小按钮
    extraSmall = RoundedCornerShape(8.dp),
    
    // 小 - 用于卡片内的小元素
    small = RoundedCornerShape(12.dp),
    
    // 中 - 默认卡片、按钮
    medium = RoundedCornerShape(16.dp),
    
    // 大 - 大型卡片、底部抽屉
    large = RoundedCornerShape(24.dp),
    
    // 特大 - 对话框、模态框
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Expressive 特殊形状
 */
object ExpressiveShapes {
    // 书籍卡片形状 - 左上右上圆角
    val bookCard = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 8.dp,
        bottomEnd = 8.dp
    )
    
    // 悬浮按钮形状 - 超圆润
    val floatingButton = RoundedCornerShape(28.dp)
    
    // 底部导航栏形状
    val bottomBar = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // 顶部应用栏形状
    val topBar = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )
    
    // 搜索框形状
    val searchBar = RoundedCornerShape(50)
    
    // 进度指示器形状
    val progressIndicator = RoundedCornerShape(50)
}

// ============================================
// 📐 分类系统专用形状
// ============================================

/**
 * 分类系统形状扩展
 */
object CategoryShapes {
    // 分类卡片形状
    val categoryCard = RoundedCornerShape(16.dp)
    
    // 书源卡片形状
    val sourceCard = RoundedCornerShape(12.dp)
    
    // 筛选芯片形状
    val filterChip = RoundedCornerShape(20.dp)
    
    // 底部弹窗形状
    val bottomSheet = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // 对话框形状
    val dialog = RoundedCornerShape(24.dp)
    
    // 浮动操作按钮形状
    val fab = RoundedCornerShape(16.dp)
    
    // 小型组件形状
    val smallComponent = RoundedCornerShape(8.dp)
    
    // 大型组件形状
    val largeComponent = RoundedCornerShape(20.dp)
}

/**
 * 根据分类类型获取对应的卡片形状
 */
fun getCategoryCardShape(categoryType: takagi.ru.paysage.data.model.CategoryType): RoundedCornerShape {
    return when (categoryType) {
        takagi.ru.paysage.data.model.CategoryType.MANGA -> CategoryShapes.categoryCard
        takagi.ru.paysage.data.model.CategoryType.NOVEL -> CategoryShapes.categoryCard
    }
}
