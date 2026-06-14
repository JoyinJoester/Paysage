package joyin.takgi.paysage.ui.components

import android.content.Context
import joyin.takgi.paysage.R
import joyin.takgi.paysage.ui.theme.PaysageColorScheme

fun getColorSchemeName(context: Context, scheme: PaysageColorScheme): String =
    context.getString(
        when (scheme) {
            PaysageColorScheme.DEFAULT -> R.string.color_scheme_monet
            PaysageColorScheme.OCEAN_TEAL -> R.string.color_scheme_ocean_teal
            PaysageColorScheme.SUNSET_ORANGE -> R.string.color_scheme_sunset_orange
            PaysageColorScheme.FOREST_GREEN -> R.string.color_scheme_forest_green
        }
    )
