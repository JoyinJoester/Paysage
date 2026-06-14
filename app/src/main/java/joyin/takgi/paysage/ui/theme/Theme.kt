@file:Suppress("DEPRECATION")

package joyin.takgi.paysage.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PaysageTeal80,
    onPrimary = PaysageInk10,
    primaryContainer = Color(0xFF004F47),
    onPrimaryContainer = PaysageMist90,
    secondary = PaysageBlue80,
    onSecondary = PaysageInk10,
    secondaryContainer = Color(0xFF173D5F),
    onSecondaryContainer = Color(0xFFD5E9FF),
    tertiary = PaysageViolet80,
    onTertiary = PaysageInk10,
    tertiaryContainer = Color(0xFF4E3B70),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = PaysageInk10,
    onBackground = PaysageMist90,
    surface = PaysageInk20,
    onSurface = PaysageMist90,
    surfaceVariant = Color(0xFF25312E),
    onSurfaceVariant = Color(0xFFC8D7D2),
    outline = Color(0xFF8BA09A),
    outlineVariant = Color(0xFF3B4945),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = PaysageTeal40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F2E4),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = PaysageBlue40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E7FF),
    onSecondaryContainer = Color(0xFF001D33),
    tertiary = PaysageViolet40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF211039),
    background = PaysageMist98,
    onBackground = PaysageInk90,
    surface = Color(0xFFFFFBFE),
    onSurface = PaysageInk90,
    surfaceVariant = PaysageMist92,
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7A75),
    outlineVariant = Color(0xFFBEC9C4)
)

private val SunsetOrangeLight = lightColorScheme(
    primary = SunsetOrange40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC9),
    onPrimaryContainer = Color(0xFF331000),
    secondary = Color(0xFF765847),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC9),
    onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF656032),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECE4AA),
    onTertiaryContainer = Color(0xFF1F1C00),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF4DED4),
    onSurfaceVariant = Color(0xFF52443D)
)

private val SunsetOrangeDark = darkColorScheme(
    primary = SunsetOrange80,
    onPrimary = Color(0xFF552000),
    primaryContainer = Color(0xFF743716),
    onPrimaryContainer = Color(0xFFFFDCC9),
    secondary = Color(0xFFE5BFA9),
    onSecondary = Color(0xFF43281D),
    secondaryContainer = Color(0xFF5C4032),
    onSecondaryContainer = Color(0xFFFFDCC9),
    tertiary = Color(0xFFCFC890),
    onTertiary = Color(0xFF353209),
    tertiaryContainer = Color(0xFF4C481D),
    onTertiaryContainer = Color(0xFFECE4AA),
    background = Color(0xFF201A17),
    onBackground = Color(0xFFECE0DB),
    surface = Color(0xFF201A17),
    onSurface = Color(0xFFECE0DB),
    surfaceVariant = Color(0xFF52443D),
    onSurfaceVariant = Color(0xFFD7C2B8)
)

private val ForestGreenLight = lightColorScheme(
    primary = ForestGreen40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8ECC6),
    onPrimaryContainer = Color(0xFF002105),
    secondary = Color(0xFF526350),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF39656B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF2),
    onTertiaryContainer = Color(0xFF001F24),
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940)
)

private val ForestGreenDark = darkColorScheme(
    primary = ForestGreen80,
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF105325),
    onPrimaryContainer = Color(0xFFC8ECC6),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3A4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFA1CED5),
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF1F4D53),
    onTertiaryContainer = Color(0xFFBCEBF2),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaysageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: PaysageColorScheme = PaysageColorScheme.DEFAULT,
    oledPureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when (colorScheme) {
        PaysageColorScheme.DEFAULT -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        PaysageColorScheme.OCEAN_TEAL -> if (darkTheme) DarkColorScheme else LightColorScheme
        PaysageColorScheme.SUNSET_ORANGE -> if (darkTheme) SunsetOrangeDark else SunsetOrangeLight
        PaysageColorScheme.FOREST_GREEN -> if (darkTheme) ForestGreenDark else ForestGreenLight
    }

    val colorScheme = if (darkTheme && oledPureBlack) {
        baseColorScheme.withPureBlackSurfaces()
    } else {
        baseColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.decorView.setBackgroundColor(colorScheme.background.toArgb())
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun androidx.compose.material3.ColorScheme.withPureBlackSurfaces(): androidx.compose.material3.ColorScheme {
    val pureBlack = Color(0xFF000000)
    val elevatedBlack = Color(0xFF050505)
    val outlineBlack = Color(0xFF141414)
    return copy(
        background = pureBlack,
        surface = pureBlack,
        surfaceDim = pureBlack,
        surfaceBright = elevatedBlack,
        surfaceContainerLowest = pureBlack,
        surfaceContainerLow = pureBlack,
        surfaceContainer = pureBlack,
        surfaceContainerHigh = elevatedBlack,
        surfaceContainerHighest = outlineBlack,
        inverseSurface = Color(0xFFF2F2F2),
        surfaceVariant = outlineBlack,
        outlineVariant = Color(0xFF1F1F1F),
        scrim = pureBlack
    )
}
