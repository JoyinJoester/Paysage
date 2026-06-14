package joyin.takgi.paysage

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import joyin.takgi.paysage.ui.theme.AppLanguage
import joyin.takgi.paysage.ui.theme.LanguageSettingsStore
import joyin.takgi.paysage.ui.theme.toLocaleListCompat
import joyin.takgi.paysage.ui.theme.withPaysageLocale
import kotlinx.coroutines.runBlocking

class PaysageApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withPaysageLocale())
    }

    override fun onCreate() {
        super.onCreate()
        applyLanguageSettings()
    }

    private fun applyLanguageSettings() {
        val languageStore = LanguageSettingsStore(this)
        val language = runBlocking { languageStore.read() }
        setAppLocale(language)
    }

    companion object {
        fun setAppLocale(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(language.toLocaleListCompat())
        }
    }
}
