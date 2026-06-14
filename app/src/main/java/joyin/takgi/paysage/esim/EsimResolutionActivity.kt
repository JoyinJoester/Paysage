package joyin.takgi.paysage.esim

import android.content.Intent
import android.content.IntentSender
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.euicc.EuiccManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.ui.theme.AppearanceSettingsStore
import joyin.takgi.paysage.ui.theme.PaysageTheme
import joyin.takgi.paysage.ui.theme.resolveDarkTheme
import joyin.takgi.paysage.ui.theme.withPaysageLocale

class EsimResolutionActivity : ComponentActivity() {
    private var requestId: String = ""

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withPaysageLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()

        setContent {
            val appearanceSettings = remember {
                AppearanceSettingsStore(this@EsimResolutionActivity).read()
            }
            val darkTheme = appearanceSettings.themeMode.resolveDarkTheme(isSystemInDarkTheme())

            PaysageTheme(
                darkTheme = darkTheme,
                colorScheme = appearanceSettings.colorScheme,
                oledPureBlack = appearanceSettings.oledPureBlack
            ) {
                ResolutionContent()
            }
        }

        if (savedInstanceState == null) {
            openResolution()
        }
    }

    private fun openResolution() {
        val resultIntent = intent.intentExtra(EXTRA_RESULT_INTENT)
        if (resultIntent == null) {
            EsimDownloadResultStore(this).write(
                EsimDownloadResultMapper.failure(
                    requestId,
                    getString(R.string.message_esim_resolution_missing)
                )
            )
            finish()
            return
        }

        val callbackIntent = EsimDownloadCallbackReceiver.pendingIntent(this, requestId)
        try {
            getSystemService(EuiccManager::class.java).startResolutionActivity(
                this,
                REQUEST_RESOLUTION,
                resultIntent,
                callbackIntent
            )
        } catch (_: IntentSender.SendIntentException) {
            EsimDownloadResultStore(this).write(
                EsimDownloadResultMapper.failure(
                    requestId,
                    getString(R.string.message_esim_resolution_open_once)
                )
            )
            finish()
        } catch (_: IllegalArgumentException) {
            EsimDownloadResultStore(this).write(
                EsimDownloadResultMapper.failure(
                    requestId,
                    getString(R.string.message_esim_resolution_invalid)
                )
            )
            finish()
        } catch (_: SecurityException) {
            EsimDownloadResultStore(this).write(
                EsimDownloadResultMapper.failure(
                    requestId,
                    getString(R.string.message_esim_resolution_denied)
                )
            )
            finish()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RESOLUTION) {
            finish()
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "joyin.takgi.paysage.esim.extra.REQUEST_ID"
        const val EXTRA_RESULT_INTENT = "joyin.takgi.paysage.esim.extra.RESULT_INTENT"
        private const val REQUEST_RESOLUTION = 7104
    }
}

@Composable
private fun ResolutionContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.message_esim_resolution_opening),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun Intent.intentExtra(key: String): Intent? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? Intent
    }
}
