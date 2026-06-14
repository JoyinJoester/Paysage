package joyin.takgi.paysage.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.PaysageApplication
import joyin.takgi.paysage.ui.components.AppearanceSelectionSheet
import joyin.takgi.paysage.ui.components.M3eAlertDialog
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eSettingsNavigationItem
import joyin.takgi.paysage.ui.components.M3eSettingsSection
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.ui.components.getColorSchemeName
import joyin.takgi.paysage.ui.navigation.PaysageBottomBar
import joyin.takgi.paysage.ui.theme.AppLanguage
import joyin.takgi.paysage.ui.theme.LanguageSettingsStore
import joyin.takgi.paysage.ui.theme.PaysageAppearanceSettings
import joyin.takgi.paysage.ui.theme.withPaysageLocale
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onForwardingSettingsClick: () -> Unit,
    onEuiccSettingsClick: () -> Unit,
    onColorSchemeClick: () -> Unit,
    appearanceSettings: PaysageAppearanceSettings,
    onAppearanceSettingsChange: (PaysageAppearanceSettings) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languageStore = remember { LanguageSettingsStore(context) }
    var statusMessage by remember { mutableStateOf("") }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    fun updateAppearanceSettings(next: PaysageAppearanceSettings) {
        onAppearanceSettingsChange(next)
        statusMessage = context.getString(R.string.message_appearance_settings_saved)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(title = stringResource(R.string.screen_settings_title))
        },
        bottomBar = {
            PaysageBottomBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item {
                M3eSettingsSection(title = stringResource(R.string.section_appearance)) {
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.label_theme_mode),
                        subtitle = getThemeModeSubtitle(context, appearanceSettings),
                        onClick = { showAppearanceSheet = true }
                    )
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.Colorize,
                        title = stringResource(R.string.label_color_scheme),
                        subtitle = getColorSchemeName(context, appearanceSettings.colorScheme),
                        onClick = onColorSchemeClick
                    )
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.label_language),
                        subtitle = getLanguageDisplayName(currentLanguage),
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            item {
                M3eSettingsSection(title = stringResource(R.string.section_feature_settings)) {
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.section_forwarding_settings),
                        subtitle = stringResource(R.string.summary_forwarding_settings),
                        onClick = onForwardingSettingsClick
                    )
                    M3eSettingsNavigationItem(
                        icon = Icons.Default.Phone,
                        title = stringResource(R.string.section_euicc_settings),
                        subtitle = stringResource(R.string.summary_euicc_settings),
                        onClick = onEuiccSettingsClick
                    )
                }
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.section_app_info), style = MaterialTheme.typography.titleLarge)
                        InfoLine(stringResource(R.string.label_version), context.appVersionName())
                        InfoLine(stringResource(R.string.label_package_name), context.packageName)
                        if (statusMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAppearanceSheet) {
        AppearanceSelectionSheet(
            currentThemeMode = appearanceSettings.themeMode,
            oledPureBlack = appearanceSettings.oledPureBlack,
            onThemeModeSelected = { mode ->
                updateAppearanceSettings(appearanceSettings.copy(themeMode = mode))
            },
            onOledPureBlackChanged = { enabled ->
                updateAppearanceSettings(appearanceSettings.copy(oledPureBlack = enabled))
            },
            onDismiss = { showAppearanceSheet = false }
        )
    }

    if (showLanguageDialog) {
        M3eAlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.label_language)) },
            text = {
                Column {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    scope.launch {
                                        languageStore.write(language)
                                        PaysageApplication.setAppLocale(language)
                                        onLanguageChanged(language)
                                        statusMessage = context.withPaysageLocale(language)
                                            .getString(R.string.message_language_changed)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == language,
                                onClick = {
                                    scope.launch {
                                        languageStore.write(language)
                                        PaysageApplication.setAppLocale(language)
                                        onLanguageChanged(language)
                                        statusMessage = context.withPaysageLocale(language)
                                            .getString(R.string.message_language_changed)
                                    }
                                }
                            )
                            Text(
                                text = getLanguageDisplayName(language),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(android.R.string.ok))
                }
            }
        )
    }

}

@Composable
private fun getLanguageDisplayName(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.language_system_default)
        AppLanguage.ENGLISH -> "English"
        AppLanguage.CHINESE -> "简体中文"
        AppLanguage.JAPANESE -> "日本語"
        AppLanguage.RUSSIAN -> "Русский"
    }
}

private fun getThemeModeSubtitle(context: Context, settings: PaysageAppearanceSettings): String {
    val modeName = when (settings.themeMode) {
        joyin.takgi.paysage.ui.theme.PaysageThemeMode.SYSTEM -> context.getString(R.string.theme_mode_system)
        joyin.takgi.paysage.ui.theme.PaysageThemeMode.LIGHT -> context.getString(R.string.theme_mode_light)
        joyin.takgi.paysage.ui.theme.PaysageThemeMode.DARK -> context.getString(R.string.theme_mode_dark)
    }
    return if (settings.oledPureBlack && settings.themeMode == joyin.takgi.paysage.ui.theme.PaysageThemeMode.DARK) {
        context.getString(R.string.theme_mode_dark_with_oled, modeName)
    } else if (settings.oledPureBlack) {
        context.getString(R.string.theme_mode_oled_enabled, modeName)
    } else {
        modeName
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun Context.appVersionName(): String =
    try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: getString(R.string.version_unknown)
    } catch (_: PackageManager.NameNotFoundException) {
        getString(R.string.version_unknown)
    }
