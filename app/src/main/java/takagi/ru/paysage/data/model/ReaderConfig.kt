package takagi.ru.paysage.data.model

import android.graphics.Color
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阅读器配置数据模型
 * 基于legado的ReadBookConfig适配到paysage项目
 */
@Keep
@Entity(tableName = "reader_configs")
data class ReaderConfig(
    @PrimaryKey
    val id: Int = 0, // 0为默认配置
    
    // 配置名称
    var name: String = "默认",
    
    // 背景设置
    var bgColor: Int = Color.parseColor("#EEEEEE"), // 白天背景色
    var bgColorNight: Int = Color.parseColor("#000000"), // 夜间背景色
    var bgImagePath: String = "", // 背景图片路径
    var bgType: BgType = BgType.COLOR, // 背景类型
    var bgAlpha: Int = 100, // 背景透明度 0-100
    
    // 文字设置
    var textColor: Int = Color.parseColor("#3E3D3B"), // 白天文字颜色
    var textColorNight: Int = Color.parseColor("#ADADAD"), // 夜间文字颜色
    var textFont: String = "", // 字体路径,空表示系统默认
    var textSize: Int = 18, // 文字大小 sp
    var textBold: TextBoldType = TextBoldType.NORMAL, // 文字粗细
    
    // 间距设置
    var letterSpacing: Float = 0.1f, // 字间距
    var lineSpacing: Float = 1.2f, // 行间距倍数
    var paragraphSpacing: Int = 8, // 段落间距 dp
    var paragraphIndent: String = "　　", // 段落缩进
    
    // 页面边距
    var paddingTop: Int = 16, // 上边距 dp
    var paddingBottom: Int = 16, // 下边距 dp
    var paddingLeft: Int = 16, // 左边距 dp
    var paddingRight: Int = 16, // 右边距 dp
    
    // 标题设置
    var titleMode: TitleMode = TitleMode.LEFT, // 标题位置
    var titleSize: Int = 20, // 标题大小 sp
    var titleTopSpacing: Int = 0, // 标题上间距 dp
    var titleBottomSpacing: Int = 8, // 标题下间距 dp
    
    // 翻页动画
    var pageMode: PageMode = PageMode.SIMULATION, // 翻页模式
    
    // 覆盖翻页配置
    var coverFlipAnimationDuration: Int = 300, // 动画时长(毫秒)
    var coverFlipSwipeThreshold: Float = 0.3f, // 吸附阈值(0f 到 1f)
    var coverFlipVelocityThreshold: Float = 1000f, // 速度阈值(dp/s)
    var coverFlipShadowEnabled: Boolean = true, // 是否启用阴影
    var coverFlipShadowMaxAlpha: Float = 0.4f, // 阴影最大透明度
    var coverFlipBounceEnabled: Boolean = true, // 是否启用边界回弹
    var coverFlipBounceMaxDisplacement: Float = 100f, // 回弹最大位移(dp)
    var coverFlipBounceDuration: Int = 200, // 回弹动画时长(毫秒)
    
    // 页眉页脚设置
    var showHeaderLine: Boolean = false, // 显示页眉分隔线
    var showFooterLine: Boolean = true, // 显示页脚分隔线
    var headerPaddingTop: Int = 0,
    var headerPaddingBottom: Int = 0,
    var headerPaddingLeft: Int = 16,
    var headerPaddingRight: Int = 16,
    var footerPaddingTop: Int = 6,
    var footerPaddingBottom: Int = 6,
    var footerPaddingLeft: Int = 16,
    var footerPaddingRight: Int = 16,
    
    // 提示信息配置
    var tipHeaderLeft: TipConfig = TipConfig.TIME,
    var tipHeaderMiddle: TipConfig = TipConfig.NONE,
    var tipHeaderRight: TipConfig = TipConfig.BATTERY,
    var tipFooterLeft: TipConfig = TipConfig.CHAPTER_TITLE,
    var tipFooterMiddle: TipConfig = TipConfig.NONE,
    var tipFooterRight: TipConfig = TipConfig.PAGE_AND_TOTAL,
    var tipColor: Int = 0, // 提示文字颜色,0表示跟随文字颜色
    
    // 其他设置
    var underline: Boolean = false, // 文字下划线
    var textFullJustify: Boolean = true, // 两端对齐
    var textBottomJustify: Boolean = true, // 底部对齐
    var darkStatusIcon: Boolean = true, // 深色状态栏图标
    
    // 屏幕设置
    var hideStatusBar: Boolean = true, // 隐藏状态栏
    var hideNavigationBar: Boolean = true, // 隐藏导航栏
    var keepScreenOn: Boolean = true, // 保持屏幕常亮
    var screenOrientation: ScreenOrientation = ScreenOrientation.UNSPECIFIED, // 屏幕方向
    var volumeKeyPage: Boolean = true, // 音量键翻页
    
    // 自动阅读设置
    var autoReadSpeed: Int = 50, // 自动阅读速度 (字/分钟)
    
    // 朗读设置
    var readAloudSpeed: Int = 5, // 朗读速度 1-10
    
    // 文本选择
    var textSelectAble: Boolean = true // 允许文本选择
) {
    /**
     * 获取当前主题的文字颜色
     */
    fun getCurrentTextColor(isNightMode: Boolean): Int {
        return if (isNightMode) textColorNight else textColor
    }
    
    /**
     * 获取当前主题的背景颜色
     */
    fun getCurrentBgColor(isNightMode: Boolean): Int {
        return if (isNightMode) bgColorNight else bgColor
    }
    
    /**
     * 获取覆盖翻页配置
     */
    fun getCoverFlipConfig(): takagi.ru.paysage.reader.animation.CoverFlipConfig {
        return takagi.ru.paysage.reader.animation.CoverFlipConfig(
            animationDuration = coverFlipAnimationDuration,
            swipeThreshold = coverFlipSwipeThreshold,
            velocityThreshold = coverFlipVelocityThreshold,
            shadowEnabled = coverFlipShadowEnabled,
            shadowMaxAlpha = coverFlipShadowMaxAlpha,
            shadowBlurRadius = 8f,
            bounceEnabled = coverFlipBounceEnabled,
            bounceMaxDisplacement = coverFlipBounceMaxDisplacement,
            bounceDuration = coverFlipBounceDuration
        )
    }
}

/**
 * 背景类型
 */
enum class BgType {
    COLOR,  // 纯色背景
    IMAGE   // 图片背景
}

/**
 * 文字粗细类型
 */
enum class TextBoldType {
    THIN,    // 细体
    NORMAL,  // 正常
    BOLD     // 粗体
}

/**
 * 标题位置
 */
enum class TitleMode {
    LEFT,    // 居左
    CENTER,  // 居中
    HIDE     // 隐藏
}

/**
 * 翻页模式
 */
enum class PageMode {
    SIMULATION,  // 仿真翻页
    COVER,       // 覆盖翻页
    SLIDE,       // 滑动翻页
    SCROLL,      // 垂直滚动
    HORIZONTAL,  // 水平滚动
    NONE         // 无动画
}

/**
 * 屏幕方向
 */
enum class ScreenOrientation {
    UNSPECIFIED,  // 跟随系统
    PORTRAIT,     // 竖屏
    LANDSCAPE     // 横屏
}

/**
 * 提示信息配置
 */
enum class TipConfig {
    NONE,           // 不显示
    TIME,           // 时间
    BATTERY,        // 电量
    PAGE,           // 页码
    TOTAL_PAGE,     // 总页数
    PAGE_AND_TOTAL, // 页码/总页数
    PROGRESS,       // 进度百分比
    CHAPTER_TITLE,  // 章节标题
    BOOK_NAME       // 书名
}
