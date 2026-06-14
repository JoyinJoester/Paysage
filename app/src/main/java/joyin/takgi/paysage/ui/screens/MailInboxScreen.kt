package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.data.MailCommandRecordEntity
import joyin.takgi.paysage.data.MailTrustedSenderEntity
import joyin.takgi.paysage.mail.MailCommandAction
import joyin.takgi.paysage.mail.MailCommandDecisionCode
import joyin.takgi.paysage.mail.MailCommandDryRunResult
import joyin.takgi.paysage.mail.MailCommandRecordPrivacy
import joyin.takgi.paysage.mail.MailCommandTemplate
import joyin.takgi.paysage.mail.MailInboxDiagnostics
import joyin.takgi.paysage.mail.MailInboxAccountConfig
import joyin.takgi.paysage.mail.MailInboxFailureKind
import joyin.takgi.paysage.mail.MailInboxRecoveryAdvisor
import joyin.takgi.paysage.mail.MailInboxRealtimeSettings
import joyin.takgi.paysage.mail.MailInboxRealtimeSettingsStore
import joyin.takgi.paysage.mail.MailInboxRealtimeServiceController
import joyin.takgi.paysage.mail.MailInboxRepository
import joyin.takgi.paysage.mail.MailInboxRuntimeStatus
import joyin.takgi.paysage.mail.allowedActionSet
import joyin.takgi.paysage.mail.displayName
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eStatusPill
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.ui.motion.PaysageAnimatedPage
import joyin.takgi.paysage.util.DateFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class MailInboxFlow {
    Overview,
    AccountSettings,
    SetupWizard,
    TrustedSender
}

@Composable
fun MailInboxScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val repository = remember { MailInboxRepository(context) }
    val realtimeSettingsStore = remember { MailInboxRealtimeSettingsStore(context) }
    val scope = rememberCoroutineScope()
    val trustedSenders by repository.observeTrustedSenders().collectAsState(initial = emptyList())
    val records by repository.observeRecentRecords().collectAsState(initial = emptyList())
    var account by remember { mutableStateOf(repository.readAccount()) }
    var runtimeStatus by remember { mutableStateOf(repository.readRuntimeStatus()) }
    var realtimeSettings by remember { mutableStateOf(realtimeSettingsStore.read()) }
    var statusMessage by remember { mutableStateOf("") }
    var latestSecret by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentFlow by remember { mutableStateOf(MailInboxFlow.Overview) }
    var dryRunSender by remember { mutableStateOf("") }
    var dryRunBody by remember { mutableStateOf("") }
    var dryRunResult by remember { mutableStateOf<MailCommandDryRunResult?>(null) }
    var dryRunMessage by remember { mutableStateOf("") }
    var isDryRunning by remember { mutableStateOf(false) }
    var busyTrustedSenderIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun updateRealtimeSettings(next: MailInboxRealtimeSettings) {
        realtimeSettings = next
        realtimeSettingsStore.write(next)
        val started = MailInboxRealtimeServiceController.reconcile(context, account, next)
        statusMessage = when {
            MailInboxRealtimeServiceController.shouldRun(account, next) && started -> {
                context.getString(R.string.message_realtime_enabled)
            }
            MailInboxRealtimeServiceController.shouldRun(account, next) -> {
                context.getString(R.string.message_realtime_start_failed)
            }
            next.enabled -> {
                context.getString(R.string.message_config_inbox_first)
            }
            else -> {
                context.getString(R.string.message_realtime_disabled)
            }
        }
    }

    fun refreshNow() {
        scope.launch {
            isRefreshing = true
            try {
                val result = repository.refreshInbox()
                statusMessage = result.message
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                statusMessage = context.getString(R.string.message_mail_refresh_error)
            } finally {
                runtimeStatus = repository.readRuntimeStatus()
                isRefreshing = false
            }
        }
    }

    fun runDryRun() {
        scope.launch {
            isDryRunning = true
            dryRunMessage = ""
            try {
                dryRunResult = repository.dryRunCommand(
                    rawSender = dryRunSender,
                    body = dryRunBody
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                dryRunResult = null
                dryRunMessage = context.getString(R.string.message_dry_run_failed)
            } finally {
                isDryRunning = false
            }
        }
    }

    fun runTrustedSenderOperation(
        sender: MailTrustedSenderEntity,
        fallbackMessage: String,
        block: suspend () -> Unit
    ) {
        if (sender.id in busyTrustedSenderIds) return
        scope.launch {
            busyTrustedSenderIds = busyTrustedSenderIds + sender.id
            try {
                block()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                statusMessage = fallbackMessage
            } finally {
                busyTrustedSenderIds = busyTrustedSenderIds - sender.id
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = when (currentFlow) {
                    MailInboxFlow.Overview -> stringResource(R.string.screen_mail_inbox_title)
                    MailInboxFlow.AccountSettings -> stringResource(R.string.title_inbox_settings)
                    MailInboxFlow.SetupWizard -> stringResource(R.string.title_mail_command_wizard)
                    MailInboxFlow.TrustedSender -> stringResource(R.string.title_authorized_mailbox)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentFlow == MailInboxFlow.Overview) {
                                onBackClick()
                            } else {
                                currentFlow = MailInboxFlow.Overview
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (currentFlow == MailInboxFlow.Overview) {
                        IconButton(
                            onClick = ::refreshNow,
                            enabled = account.enabled && account.isConfigured && !isRefreshing
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh_inbox))
                        }
                    }
                }
            )
        }
    ) { padding ->
        PaysageAnimatedPage(
            targetState = currentFlow,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
            isForward = { initial, target -> target.ordinal > initial.ordinal }
        ) { flow ->
            when (flow) {
                MailInboxFlow.Overview -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            MailInboxStatusCard(
                                account = account,
                                trustedCount = trustedSenders.size,
                                forwardingPaused = repository.isForwardingPaused(),
                                runtimeStatus = runtimeStatus,
                                statusMessage = statusMessage,
                                latestSecret = latestSecret,
                                isRefreshing = isRefreshing,
                                onCopyLatestSecret = {
                                    clipboardManager.setText(AnnotatedString(latestSecret))
                                    latestSecret = ""
                                    statusMessage = context.getString(R.string.message_secret_copied)
                                },
                                onConfigure = { currentFlow = MailInboxFlow.AccountSettings },
                                onOpenWizard = { currentFlow = MailInboxFlow.SetupWizard },
                                onRefresh = ::refreshNow
                            )
                        }

                        item {
                            MailInboxRealtimeCard(
                                account = account,
                                settings = realtimeSettings,
                                onSettingsChange = ::updateRealtimeSettings
                            )
                        }

                        if (account.isConfigured) {
                            item {
                                TrustedSenderSectionHeader(
                                    hasSenders = trustedSenders.isNotEmpty(),
                                    onAddSender = { currentFlow = MailInboxFlow.TrustedSender }
                                )
                            }

                            if (trustedSenders.isNotEmpty()) {
                                items(trustedSenders, key = { it.id }) { sender ->
                                    TrustedSenderRow(
                                        sender = sender,
                                        isBusy = sender.id in busyTrustedSenderIds,
                                        onEnabledChange = { enabled ->
                                            runTrustedSenderOperation(sender, context.getString(R.string.message_sender_update_failed)) {
                                                repository.setSenderEnabled(sender, enabled)
                                                val status = if (enabled) context.getString(R.string.suffix_enabled) else context.getString(R.string.suffix_disabled)
                                                statusMessage = "${displayMailAddress(sender.email)} $status"
                                            }
                                        },
                                        onRotateSecret = {
                                            runTrustedSenderOperation(sender, context.getString(R.string.message_secret_rotate_failed)) {
                                                latestSecret = repository.rotateSenderSecret(sender)
                                                statusMessage = displayMailAddress(sender.email) + context.getString(R.string.suffix_secret_rotated)
                                            }
                                        },
                                        onDelete = {
                                            runTrustedSenderOperation(sender, context.getString(R.string.message_sender_delete_failed)) {
                                                repository.deleteTrustedSender(sender)
                                                latestSecret = ""
                                                statusMessage = context.getString(R.string.message_sender_deleted)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (trustedSenders.isNotEmpty()) {
                            item {
                                MailInboxSmokeAssistantCard(
                                    account = account,
                                    runtimeStatus = runtimeStatus,
                                    realtimeSettings = realtimeSettings,
                                    trustedSenders = trustedSenders,
                                    dryRunSender = dryRunSender,
                                    onDryRunSenderChange = {
                                        dryRunSender = it
                                        dryRunMessage = ""
                                    },
                                    dryRunBody = dryRunBody,
                                    onDryRunBodyChange = {
                                        dryRunBody = it
                                        dryRunMessage = ""
                                    },
                                    dryRunResult = dryRunResult,
                                    dryRunMessage = dryRunMessage,
                                    isDryRunning = isDryRunning,
                                    onDryRun = ::runDryRun,
                                    onCopy = { text, message ->
                                        clipboardManager.setText(AnnotatedString(text))
                                        statusMessage = message
                                    }
                                )
                            }

                            item {
                                RecentMailCommandRecords(records = records)
                            }
                        }
                    }
                }

                MailInboxFlow.AccountSettings -> {
                    MailAccountPage(
                        modifier = Modifier.padding(padding),
                        initial = account,
                        repository = repository,
                        onDismiss = { currentFlow = MailInboxFlow.Overview },
                        onSaved = { next ->
                            account = next
                            runtimeStatus = repository.readRuntimeStatus()
                            val started = MailInboxRealtimeServiceController.reconcile(
                                context = context,
                                account = next,
                                settings = realtimeSettings
                            )
                            statusMessage = when {
                                MailInboxRealtimeServiceController.shouldRun(next, realtimeSettings) && started -> {
                                    context.getString(R.string.message_mailbox_saved_realtime_enabled)
                                }
                                MailInboxRealtimeServiceController.shouldRun(next, realtimeSettings) -> {
                                    context.getString(R.string.message_mailbox_saved_realtime_failed)
                                }
                                else -> {
                                    context.getString(R.string.message_mailbox_saved)
                                }
                            }
                            currentFlow = MailInboxFlow.Overview
                        }
                    )
                }

                MailInboxFlow.SetupWizard -> {
                    MailInboxSetupWizardPage(
                        modifier = Modifier.padding(padding),
                        initial = account,
                        repository = repository,
                        onDismiss = { currentFlow = MailInboxFlow.Overview },
                        onCompleted = { nextAccount, secret, message ->
                            account = nextAccount
                            runtimeStatus = repository.readRuntimeStatus()
                            val started = MailInboxRealtimeServiceController.reconcile(
                                context = context,
                                account = nextAccount,
                                settings = realtimeSettings
                            )
                            latestSecret = secret
                            statusMessage = when {
                                MailInboxRealtimeServiceController.shouldRun(nextAccount, realtimeSettings) && started -> {
                                    message + context.getString(R.string.suffix_realtime_enabled)
                                }
                                MailInboxRealtimeServiceController.shouldRun(nextAccount, realtimeSettings) -> {
                                    message + context.getString(R.string.suffix_realtime_failed)
                                }
                                else -> {
                                    message
                                }
                            }
                            currentFlow = MailInboxFlow.Overview
                        }
                    )
                }

                MailInboxFlow.TrustedSender -> {
                    TrustedSenderPage(
                        modifier = Modifier.padding(padding),
                        repository = repository,
                        onDismiss = { currentFlow = MailInboxFlow.Overview },
                        onSaved = { secret ->
                            latestSecret = secret
                            statusMessage = context.getString(R.string.message_trusted_sender_saved)
                            currentFlow = MailInboxFlow.Overview
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MailInboxStatusCard(
    account: MailInboxAccountConfig,
    trustedCount: Int,
    forwardingPaused: Boolean,
    runtimeStatus: MailInboxRuntimeStatus,
    statusMessage: String,
    latestSecret: String,
    isRefreshing: Boolean,
    onCopyLatestSecret: () -> Unit,
    onConfigure: () -> Unit,
    onOpenWizard: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val ready = account.enabled && account.isConfigured && trustedCount > 0

    M3ePanel(modifier = Modifier.fillMaxWidth(), prominent = ready) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !account.isConfigured -> stringResource(R.string.title_connect_mailbox)
                            trustedCount == 0 -> stringResource(R.string.title_add_trusted_sender)
                            else -> stringResource(R.string.title_command_center_ready)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = when {
                            !account.isConfigured -> stringResource(R.string.detail_configure_imap_first)
                            trustedCount == 0 -> stringResource(R.string.detail_only_trusted_senders)
                            else -> stringResource(R.string.detail_security_enabled)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                M3eStatusPill(
                    text = if (ready) stringResource(R.string.state_available_short) else stringResource(R.string.status_needs_config),
                    active = ready,
                    icon = if (ready) Icons.Default.CheckCircle else Icons.Default.Warning
                )
            }

            InfoLine(stringResource(R.string.label_mailbox), if (account.isConfigured) displayMailAddress(account.username) else stringResource(R.string.status_not_configured))
            InfoLine(stringResource(R.string.label_trusted_senders), "$trustedCount ${stringResource(R.string.unit_items)}")
            InfoLine(stringResource(R.string.label_forward_status), if (forwardingPaused) stringResource(R.string.status_paused) else stringResource(R.string.status_running))
            InfoLine(
                stringResource(R.string.label_last_check),
                runtimeStatus.lastCheckAt
                    .takeIf { it > 0L }
                    ?.let(DateFormatter::format)
                    ?: stringResource(R.string.value_none)
            )
            val failureKind = when {
                !account.isConfigured -> MailInboxFailureKind.InvalidConfig
                !account.enabled -> MailInboxFailureKind.Disabled
                runtimeStatus.lastFailureKind == MailInboxFailureKind.CommandRejected &&
                    runtimeStatus.lastRejected > 0 -> MailInboxFailureKind.CommandRejected
                runtimeStatus.lastFailureAt > runtimeStatus.lastSuccessAt -> runtimeStatus.lastFailureKind
                else -> MailInboxFailureKind.None
            }
            MailInboxRecoveryAdvisor.adviceFor(context, failureKind)?.let { advice ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(advice.title, style = MaterialTheme.typography.titleSmall)
                    Text(advice.message, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (latestSecret.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.message_new_secret_generated),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onCopyLatestSecret) {
                        Text(stringResource(R.string.button_copy_new_secret))
                    }
                }
            }
            if (statusMessage.isNotBlank()) {
                Text(statusMessage, style = MaterialTheme.typography.bodySmall)
            }

            if (!account.isConfigured) {
                M3eActionButton(
                    text = stringResource(R.string.button_setup_wizard),
                    onClick = onOpenWizard,
                    modifier = Modifier.fillMaxWidth(),
                    prominent = true,
                    icon = Icons.Default.Email
                )
                OutlinedButton(
                    onClick = onConfigure,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_manual_config))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenWizard,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.button_wizard))
                    }
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f),
                        enabled = account.enabled && !isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRefreshing) stringResource(R.string.status_refreshing) else stringResource(R.string.action_refresh))
                    }
                }
                OutlinedButton(
                    onClick = onConfigure,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.button_mailbox_settings))
                }
            }
        }
    }
}

@Composable
private fun MailInboxRealtimeCard(
    account: MailInboxAccountConfig,
    settings: MailInboxRealtimeSettings,
    onSettingsChange: (MailInboxRealtimeSettings) -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(stringResource(R.string.title_realtime_monitoring), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.detail_realtime_monitoring),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { enabled ->
                        onSettingsChange(settings.copy(enabled = enabled))
                    },
                    enabled = account.enabled && account.isConfigured
                )
            }

            InfoLine(
                stringResource(R.string.label_mode),
                if (settings.enabled) stringResource(R.string.mode_realtime) else stringResource(R.string.mode_periodic)
            )
            InfoLine(
                stringResource(R.string.label_reconnect_interval),
                "${settings.idleReconnectMinutes} ${stringResource(R.string.unit_minutes)}"
            )
            InfoLine(
                stringResource(R.string.label_background_impact),
                if (settings.enabled) stringResource(R.string.impact_high_foreground) else stringResource(R.string.impact_low_periodic)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 30).forEach { minutes ->
                    OutlinedButton(
                        onClick = {
                            onSettingsChange(settings.copy(idleReconnectMinutes = minutes))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = settings.idleReconnectMinutes != minutes
                    ) {
                        Text("${minutes}${stringResource(R.string.suffix_minutes_short)}")
                    }
                }
            }

            if (!account.enabled || !account.isConfigured) {
                Text(
                    text = stringResource(R.string.message_realtime_needs_config),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MailInboxSetupWizardPage(
    modifier: Modifier = Modifier,
    initial: MailInboxAccountConfig,
    repository: MailInboxRepository,
    onDismiss: () -> Unit,
    onCompleted: (MailInboxAccountConfig, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var step by remember { mutableStateOf(0) }
    var host by remember { mutableStateOf(initial.host) }
    var port by remember { mutableStateOf(initial.port.toString()) }
    var username by remember { mutableStateOf(initial.username) }
    var password by remember { mutableStateOf(initial.password) }
    var useSsl by remember { mutableStateOf(initial.useSsl) }
    var senderEmail by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf(MailInboxRepository.generateSecret()) }
    var allowedActions by remember { mutableStateOf(setOf(MailCommandAction.Status)) }
    var message by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    val stepTitles = listOf(
        context.getString(R.string.step_connect_mailbox),
        context.getString(R.string.step_test_inbox),
        context.getString(R.string.step_trigger_mailbox),
        context.getString(R.string.step_allowed_actions),
        context.getString(R.string.step_security_confirmation)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.format_step_progress, step + 1, stepTitles.size, stepTitles[step]),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PaysageAnimatedPage(
            targetState = step,
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopStart,
            isForward = { initial, target -> target > initial }
        ) { currentStep ->
            when (currentStep) {
                0 -> WizardAccountStep(
                    host = host,
                    onHostChange = { host = it },
                    port = port,
                    onPortChange = { port = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    useSsl = useSsl,
                    onUseSslChange = { useSsl = it }
                )
                1 -> WizardTestStep(
                    account = MailInboxAccountConfig(
                        host = host,
                        port = port.toIntOrNull() ?: 993,
                        username = username,
                        password = password,
                        useSsl = useSsl,
                        enabled = true
                    ),
                    message = message,
                    isBusy = isBusy,
                    onTestConnection = {
                        scope.launch {
                            isBusy = true
                            try {
                                val next = MailInboxAccountConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 993,
                                    username = username,
                                    password = password,
                                    useSsl = useSsl,
                                    enabled = true
                                )
                                val result = repository.testConnection(next)
                                message = result.message
                                if (result.success) {
                                    repository.saveAccount(next)
                                }
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                message = context.getString(R.string.message_connection_test_failed)
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    onTestInbox = {
                        scope.launch {
                            isBusy = true
                            try {
                                val next = MailInboxAccountConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 993,
                                    username = username,
                                    password = password,
                                    useSsl = useSsl,
                                    enabled = true
                                )
                                repository.saveAccount(next)
                                message = repository.refreshInbox().message
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                message = context.getString(R.string.message_inbox_test_failed)
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                )
                2 -> WizardSenderStep(
                    senderEmail = senderEmail,
                    onSenderEmailChange = { senderEmail = it },
                    secret = secret,
                    onSecretChange = { secret = it },
                    onGenerateSecret = { secret = MailInboxRepository.generateSecret() },
                    onCopySecret = {
                        clipboardManager.setText(AnnotatedString(secret))
                        message = context.getString(R.string.message_secret_copied)
                    }
                )
                3 -> WizardActionStep(
                    allowedActions = allowedActions,
                    onAllowedActionsChange = { allowedActions = it }
                )
                4 -> WizardSecurityStep(
                    senderEmail = senderEmail,
                    secret = secret,
                    allowedActions = allowedActions
                )
            }
        }

        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    if (step == 0) {
                        onDismiss()
                    } else {
                        step -= 1
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (step == 0) stringResource(R.string.action_cancel) else stringResource(R.string.action_previous))
            }
            Button(
                onClick = {
                    if (step < stepTitles.lastIndex) {
                        step += 1
                        message = ""
                    } else {
                        scope.launch {
                            isBusy = true
                            try {
                                val next = MailInboxAccountConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 993,
                                    username = username,
                                    password = password,
                                    useSsl = useSsl,
                                    enabled = true
                                )
                                repository.saveAccount(next)
                                repository.saveTrustedSender(
                                    email = senderEmail,
                                    allowedActions = allowedActions,
                                    secret = secret,
                                    enabled = true
                                ).fold(
                                    onSuccess = {
                                        onCompleted(next, secret, context.getString(R.string.message_mail_command_rule_saved))
                                    },
                                    onFailure = { error ->
                                        message = error.message ?: context.getString(R.string.message_mail_command_rule_save_failed)
                                    }
                                )
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                message = context.getString(R.string.message_mail_command_rule_save_error)
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isBusy && canAdvanceWizardStep(
                    step = step,
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    senderEmail = senderEmail,
                    secret = secret
                )
            ) {
                Text(if (step == stepTitles.lastIndex) stringResource(R.string.action_complete) else stringResource(R.string.action_continue))
            }
        }
    }
}

@Composable
private fun WizardAccountStep(
    host: String,
    onHostChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    useSsl: Boolean,
    onUseSslChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = host,
            onValueChange = onHostChange,
            label = { Text(stringResource(R.string.label_imap_server)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text(stringResource(R.string.label_port)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.label_email_account)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.label_auth_code_or_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        SettingsSwitchLine(context.getString(R.string.label_ssl_tls), useSsl, onUseSslChange)
    }
}

@Composable
private fun WizardTestStep(
    account: MailInboxAccountConfig,
    message: String,
    isBusy: Boolean,
    onTestConnection: () -> Unit,
    onTestInbox: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoLine(context.getString(R.string.label_mailbox_short), account.username.takeIf { it.isNotBlank() }?.let(::displayMailAddress) ?: context.getString(R.string.value_not_filled))
        InfoLine(context.getString(R.string.label_server), "${account.host}:${account.port}")
        Text(
            text = context.getString(R.string.hint_test_connection_detail),
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onTestConnection,
                modifier = Modifier.weight(1f),
                enabled = !isBusy
            ) {
                Text(if (isBusy) context.getString(R.string.status_testing) else context.getString(R.string.button_test_connection))
            }
            OutlinedButton(
                onClick = onTestInbox,
                modifier = Modifier.weight(1f),
                enabled = !isBusy
            ) {
                Text(context.getString(R.string.button_test_inbox_short))
            }
        }
        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WizardSenderStep(
    senderEmail: String,
    onSenderEmailChange: (String) -> Unit,
    secret: String,
    onSecretChange: (String) -> Unit,
    onGenerateSecret: () -> Unit,
    onCopySecret: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = senderEmail,
            onValueChange = onSenderEmailChange,
            label = { Text(stringResource(R.string.label_trigger_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = secret,
            onValueChange = onSecretChange,
            label = { Text(stringResource(R.string.label_command_secret)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onGenerateSecret,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.button_regenerate))
            }
            OutlinedButton(
                onClick = onCopySecret,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.button_copy_secret))
            }
        }
    }
}

@Composable
private fun WizardActionStep(
    allowedActions: Set<MailCommandAction>,
    onAllowedActionsChange: (Set<MailCommandAction>) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.hint_grant_actions_only_needed), style = MaterialTheme.typography.bodySmall)
        MailCommandAction.entries.forEach { action ->
            ActionCheckboxRow(
                action = action,
                checked = action in allowedActions,
                onCheckedChange = { checked ->
                    val next = if (checked) {
                        allowedActions + action
                    } else {
                        (allowedActions - action).ifEmpty { setOf(MailCommandAction.Status) }
                    }
                    onAllowedActionsChange(next)
                }
            )
        }
    }
}

@Composable
private fun WizardSecurityStep(
    senderEmail: String,
    secret: String,
    allowedActions: Set<MailCommandAction>
) {
    val context = LocalContext.current
    val exampleCommand = remember {
        MailCommandTemplate.keyCommand(
            action = MailCommandAction.Status,
            key = MailCommandTemplate.KEY_PLACEHOLDER
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoLine(context.getString(R.string.label_trusted_senders), senderEmail.takeIf { it.isNotBlank() }?.let(::displayMailAddress) ?: context.getString(R.string.value_not_filled))
        InfoLine(
            context.getString(R.string.label_allowed_actions_list),
            allowedActions.joinToString(context.getString(R.string.separator_list)) { it.displayName(context) }
        )
        InfoLine(context.getString(R.string.label_secret_length), "${secret.length} ${context.getString(R.string.unit_characters)}")
        Text(
            text = context.getString(R.string.hint_command_security_format),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = exampleCommand,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun canAdvanceWizardStep(
    step: Int,
    host: String,
    port: String,
    username: String,
    password: String,
    senderEmail: String,
    secret: String
): Boolean =
    when (step) {
        0, 1 -> host.isNotBlank() &&
            port.toIntOrNull() in 1..65535 &&
            username.isNotBlank() &&
            password.isNotBlank()
        2, 4 -> senderEmail.isNotBlank() && secret.length >= 12
        else -> true
    }

@Composable
private fun TrustedSenderSectionHeader(
    hasSenders: Boolean,
    onAddSender: () -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_trusted_senders), style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (hasSenders) stringResource(R.string.detail_per_sender_permission) else stringResource(R.string.detail_add_first_sender),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FilledTonalButton(onClick = onAddSender) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
}

@Composable
private fun TrustedSenderRow(
    sender: MailTrustedSenderEntity,
    isBusy: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRotateSecret: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayMailAddress(sender.email), style = MaterialTheme.typography.titleMedium)
                    Text(
                        sender.allowedActionSet().joinToString(context.getString(R.string.separator_list)) {
                            it.displayName(context)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = sender.enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !isBusy
                )
            }
            if (isBusy) {
                Text(context.getString(R.string.status_processing), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRotateSecret,
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.button_refresh_secret))
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
private fun MailInboxSmokeAssistantCard(
    account: MailInboxAccountConfig,
    runtimeStatus: MailInboxRuntimeStatus,
    realtimeSettings: MailInboxRealtimeSettings,
    trustedSenders: List<MailTrustedSenderEntity>,
    dryRunSender: String,
    onDryRunSenderChange: (String) -> Unit,
    dryRunBody: String,
    onDryRunBodyChange: (String) -> Unit,
    dryRunResult: MailCommandDryRunResult?,
    dryRunMessage: String,
    isDryRunning: Boolean,
    onDryRun: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    val context = LocalContext.current
    val sender = trustedSenders.firstOrNull { it.enabled } ?: trustedSenders.first()
    val allowedActions = sender.allowedActionSet()
    val keyPlaceholder = MailCommandTemplate.KEY_PLACEHOLDER
    val readinessIssues = MailInboxDiagnostics.readinessIssues(account, trustedSenders)

    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.title_smoke_assistant), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.detail_smoke_assistant),
                style = MaterialTheme.typography.bodySmall
            )
            InfoLine(context.getString(R.string.label_test_sender), displayMailAddress(sender.email))
            InfoLine(
                context.getString(R.string.label_allowed_actions_list),
                allowedActions.joinToString(context.getString(R.string.separator_list)) { it.displayName(context) }
            )
            InfoLine(context.getString(R.string.label_acceptance_readiness),
                if (readinessIssues.isEmpty()) {
                    context.getString(R.string.status_ready_smoke)
                } else {
                    "${readinessIssues.size} ${context.getString(R.string.suffix_items_pending)}"
                }
            )
            if (readinessIssues.isNotEmpty()) {
                readinessIssues.take(3).forEach { issue ->
                    Text(
                        text = issue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SmokeCopyButton(
                text = context.getString(R.string.button_copy_status_query),
                onClick = {
                    onCopy(
                        MailCommandTemplate.keyCommand(
                            action = MailCommandAction.Status,
                            key = keyPlaceholder
                        ),
                        context.getString(R.string.message_status_template_copied)
                    )
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmokeCopyButton(
                    text = context.getString(R.string.button_wrong_key),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onCopy(
                            MailCommandTemplate.wrongKeyCommand(),
                            context.getString(R.string.message_wrong_key_copied)
                        )
                    }
                )
                SmokeCopyButton(
                    text = context.getString(R.string.button_expired_command),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onCopy(
                            MailCommandTemplate.expiredCommand(key = keyPlaceholder),
                            context.getString(R.string.message_expired_template_copied)
                        )
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmokeCopyButton(
                    text = context.getString(R.string.button_replay_pair),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val pair = MailCommandTemplate.replayPair(key = keyPlaceholder)
                        onCopy(
                            "${pair.first}\n\n--- send again ---\n\n${pair.second}",
                            context.getString(R.string.message_replay_template_copied)
                        )
                    }
                )
                SmokeCopyButton(
                    text = context.getString(R.string.button_pause_resume),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onCopy(
                            listOf(
                                MailCommandTemplate.keyCommand(
                                    action = MailCommandAction.PauseForwarding,
                                    key = keyPlaceholder
                                ),
                                MailCommandTemplate.keyCommand(
                                    action = MailCommandAction.ResumeForwarding,
                                    key = keyPlaceholder
                                )
                            ).joinToString("\n\n--- send after pause is observed ---\n\n"),
                            context.getString(R.string.message_pause_resume_copied)
                        )
                    }
                )
            }

            Text(
                text = context.getString(R.string.hint_hmac_smoke),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SmokeCopyButton(
                text = context.getString(R.string.button_copy_diagnostic_report),
                onClick = {
                    onCopy(
                        MailInboxDiagnostics.buildReport(
                            account = account,
                            runtimeStatus = runtimeStatus,
                            realtimeSettings = realtimeSettings,
                            trustedSenders = trustedSenders
                        ),
                        context.getString(R.string.message_diagnostic_copied)
                    )
                }
            )

            Text(stringResource(R.string.title_local_dry_run), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = dryRunSender,
                onValueChange = onDryRunSenderChange,
                label = { Text(stringResource(R.string.label_sender_email_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dryRunBody,
                onValueChange = onDryRunBodyChange,
                label = { Text(stringResource(R.string.label_mail_body)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )
            OutlinedButton(
                onClick = onDryRun,
                modifier = Modifier.fillMaxWidth(),
                enabled = dryRunSender.isNotBlank() && dryRunBody.isNotBlank() && !isDryRunning
            ) {
                Text(if (isDryRunning) stringResource(R.string.status_dry_running) else stringResource(R.string.button_run_dry_run))
            }
            if (dryRunMessage.isNotBlank()) {
                Text(dryRunMessage, style = MaterialTheme.typography.bodySmall)
            }
            dryRunResult?.let { result ->
                InfoLine(context.getString(R.string.label_dry_run_result), if (result.allowed) context.getString(R.string.result_allowed) else context.getString(R.string.result_rejected))
                InfoLine(context.getString(R.string.label_decision_code), result.code.name)
                InfoLine(context.getString(R.string.label_action), result.action?.displayName(context) ?: context.getString(R.string.value_none))
                Text(result.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SmokeCopyButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable
private fun RecentMailCommandRecords(records: List<MailCommandRecordEntity>) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.title_recent_records), style = MaterialTheme.typography.titleLarge)
            if (records.isEmpty()) {
                Text(stringResource(R.string.message_no_records), style = MaterialTheme.typography.bodySmall)
            } else {
                records.take(8).forEach { record ->
                    MailCommandRecordRow(record)
                }
            }
        }
    }
}

@Composable
private fun MailCommandRecordRow(record: MailCommandRecordEntity) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = MailCommandAction.fromWireName(record.action)?.displayName(context)
                    ?: decisionDisplayName(context, record.decisionCode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(record.resultMessage, style = MaterialTheme.typography.bodySmall)
            if (record.subject.isNotBlank()) {
                Text(record.subject, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (record.executed) context.getString(R.string.status_executed) else context.getString(R.string.status_ignored),
                style = MaterialTheme.typography.labelMedium,
                color = if (record.executed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = DateFormatter.format(record.processedAtMillis),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MailAccountPage(
    modifier: Modifier = Modifier,
    initial: MailInboxAccountConfig,
    repository: MailInboxRepository,
    onDismiss: () -> Unit,
    onSaved: (MailInboxAccountConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf(initial.host) }
    var port by remember { mutableStateOf(initial.port.toString()) }
    var username by remember { mutableStateOf(initial.username) }
    var password by remember { mutableStateOf(initial.password) }
    var useSsl by remember { mutableStateOf(initial.useSsl) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var isTesting by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text(stringResource(R.string.label_imap_server)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text(stringResource(R.string.label_port)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.label_email_account)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.label_auth_code_or_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        SettingsSwitchLine(context.getString(R.string.label_ssl_tls), useSsl) { useSsl = it }
        SettingsSwitchLine(context.getString(R.string.button_enable_inbox_check), enabled) { enabled = it }

        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isTesting = true
                        try {
                            val result = repository.testConnection(
                                MailInboxAccountConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 993,
                                    username = username,
                                    password = password,
                                    useSsl = useSsl,
                                    enabled = enabled
                                )
                            )
                            message = result.message
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            message = context.getString(R.string.message_connection_test_failed)
                        } finally {
                            isTesting = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isTesting && !isSaving
            ) {
                Text(if (isTesting) context.getString(R.string.status_testing) else context.getString(R.string.button_test_connection_short))
            }
        }
        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    try {
                        val next = MailInboxAccountConfig(
                            host = host,
                            port = port.toIntOrNull() ?: 993,
                            username = username,
                            password = password,
                            useSsl = useSsl,
                            enabled = enabled
                        )
                        repository.saveAccount(next)
                        onSaved(next)
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        message = context.getString(R.string.message_inbox_settings_save_failed)
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTesting && !isSaving
        ) {
            Text(if (isSaving) stringResource(R.string.status_saving) else stringResource(R.string.action_save))
        }
    }
}

@Composable
private fun TrustedSenderPage(
    modifier: Modifier = Modifier,
    repository: MailInboxRepository,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var email by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf(MailInboxRepository.generateSecret()) }
    var enabled by remember { mutableStateOf(true) }
    var allowedActions by remember { mutableStateOf(setOf(MailCommandAction.Status)) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.label_sender_email_short)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text(stringResource(R.string.label_command_secret)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    secret = MailInboxRepository.generateSecret()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_generate))
            }
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(secret))
                    message = context.getString(R.string.message_secret_copied)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_copy))
            }
        }
        Text(stringResource(R.string.label_allowed_commands), style = MaterialTheme.typography.titleMedium)
        MailCommandAction.entries.forEach { action ->
            ActionCheckboxRow(
                action = action,
                checked = action in allowedActions,
                onCheckedChange = { checked ->
                    allowedActions = if (checked) {
                        allowedActions + action
                    } else {
                        (allowedActions - action).ifEmpty { setOf(MailCommandAction.Status) }
                    }
                }
            )
        }
        SettingsSwitchLine(stringResource(R.string.label_enable), enabled) { enabled = it }

        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_cancel_button))
            }
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            repository.saveTrustedSender(
                                email = email,
                                allowedActions = allowedActions,
                                secret = secret,
                                enabled = enabled
                            ).fold(
                                onSuccess = { onSaved(secret) },
                                onFailure = { error ->
                                    message = error.message ?: context.getString(R.string.message_trusted_sender_save_failed)
                                }
                            )
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            message = context.getString(R.string.message_trusted_sender_save_error)
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                Text(if (isSaving) stringResource(R.string.status_saving) else stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun ActionCheckboxRow(
    action: MailCommandAction,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(action.displayName(context), style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSwitchLine(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun displayMailAddress(raw: String): String =
    MailCommandRecordPrivacy.redactAddress(raw)

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun decisionDisplayName(context: android.content.Context, code: String): String =
    when (runCatching { MailCommandDecisionCode.valueOf(code) }.getOrNull()) {
        MailCommandDecisionCode.SenderNotWhitelisted -> context.getString(R.string.decision_sender_not_whitelisted)
        MailCommandDecisionCode.NoCommand -> context.getString(R.string.decision_no_command)
        MailCommandDecisionCode.InvalidCommand -> context.getString(R.string.decision_invalid_command)
        MailCommandDecisionCode.InvalidAuthenticator -> context.getString(R.string.decision_invalid_authenticator)
        MailCommandDecisionCode.MissingAuthenticator -> context.getString(R.string.decision_missing_authenticator)
        MailCommandDecisionCode.Expired -> context.getString(R.string.decision_expired)
        MailCommandDecisionCode.ExpiresTooFar -> context.getString(R.string.decision_expires_too_far)
        MailCommandDecisionCode.NonceReused -> context.getString(R.string.decision_nonce_reused)
        MailCommandDecisionCode.ActionNotAllowed -> context.getString(R.string.decision_action_not_allowed)
        MailCommandDecisionCode.SenderDisabled -> context.getString(R.string.decision_sender_disabled)
        MailCommandDecisionCode.InvalidSender -> context.getString(R.string.decision_invalid_sender)
        MailCommandDecisionCode.Allowed -> context.getString(R.string.decision_allowed)
        null -> context.getString(R.string.decision_generic_record)
    }
