package joyin.takgi.paysage.esim

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.theme.AppearanceSettingsStore
import joyin.takgi.paysage.ui.theme.PaysageTheme
import joyin.takgi.paysage.ui.theme.resolveDarkTheme
import joyin.takgi.paysage.ui.theme.withPaysageLocale

class EsimActivationActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withPaysageLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initialInput = intent.getStringExtra(EXTRA_INITIAL_INPUT).orEmpty()

        setContent {
            val appearanceSettings = remember {
                AppearanceSettingsStore(this@EsimActivationActivity).read()
            }
            val darkTheme = appearanceSettings.themeMode.resolveDarkTheme(isSystemInDarkTheme())

            PaysageTheme(
                darkTheme = darkTheme,
                colorScheme = appearanceSettings.colorScheme,
                oledPureBlack = appearanceSettings.oledPureBlack
            ) {
                EsimActivationPage(
                    initialActivationInput = initialInput,
                    onBack = { finish() },
                    onSubmitted = { message ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_STATUS_MESSAGE, message)
                        )
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_INITIAL_INPUT = "joyin.takgi.paysage.extra.ACTIVATION_INITIAL_INPUT"
        const val EXTRA_STATUS_MESSAGE = "joyin.takgi.paysage.extra.ACTIVATION_STATUS_MESSAGE"

        fun intent(context: Context, initialActivationInput: String = ""): Intent =
            Intent(context, EsimActivationActivity::class.java)
                .putExtra(EXTRA_INITIAL_INPUT, initialActivationInput)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EsimActivationPage(
    initialActivationInput: String,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val gateway = remember { EsimSystemGateway(context) }
    val settingsStore = remember { EsimSettingsStore(context) }
    val settings = remember { settingsStore.read() }
    val supportState = remember { gateway.supportState() }
    val initialCode = remember(initialActivationInput) {
        activationCodeFromInput(initialActivationInput)
    }

    var smdpInput by remember(initialActivationInput) { mutableStateOf(initialCode?.smdpAddress.orEmpty()) }
    var matchingIdInput by remember(initialActivationInput) { mutableStateOf(initialCode?.matchingId.orEmpty()) }
    var confirmationInput by remember(initialActivationInput) { mutableStateOf(initialCode?.confirmationCode.orEmpty()) }
    var imeiInput by remember(initialActivationInput) { mutableStateOf(initialCode?.imei.orEmpty()) }
    var lpaOid by remember(initialActivationInput) { mutableStateOf(initialCode?.oid) }
    var confirmationCodeRequired by remember(initialActivationInput) {
        mutableStateOf(initialCode?.confirmationCodeRequired == true)
    }
    val initialConfirmationRequiredMessage = stringResource(R.string.message_lpa_split_requires_confirmation_initial)
    val initialSplitMessage = stringResource(R.string.message_lpa_split_initial)
    val initialSplitFailedMessage = stringResource(R.string.message_lpa_auto_split_failed)
    var statusMessage by remember(
        initialActivationInput,
        initialCode,
        initialConfirmationRequiredMessage,
        initialSplitMessage,
        initialSplitFailedMessage
    ) {
        mutableStateOf(
            when {
                initialActivationInput.isBlank() -> ""
                initialCode != null && initialCode.confirmationCodeRequired -> initialConfirmationRequiredMessage
                initialCode != null -> initialSplitMessage
                else -> initialSplitFailedMessage
            }
        )
    }
    val fullLpaRequiresConfirmationMessage = stringResource(R.string.message_lpa_split_full_requires_confirmation)
    val fullLpaSplitMessage = stringResource(R.string.message_lpa_split_full)

    fun applyActivationCode(code: EsimActivationCode, message: String) {
        smdpInput = code.smdpAddress.orEmpty()
        matchingIdInput = code.matchingId.orEmpty()
        lpaOid = code.oid
        confirmationCodeRequired = code.confirmationCodeRequired
        if (confirmationInput.isBlank()) {
            confirmationInput = code.confirmationCode.orEmpty()
        }
        statusMessage = message
    }

    fun handleActivationPaste(value: String): Boolean {
        val code = activationCodeFromInput(value) ?: return false
        val message = if (code.confirmationCodeRequired) {
            fullLpaRequiresConfirmationMessage
        } else {
            fullLpaSplitMessage
        }
        applyActivationCode(code, message)
        return true
    }

    val smdpError = remember(smdpInput) {
        smdpInput.takeIf { it.isNotBlank() }
            ?.let { EsimSmdpAddressValidator.error(EsimSmdpAddressNormalizer.normalize(it)) }
    }
    val imeiError = remember(imeiInput) {
        imeiInput.takeIf { it.isNotBlank() && it.length !in 14..16 }
            ?.let { esimText(R.string.message_common_imei_length_hint) }
    }
    val draft = remember(smdpInput, matchingIdInput, lpaOid, confirmationCodeRequired, imeiInput) {
        EsimActivationCodeComposer.compose(
            smdpAddress = smdpInput,
            matchingId = matchingIdInput,
            oid = lpaOid,
            confirmationCodeRequired = confirmationCodeRequired,
            imei = imeiInput
        )
    }
    val activationCode = remember(draft, confirmationInput, imeiInput) {
        draft.activationCode
            ?.withStandaloneConfirmationCode(confirmationInput)
            ?.withImei(imeiInput)
    }
    val preflight = activationCode?.let {
        EsimActivationCodePreflight.analyze(
            activationCode = it,
            standaloneConfirmationCodeProvided = confirmationInput.isNotBlank()
        )
    }
    val canDownload = supportState.canRequestProfileDownload &&
        activationCode?.isValid == true &&
        preflight?.hasErrors == false
    val confirmationLabel = if (confirmationCodeRequired) {
        stringResource(R.string.label_confirmation_code_required)
    } else {
        stringResource(R.string.label_confirmation_code_optional)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_add_external_esim)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 3.dp
            ) {
                Button(
                    onClick = {
                        activationCode?.let { code ->
                            val result = gateway.requestProfileDownload(
                                activationCode = code,
                                switchAfterDownload = settings.switchAfterDownload
                            )
                            onSubmitted(result.message)
                        }
                    },
                    enabled = canDownload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (settings.switchAfterDownload) {
                            stringResource(R.string.action_download_and_enable)
                        } else {
                            stringResource(R.string.action_download_only)
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.title_carrier_activation_info), style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = smdpInput,
                            onValueChange = { value ->
                                if (!handleActivationPaste(value)) {
                                    smdpInput = value.trim()
                                    statusMessage = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.label_smdp_server)) },
                            placeholder = { Text("smdp.example.com") },
                            isError = smdpError != null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        FieldMessage(smdpError)
                        SensitiveOutlinedTextField(
                            value = matchingIdInput,
                            onValueChange = { value ->
                                if (!handleActivationPaste(value)) {
                                    matchingIdInput = value.trim()
                                    statusMessage = ""
                                }
                            },
                            label = stringResource(R.string.label_activation_code_matching_id),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardType = KeyboardType.Text
                        )
                        SensitiveOutlinedTextField(
                            value = confirmationInput,
                            onValueChange = { confirmationInput = it.trim() },
                            modifier = Modifier.fillMaxWidth(),
                            label = confirmationLabel,
                            keyboardType = KeyboardType.Text,
                            isError = confirmationCodeRequired && confirmationInput.isBlank()
                        )
                        FieldMessage(
                            if (confirmationCodeRequired && confirmationInput.isBlank()) {
                                esimText(R.string.message_confirmation_code_required_for_lpa)
                            } else {
                                null
                            }
                        )
                        SensitiveOutlinedTextField(
                            value = imeiInput,
                            onValueChange = { value ->
                                imeiInput = value.filter { it.isDigit() }.take(16)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(R.string.label_imei_optional),
                            keyboardType = KeyboardType.NumberPassword,
                            isError = false
                        )
                        FieldMessage(imeiError)
                    }
                }
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.title_local_preflight), style = MaterialTheme.typography.titleLarge)
                        if (preflight == null) {
                            Text(
                                stringResource(R.string.message_fill_smdp_to_show_preflight),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(localizedText(preflight.summary), style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                        }
                    }
                }
            }

            preflight?.let { report ->
                items(report.checks) { check ->
                    M3ePanel(modifier = Modifier.fillMaxWidth(), elevated = false) {
                        ActivationCheckLine(check)
                    }
                }
            }

            if (!supportState.canRequestProfileDownload ||
                statusMessage.isNotBlank() ||
                (smdpInput.isNotBlank() && draft.activationCode == null)
            ) {
                item {
                    M3ePanel(modifier = Modifier.fillMaxWidth(), elevated = false) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!supportState.canRequestProfileDownload) {
                                Text(
                                    stringResource(R.string.message_system_esim_download_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (statusMessage.isNotBlank()) {
                                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (smdpInput.isNotBlank() && draft.activationCode == null) {
                                Text(localizedText(draft.message), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensitiveOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false
) {
    var visible by remember(label) { mutableStateOf(false) }
    val visibilityDescription = stringResource(
        if (visible) R.string.action_hide_sensitive_field else R.string.action_show_sensitive_field,
        label
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = visibilityDescription
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
private fun FieldMessage(message: EsimText?) {
    if (message != null) {
        Text(
            text = localizedText(message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ActivationCheckLine(check: EsimActivationCheck) {
    val label = when (check.severity) {
        EsimActivationCheckSeverity.Pass -> stringResource(R.string.status_preflight_pass)
        EsimActivationCheckSeverity.Warning -> stringResource(R.string.status_preflight_warning)
        EsimActivationCheckSeverity.Error -> stringResource(R.string.status_preflight_error)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label · ${localizedText(check.title)}", fontWeight = FontWeight.SemiBold)
        Text(localizedText(check.message), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun localizedText(text: EsimText): String =
    stringResource(text.resId, *text.args.toTypedArray())

private fun activationCodeFromInput(input: String): EsimActivationCode? {
    if (input.isBlank()) return null
    EsimActivationCodeExtractor.extract(input).activationCode?.let { return it }
    return runCatching { EsimActivationCodeParser.parse(input) }
        .getOrNull()
        ?.takeIf { it.isValid }
}
