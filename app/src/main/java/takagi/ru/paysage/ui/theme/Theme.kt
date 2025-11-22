package takagi.ru.paysage.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import takagi.ru.paysage.data.model.ColorScheme
import takagi.ru.paysage.data.model.ThemeMode

/**
 * Material 3 Expressive - Default Light Color Scheme
 */
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

/**
 * Material 3 Expressive - Default Dark Color Scheme
 */
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

// ============================================
// 🎨 海洋蓝 (Ocean Blue)
// ============================================
private val OceanBlueLightColorScheme = lightColorScheme(
    primary = OceanBluePrimary,
    onPrimary = OceanBlueOnPrimary,
    primaryContainer = OceanBluePrimaryContainer,
    onPrimaryContainer = OceanBlueOnPrimaryContainer,
    secondary = OceanBlueSecondary,
    onSecondary = OceanBlueOnSecondary,
    secondaryContainer = OceanBlueSecondaryContainer,
    onSecondaryContainer = OceanBlueOnSecondaryContainer,
    tertiary = OceanBlueTertiary,
    onTertiary = OceanBlueOnTertiary,
    tertiaryContainer = OceanBlueTertiaryContainer,
    onTertiaryContainer = OceanBlueOnTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    surface = OceanBlueSurface,
    onSurface = OceanBlueOnSurface,
    surfaceVariant = OceanBlueSurfaceVariant,
    onSurfaceVariant = OceanBlueOnSurfaceVariant,
    background = OceanBlueBackground,
    onBackground = OceanBlueOnBackground,
    outline = OceanBlueOutline,
    outlineVariant = OceanBlueOutlineVariant
)

private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = OceanBluePrimaryDark,
    onPrimary = OceanBlueOnPrimaryDark,
    primaryContainer = OceanBluePrimaryContainerDark,
    onPrimaryContainer = OceanBlueOnPrimaryContainerDark,
    secondary = OceanBlueSecondaryDark,
    onSecondary = OceanBlueOnSecondaryDark,
    secondaryContainer = OceanBlueSecondaryContainerDark,
    onSecondaryContainer = OceanBlueOnSecondaryContainerDark,
    tertiary = OceanBlueTertiaryDark,
    onTertiary = OceanBlueOnTertiaryDark,
    tertiaryContainer = OceanBlueTertiaryContainerDark,
    onTertiaryContainer = OceanBlueOnTertiaryContainerDark,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surface = OceanBlueSurfaceDark,
    onSurface = OceanBlueOnSurfaceDark,
    surfaceVariant = OceanBlueSurfaceVariantDark,
    onSurfaceVariant = OceanBlueOnSurfaceVariantDark,
    background = OceanBlueBackgroundDark,
    onBackground = OceanBlueOnBackgroundDark,
    outline = OceanBlueOutlineDark,
    outlineVariant = OceanBlueOutlineVariantDark
)

// ============================================
// 🌅 日落橙 (Sunset Orange)
// ============================================
private val SunsetOrangeLightColorScheme = lightColorScheme(
    primary = SunsetOrangePrimary,
    onPrimary = SunsetOrangeOnPrimary,
    primaryContainer = SunsetOrangePrimaryContainer,
    onPrimaryContainer = SunsetOrangeOnPrimaryContainer,
    secondary = SunsetOrangeSecondary,
    onSecondary = SunsetOrangeOnSecondary,
    secondaryContainer = SunsetOrangeSecondaryContainer,
    onSecondaryContainer = SunsetOrangeOnSecondaryContainer,
    tertiary = SunsetOrangeTertiary,
    onTertiary = SunsetOrangeOnTertiary,
    tertiaryContainer = SunsetOrangeTertiaryContainer,
    onTertiaryContainer = SunsetOrangeOnTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    surface = SunsetOrangeSurface,
    onSurface = SunsetOrangeOnSurface,
    surfaceVariant = SunsetOrangeSurfaceVariant,
    onSurfaceVariant = SunsetOrangeOnSurfaceVariant,
    background = SunsetOrangeBackground,
    onBackground = SunsetOrangeOnBackground,
    outline = SunsetOrangeOutline,
    outlineVariant = SunsetOrangeOutlineVariant
)

private val SunsetOrangeDarkColorScheme = darkColorScheme(
    primary = SunsetOrangePrimaryDark,
    onPrimary = SunsetOrangeOnPrimaryDark,
    primaryContainer = SunsetOrangePrimaryContainerDark,
    onPrimaryContainer = SunsetOrangeOnPrimaryContainerDark,
    secondary = SunsetOrangeSecondaryDark,
    onSecondary = SunsetOrangeOnSecondaryDark,
    secondaryContainer = SunsetOrangeSecondaryContainerDark,
    onSecondaryContainer = SunsetOrangeOnSecondaryContainerDark,
    tertiary = SunsetOrangeTertiaryDark,
    onTertiary = SunsetOrangeOnTertiaryDark,
    tertiaryContainer = SunsetOrangeTertiaryContainerDark,
    onTertiaryContainer = SunsetOrangeOnTertiaryContainerDark,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surface = SunsetOrangeSurfaceDark,
    onSurface = SunsetOrangeOnSurfaceDark,
    surfaceVariant = SunsetOrangeSurfaceVariantDark,
    onSurfaceVariant = SunsetOrangeOnSurfaceVariantDark,
    background = SunsetOrangeBackgroundDark,
    onBackground = SunsetOrangeOnBackgroundDark,
    outline = SunsetOrangeOutlineDark,
    outlineVariant = SunsetOrangeOutlineVariantDark
)

// ============================================
// 🌲 森林绿 (Forest Green)
// ============================================
private val ForestGreenLightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = ForestGreenOnPrimary,
    primaryContainer = ForestGreenPrimaryContainer,
    onPrimaryContainer = ForestGreenOnPrimaryContainer,
    secondary = ForestGreenSecondary,
    onSecondary = ForestGreenOnSecondary,
    secondaryContainer = ForestGreenSecondaryContainer,
    onSecondaryContainer = ForestGreenOnSecondaryContainer,
    tertiary = ForestGreenTertiary,
    onTertiary = ForestGreenOnTertiary,
    tertiaryContainer = ForestGreenTertiaryContainer,
    onTertiaryContainer = ForestGreenOnTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    surface = ForestGreenSurface,
    onSurface = ForestGreenOnSurface,
    surfaceVariant = ForestGreenSurfaceVariant,
    onSurfaceVariant = ForestGreenOnSurfaceVariant,
    background = ForestGreenBackground,
    onBackground = ForestGreenOnBackground,
    outline = ForestGreenOutline,
    outlineVariant = ForestGreenOutlineVariant
)

private val ForestGreenDarkColorScheme = darkColorScheme(
    primary = ForestGreenPrimaryDark,
    onPrimary = ForestGreenOnPrimaryDark,
    primaryContainer = ForestGreenPrimaryContainerDark,
    onPrimaryContainer = ForestGreenOnPrimaryContainerDark,
    secondary = ForestGreenSecondaryDark,
    onSecondary = ForestGreenOnSecondaryDark,
    secondaryContainer = ForestGreenSecondaryContainerDark,
    onSecondaryContainer = ForestGreenOnSecondaryContainerDark,
    tertiary = ForestGreenTertiaryDark,
    onTertiary = ForestGreenOnTertiaryDark,
    tertiaryContainer = ForestGreenTertiaryContainerDark,
    onTertiaryContainer = ForestGreenOnTertiaryContainerDark,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surface = ForestGreenSurfaceDark,
    onSurface = ForestGreenOnSurfaceDark,
    surfaceVariant = ForestGreenSurfaceVariantDark,
    onSurfaceVariant = ForestGreenOnSurfaceVariantDark,
    background = ForestGreenBackgroundDark,
    onBackground = ForestGreenOnBackgroundDark,
    outline = ForestGreenOutlineDark,
    outlineVariant = ForestGreenOutlineVariantDark
)

// ============================================
// 💜 科技紫 (Tech Purple)
// ============================================
private val TechPurpleLightColorScheme = lightColorScheme(
    primary = TechPurplePrimary,
    onPrimary = TechPurpleOnPrimary,
    primaryContainer = TechPurplePrimaryContainer,
    onPrimaryContainer = TechPurpleOnPrimaryContainer,
    secondary = TechPurpleSecondary,
    onSecondary = TechPurpleOnSecondary,
    secondaryContainer = TechPurpleSecondaryContainer,
    onSecondaryContainer = TechPurpleOnSecondaryContainer,
    tertiary = TechPurpleTertiary,
    onTertiary = TechPurpleOnTertiary,
    tertiaryContainer = TechPurpleTertiaryContainer,
    onTertiaryContainer = TechPurpleOnTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    surface = TechPurpleSurface,
    onSurface = TechPurpleOnSurface,
    surfaceVariant = TechPurpleSurfaceVariant,
    onSurfaceVariant = TechPurpleOnSurfaceVariant,
    background = TechPurpleBackground,
    onBackground = TechPurpleOnBackground,
    outline = TechPurpleOutline,
    outlineVariant = TechPurpleOutlineVariant
)

private val TechPurpleDarkColorScheme = darkColorScheme(
    primary = TechPurplePrimaryDark,
    onPrimary = TechPurpleOnPrimaryDark,
    primaryContainer = TechPurplePrimaryContainerDark,
    onPrimaryContainer = TechPurpleOnPrimaryContainerDark,
    secondary = TechPurpleSecondaryDark,
    onSecondary = TechPurpleOnSecondaryDark,
    secondaryContainer = TechPurpleSecondaryContainerDark,
    onSecondaryContainer = TechPurpleOnSecondaryContainerDark,
    tertiary = TechPurpleTertiaryDark,
    onTertiary = TechPurpleOnTertiaryDark,
    tertiaryContainer = TechPurpleTertiaryContainerDark,
    onTertiaryContainer = TechPurpleOnTertiaryContainerDark,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surface = TechPurpleSurfaceDark,
    onSurface = TechPurpleOnSurfaceDark,
    surfaceVariant = TechPurpleSurfaceVariantDark,
    onSurfaceVariant = TechPurpleOnSurfaceVariantDark,
    background = TechPurpleBackgroundDark,
    onBackground = TechPurpleOnBackgroundDark,
    outline = TechPurpleOutlineDark,
    outlineVariant = TechPurpleOutlineVariantDark
)

// ============================================
// 🐍 黑曼巴 (Black Mamba)
// ============================================
private val BlackMambaLightColorScheme = lightColorScheme(
    primary = BlackMambaPrimary,
    onPrimary = BlackMambaOnPrimary,
    primaryContainer = BlackMambaPrimaryContainer,
    onPrimaryContainer = BlackMambaOnPrimaryContainer,
    secondary = BlackMambaSecondary,
    onSecondary = BlackMambaOnSecondary,
    secondaryContainer = BlackMambaSecondaryContainer,
    onSecondaryContainer = BlackMambaOnSecondaryContainer,
    tertiary = BlackMambaTertiary,
    onTertiary = BlackMambaOnTertiary,
    tertiaryContainer = BlackMambaTertiaryContainer,
    onTertiaryContainer = BlackMambaOnTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    surface = BlackMambaSurface,
    onSurface = BlackMambaOnSurface,
    surfaceVariant = BlackMambaSurfaceVariant,
    onSurfaceVariant = BlackMambaOnSurfaceVariant,
    background = BlackMambaBackground,
    onBackground = BlackMambaOnBackground,
    outline = BlackMambaOutline,
    outlineVariant = BlackMambaOutlineVariant
)

private val BlackMambaDarkColorScheme = darkColorScheme(
    primary = BlackMambaPrimaryDark,
    onPrimary = BlackMambaOnPrimaryDark,
    primaryContainer = BlackMambaPrimaryContainerDark,
    onPrimaryContainer = BlackMambaOnPrimaryContainerDark,
    secondary = BlackMambaSecondaryDark,
    onSecondary = BlackMambaOnSecondaryDark,
    secondaryContainer = BlackMambaSecondaryContainerDark,
    onSecondaryContainer = BlackMambaOnSecondaryContainerDark,
    tertiary = BlackMambaTertiaryDark,
    onTertiary = BlackMambaOnTertiaryDark,
    tertiaryContainer = BlackMambaTertiaryContainerDark,
    onTertiaryContainer = BlackMambaOnTertiaryContainerDark,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surface = BlackMambaSurfaceDark,
    onSurface = BlackMambaOnSurfaceDark,
    surfaceVariant = BlackMambaSurfaceVariantDark,
    onSurfaceVariant = BlackMambaOnSurfaceVariantDark,
    background = BlackMambaBackgroundDark,
    onBackground = BlackMambaOnBackgroundDark,
    outline = BlackMambaOutlineDark,
    outlineVariant = BlackMambaOutlineVariantDark
)

// ============================================
// 🕴️ 小黑紫 (Grey Style)
// ============================================
private val GreyStyleLightColorScheme = lightColorScheme(
    primary = GreyStylePrimary,
    onPrimary = GreyStyleOnPrimary,
    primaryContainer = GreyStylePrimaryContainer,
    onPrimaryContainer = GreyStyleOnPrimaryContainer,
    secondary = GreyStyleSecondary,
    onSecondary = GreyStyleOnSecondary,
    secondaryContainer = GreyStyleSecondaryContainer,
    onSecondaryContainer = GreyStyleOnSecondaryContainer,
    tertiary = GreyStyleTertiary,
    onTertiary = GreyStyleOnTertiary,
    tertiaryContainer = GreyStyleTertiaryContainer,
    onTertiaryContainer = GreyStyleOnTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    surface = GreyStyleSurface,
    onSurface = GreyStyleOnSurface,
    surfaceVariant = GreyStyleSurfaceVariant,
    onSurfaceVariant = GreyStyleOnSurfaceVariant,
    background = GreyStyleBackground,
    onBackground = GreyStyleOnBackground,
    outline = GreyStyleOutline,
    outlineVariant = GreyStyleOutlineVariant
)

private val GreyStyleDarkColorScheme = darkColorScheme(
    primary = GreyStylePrimaryDark,
    onPrimary = GreyStyleOnPrimaryDark,
    primaryContainer = GreyStylePrimaryContainerDark,
    onPrimaryContainer = GreyStyleOnPrimaryContainerDark,
    secondary = GreyStyleSecondaryDark,
    onSecondary = GreyStyleOnSecondaryDark,
    secondaryContainer = GreyStyleSecondaryContainerDark,
    onSecondaryContainer = GreyStyleOnSecondaryContainerDark,
    tertiary = GreyStyleTertiaryDark,
    onTertiary = GreyStyleOnTertiaryDark,
    tertiaryContainer = GreyStyleTertiaryContainerDark,
    onTertiaryContainer = GreyStyleOnTertiaryContainerDark,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surface = GreyStyleSurfaceDark,
    onSurface = GreyStyleOnSurfaceDark,
    surfaceVariant = GreyStyleSurfaceVariantDark,
    onSurfaceVariant = GreyStyleOnSurfaceVariantDark,
    background = GreyStyleBackgroundDark,
    onBackground = GreyStyleOnBackgroundDark,
    outline = GreyStyleOutlineDark,
    outlineVariant = GreyStyleOutlineVariantDark
)

// ============================================
// 🎨 自定义方案
// ============================================
private fun customDarkColorScheme(primary: Long, secondary: Long, tertiary: Long) = darkColorScheme(
    primary = Color(primary),
    secondary = Color(secondary),
    tertiary = Color(tertiary)
)

private fun customLightColorScheme(primary: Long, secondary: Long, tertiary: Long) = lightColorScheme(
    primary = Color(primary),
    secondary = Color(secondary),
    tertiary = Color(tertiary)
)

/**
 * Material 3 Expressive Theme
 * 
 * @param themeMode 主题模式（浅色/深色/跟随系统）
 * @param colorScheme 配色方案
 * @param dynamicColor 是否使用动态颜色（Android 12+）
 * @param customPrimaryColor 自定义主色
 * @param customSecondaryColor 自定义次要色
 * @param customTertiaryColor 自定义第三色
 * @param content 内容组件
 */
@Composable
fun PaysageTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorScheme: ColorScheme = ColorScheme.DEFAULT,
    dynamicColor: Boolean = true,
    customPrimaryColor: Long = 0xFFFF6B35,
    customSecondaryColor: Long = 0xFF6A4C9C,
    customTertiaryColor: Long = 0xFF00BFA5,
    content: @Composable () -> Unit
) {
    // 根据 ThemeMode 决定是否使用深色主题
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val finalColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        colorScheme == ColorScheme.OCEAN_BLUE -> {
            if (darkTheme) OceanBlueDarkColorScheme else OceanBlueLightColorScheme
        }
        
        colorScheme == ColorScheme.SUNSET_ORANGE -> {
            if (darkTheme) SunsetOrangeDarkColorScheme else SunsetOrangeLightColorScheme
        }
        
        colorScheme == ColorScheme.FOREST_GREEN -> {
            if (darkTheme) ForestGreenDarkColorScheme else ForestGreenLightColorScheme
        }
        
        colorScheme == ColorScheme.TECH_PURPLE -> {
            if (darkTheme) TechPurpleDarkColorScheme else TechPurpleLightColorScheme
        }
        
        colorScheme == ColorScheme.BLACK_MAMBA -> {
            if (darkTheme) BlackMambaDarkColorScheme else BlackMambaLightColorScheme
        }
        
        colorScheme == ColorScheme.GREY_STYLE -> {
            if (darkTheme) GreyStyleDarkColorScheme else GreyStyleLightColorScheme
        }
        
        colorScheme == ColorScheme.CUSTOM -> {
            if (darkTheme) {
                customDarkColorScheme(customPrimaryColor, customSecondaryColor, customTertiaryColor)
            } else {
                customLightColorScheme(customPrimaryColor, customSecondaryColor, customTertiaryColor)
            }
        }
        
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
