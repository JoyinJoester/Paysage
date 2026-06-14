package joyin.takgi.paysage

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import joyin.takgi.paysage.ui.screens.AccountDetailScreen
import joyin.takgi.paysage.ui.screens.AccountsScreen
import joyin.takgi.paysage.ui.screens.ColorSchemeScreen
import joyin.takgi.paysage.ui.screens.EuiccSettingsScreen
import joyin.takgi.paysage.ui.screens.EsimScreen
import joyin.takgi.paysage.ui.screens.FilterScreen
import joyin.takgi.paysage.ui.screens.ForwardingSettingsScreen
import joyin.takgi.paysage.ui.screens.HomeScreen
import joyin.takgi.paysage.ui.screens.LogScreen
import joyin.takgi.paysage.ui.screens.MailInboxScreen
import joyin.takgi.paysage.ui.screens.SettingsScreen
import joyin.takgi.paysage.ui.navigation.PaysageTab
import joyin.takgi.paysage.ui.components.M3eScaffoldBackground
import joyin.takgi.paysage.ui.motion.paysageScreenEnter
import joyin.takgi.paysage.ui.motion.paysageScreenExit
import joyin.takgi.paysage.ui.theme.AppearanceSettingsStore
import joyin.takgi.paysage.ui.theme.LanguageSettingsStore
import joyin.takgi.paysage.ui.theme.PaysageTheme
import joyin.takgi.paysage.ui.theme.resolveDarkTheme
import joyin.takgi.paysage.ui.theme.withPaysageLocale
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import joyin.takgi.paysage.mail.MailInboxAccountStore
import joyin.takgi.paysage.mail.MailInboxReliabilityManager
import joyin.takgi.paysage.mail.MailInboxRealtimeSettingsStore
import joyin.takgi.paysage.mail.MailInboxRealtimeServiceController
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withPaysageLocale())
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestPermissions()
        SmsReliabilityManager.ensureScheduled(this)
        MailInboxReliabilityManager.ensureScheduled(this)
        restoreMailInboxRealtimeMode()

        setContent {
            val appearanceStore = remember { AppearanceSettingsStore(this@MainActivity) }
            val languageStore = remember { LanguageSettingsStore(this@MainActivity) }
            var appearanceSettings by remember { mutableStateOf(appearanceStore.read()) }
            var appLanguage by remember {
                mutableStateOf(runBlocking { languageStore.read() })
            }
            val localizedContext = remember(appLanguage) {
                this@MainActivity.withPaysageLocale(appLanguage)
            }
            val localizedConfiguration = remember(localizedContext) {
                Configuration(localizedContext.resources.configuration)
            }
            val darkTheme = appearanceSettings.themeMode.resolveDarkTheme(isSystemInDarkTheme())

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                PaysageTheme(
                    darkTheme = darkTheme,
                    colorScheme = appearanceSettings.colorScheme,
                    oledPureBlack = appearanceSettings.oledPureBlack
                ) {
                    M3eScaffoldBackground(modifier = Modifier.fillMaxSize()) {
                        var selectedTab by remember { mutableIntStateOf(0) }
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = "main",
                            enterTransition = { paysageScreenEnter() },
                            exitTransition = { paysageScreenExit() },
                            popEnterTransition = { paysageScreenEnter() },
                            popExitTransition = { paysageScreenExit() }
                        ) {
                            composable("main") {
                                when (PaysageTab.fromIndex(selectedTab)) {
                                    PaysageTab.Home -> HomeScreen(
                                        onSettingsClick = { selectedTab = PaysageTab.Settings.index },
                                        onFilterClick = { navController.navigate("filter") },
                                        onLogsClick = { navController.navigate("logs") },
                                        onMailInboxClick = { navController.navigate("mailInbox") },
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it }
                                    )
                                    PaysageTab.Esim -> EsimScreen(
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it }
                                    )
                                    PaysageTab.Settings -> SettingsScreen(
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it },
                                        onForwardingSettingsClick = { navController.navigate("forwardingSettings") },
                                        onEuiccSettingsClick = { navController.navigate("euiccSettings") },
                                        onColorSchemeClick = { navController.navigate("colorScheme") },
                                        appearanceSettings = appearanceSettings,
                                        onAppearanceSettingsChange = { nextSettings ->
                                            appearanceSettings = nextSettings
                                            appearanceStore.write(nextSettings)
                                        },
                                        currentLanguage = appLanguage,
                                        onLanguageChanged = { language ->
                                            appLanguage = language
                                        }
                                    )
                                }
                            }
                            composable("filter") {
                                FilterScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable("logs") {
                                LogScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable("accounts") {
                                AccountsScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onEditAccount = { navController.navigate("account/$it") },
                                    onAddAccount = { navController.navigate("account/0") }
                                )
                            }
                            composable("mailInbox") {
                                MailInboxScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable("forwardingSettings") {
                                ForwardingSettingsScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onAccountsClick = { navController.navigate("accounts") },
                                    onMailInboxClick = { navController.navigate("mailInbox") }
                                )
                            }
                            composable("euiccSettings") {
                                EuiccSettingsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            composable("colorScheme") {
                                ColorSchemeScreen(
                                    appearanceSettings = appearanceSettings,
                                    onAppearanceSettingsChange = { nextSettings ->
                                        appearanceSettings = nextSettings
                                        appearanceStore.write(nextSettings)
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            composable("account/{id}") { backStackEntry ->
                                val accountId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                                AccountDetailScreen(
                                    accountId = accountId,
                                    onBackClick = { navController.popBackStack() },
                                    onSaved = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun restoreMailInboxRealtimeMode() {
        val account = MailInboxAccountStore(this).read()
        val realtimeSettings = MailInboxRealtimeSettingsStore(this).read()
        MailInboxRealtimeServiceController.reconcile(this, account, realtimeSettings)
    }
}
