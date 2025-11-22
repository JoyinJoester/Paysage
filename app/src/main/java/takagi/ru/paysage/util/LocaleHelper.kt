package takagi.ru.paysage.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import takagi.ru.paysage.data.model.Language
import java.util.*

/**
 * 语言切换辅助类
 */
object LocaleHelper {
    
    /**
     * 设置应用语言
     */
    fun setLocale(context: Context, language: Language): Context {
        val locale = when (language) {
            Language.SYSTEM -> getSystemLocale()
            Language.ENGLISH -> Locale.ENGLISH
            Language.CHINESE -> Locale.CHINA
        }
        
        return updateResources(context, locale)
    }
    
    /**
     * 获取系统语言
     */
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Locale.getDefault()
        } else {
            Locale.getDefault()
        }
    }
    
    /**
     * 更新资源配置
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(context: Context): Language {
        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return when (currentLocale.language) {
            "zh" -> Language.CHINESE
            "en" -> Language.ENGLISH
            else -> Language.SYSTEM
        }
    }
}
