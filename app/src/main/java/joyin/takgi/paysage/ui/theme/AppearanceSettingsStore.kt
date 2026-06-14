package joyin.takgi.paysage.ui.theme

import android.content.Context

enum class PaysageThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class PaysageColorScheme {
    DEFAULT,
    OCEAN_TEAL,
    SUNSET_ORANGE,
    FOREST_GREEN
}

data class PaysageAppearanceSettings(
    val themeMode: PaysageThemeMode = PaysageThemeMode.SYSTEM,
    val colorScheme: PaysageColorScheme = PaysageColorScheme.DEFAULT,
    val oledPureBlack: Boolean = false
)

fun PaysageThemeMode.resolveDarkTheme(systemDarkTheme: Boolean): Boolean =
    when (this) {
        PaysageThemeMode.SYSTEM -> systemDarkTheme
        PaysageThemeMode.LIGHT -> false
        PaysageThemeMode.DARK -> true
    }

class AppearanceSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "paysage_appearance_settings",
        Context.MODE_PRIVATE
    )

    fun read(): PaysageAppearanceSettings =
        PaysageAppearanceSettings(
            themeMode = preferences.getString(KEY_THEME_MODE, PaysageThemeMode.SYSTEM.name)
                ?.let { raw -> runCatching { PaysageThemeMode.valueOf(raw) }.getOrNull() }
                ?: PaysageThemeMode.SYSTEM,
            colorScheme = preferences.getString(KEY_COLOR_SCHEME, PaysageColorScheme.DEFAULT.name)
                ?.let { raw -> runCatching { PaysageColorScheme.valueOf(raw) }.getOrNull() }
                ?: PaysageColorScheme.DEFAULT,
            oledPureBlack = preferences.getBoolean(KEY_OLED_PURE_BLACK, false)
        )

    fun write(settings: PaysageAppearanceSettings) {
        preferences.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_COLOR_SCHEME, settings.colorScheme.name)
            .putBoolean(KEY_OLED_PURE_BLACK, settings.oledPureBlack)
            .apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_COLOR_SCHEME = "color_scheme"
        private const val KEY_OLED_PURE_BLACK = "oled_pure_black"
    }
}
