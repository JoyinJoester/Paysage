package joyin.takgi.paysage.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.runBlocking
import java.util.Locale

fun Context.withPaysageLocale(): Context {
    val storeContext = applicationContext ?: this
    val language = runCatching {
        runBlocking { LanguageSettingsStore(storeContext).read() }
    }.getOrDefault(AppLanguage.SYSTEM)
    return withPaysageLocale(language)
}

fun Context.withPaysageLocale(language: AppLanguage): Context {
    val localeList = language.toLocale()?.let { LocaleList(it) }
        ?: Resources.getSystem().configuration.locales
    if (localeList.size() > 0) {
        Locale.setDefault(localeList[0])
    }
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(localeList)
    return createConfigurationContext(configuration)
}

fun AppLanguage.toLocaleListCompat(): LocaleListCompat {
    val tag = localeTag() ?: return LocaleListCompat.getEmptyLocaleList()
    return LocaleListCompat.forLanguageTags(tag)
}

private fun AppLanguage.toLocale(): Locale? =
    localeTag()?.let(Locale::forLanguageTag)

private fun AppLanguage.localeTag(): String? =
    when (this) {
        AppLanguage.SYSTEM -> null
        AppLanguage.ENGLISH -> "en"
        AppLanguage.CHINESE -> "zh-CN"
        AppLanguage.JAPANESE -> "ja"
        AppLanguage.RUSSIAN -> "ru"
    }
