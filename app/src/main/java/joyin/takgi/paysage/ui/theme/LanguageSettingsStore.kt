package joyin.takgi.paysage.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_settings")

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    ENGLISH("en", "English"),
    CHINESE("zh", "简体中文"),
    JAPANESE("ja", "日本語"),
    RUSSIAN("ru", "Русский");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: SYSTEM
    }
}

class LanguageSettingsStore(private val context: Context) {
    private val languageKey = stringPreferencesKey("app_language")

    suspend fun read(): AppLanguage {
        val preferences = context.languageDataStore.data.first()
        val code = preferences[languageKey] ?: "system"
        return AppLanguage.fromCode(code)
    }

    suspend fun write(language: AppLanguage) {
        context.languageDataStore.edit { preferences ->
            preferences[languageKey] = language.code
        }
    }

    fun observeLanguage() = context.languageDataStore.data.map { preferences ->
        val code = preferences[languageKey] ?: "system"
        AppLanguage.fromCode(code)
    }
}
