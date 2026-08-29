package joyin.takgi.paysage

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import joyin.takgi.paysage.reliability.root.RootShell
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

    override fun onTerminate() {
        // 应用进程被系统终止时关闭持久 su 会话(正常退出场景由 RootMode 页面处理)
        RootShell.close()
        super.onTerminate()
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
