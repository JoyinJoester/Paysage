@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package joyin.takgi.paysage.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import joyin.takgi.paysage.R
import joyin.takgi.paysage.esim.EsimActivationCodeExtractor
import joyin.takgi.paysage.esim.EsimActivationActivity
import joyin.takgi.paysage.esim.EsimDownloadHistoryPolicy
import joyin.takgi.paysage.esim.EsimDownloadResult
import joyin.takgi.paysage.esim.EsimDownloadResultStore
import joyin.takgi.paysage.esim.EsimDownloadStartResult
import joyin.takgi.paysage.esim.EsimEidFormatter
import joyin.takgi.paysage.esim.EsimEidReadResult
import joyin.takgi.paysage.esim.EsimEidReadStatus
import joyin.takgi.paysage.esim.EsimEuiccInfoSummary
import joyin.takgi.paysage.esim.EsimEuiccOperationErrorCatalog
import joyin.takgi.paysage.esim.EsimEuiccPortAvailability
import joyin.takgi.paysage.esim.EsimExperienceStore
import joyin.takgi.paysage.esim.EsimExternalProfileReadResult
import joyin.takgi.paysage.esim.EsimExternalProfileState
import joyin.takgi.paysage.esim.EsimExternalProfileSummary
import joyin.takgi.paysage.esim.EsimExternalProfileSource
import joyin.takgi.paysage.esim.EsimExternalEuiccDetailsResult
import joyin.takgi.paysage.esim.EsimExternalEuiccDetails
import joyin.takgi.paysage.esim.EsimExternalEuiccNotification
import joyin.takgi.paysage.esim.EsimIsdRProbeResult
import joyin.takgi.paysage.esim.EsimOmapiReaderSummary
import joyin.takgi.paysage.esim.EsimPersistentDiagnostics
import joyin.takgi.paysage.esim.EsimQrScannerActivity
import joyin.takgi.paysage.esim.EsimSettingsStore
import joyin.takgi.paysage.esim.EsimSubscriptionSummary
import joyin.takgi.paysage.esim.EsimSupportReportBuilder
import joyin.takgi.paysage.esim.EsimSupportReportInput
import joyin.takgi.paysage.esim.EsimSupportState
import joyin.takgi.paysage.esim.EsimSystemGateway
import joyin.takgi.paysage.esim.EsimText
import joyin.takgi.paysage.esim.EsimUsbCcidReaderSummary
import joyin.takgi.paysage.esim.EsimUserSettings
import joyin.takgi.paysage.esim.isdrAidListSummary
import joyin.takgi.paysage.esim.label
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3eAlertDialog
import joyin.takgi.paysage.ui.components.M3eDropdownMenu
import joyin.takgi.paysage.ui.components.M3eFabMenuAction
import joyin.takgi.paysage.ui.components.M3eFabMenuHost
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eStatusPill
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.ui.navigation.PaysageBottomBar
import joyin.takgi.paysage.util.DateFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EsimScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onEuiccSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val gateway = remember { EsimSystemGateway(context) }
    val araMAccessRuleSha1 = remember { gateway.araMAccessRuleSha1() }
    val resultStore = remember { EsimDownloadResultStore(context) }
    val settingsStore = remember { EsimSettingsStore(context) }
    val experienceStore = remember { EsimExperienceStore(context) }

    var settings by remember { mutableStateOf(settingsStore.read()) }
    var compatibilityChecked by rememberSaveable {
        mutableStateOf(experienceStore.hasCompletedCompatibilityCheck())
    }
    var supportState by remember { mutableStateOf<EsimSupportState?>(null) }
    var euiccInfo by remember { mutableStateOf<EsimEuiccInfoSummary?>(null) }
    var activeSubscriptions by remember { mutableStateOf(emptyList<EsimSubscriptionSummary>()) }
    var manageableSubscriptions by remember { mutableStateOf(emptyList<EsimSubscriptionSummary>()) }
    var externalProfileResult by remember {
        mutableStateOf(
            EsimExternalProfileReadResult(
                profiles = emptyList(),
                messages = emptyList(),
                summaryMessageOverride = context.getString(R.string.message_no_external_profiles_read)
            )
        )
    }
    var externalEuiccDetails by remember {
        mutableStateOf(
            EsimExternalEuiccDetailsResult(
                details = emptyList(),
                messages = emptyList(),
                summaryMessageOverride = context.getString(R.string.message_no_external_euicc_details_read)
            )
        )
    }
    var hasAttemptedExternalProfileRead by rememberSaveable { mutableStateOf(false) }
    var usbReaders by remember { mutableStateOf(emptyList<EsimUsbCcidReaderSummary>()) }
    var omapiReaders by remember { mutableStateOf<List<EsimOmapiReaderSummary>?>(null) }
    var lastResult by remember { mutableStateOf(resultStore.read()) }
    var history by remember { mutableStateOf(resultStore.readHistory()) }
    var eidResult by remember { mutableStateOf<EsimEidReadResult?>(null) }
    var hasPhoneStatePermission by remember { mutableStateOf(context.hasPhoneStatePermission()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var showAddMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<EsimSubscriptionSummary?>(null) }
    var selectedExternalProfile by remember { mutableStateOf<EsimExternalProfileSummary?>(null) }
    val usbAtrSummaries = remember { mutableStateMapOf<String, String>() }
    val usbIsdRResults = remember { mutableStateMapOf<String, EsimIsdRProbeResult>() }
    val omapiIsdRResults = remember { mutableStateMapOf<String, EsimIsdRProbeResult>() }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhoneStatePermission = granted
        scope.launch {
            refreshEsimState(
                context = context,
                gateway = gateway,
                resultStore = resultStore,
                settings = settings,
                hasPhoneStatePermission = granted,
                onLoading = { isLoading = it },
                onStatus = { statusMessage = it },
                onSupportState = { supportState = it },
                onEuiccInfo = { euiccInfo = it },
                onActiveSubscriptions = { activeSubscriptions = it },
                onManageableSubscriptions = { manageableSubscriptions = it },
                onUsbReaders = { usbReaders = it },
                onOmapiReaders = { omapiReaders = it },
                onLastResult = { lastResult = it },
                onHistory = { history = it }
            )
        }
    }

    val activationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            statusMessage = result.data
                ?.getStringExtra(EsimActivationActivity.EXTRA_STATUS_MESSAGE)
                .orEmpty()
                .ifBlank { context.getString(R.string.message_esim_add_submitted) }
            lastResult = resultStore.read()
            history = resultStore.readHistory()
            if (settings.refreshAfterSystemOperation) {
                scope.launch {
                    settings = settingsStore.read()
                    hasPhoneStatePermission = context.hasPhoneStatePermission()
                    refreshEsimState(
                context = context,
                gateway = gateway,
                        resultStore = resultStore,
                        settings = settings,
                        hasPhoneStatePermission = hasPhoneStatePermission,
                        onLoading = { isLoading = it },
                        onStatus = { statusMessage = it },
                        onSupportState = { supportState = it },
                        onEuiccInfo = { euiccInfo = it },
                        onActiveSubscriptions = { activeSubscriptions = it },
                        onManageableSubscriptions = { manageableSubscriptions = it },
                        onUsbReaders = { usbReaders = it },
                        onOmapiReaders = { omapiReaders = it },
                        onLastResult = { lastResult = it },
                        onHistory = { history = it }
                    )
                }
            }
        }
    }

    fun openActivationPage(initialInput: String = "") {
        activationLauncher.launch(
            EsimActivationActivity.intent(
                context = context,
                initialActivationInput = initialInput
            )
        )
    }

    fun openActivationFromQrPayload(payload: String, sourceLabel: String) {
        val draft = EsimActivationCodeExtractor.extract(payload)
        statusMessage = "$sourceLabel: ${draft.message.resolve(context)}"
        openActivationPage(draft.activationCode?.normalizedLpa ?: payload.trim())
    }

    val qrScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val content = result.data
                ?.getStringExtra(EsimQrScannerActivity.EXTRA_QR_CONTENT)
                .orEmpty()
            if (content.isNotBlank()) {
                val sourceLabel = result.data
                    ?.getStringExtra(EsimQrScannerActivity.EXTRA_QR_SOURCE)
                    .orEmpty()
                    .ifBlank { context.getString(R.string.source_qr_scan) }
                openActivationFromQrPayload(content, sourceLabel)
            }
        }
    }

    suspend fun refresh(markChecked: Boolean = false) {
        settings = settingsStore.read()
        hasPhoneStatePermission = context.hasPhoneStatePermission()
        refreshEsimState(
                context = context,
                gateway = gateway,
            resultStore = resultStore,
            settings = settings,
            hasPhoneStatePermission = hasPhoneStatePermission,
            onLoading = { isLoading = it },
            onStatus = { statusMessage = it },
            onSupportState = { supportState = it },
            onEuiccInfo = { euiccInfo = it },
            onActiveSubscriptions = { activeSubscriptions = it },
            onManageableSubscriptions = { manageableSubscriptions = it },
            onUsbReaders = { usbReaders = it },
            onOmapiReaders = { omapiReaders = it },
            onLastResult = { lastResult = it },
            onHistory = { history = it }
        )
        if (markChecked) {
            experienceStore.markCompatibilityChecked()
            compatibilityChecked = true
        }
    }

    suspend fun loadExternalProfiles() {
        isLoading = true
        hasAttemptedExternalProfileRead = true
        statusMessage = context.getString(R.string.message_reading_external_profiles)
        externalProfileResult = EsimExternalProfileReadResult(
            profiles = emptyList(),
            messages = listOf(statusMessage),
            summaryMessageOverride = statusMessage
        )
        externalEuiccDetails = EsimExternalEuiccDetailsResult(
            details = emptyList(),
            messages = listOf(statusMessage),
            summaryMessageOverride = statusMessage
        )
        val result = try {
            gateway.externalProfileSummaries()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val message = error.message ?: context.getString(R.string.message_external_profiles_read_failed)
            EsimExternalProfileReadResult(
                profiles = emptyList(),
                messages = listOf(message),
                summaryMessageOverride = message
            )
        }
        externalProfileResult = result
        statusMessage = result.summaryMessage
        isLoading = false
    }

    fun loadExternalDetails() {
        scope.launch {
            isLoading = true
            statusMessage = context.getString(R.string.message_reading_external_profiles)
            externalEuiccDetails = try {
                gateway.externalEuiccDetails()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = error.message ?: context.getString(R.string.message_external_euicc_details_read_failed)
                EsimExternalEuiccDetailsResult(
                    details = emptyList(),
                    messages = listOf(message),
                    summaryMessageOverride = message
                )
            }
            statusMessage = externalEuiccDetails.summaryMessage
            isLoading = false
        }
    }

    fun switchExternalProfile(profile: EsimExternalProfileSummary) {
        scope.launch {
            isLoading = true
            statusMessage = context.getString(R.string.message_esim_external_switching)
            val result = gateway.requestSwitchExternalProfile(profile.commandId)
            statusMessage = result.message
            lastResult = resultStore.read()
            history = resultStore.readHistory()
            if (result.started && settings.refreshAfterSystemOperation) {
                loadExternalProfiles()
            } else {
                isLoading = false
            }
        }
    }

    fun runExternalProfileOperation(
        profile: EsimExternalProfileSummary,
        loadingMessage: String,
        operation: suspend () -> EsimDownloadStartResult
    ) {
        scope.launch {
            isLoading = true
            selectedExternalProfile = null
            statusMessage = loadingMessage
            val result = operation()
            statusMessage = result.message
            lastResult = resultStore.read()
            history = resultStore.readHistory()
            if (result.started && settings.refreshAfterSystemOperation) {
                loadExternalProfiles()
            } else {
                isLoading = false
            }
        }
    }

    fun runExternalNotificationOperation(
        loadingMessage: String,
        operation: suspend () -> EsimDownloadStartResult
    ) {
        scope.launch {
            isLoading = true
            statusMessage = loadingMessage
            val result = operation()
            statusMessage = result.message
            externalEuiccDetails = try {
                gateway.externalEuiccDetails()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = error.message ?: context.getString(R.string.message_external_euicc_details_read_failed)
                EsimExternalEuiccDetailsResult(
                    details = emptyList(),
                    messages = listOf(message),
                    summaryMessageOverride = message
                )
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (compatibilityChecked && settings.runCompatibilityCheckOnOpen) {
            refresh()
        }
    }

    val reportInput = supportState?.let { state ->
        euiccInfo?.let { info ->
            EsimSupportReportInput(
                supportState = state,
                euiccInfo = info,
                lastResult = lastResult,
                history = history,
                usbReaders = usbReaders,
                omapiReaders = omapiReaders,
                usbIsdRResults = usbIsdRResults.toMap(),
                omapiIsdRResults = omapiIsdRResults.toMap(),
                aidListSummary = settings.isdrAidListSummary(),
                es10xMss = settings.es10xMss,
                showNonOperationalProfiles = settings.showNonOperationalProfiles,
                autoHandleExternalNotifications = settings.autoHandleExternalNotifications,
                removeHandledExternalNotifications = settings.removeHandledExternalNotifications,
                sgp22IsdRReady = usbIsdRResults.values.any { it.success } ||
                    omapiIsdRResults.values.any { it.success },
                externalProfileCount = externalProfileResult.profiles.size,
                externalSourceCount = externalProfileResult.availableSources.size,
                externalNotificationCount = externalEuiccDetails.details.sumOf { detail ->
                    detail.notifications.size
                },
                externalNotificationSourceCount = externalEuiccDetails.details.count { detail ->
                    detail.notifications.isNotEmpty()
                },
                externalProfileReadSummary = externalProfileResult.summaryMessage,
                externalVendorSummaries = externalEuiccDetails.details.mapNotNull { detail ->
                    detail.info?.vendor?.let { vendor ->
                        "${detail.source.label}: ${vendor.name} / ${vendor.model} / FW ${vendor.firmwareVersion}"
                    }
                },
                persistentDiagnostics = EsimPersistentDiagnostics.read(context),
                paysageAraMSha1 = araMAccessRuleSha1
            )
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_esim_title),
                actions = {
                    if (compatibilityChecked) {
                        IconButton(
                            onClick = { scope.launch { refresh(markChecked = true) } },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh_euicc_status))
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more_euicc_operations))
                            }
                            M3eDropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_euicc_details)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDetailsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_advanced_diagnostics)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDiagnosticsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.section_euicc_settings)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onEuiccSettingsClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_system_fallback_entry)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        if (activity != null && !gateway.openManagement(activity)) {
                                            statusMessage = context.getString(R.string.message_cannot_open_system_esim)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_support_report)) },
                                    enabled = reportInput != null,
                                    onClick = {
                                        showOverflowMenu = false
                                        showSupportDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            PaysageBottomBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!compatibilityChecked) {
                CompatibilityStartScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    isLoading = isLoading,
                    statusMessage = statusMessage,
                    onStart = { scope.launch { refresh(markChecked = true) } }
                )
            } else {
                M3eFabMenuHost(
                    expanded = showAddMenu,
                    onExpandedChange = { showAddMenu = it },
                    collapsedIcon = Icons.Default.Add,
                    expandedIcon = Icons.Default.Close,
                    contentDescription = if (showAddMenu) stringResource(R.string.action_close_add_menu) else stringResource(R.string.action_add_external_esim),
                    actions = listOf(
                        M3eFabMenuAction(
                            text = stringResource(R.string.action_scan_qr_add),
                            icon = Icons.Default.QrCodeScanner,
                            onClick = {
                                qrScannerLauncher.launch(
                                    Intent(context, EsimQrScannerActivity::class.java)
                                )
                            }
                        ),
                        M3eFabMenuAction(
                            text = stringResource(R.string.action_enter_activation_code),
                            icon = Icons.Default.Edit,
                            onClick = {
                                openActivationPage()
                            }
                        )
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    EsimWorkspace(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        isLoading = isLoading,
                        statusMessage = statusMessage,
                        supportState = supportState,
                        externalProfiles = externalProfileResult.profiles,
                        externalProfileMessage = externalProfileResult.summaryMessage,
                        hasAttemptedExternalProfileRead = hasAttemptedExternalProfileRead,
                        manageableSubscriptions = manageableSubscriptions,
                        usbReaders = usbReaders,
                        omapiReaders = omapiReaders,
                        araMAccessRuleSha1 = araMAccessRuleSha1,
                        lastResult = lastResult,
                        onRequestUsbPermission = { reader ->
                            statusMessage = gateway.requestUsbPermission(reader.deviceName).message
                        },
                        onReadExternalProfiles = { scope.launch { loadExternalProfiles() } },
                        onSwitchExternalProfile = ::switchExternalProfile,
                        onManageExternalProfile = { selectedExternalProfile = it },
                        onOpenDetails = {
                            showDetailsDialog = true
                            loadExternalDetails()
                        }
                    )
                }
            }
        }
    }

    if (showDetailsDialog) {
        EsimDetailsDialog(
            euiccInfo = euiccInfo,
            eidResult = eidResult,
            externalProfiles = externalProfileResult.profiles,
            externalEuiccDetails = externalEuiccDetails,
            manageableSubscriptions = manageableSubscriptions,
            activeSubscriptions = activeSubscriptions,
            hasPhoneStatePermission = hasPhoneStatePermission,
            lastResult = lastResult,
            history = history,
            showHistory = settings.showOperationHistory,
            canOpenSupport = reportInput != null,
            onDismiss = { showDetailsDialog = false },
            onReadEid = { eidResult = gateway.readEidOnUserRequest() },
            onCopyEid = {
                val eid = eidResult?.eid.orEmpty()
                if (eid.isNotBlank()) context.copyPlainText("Paysage EID", eid)
            },
            onRequestPhonePermission = {
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            },
            onManageProfile = { selectedProfile = it },
            onHandleNotification = { notification ->
                runExternalNotificationOperation(
                    loadingMessage = context.getString(R.string.message_external_notification_handling)
                ) {
                    gateway.requestHandleExternalNotification(notification.commandId)
                }
            },
            onDeleteNotification = { notification ->
                runExternalNotificationOperation(
                    loadingMessage = context.getString(R.string.message_external_notification_deleting)
                ) {
                    gateway.requestDeleteExternalNotification(notification.commandId)
                }
            },
            onHandleAllNotifications = { source ->
                runExternalNotificationOperation(
                    loadingMessage = context.getString(R.string.message_external_notification_handling)
                ) {
                    gateway.requestHandleAllExternalNotifications(source.identity)
                }
            },
            onDeleteAllNotifications = { source ->
                runExternalNotificationOperation(
                    loadingMessage = context.getString(R.string.message_external_notification_deleting)
                ) {
                    gateway.requestDeleteAllExternalNotifications(source.identity)
                }
            },
            onClearHistory = {
                resultStore.clearHistory()
                history = emptyList()
                statusMessage = context.getString(R.string.message_esim_history_cleared)
            },
            onOpenSupport = { showSupportDialog = true }
        )
    }

    selectedProfile?.let { profile ->
        ProfileActionDialog(
            subscription = profile,
            availablePorts = euiccInfo?.ports.orEmpty()
                .filter { it.availability != EsimEuiccPortAvailability.Unavailable }
                .map { it.portIndex },
            onDismiss = { selectedProfile = null },
            onSwitch = { portIndex ->
                val result = gateway.requestSwitchSubscription(
                    subscriptionId = profile.subscriptionId,
                    portIndex = portIndex,
                    cardId = profile.cardId
                )
                statusMessage = result.message
                lastResult = resultStore.read()
                history = resultStore.readHistory()
                selectedProfile = null
                if (settings.refreshAfterSystemOperation) {
                    scope.launch { refresh() }
                }
            },
            onRename = { nickname ->
                val result = gateway.requestRenameSubscription(
                    subscriptionId = profile.subscriptionId,
                    nickname = nickname,
                    cardId = profile.cardId
                )
                statusMessage = result.message
                lastResult = resultStore.read()
                history = resultStore.readHistory()
                selectedProfile = null
                if (settings.refreshAfterSystemOperation) {
                    scope.launch { refresh() }
                }
            },
            onDelete = {
                val result = gateway.requestDeleteSubscription(
                    subscriptionId = profile.subscriptionId,
                    cardId = profile.cardId
                )
                statusMessage = result.message
                lastResult = resultStore.read()
                history = resultStore.readHistory()
                selectedProfile = null
                if (settings.refreshAfterSystemOperation) {
                    scope.launch { refresh() }
                }
            }
        )
    }

    selectedExternalProfile?.let { profile ->
        ExternalProfileActionDialog(
            profile = profile,
            onDismiss = { selectedExternalProfile = null },
            onEnable = {
                runExternalProfileOperation(
                    profile = profile,
                    loadingMessage = context.getString(R.string.message_esim_external_switching)
                ) {
                    gateway.requestSwitchExternalProfile(profile.commandId)
                }
            },
            onDisable = {
                runExternalProfileOperation(
                    profile = profile,
                    loadingMessage = context.getString(R.string.message_external_profile_disabling)
                ) {
                    gateway.requestDisableExternalProfile(profile.commandId)
                }
            },
            onRename = { nickname ->
                runExternalProfileOperation(
                    profile = profile,
                    loadingMessage = context.getString(R.string.message_external_profile_renaming)
                ) {
                    gateway.requestRenameExternalProfile(profile.commandId, nickname)
                }
            },
            onDelete = {
                runExternalProfileOperation(
                    profile = profile,
                    loadingMessage = context.getString(R.string.message_external_profile_deleting)
                ) {
                    gateway.requestDeleteExternalProfile(profile.commandId)
                }
            },
            onMemoryReset = {
                runExternalProfileOperation(
                    profile = profile,
                    loadingMessage = context.getString(R.string.message_external_euicc_memory_resetting)
                ) {
                    gateway.requestResetExternalEuicc(profile.commandId)
                }
            }
        )
    }

    if (showDiagnosticsDialog) {
        DiagnosticsDialog(
            usbReaders = usbReaders,
            omapiReaders = omapiReaders,
            usbAtrSummaries = usbAtrSummaries,
            usbIsdRResults = usbIsdRResults,
            omapiIsdRResults = omapiIsdRResults,
            onDismiss = { showDiagnosticsDialog = false },
            onRefresh = { scope.launch { refresh() } },
            onRequestUsbPermission = { reader ->
                statusMessage = gateway.requestUsbPermission(reader.deviceName).message
            },
            onReadAtr = { reader ->
                scope.launch {
                    statusMessage = context.getString(R.string.message_reading_usb_atr)
                    val result = gateway.readUsbAtr(reader.deviceName)
                    val summary = if (result.success) {
                        context.getString(R.string.message_atr_read_success, (result.atrHex?.length ?: 0) / 2)
                    } else {
                        result.message
                    }
                    usbAtrSummaries[reader.deviceName] = summary
                    statusMessage = summary
                }
            },
            onProbeUsb = { reader ->
                scope.launch {
                    statusMessage = context.getString(R.string.message_probing_usb_isdr)
                    val result = gateway.probeUsbIsdR(reader.deviceName)
                    usbIsdRResults[reader.deviceName] = result
                    statusMessage = result.message
                }
            },
            onProbeOmapi = { reader ->
                scope.launch {
                    statusMessage = context.getString(R.string.message_probing_omapi_isdr)
                    val result = gateway.probeOmapiIsdR(
                        readerName = reader.name,
                        logicalSlotIndex = reader.logicalSlotIndex
                    )
                    omapiIsdRResults[reader.diagnosticKey] = result
                    statusMessage = result.message
                }
            }
        )
    }

    if (showSupportDialog && reportInput != null) {
        SupportDialog(
            onDismiss = { showSupportDialog = false },
            onCopyReport = {
                context.copyPlainText(
                    label = "Paysage eSIM support report",
                    text = EsimSupportReportBuilder.build(reportInput, context)
                )
            }
        )
    }
}

private suspend fun refreshEsimState(
    context: Context,
    gateway: EsimSystemGateway,
    resultStore: EsimDownloadResultStore,
    settings: EsimUserSettings,
    hasPhoneStatePermission: Boolean,
    onLoading: (Boolean) -> Unit,
    onStatus: (String) -> Unit,
    onSupportState: (EsimSupportState) -> Unit,
    onEuiccInfo: (EsimEuiccInfoSummary) -> Unit,
    onActiveSubscriptions: (List<EsimSubscriptionSummary>) -> Unit,
    onManageableSubscriptions: (List<EsimSubscriptionSummary>) -> Unit,
    onUsbReaders: (List<EsimUsbCcidReaderSummary>) -> Unit,
    onOmapiReaders: (List<EsimOmapiReaderSummary>?) -> Unit,
    onLastResult: (EsimDownloadResult) -> Unit,
    onHistory: (List<EsimDownloadResult>) -> Unit
) {
    onLoading(true)
    onStatus(context.getString(R.string.message_checking_euicc_capability))
    val state = gateway.supportState()
    onSupportState(state)
    onEuiccInfo(gateway.euiccInfoSummary())
    onActiveSubscriptions(
        if (hasPhoneStatePermission) {
            runCatching { gateway.activeSubscriptionSummaries() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    )
    onManageableSubscriptions(gateway.accessibleSubscriptionSummaries())
    val usbReaderList = gateway.usbCcidReaders()
    val omapiReaderList = gateway.omapiReaderSummaries()
    onUsbReaders(usbReaderList)
    onOmapiReaders(omapiReaderList)
    onLastResult(resultStore.read())
    onHistory(resultStore.readHistory())
    onStatus(externalEuiccStatusMessage(context, state, usbReaderList, omapiReaderList))
    onLoading(false)
}

private fun externalEuiccStatusMessage(
    context: Context,
    state: EsimSupportState,
    usbReaders: List<EsimUsbCcidReaderSummary>,
    omapiReaders: List<EsimOmapiReaderSummary>?
): String {
    val omapiPresent = omapiReaders.orEmpty().any { it.isSecureElementPresent }
    val omapiUiccDetected = omapiReaders.orEmpty().any { it.isUicc }
    return when {
        usbReaders.any { it.hasPermission } -> context.getString(R.string.status_authorized_usb_euicc_detected)
        usbReaders.isNotEmpty() -> context.getString(R.string.status_usb_euicc_detected)
        omapiPresent -> context.getString(R.string.status_omapi_se_detected)
        omapiUiccDetected -> context.getString(R.string.status_omapi_uicc_reader_detected)
        state.hasUsbHostFeature || state.hasOmapiUiccFeature -> context.getString(R.string.status_no_root_ready)
        state.canOpenManagement -> context.getString(R.string.status_no_external_channel)
        else -> context.getString(R.string.status_check_complete)
    }
}

@Composable
private fun CompatibilityStartScreen(
    modifier: Modifier,
    isLoading: Boolean,
    statusMessage: String,
    onStart: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        M3ePanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            prominent = true
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                M3eStatusPill(stringResource(R.string.label_step_one), active = true, icon = Icons.Default.CheckCircle)
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.title_check_external_euicc), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.detail_no_root_check),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isLoading) {
                    CircularProgressIndicator()
                    Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                } else {
                    M3eActionButton(
                        text = stringResource(R.string.button_start_check),
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        prominent = true,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }
        }
    }
}

@Composable
private fun EsimWorkspace(
    modifier: Modifier,
    isLoading: Boolean,
    statusMessage: String,
    supportState: EsimSupportState?,
    externalProfiles: List<EsimExternalProfileSummary>,
    externalProfileMessage: String,
    hasAttemptedExternalProfileRead: Boolean,
    manageableSubscriptions: List<EsimSubscriptionSummary>,
    usbReaders: List<EsimUsbCcidReaderSummary>,
    omapiReaders: List<EsimOmapiReaderSummary>?,
    araMAccessRuleSha1: String,
    lastResult: EsimDownloadResult,
    onRequestUsbPermission: (EsimUsbCcidReaderSummary) -> Unit,
    onReadExternalProfiles: () -> Unit,
    onSwitchExternalProfile: (EsimExternalProfileSummary) -> Unit,
    onManageExternalProfile: (EsimExternalProfileSummary) -> Unit,
    onOpenDetails: () -> Unit
) {
    val accessRuleRequired = "$statusMessage $externalProfileMessage".indicatesOmapiAccessRuleDenied()
    val uiccOmapiReaders = omapiReaders.orEmpty().filter { it.isUicc }
    val hasExternalReader = usbReaders.isNotEmpty() ||
        uiccOmapiReaders.isNotEmpty()
    val hasAuthorizedReader = usbReaders.any { it.hasPermission } ||
        (uiccOmapiReaders.isNotEmpty() && !accessRuleRequired)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            ExternalEuiccHomePanel(
                isLoading = isLoading,
                statusMessage = statusMessage,
                supportState = supportState,
                externalProfiles = externalProfiles,
                externalProfileMessage = externalProfileMessage,
                hasAttemptedExternalProfileRead = hasAttemptedExternalProfileRead,
                hasExternalReader = hasExternalReader,
                hasAuthorizedReader = hasAuthorizedReader,
                systemFallbackProfileCount = manageableSubscriptions.size,
                usbReaders = usbReaders,
                omapiReaders = omapiReaders,
                araMAccessRuleSha1 = araMAccessRuleSha1,
                accessRuleRequired = accessRuleRequired,
                onRequestUsbPermission = onRequestUsbPermission,
                onReadExternalProfiles = onReadExternalProfiles,
                onSwitchProfile = onSwitchExternalProfile,
                onManageProfile = onManageExternalProfile,
                onOpenDetails = onOpenDetails
            )
        }
        item {
            RecentOperationStrip(
                statusMessage = statusMessage,
                lastResult = lastResult,
                onOpenDetails = onOpenDetails
            )
        }
    }
}

@Composable
private fun ExternalEuiccHomePanel(
    isLoading: Boolean,
    statusMessage: String,
    supportState: EsimSupportState?,
    externalProfiles: List<EsimExternalProfileSummary>,
    externalProfileMessage: String,
    hasAttemptedExternalProfileRead: Boolean,
    hasExternalReader: Boolean,
    hasAuthorizedReader: Boolean,
    systemFallbackProfileCount: Int,
    usbReaders: List<EsimUsbCcidReaderSummary>,
    omapiReaders: List<EsimOmapiReaderSummary>?,
    araMAccessRuleSha1: String,
    accessRuleRequired: Boolean,
    onRequestUsbPermission: (EsimUsbCcidReaderSummary) -> Unit,
    onReadExternalProfiles: () -> Unit,
    onSwitchProfile: (EsimExternalProfileSummary) -> Unit,
    onManageProfile: (EsimExternalProfileSummary) -> Unit,
    onOpenDetails: () -> Unit
) {
    val context = LocalContext.current
    val omapiUiccCount = omapiReaders.orEmpty().count { it.isUicc }
    val authorizedUsbCount = usbReaders.count { it.hasPermission }
    val firstUnauthorizedUsb = usbReaders.firstOrNull { !it.hasPermission }

    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.title_external_euicc), style = MaterialTheme.typography.titleLarge)
                    Text(
                        externalReaderSummary(
                            context = context,
                            usbReaders = usbReaders,
                            authorizedUsbCount = authorizedUsbCount,
                            omapiUiccCount = omapiUiccCount,
                            supportState = supportState
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                M3eStatusPill(
                    text = when {
                        isLoading -> stringResource(R.string.status_checking)
                        hasAuthorizedReader -> stringResource(R.string.status_can_proceed)
                        hasExternalReader -> stringResource(R.string.status_awaiting_authorization)
                        supportState?.hasUsbHostFeature == true || supportState?.hasOmapiUiccFeature == true -> stringResource(R.string.status_awaiting_insertion)
                        else -> stringResource(R.string.status_needs_external)
                    },
                    active = hasAuthorizedReader,
                    icon = Icons.Default.CheckCircle
                )
            }

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(statusMessage.ifBlank { stringResource(R.string.message_refreshing_external_euicc) }, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    statusMessage.ifBlank { stringResource(R.string.message_insert_external_euicc_hint) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (accessRuleRequired) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.title_omapi_access_rule_required),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.message_omapi_access_rule_required),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (araMAccessRuleSha1.isNotBlank()) {
                        Text(
                            stringResource(R.string.label_ara_m_sha1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            araMAccessRuleSha1,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        M3eActionButton(
                            text = stringResource(R.string.button_copy_ara_m_sha1),
                            onClick = { context.copyPlainText("Paysage ARA-M SHA-1", araMAccessRuleSha1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            firstUnauthorizedUsb?.let { reader ->
                ReaderLine(reader)
            }

            if (firstUnauthorizedUsb != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    M3eActionButton(
                        text = stringResource(R.string.button_authorize_reader),
                        onClick = { onRequestUsbPermission(firstUnauthorizedUsb) },
                        prominent = true,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.title_profiles), style = MaterialTheme.typography.titleMedium)
            when {
                externalProfiles.isNotEmpty() -> {
                    Text(externalProfileMessage, style = MaterialTheme.typography.bodySmall)
                    externalProfiles.forEach { profile ->
                        ExternalProfileRow(
                            profile = profile,
                            onSwitchProfile = onSwitchProfile,
                            onManageProfile = onManageProfile
                        )
                    }
                }

                !hasExternalReader -> EmptyProfileState(
                    title = stringResource(R.string.title_no_external_euicc),
                    message = stringResource(R.string.message_insert_external_euicc_profile_hint)
                )

                !hasAuthorizedReader -> EmptyProfileState(
                    title = stringResource(R.string.title_reader_awaiting_authorization),
                    message = stringResource(R.string.message_authorize_usb_first),
                    primaryAction = stringResource(R.string.action_view_details),
                    onPrimaryAction = onOpenDetails
                )

                else -> EmptyProfileState(
                    title = stringResource(R.string.title_no_displayable_profiles),
                    message = if (hasAttemptedExternalProfileRead) {
                        externalProfileMessage
                    } else {
                        stringResource(R.string.message_external_profiles_not_loaded)
                    },
                    primaryAction = stringResource(R.string.button_read_external_profiles),
                    primaryActionEnabled = !isLoading,
                    onPrimaryAction = onReadExternalProfiles
                )
            }

            if (systemFallbackProfileCount > 0) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.label_system_fallback_profiles_count, systemFallbackProfileCount),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = onOpenDetails) {
                        Text(stringResource(R.string.action_view))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalProfileRow(
    profile: EsimExternalProfileSummary,
    onSwitchProfile: (EsimExternalProfileSummary) -> Unit,
    onManageProfile: (EsimExternalProfileSummary) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(profile.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        profile.providerName.ifBlank { stringResource(R.string.value_carrier_not_disclosed) },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(
                            R.string.format_external_profile_meta,
                            profile.maskedIccid,
                            profile.commandId
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        profile.source.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            M3eStatusPill(
                text = if (profile.state == EsimExternalProfileState.Enabled) {
                    stringResource(R.string.status_enabled)
                } else {
                    stringResource(R.string.status_disabled)
                },
                active = profile.state == EsimExternalProfileState.Enabled,
                icon = Icons.Default.CheckCircle
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (profile.state == EsimExternalProfileState.Disabled) {
                OutlinedButton(onClick = { onSwitchProfile(profile) }) {
                    Text(stringResource(R.string.button_switch))
                }
            }
            TextButton(onClick = { onManageProfile(profile) }) {
                Text(stringResource(R.string.button_manage))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun ExternalProfileActionDialog(
    profile: EsimExternalProfileSummary,
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onMemoryReset: () -> Unit
) {
    var nickname by remember(profile.commandId) {
        mutableStateOf(profile.nickname.ifBlank { profile.displayName })
    }
    var confirmDelete by remember(profile.commandId) { mutableStateOf(false) }
    var confirmReset by remember(profile.commandId) { mutableStateOf(false) }

    M3eAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_external_profile_management)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(profile.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(profile.source.label, style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.format_external_profile_meta, profile.maskedIccid, profile.commandId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(R.string.label_profile_nickname)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (profile.state == EsimExternalProfileState.Enabled) {
                        OutlinedButton(onClick = onDisable) {
                            Text(stringResource(R.string.button_disable_profile))
                        }
                    } else {
                        Button(onClick = onEnable) {
                            Text(stringResource(R.string.button_switch))
                        }
                    }
                    OutlinedButton(onClick = { onRename(nickname) }) {
                        Text(stringResource(R.string.button_rename))
                    }
                }
                HorizontalDivider()
                Text(
                    stringResource(R.string.message_external_profile_danger_zone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (confirmDelete) {
                    Text(stringResource(R.string.message_confirm_delete_external_profile), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (confirmDelete) stringResource(R.string.button_confirm_delete) else stringResource(R.string.action_delete))
                }
                if (confirmReset) {
                    Text(stringResource(R.string.message_confirm_external_memory_reset), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = { if (confirmReset) onMemoryReset() else confirmReset = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (confirmReset) stringResource(R.string.button_confirm_external_memory_reset) else stringResource(R.string.button_external_memory_reset))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun EmptyProfileState(
    title: String,
    message: String,
    primaryAction: String? = null,
    primaryActionEnabled: Boolean = true,
    onPrimaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (primaryAction != null && onPrimaryAction != null) {
            OutlinedButton(
                onClick = onPrimaryAction,
                enabled = primaryActionEnabled
            ) {
                Text(primaryAction)
            }
        }
    }
}

@Composable
private fun RecentOperationStrip(
    statusMessage: String,
    lastResult: EsimDownloadResult,
    onOpenDetails: () -> Unit
) {
    if (statusMessage.isBlank() && lastResult.updatedAtMillis <= 0L) return
    val context = LocalContext.current

    M3ePanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        elevated = false
    ) {
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
                Text(
                    if (lastResult.updatedAtMillis > 0L) EsimDownloadHistoryPolicy.title(context, lastResult) else stringResource(R.string.label_current_status),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    recentOperationText(context, statusMessage, lastResult),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onOpenDetails) {
                Text(stringResource(R.string.action_details))
            }
        }
    }
}

@Composable
private fun ReaderLine(reader: EsimUsbCcidReaderSummary) {
    Text(
        "${reader.manufacturerName} / ${reader.productName}",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
    )
}

private fun externalReaderSummary(
    context: Context,
    usbReaders: List<EsimUsbCcidReaderSummary>,
    authorizedUsbCount: Int,
    omapiUiccCount: Int,
    supportState: EsimSupportState?
): String = when {
    usbReaders.isNotEmpty() -> context.getString(R.string.summary_usb_readers, usbReaders.size, authorizedUsbCount)
    omapiUiccCount > 0 -> context.getString(R.string.summary_omapi_uicc_readers, omapiUiccCount)
    supportState?.hasUsbHostFeature == true -> context.getString(R.string.summary_usb_host_waiting)
    supportState?.hasOmapiUiccFeature == true -> context.getString(R.string.summary_omapi_uicc_waiting)
    supportState == null -> context.getString(R.string.summary_not_refreshed)
    else -> context.getString(R.string.summary_no_root_use_external)
}

private fun recentOperationText(
    context: Context,
    statusMessage: String,
    lastResult: EsimDownloadResult
): String = when {
    lastResult.updatedAtMillis > 0L -> lastResult.message
    statusMessage.isNotBlank() -> statusMessage
    else -> context.getString(R.string.message_no_recent_operation)
}

private fun String.indicatesOmapiAccessRuleDenied(): Boolean =
    contains("访问规则拒绝") ||
        contains("ARA-M", ignoreCase = true) ||
        contains("access rule", ignoreCase = true) ||
        contains("access rules", ignoreCase = true)

@Composable
private fun EsimDetailsDialog(
    euiccInfo: EsimEuiccInfoSummary?,
    eidResult: EsimEidReadResult?,
    externalProfiles: List<EsimExternalProfileSummary>,
    externalEuiccDetails: EsimExternalEuiccDetailsResult,
    manageableSubscriptions: List<EsimSubscriptionSummary>,
    activeSubscriptions: List<EsimSubscriptionSummary>,
    hasPhoneStatePermission: Boolean,
    lastResult: EsimDownloadResult,
    history: List<EsimDownloadResult>,
    showHistory: Boolean,
    canOpenSupport: Boolean,
    onDismiss: () -> Unit,
    onReadEid: () -> Unit,
    onCopyEid: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onManageProfile: (EsimSubscriptionSummary) -> Unit,
    onHandleNotification: (EsimExternalEuiccNotification) -> Unit,
    onDeleteNotification: (EsimExternalEuiccNotification) -> Unit,
    onHandleAllNotifications: (EsimExternalProfileSource) -> Unit,
    onDeleteAllNotifications: (EsimExternalProfileSource) -> Unit,
    onClearHistory: () -> Unit,
    onOpenSupport: () -> Unit
) {
    M3eAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_euicc_details)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    EuiccCompactCard(
                        info = euiccInfo,
                        eidResult = eidResult,
                        onReadEid = onReadEid,
                        onCopyEid = onCopyEid
                    )
                }
                item {
                    ExternalProfileDetailsCard(externalProfiles)
                }
                item {
                    ExternalEuiccDetailsCard(
                        detailsResult = externalEuiccDetails,
                        onHandleNotification = onHandleNotification,
                        onDeleteNotification = onDeleteNotification,
                        onHandleAllNotifications = onHandleAllNotifications,
                        onDeleteAllNotifications = onDeleteAllNotifications
                    )
                }
                item {
                    ProfileListCard(
                        subscriptions = manageableSubscriptions,
                        onManageProfile = onManageProfile
                    )
                }
                item {
                    ActiveSubscriptionsCard(
                        hasPhoneStatePermission = hasPhoneStatePermission,
                        activeSubscriptions = activeSubscriptions,
                        onRequestPermission = onRequestPhonePermission
                    )
                }
                item {
                    OperationSummaryCard(
                        lastResult = lastResult,
                        history = history,
                        showHistory = showHistory,
                        canOpenSupport = canOpenSupport,
                        onClearHistory = onClearHistory,
                        onOpenSupport = onOpenSupport
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_complete)) }
        }
    )
}

@Composable
private fun DetailsSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun EuiccCompactCard(
    info: EsimEuiccInfoSummary?,
    eidResult: EsimEidReadResult?,
    onReadEid: () -> Unit,
    onCopyEid: () -> Unit
) {
    val context = LocalContext.current
    DetailsSection {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.title_euicc_summary), style = MaterialTheme.typography.titleMedium)
            if (info == null) {
                Text(stringResource(R.string.message_not_read_yet), style = MaterialTheme.typography.bodySmall)
            } else {
                InfoLine(stringResource(R.string.label_os), info.osVersion ?: stringResource(R.string.value_not_disclosed))
                InfoLine(stringResource(R.string.label_available_space), info.memory.displayText)
                InfoLine(stringResource(R.string.label_ports), if (info.ports.isEmpty()) stringResource(R.string.value_not_disclosed) else "${info.ports.size}${stringResource(R.string.suffix_ports_count)}")
                Text(info.sensitiveIdentifierPolicy, style = MaterialTheme.typography.bodySmall)
            }
            eidResult?.let {
                Text(it.eid?.let(EsimEidFormatter::masked) ?: it.message, style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReadEid) { Text(stringResource(R.string.button_read_eid)) }
                if (eidResult?.status == EsimEidReadStatus.Succeeded) {
                    TextButton(onClick = onCopyEid) { Text(stringResource(R.string.button_copy_full_eid)) }
                }
            }
        }
    }
}

@Composable
private fun ExternalEuiccDetailsCard(
    detailsResult: EsimExternalEuiccDetailsResult,
    onHandleNotification: (EsimExternalEuiccNotification) -> Unit,
    onDeleteNotification: (EsimExternalEuiccNotification) -> Unit,
    onHandleAllNotifications: (EsimExternalProfileSource) -> Unit,
    onDeleteAllNotifications: (EsimExternalProfileSource) -> Unit
) {
    DetailsSection {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.title_external_euicc_details), style = MaterialTheme.typography.titleMedium)
            if (detailsResult.details.isEmpty()) {
                Text(
                    detailsResult.messages.firstOrNull()
                        ?: stringResource(R.string.message_no_external_euicc_details_read),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                detailsResult.details.forEach { detail ->
                    ExternalEuiccDetailBlock(
                        detail = detail,
                        onHandleNotification = onHandleNotification,
                        onDeleteNotification = onDeleteNotification,
                        onHandleAllNotifications = onHandleAllNotifications,
                        onDeleteAllNotifications = onDeleteAllNotifications
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ExternalEuiccDetailBlock(
    detail: EsimExternalEuiccDetails,
    onHandleNotification: (EsimExternalEuiccNotification) -> Unit,
    onDeleteNotification: (EsimExternalEuiccNotification) -> Unit,
    onHandleAllNotifications: (EsimExternalProfileSource) -> Unit,
    onDeleteAllNotifications: (EsimExternalProfileSource) -> Unit
) {
    Text(detail.source.label, fontWeight = FontWeight.SemiBold)
    detail.maskedEid?.let {
        InfoLine(stringResource(R.string.label_eid), it)
    }
    detail.info?.let { info ->
        info.vendor?.let { vendor ->
            InfoLine(stringResource(R.string.label_vendor), vendor.name)
            InfoLine(stringResource(R.string.label_model), vendor.model)
        }
        InfoLine(stringResource(R.string.label_sgp22_version), info.sgp22Version)
        InfoLine(stringResource(R.string.label_profile_version), info.profileVersion)
        InfoLine(stringResource(R.string.label_firmware_version), info.firmwareVersion)
        InfoLine(stringResource(R.string.label_global_platform_version), info.globalPlatformVersion)
        InfoLine(stringResource(R.string.label_sas_accreditation), info.sasAccreditationNumber.ifBlank { stringResource(R.string.value_not_disclosed) })
        InfoLine(stringResource(R.string.label_pp_version), info.ppVersion)
        InfoLine(stringResource(R.string.label_free_nvram), info.freeNvram)
        InfoLine(stringResource(R.string.label_free_ram), info.freeRam)
        InfoLine(stringResource(R.string.label_ci_pkid_counts), "${info.signingPkidCount}/${info.verificationPkidCount}")
    } ?: Text(stringResource(R.string.message_external_euicc_info2_unavailable), style = MaterialTheme.typography.bodySmall)

    Text(stringResource(R.string.title_external_notifications), style = MaterialTheme.typography.titleSmall)
    if (detail.notifications.isEmpty()) {
        Text(stringResource(R.string.message_no_external_notifications), style = MaterialTheme.typography.bodySmall)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onHandleAllNotifications(detail.source) }) {
                Text(stringResource(R.string.button_handle_all_notifications))
            }
            TextButton(onClick = { onDeleteAllNotifications(detail.source) }) {
                Text(stringResource(R.string.button_delete_all_notifications))
            }
        }
        detail.notifications.forEach { notification ->
            ExternalNotificationRow(
                notification = notification,
                onHandleNotification = onHandleNotification,
                onDeleteNotification = onDeleteNotification
            )
        }
    }
}

@Composable
private fun ExternalNotificationRow(
    notification: EsimExternalEuiccNotification,
    onHandleNotification: (EsimExternalEuiccNotification) -> Unit,
    onDeleteNotification: (EsimExternalEuiccNotification) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.format_external_notification_title, notification.operation, notification.commandId),
            fontWeight = FontWeight.SemiBold
        )
        InfoLine(stringResource(R.string.label_iccid), notification.maskedIccid)
        InfoLine(stringResource(R.string.label_notification_address), notification.notificationAddress.ifBlank { stringResource(R.string.value_not_disclosed) })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onHandleNotification(notification) }) {
                Text(stringResource(R.string.button_handle_notification))
            }
            TextButton(onClick = { onDeleteNotification(notification) }) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun ExternalProfileDetailsCard(
    profiles: List<EsimExternalProfileSummary>
) {
    DetailsSection {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.title_external_profiles), style = MaterialTheme.typography.titleMedium)
            if (profiles.isEmpty()) {
                Text(stringResource(R.string.message_no_external_profiles_read), style = MaterialTheme.typography.bodySmall)
            } else {
                profiles.forEach { profile ->
                    InfoLine(profile.displayName, profile.commandId)
                    InfoLine(stringResource(R.string.label_iccid), profile.maskedIccid)
                    InfoLine(
                        stringResource(R.string.label_profile_state),
                        if (profile.state == EsimExternalProfileState.Enabled) {
                            stringResource(R.string.status_enabled)
                        } else {
                            stringResource(R.string.status_disabled)
                        }
                    )
                    Text(profile.source.label, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ProfileListCard(
    subscriptions: List<EsimSubscriptionSummary>,
    onManageProfile: (EsimSubscriptionSummary) -> Unit
) {
    val context = LocalContext.current
    DetailsSection {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.title_system_fallback_profiles), style = MaterialTheme.typography.titleMedium)
            if (subscriptions.isEmpty()) {
                Text(stringResource(R.string.message_no_internal_profiles),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                subscriptions.forEach { subscription ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(subscription.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(subscription.carrierName.ifBlank { context.getString(R.string.value_carrier_not_disclosed) }, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedButton(onClick = { onManageProfile(subscription) }) { Text(stringResource(R.string.button_manage)) }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ActiveSubscriptionsCard(
    hasPhoneStatePermission: Boolean,
    activeSubscriptions: List<EsimSubscriptionSummary>,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    DetailsSection {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.title_active_sim_esim), style = MaterialTheme.typography.titleMedium)
            if (!hasPhoneStatePermission) {
                Text(stringResource(R.string.message_authorize_to_show_subscriptions), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onRequestPermission) { Text(stringResource(R.string.button_authorize_read)) }
            } else if (activeSubscriptions.isEmpty()) {
                Text(stringResource(R.string.message_no_active_subscriptions), style = MaterialTheme.typography.bodySmall)
            } else {
                activeSubscriptions.forEach { subscription ->
                    InfoLine(subscription.displayName, subscription.portIndex?.let { context.getString(R.string.format_port_number, it + 1) } ?: context.getString(R.string.value_port_not_disclosed))
                }
            }
        }
    }
}

@Composable
private fun OperationSummaryCard(
    lastResult: EsimDownloadResult,
    history: List<EsimDownloadResult>,
    showHistory: Boolean,
    canOpenSupport: Boolean = true,
    onClearHistory: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val context = LocalContext.current
    DetailsSection {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.title_recent_operations), style = MaterialTheme.typography.titleMedium)
            Text(EsimDownloadHistoryPolicy.title(context, lastResult), style = MaterialTheme.typography.bodyMedium)
            Text(lastResult.message, style = MaterialTheme.typography.bodySmall)
            if (lastResult.updatedAtMillis > 0L) {
                Text(DateFormatter.format(lastResult.updatedAtMillis), style = MaterialTheme.typography.bodySmall)
            }
            EsimEuiccOperationErrorCatalog.explain(context, lastResult)?.let { explanation ->
                Text(explanation.recoveryHint, style = MaterialTheme.typography.bodySmall)
            }
            if (showHistory && history.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.format_history_count, history.size), style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenSupport, enabled = canOpenSupport) { Text(stringResource(R.string.button_support_report)) }
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) { Text(stringResource(R.string.button_clear_history)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileActionDialog(
    subscription: EsimSubscriptionSummary,
    availablePorts: List<Int>,
    onDismiss: () -> Unit,
    onSwitch: (Int?) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var selectedPort by remember { mutableStateOf<Int?>(null) }
    var nickname by remember(subscription.subscriptionId) { mutableStateOf(subscription.displayName) }
    var confirmDelete by remember { mutableStateOf(false) }

    M3eAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(subscription.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine(context.getString(R.string.label_carrier), subscription.carrierName.ifBlank { context.getString(R.string.value_not_disclosed) })
                InfoLine(context.getString(R.string.label_subscription_id), subscription.subscriptionId.toString())
                InfoLine(context.getString(R.string.label_card_id), subscription.cardId?.toString() ?: context.getString(R.string.value_not_disclosed))
                InfoLine(context.getString(R.string.label_current_port), subscription.portIndex?.let { "${it + 1}" } ?: context.getString(R.string.value_port_not_disclosed))
                if (availablePorts.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedPort == null,
                            onClick = { selectedPort = null },
                            label = { Text(stringResource(R.string.label_system_auto)) }
                        )
                        availablePorts.forEach { port ->
                            FilterChip(
                                selected = selectedPort == port,
                                onClick = { selectedPort = port },
                                label = { Text(stringResource(R.string.format_port_label, port + 1)) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_display_name)) },
                    singleLine = true
                )
                if (confirmDelete) {
                    Text(stringResource(R.string.message_confirm_delete_esim), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSwitch(selectedPort) }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedPort?.let { stringResource(R.string.format_switch_to_port, it + 1) } ?: stringResource(R.string.button_switch))
                }
                OutlinedButton(onClick = { onRename(nickname) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.button_rename))
                }
                OutlinedButton(
                    onClick = {
                        if (confirmDelete) onDelete() else confirmDelete = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (confirmDelete) stringResource(R.string.button_confirm_delete) else stringResource(R.string.action_delete))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun DiagnosticsDialog(
    usbReaders: List<EsimUsbCcidReaderSummary>,
    omapiReaders: List<EsimOmapiReaderSummary>?,
    usbAtrSummaries: Map<String, String>,
    usbIsdRResults: Map<String, EsimIsdRProbeResult>,
    omapiIsdRResults: Map<String, EsimIsdRProbeResult>,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onRequestUsbPermission: (EsimUsbCcidReaderSummary) -> Unit,
    onReadAtr: (EsimUsbCcidReaderSummary) -> Unit,
    onProbeUsb: (EsimUsbCcidReaderSummary) -> Unit,
    onProbeOmapi: (EsimOmapiReaderSummary) -> Unit
) {
    val context = LocalContext.current
    M3eAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_advanced_diagnostics)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(stringResource(R.string.label_usb_ccid), style = MaterialTheme.typography.titleSmall)
                    if (usbReaders.isEmpty()) {
                        Text(stringResource(R.string.message_no_usb_ccid_readers), style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(usbReaders) { reader ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${reader.manufacturerName} / ${reader.productName}", fontWeight = FontWeight.SemiBold)
                        StatusLine(context.getString(R.string.label_usb_authorization), reader.hasPermission)
                        usbAtrSummaries[reader.deviceName]?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        usbIsdRResults[reader.deviceName]?.let { ProbeResult(it) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onRequestUsbPermission(reader) }) { Text(stringResource(R.string.button_authorize)) }
                            OutlinedButton(onClick = { onReadAtr(reader) }, enabled = reader.hasPermission) { Text(stringResource(R.string.button_atr)) }
                            Button(onClick = { onProbeUsb(reader) }, enabled = reader.hasPermission) { Text(stringResource(R.string.button_isdr)) }
                        }
                        HorizontalDivider()
                    }
                }
                item {
                    Text(stringResource(R.string.label_omapi), style = MaterialTheme.typography.titleSmall)
                    when {
                        omapiReaders == null -> Text(stringResource(R.string.message_not_detected_yet), style = MaterialTheme.typography.bodySmall)
                        omapiReaders.isEmpty() -> Text(stringResource(R.string.message_no_omapi_readers), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!omapiReaders.isNullOrEmpty()) {
                    items(omapiReaders) { reader ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(reader.displayLabel, fontWeight = FontWeight.SemiBold)
                            StatusLine(context.getString(R.string.label_uicc_reader), reader.isUicc)
                            StatusLine(context.getString(R.string.label_secure_element_present), reader.isSecureElementPresent)
                            omapiIsdRResults[reader.diagnosticKey]?.let { ProbeResult(it) }
                            Button(
                                onClick = { onProbeOmapi(reader) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.button_select_isdr))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.action_refresh)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun SupportDialog(
    onDismiss: () -> Unit,
    onCopyReport: () -> Unit
) {
    M3eAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_support_troubleshooting_report)) },
        text = {
            Text(stringResource(R.string.message_diagnostics_privacy), style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(onClick = onCopyReport) { Text(stringResource(R.string.button_copy_sanitized_report)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun ProbeResult(result: EsimIsdRProbeResult) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        StatusLine(context.getString(R.string.label_isdr), result.success)
        Text(result.message, style = MaterialTheme.typography.bodySmall)
        result.statusWord?.let { InfoLine(context.getString(R.string.label_status_word), it) }
        if (result.responseByteCount > 0) {
            InfoLine(context.getString(R.string.label_response_length), context.getString(R.string.format_bytes_count, result.responseByteCount))
        }
        result.fciSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun StatusLine(label: String, enabled: Boolean) {
    val context = LocalContext.current
    InfoLine(label, if (enabled) context.getString(R.string.value_available) else context.getString(R.string.value_unavailable))
}

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

private fun Context.hasPhoneStatePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.copyPlainText(label: String, text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, getString(R.string.message_already_copied), Toast.LENGTH_SHORT).show()
}

private fun EsimText.resolve(context: Context): String =
    context.getString(resId, *args.toTypedArray())

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
