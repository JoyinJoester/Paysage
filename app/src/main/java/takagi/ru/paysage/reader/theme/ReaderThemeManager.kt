package takagi.ru.paysage.reader.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 阅读主题管理器
 * 
 * 负责管理和切换阅读主题
 */
class ReaderThemeManager {
    private var _currentTheme by mutableStateOf(ReaderTheme.Default)
    
    /**
     * 获取当前主题
     */
    fun getCurrentTheme(): ReaderTheme = _currentTheme
    
    /**
     * 设置主题
     */
    fun setTheme(theme: ReaderTheme) {
        _currentTheme = theme
    }
    
    /**
     * 根据 ID 设置主题
     */
    fun setThemeById(id: String) {
        _currentTheme = ReaderTheme.getThemeById(id)
    }
    
    /**
     * 切换到下一个主题
     */
    fun nextTheme() {
        val allThemes = ReaderTheme.AllThemes
        val currentIndex = allThemes.indexOf(_currentTheme)
        val nextIndex = (currentIndex + 1) % allThemes.size
        _currentTheme = allThemes[nextIndex]
    }
    
    /**
     * 切换到上一个主题
     */
    fun previousTheme() {
        val allThemes = ReaderTheme.AllThemes
        val currentIndex = allThemes.indexOf(_currentTheme)
        val previousIndex = if (currentIndex == 0) allThemes.size - 1 else currentIndex - 1
        _currentTheme = allThemes[previousIndex]
    }
}

/**
 * 全局阅读主题管理器
 */
object GlobalReaderThemeManager {
    private val instance = ReaderThemeManager()
    
    fun getCurrentTheme(): ReaderTheme = instance.getCurrentTheme()
    fun setTheme(theme: ReaderTheme) = instance.setTheme(theme)
    fun setThemeById(id: String) = instance.setThemeById(id)
    fun nextTheme() = instance.nextTheme()
    fun previousTheme() = instance.previousTheme()
}

/**
 * CompositionLocal for ReaderTheme
 */
val LocalReaderTheme = compositionLocalOf { ReaderTheme.Default }

/**
 * 提供阅读主题的 Composable
 */
@Composable
fun ProvideReaderTheme(
    theme: ReaderTheme = ReaderTheme.Default,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalReaderTheme provides theme) {
        content()
    }
}

/**
 * 获取当前阅读主题
 */
@Composable
fun currentReaderTheme(): ReaderTheme {
    return LocalReaderTheme.current
}
