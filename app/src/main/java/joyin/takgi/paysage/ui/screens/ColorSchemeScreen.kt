package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.ui.components.getColorSchemeName
import joyin.takgi.paysage.ui.theme.PaysageAppearanceSettings
import joyin.takgi.paysage.ui.theme.PaysageColorScheme

@Composable
fun ColorSchemeScreen(
    appearanceSettings: PaysageAppearanceSettings,
    onAppearanceSettingsChange: (PaysageAppearanceSettings) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.label_color_scheme),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.detail_color_scheme_selection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(PaysageColorScheme.entries) { scheme ->
                ColorSchemeOption(
                    name = getColorSchemeName(context, scheme),
                    colors = previewColors(scheme),
                    selected = appearanceSettings.colorScheme == scheme,
                    onClick = {
                        onAppearanceSettingsChange(appearanceSettings.copy(colorScheme = scheme))
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorSchemeOption(
    name: String,
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun previewColors(scheme: PaysageColorScheme): List<Color> =
    when (scheme) {
        PaysageColorScheme.DEFAULT -> listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
        PaysageColorScheme.OCEAN_TEAL -> listOf(
            Color(0xFF006A60),
            Color(0xFF275F8F),
            Color(0xFF8FE7D6)
        )
        PaysageColorScheme.SUNSET_ORANGE -> listOf(
            Color(0xFF8F4E2E),
            Color(0xFFFFB59D),
            Color(0xFF656032)
        )
        PaysageColorScheme.FOREST_GREEN -> listOf(
            Color(0xFF2E6B3A),
            Color(0xFFA6D4A5),
            Color(0xFF39656B)
        )
    }
