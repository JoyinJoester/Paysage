package joyin.takgi.paysage.ui.screens

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import joyin.takgi.paysage.R
import joyin.takgi.paysage.accessibility.SmsAccessibilityService
import joyin.takgi.paysage.data.AppDatabase
import joyin.takgi.paysage.data.ForwardLog
import joyin.takgi.paysage.data.PendingForwardMessage
import joyin.takgi.paysage.mail.MailInboxFailureKind
import joyin.takgi.paysage.mail.MailInboxRecoveryAdvisor
import joyin.takgi.paysage.mail.MailInboxRealtimeSettingsStore
import joyin.takgi.paysage.mail.MailInboxRepository
import joyin.takgi.paysage.mail.MailInboxRuntimeStatus
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import joyin.takgi.paysage.service.SmsKeepAliveService
import joyin.takgi.paysage.ui.components.M3eDropdownMenu
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eStatusPill
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.ui.navigation.PaysageBottomBar
import joyin.takgi.paysage.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onFilterClick: () -> Unit,
    onLogsClick: () -> Unit,
    onMailInboxClick: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val mailInboxRepository = remember { MailInboxRepository(context) }
    val mailInboxRealtimeSettingsStore = remember { MailInboxRealtimeSettingsStore(context) }
    val logDao = remember { db.forwardLogDao() }
    val accountDao = remember { db.forwardAccountDao() }
    val pendingDao = remember { db.pendingForwardDao() }
    val trustedMailSenders by mailInboxRepository.observeTrustedSenders().collectAsState(initial = emptyList())
    val todayCount by logDao.getTodayCount().collectAsState(initial = 0)
    val recentLogs by logDao.getRecent(50).collectAsState(initial = emptyList())
    val lastSuccessTimestamp by logDao.getLastSuccessTimestamp().collectAsState(initial = null)
    val pendingCount by pendingDao.observePendingCount().collectAsState(initial = 0)
    val pendingMessages by pendingDao.observeRecent(3).collectAsState(initial = emptyList())
    val enabledAccounts by accountDao.getEnabled().collectAsState(initial = emptyList())
    var statusMessage by remember { mutableStateOf("") }
    var mailAccount by remember { mutableStateOf(mailInboxRepository.readAccount()) }
    var mailRuntimeStatus by remember { mutableStateOf(mailInboxRepository.readRuntimeStatus()) }
    var mailRealtimeEnabled by remember { mutableStateOf(mailInboxRealtimeSettingsStore.read().enabled) }
    var showMenu by remember { mutableStateOf(false) }
    var showRuntimeDetails by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(context.isSmsAccessibilityServiceEnabled())
    }
    var isBatteryOptimized by remember {
        mutableStateOf(!SmsReliabilityManager.isIgnoringBatteryOptimizations(context))
    }
    var isKeepAliveRunning by remember {
        mutableStateOf(SmsReliabilityManager.isKeepAliveServiceRunning(context))
    }
    var hasReceiveSmsPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.RECEIVE_SMS))
    }
    var hasReadSmsPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.READ_SMS))
    }
    var canPostNotifications by remember {
        mutableStateOf(context.canPostNotifications())
    }

    fun refreshRuntimeState() {
        isAccessibilityEnabled = context.isSmsAccessibilityServiceEnabled()
        isBatteryOptimized = !SmsReliabilityManager.isIgnoringBatteryOptimizations(context)
        isKeepAliveRunning = SmsReliabilityManager.isKeepAliveServiceRunning(context)
        hasReceiveSmsPermission = context.hasPermission(Manifest.permission.RECEIVE_SMS)
        hasReadSmsPermission = context.hasPermission(Manifest.permission.READ_SMS)
        canPostNotifications = context.canPostNotifications()
        mailAccount = mailInboxRepository.readAccount()
        mailRuntimeStatus = mailInboxRepository.readRuntimeStatus()
        mailRealtimeEnabled = mailInboxRealtimeSettingsStore.read().enabled
    }

    DisposableEffect(context) {
        refreshRuntimeState()

        val activity = context.findActivity()
        if (activity == null) {
            onDispose {}
        } else {
            val callbacks = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(resumedActivity: Activity) {
                    if (resumedActivity === activity) {
                        refreshRuntimeState()
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            }

            activity.application.registerActivityLifecycleCallbacks(callbacks)
            onDispose {
                activity.application.unregisterActivityLifecycleCallbacks(callbacks)
            }
        }
    }

    val health = remember(
        recentLogs,
        pendingCount,
        isBatteryOptimized,
        isAccessibilityEnabled,
        lastSuccessTimestamp,
        enabledAccounts,
        isKeepAliveRunning,
        hasReceiveSmsPermission,
        hasReadSmsPermission,
        canPostNotifications
    ) {
        SmsHealthUiState.from(
            context = context,
            logs = recentLogs,
            pendingCount = pendingCount,
            batteryOptimized = isBatteryOptimized,
            accessibilityEnabled = isAccessibilityEnabled,
            lastSuccessTimestamp = lastSuccessTimestamp,
            activeAccountCount = enabledAccounts.size,
            keepAliveRunning = isKeepAliveRunning,
            hasReceiveSmsPermission = hasReceiveSmsPermission,
            hasReadSmsPermission = hasReadSmsPermission,
            canPostNotifications = canPostNotifications
        )
    }

    val mailHealth = remember(
        mailAccount,
        mailRuntimeStatus,
        trustedMailSenders,
        isBatteryOptimized,
        mailRealtimeEnabled
    ) {
        MailInboxHealthUiState.from(
            context = context,
            accountEnabled = mailAccount.enabled,
            accountConfigured = mailAccount.isConfigured,
            trustedSenderCount = trustedMailSenders.size,
            runtimeStatus = mailRuntimeStatus,
            batteryOptimized = isBatteryOptimized,
            realtimeEnabled = mailRealtimeEnabled
        )
    }

    fun startStableMode(message: String) {
        SmsReliabilityManager.ensureScheduled(context)
        context.startKeepAliveService()
        if (pendingCount > 0) {
            SmsReliabilityManager.enqueueImmediateRetry(context)
        }
        refreshRuntimeState()
        statusMessage = message
    }

    fun retryNow() {
        SmsReliabilityManager.ensureScheduled(context)
        SmsReliabilityManager.enqueueImmediateRetry(context)
        statusMessage = context.getString(R.string.message_retry_scheduled)
    }

    fun openBatteryWizardMessage(): String {
        val activity = context.findActivity()
        return if (activity != null &&
            SmsReliabilityManager.openBatteryOptimizationWizard(activity)
        ) {
            context.getString(R.string.message_battery_wizard_opened)
        } else {
            context.getString(R.string.message_battery_wizard_failed)
        }
    }

    fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_home_title),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more_forward_options))
                    }
                    M3eDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_runtime_details)) },
                            onClick = {
                                showMenu = false
                                showRuntimeDetails = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_filter_rules)) },
                            onClick = {
                                showMenu = false
                                onFilterClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_forward_logs)) },
                            onClick = {
                                showMenu = false
                                onLogsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_settings)) },
                            onClick = {
                                showMenu = false
                                onSettingsClick()
                            }
                        )
                    }
                }
            )
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
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                HomeStatusSummary(
                    health = health,
                    todayCount = todayCount,
                    statusMessage = statusMessage,
                    onOptimize = {
                        startStableMode(context.getString(R.string.message_stable_mode_started))
                    },
                    onOpenDetails = { showRuntimeDetails = true }
                )
            }

            if (health.issues.isNotEmpty()) {
                item {
                    HomeAttentionRow(
                        issues = health.issues,
                        onIssueAction = { issue ->
                            when (issue.type) {
                                SmsHealthIssueType.Battery -> {
                                    statusMessage = openBatteryWizardMessage()
                                }
                                SmsHealthIssueType.Accessibility -> openAccessibilitySettings()
                                SmsHealthIssueType.Account -> onSettingsClick()
                                SmsHealthIssueType.Permission,
                                SmsHealthIssueType.Notification -> context.openAppDetails()
                                SmsHealthIssueType.Pending -> retryNow()
                                SmsHealthIssueType.Guard -> startStableMode(context.getString(R.string.message_stable_guard_started))
                                SmsHealthIssueType.Delivery -> onLogsClick()
                            }
                        }
                    )
                }
            }

            item {
                HomeSectionLabel(text = stringResource(R.string.label_quick_actions))
            }

            item {
                HomeNavigationRow(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.label_mail_command),
                    subtitle = mailHealth.statusMessage,
                    statusLabel = mailHealth.statusLabel,
                    statusActive = mailHealth.isHealthy,
                    onClick = onMailInboxClick
                )
            }

            item {
                HomeNavigationRow(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = stringResource(R.string.action_filter_rules),
                    subtitle = stringResource(R.string.filter_type_whitelist) + " / " +
                        stringResource(R.string.filter_type_blacklist) + " / " +
                        stringResource(R.string.filter_type_keyword),
                    onClick = onFilterClick
                )
            }

            item {
                HomeNavigationRow(
                    icon = Icons.Default.Phone,
                    title = stringResource(R.string.action_forward_logs),
                    subtitle = stringResource(R.string.label_last_success) + ": " + health.lastSuccessText,
                    onClick = onLogsClick
                )
            }

            item {
                HomeNavigationRow(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.action_settings),
                    subtitle = stringResource(R.string.detail_appearance_selection),
                    onClick = onSettingsClick
                )
            }
        }
    }

    if (showRuntimeDetails) {
        ForwardRuntimeDialog(
            health = health,
            pendingMessages = pendingMessages,
            onDismiss = { showRuntimeDetails = false }
        )
    }
}

@Composable
private fun HomeStatusSummary(
    health: SmsHealthUiState,
    todayCount: Int,
    statusMessage: String,
    onOptimize: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val supportingColor = if (health.isHealthy) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (health.isHealthy) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (health.isHealthy) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = health.statusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = health.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                M3eStatusPill(
                    text = health.statusLabel,
                    active = health.isHealthy,
                    icon = if (health.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HomeMetricText(
                    label = stringResource(R.string.label_today),
                    value = stringResource(R.string.unit_count, todayCount),
                    labelColor = supportingColor,
                    modifier = Modifier.weight(1f)
                )
                HomeMetricText(
                    label = stringResource(R.string.label_success_rate),
                    value = health.successRate,
                    labelColor = supportingColor,
                    modifier = Modifier.weight(1f)
                )
                HomeMetricText(
                    label = stringResource(R.string.label_cache),
                    value = "${health.pendingCount}",
                    labelColor = supportingColor,
                    modifier = Modifier.weight(1f)
                )
            }

            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenDetails) {
                    Text(stringResource(R.string.button_runtime_details))
                }
                if (!health.isHealthy) {
                    FilledTonalButton(onClick = onOptimize) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.button_stable_optimization))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetricText(
    label: String,
    value: String,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun HomeNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    statusLabel: String? = null,
    statusActive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeIconBubble(icon = icon)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            statusLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (statusActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 112.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeAttentionRow(
    issues: List<SmsHealthIssue>,
    onIssueAction: (SmsHealthIssue) -> Unit
) {
    val firstIssue = issues.firstOrNull() ?: return
    Surface(
        onClick = { onIssueAction(firstIssue) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeIconBubble(
                icon = Icons.Default.Warning,
                prominent = true
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_needs_attention) + " · " +
                        stringResource(R.string.format_items_unit, issues.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = firstIssue.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = firstIssue.message,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = { onIssueAction(firstIssue) }) {
                Text(firstIssue.actionLabel)
            }
        }
    }
}

@Composable
private fun HomeIconBubble(
    icon: ImageVector,
    prominent: Boolean = false
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = if (prominent) {
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (prominent) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ForwardRuntimeDialog(
    health: SmsHealthUiState,
    pendingMessages: List<PendingForwardMessage>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.action_runtime_details),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.label_listening_chain), style = MaterialTheme.typography.titleMedium)
                        InfoLine("SMS Broadcast", health.broadcastState)
                        InfoLine("ContentObserver", health.observerState)
                        InfoLine(stringResource(R.string.label_accessibility_enhancement), health.accessibilityState)
                    }
                }

                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.label_keepalive_retry), style = MaterialTheme.typography.titleMedium)
                        InfoLine("Foreground Service", health.keepAliveState)
                        InfoLine(stringResource(R.string.label_work_manager), context.getString(R.string.status_scheduled))
                        InfoLine(stringResource(R.string.label_alarm_manager), context.getString(R.string.status_scheduled))
                        InfoLine(stringResource(R.string.label_notification_reminder), health.notificationState)
                        InfoLine(stringResource(R.string.label_offline_cache), context.getString(R.string.unit_count, health.pendingCount))
                    }
                }

                if (pendingMessages.isNotEmpty()) {
                    M3ePanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.label_recent_cache), style = MaterialTheme.typography.titleMedium)
                            pendingMessages.forEach { pending ->
                                Text(
                                    "${pending.sender.ifBlank { context.getString(R.string.label_unknown_number) }} · ${pending.attempts} ${context.getString(R.string.label_attempts)} · ${pending.nextRetryText(context)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
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
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class MailInboxHealthUiState(
    val failureKind: MailInboxFailureKind,
    val statusLabel: String,
    val statusMessage: String,
    val trustedSenderCount: Int,
    val lastCheckText: String,
    val batteryImpactText: String,
    val lastExecuted: Int,
    val lastIgnored: Int,
    val lastRejected: Int,
    val canRefresh: Boolean,
    val recoveryAdvice: joyin.takgi.paysage.mail.MailInboxRecoveryAdvice?
) {
    val isHealthy: Boolean
        get() = failureKind == MailInboxFailureKind.None

    companion object {
        fun from(
            context: Context,
            accountEnabled: Boolean,
            accountConfigured: Boolean,
            trustedSenderCount: Int,
            runtimeStatus: MailInboxRuntimeStatus,
            batteryOptimized: Boolean,
            realtimeEnabled: Boolean
        ): MailInboxHealthUiState {
            val lastFailureIsCurrent = runtimeStatus.lastFailureAt > runtimeStatus.lastSuccessAt
            val failureKind = when {
                !accountConfigured -> MailInboxFailureKind.InvalidConfig
                !accountEnabled -> MailInboxFailureKind.Disabled
                runtimeStatus.lastFailureKind == MailInboxFailureKind.CommandRejected &&
                    runtimeStatus.lastRejected > 0 -> MailInboxFailureKind.CommandRejected
                lastFailureIsCurrent -> runtimeStatus.lastFailureKind
                accountEnabled && accountConfigured && batteryOptimized -> MailInboxFailureKind.BackgroundRestricted
                else -> MailInboxFailureKind.None
            }
            val statusLabel = when {
                !accountConfigured -> context.getString(R.string.mail_status_not_configured_short)
                !accountEnabled -> context.getString(R.string.mail_status_disabled_short)
                trustedSenderCount == 0 -> context.getString(R.string.mail_status_waiting_auth)
                failureKind == MailInboxFailureKind.BackgroundRestricted -> context.getString(R.string.mail_status_restricted)
                failureKind == MailInboxFailureKind.CommandRejected -> context.getString(R.string.mail_status_needs_check)
                failureKind != MailInboxFailureKind.None -> context.getString(R.string.mail_status_abnormal)
                runtimeStatus.lastCheckAt == 0L -> context.getString(R.string.mail_status_waiting_check)
                else -> context.getString(R.string.mail_status_running_short)
            }
            val statusMessage = when (failureKind) {
                MailInboxFailureKind.InvalidConfig -> context.getString(R.string.mail_detail_config_imap_first)
                MailInboxFailureKind.Disabled -> context.getString(R.string.mail_detail_disabled)
                MailInboxFailureKind.None -> when {
                    trustedSenderCount == 0 -> context.getString(R.string.mail_detail_add_trusted_sender)
                    runtimeStatus.lastCheckAt == 0L -> context.getString(R.string.mail_detail_ready_manual_refresh)
                    else -> runtimeStatus.lastMessage.ifBlank { context.getString(R.string.mail_detail_check_running_normal) }
                }
                MailInboxFailureKind.BackgroundRestricted -> context.getString(R.string.mail_detail_battery_may_delay)
                MailInboxFailureKind.CommandRejected -> context.getString(R.string.mail_detail_command_rejected)
                else -> runtimeStatus.lastMessage.ifBlank { context.getString(R.string.mail_detail_last_check_failed) }
            }
            return MailInboxHealthUiState(
                failureKind = failureKind,
                statusLabel = statusLabel,
                statusMessage = statusMessage,
                trustedSenderCount = trustedSenderCount,
                lastCheckText = runtimeStatus.lastCheckAt
                    .takeIf { it > 0L }
                    ?.let(DateFormatter::format)
                    ?: context.getString(R.string.value_none),
                batteryImpactText = if (accountEnabled && accountConfigured) {
                    if (realtimeEnabled) {
                        context.getString(R.string.label_battery_impact_high_realtime)
                    } else if (batteryOptimized) {
                        context.getString(R.string.label_battery_impact_restricted)
                    } else {
                        context.getString(R.string.label_battery_impact_low_periodic)
                    }
                } else {
                    context.getString(R.string.label_battery_impact_no_check)
                },
                lastExecuted = runtimeStatus.lastExecuted,
                lastIgnored = runtimeStatus.lastIgnored,
                lastRejected = runtimeStatus.lastRejected,
                canRefresh = accountEnabled && accountConfigured,
                recoveryAdvice = MailInboxRecoveryAdvisor.adviceFor(context, failureKind)
            )
        }
    }
}

private enum class SmsHealthIssueType {
    Battery,
    Accessibility,
    Account,
    Permission,
    Notification,
    Pending,
    Guard,
    Delivery
}

private data class SmsHealthIssue(
    val type: SmsHealthIssueType,
    val title: String,
    val message: String,
    val actionLabel: String
)

private data class SmsHealthUiState(
    val statusLabel: String,
    val statusTitle: String,
    val statusMessage: String,
    val successRate: String,
    val pendingCount: Int,
    val activeAccountCount: Int,
    val lastSuccessText: String,
    val broadcastState: String,
    val observerState: String,
    val accessibilityState: String,
    val keepAliveState: String,
    val notificationState: String,
    val issues: List<SmsHealthIssue>
) {
    val isHealthy: Boolean
        get() = issues.isEmpty()

    companion object {
        fun from(
            context: Context,
            logs: List<ForwardLog>,
            pendingCount: Int,
            batteryOptimized: Boolean,
            accessibilityEnabled: Boolean,
            lastSuccessTimestamp: Long?,
            activeAccountCount: Int,
            keepAliveRunning: Boolean,
            hasReceiveSmsPermission: Boolean,
            hasReadSmsPermission: Boolean,
            canPostNotifications: Boolean
        ): SmsHealthUiState {
            val considered = logs.filterNot { it.filtered }
            val successCount = considered.count { it.emailSuccess || it.telegramSuccess }
            val failureCount = considered.count { !it.emailSuccess && !it.telegramSuccess }
            val successRate = if (considered.isEmpty()) {
                context.getString(R.string.value_none)
            } else {
                "${successCount * 100 / considered.size}%"
            }
            val issues = buildList {
                if (!hasReceiveSmsPermission) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Permission,
                            title = context.getString(R.string.issue_sms_broadcast_permission),
                            message = context.getString(R.string.issue_sms_broadcast_detail),
                            actionLabel = context.getString(R.string.action_app_info)
                        )
                    )
                }
                if (!hasReadSmsPermission) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Permission,
                            title = context.getString(R.string.issue_sms_read_permission),
                            message = context.getString(R.string.issue_sms_read_detail),
                            actionLabel = context.getString(R.string.action_app_info)
                        )
                    )
                }
                if (activeAccountCount == 0) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Account,
                            title = context.getString(R.string.issue_no_account),
                            message = context.getString(R.string.issue_no_account_detail),
                            actionLabel = context.getString(R.string.action_go_settings)
                        )
                    )
                }
                if (batteryOptimized) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Battery,
                            title = context.getString(R.string.issue_battery_optimized),
                            message = context.getString(R.string.issue_battery_detail),
                            actionLabel = context.getString(R.string.action_optimize)
                        )
                    )
                }
                if (!keepAliveRunning) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Guard,
                            title = context.getString(R.string.issue_guard_not_running),
                            message = context.getString(R.string.issue_guard_detail),
                            actionLabel = context.getString(R.string.action_start)
                        )
                    )
                }
                if (!accessibilityEnabled) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Accessibility,
                            title = context.getString(R.string.issue_accessibility_not_enabled),
                            message = context.getString(R.string.issue_accessibility_detail),
                            actionLabel = context.getString(R.string.action_enable)
                        )
                    )
                }
                if (!canPostNotifications) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Notification,
                            title = context.getString(R.string.issue_notification_permission),
                            message = context.getString(R.string.issue_notification_detail),
                            actionLabel = context.getString(R.string.action_app_info)
                        )
                    )
                }
                if (pendingCount > 0) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Pending,
                            title = context.getString(R.string.issue_pending_messages),
                            message = context.getString(R.string.issue_pending_detail),
                            actionLabel = context.getString(R.string.action_retry)
                        )
                    )
                }
                if (considered.size >= 3 && successCount == 0 && failureCount > 0) {
                    add(
                        SmsHealthIssue(
                            type = SmsHealthIssueType.Delivery,
                            title = context.getString(R.string.issue_delivery_failed),
                            message = context.getString(R.string.issue_delivery_detail),
                            actionLabel = context.getString(R.string.action_view_logs)
                        )
                    )
                }
            }
            val blockedBySetup = !hasReceiveSmsPermission || !hasReadSmsPermission || activeAccountCount == 0
            val statusLabel = when {
                blockedBySetup -> context.getString(R.string.status_needs_config)
                issues.isNotEmpty() -> context.getString(R.string.status_needs_optimization)
                considered.isEmpty() -> context.getString(R.string.status_waiting_sms)
                else -> context.getString(R.string.status_running)
            }
            val statusTitle = when {
                blockedBySetup -> context.getString(R.string.title_forward_not_ready)
                issues.isNotEmpty() -> context.getString(R.string.title_forward_can_optimize)
                considered.isEmpty() -> context.getString(R.string.title_waiting_first_sms)
                else -> context.getString(R.string.title_forward_running)
            }
            val statusMessage = when {
                blockedBySetup -> context.getString(R.string.detail_complete_permission_channel)
                issues.isNotEmpty() -> context.getString(R.string.detail_core_available_risks)
                considered.isEmpty() -> context.getString(R.string.detail_listening_ready)
                else -> context.getString(R.string.detail_all_systems_good)
            }
            return SmsHealthUiState(
                statusLabel = statusLabel,
                statusTitle = statusTitle,
                statusMessage = statusMessage,
                successRate = successRate,
                pendingCount = pendingCount,
                activeAccountCount = activeAccountCount,
                lastSuccessText = lastSuccessTimestamp?.let(DateFormatter::format) ?: context.getString(R.string.value_none),
                broadcastState = if (hasReceiveSmsPermission) context.getString(R.string.state_available_short) else context.getString(R.string.status_unauthorized),
                observerState = when {
                    !hasReadSmsPermission -> context.getString(R.string.status_unauthorized)
                    keepAliveRunning -> context.getString(R.string.state_guarding_short)
                    else -> context.getString(R.string.state_pending_start_short)
                },
                accessibilityState = if (accessibilityEnabled) context.getString(R.string.state_opened) else context.getString(R.string.state_not_opened),
                keepAliveState = if (keepAliveRunning) context.getString(R.string.status_running) else context.getString(R.string.status_not_running),
                notificationState = if (canPostNotifications) context.getString(R.string.state_can_remind) else context.getString(R.string.status_unauthorized),
                issues = issues
            )
        }
    }
}

private fun PendingForwardMessage.nextRetryText(context: Context): String =
    if (nextAttemptAt <= System.currentTimeMillis()) {
        context.getString(R.string.message_retry_time_already_reached)
    } else {
        context.getString(R.string.format_next_retry, DateFormatter.format(nextAttemptAt))
    }

private fun Context.startKeepAliveService() {
    val intent = Intent(this, SmsKeepAliveService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

private fun Context.isSmsAccessibilityServiceEnabled(): Boolean {
    val accessibilityEnabled = Settings.Secure.getInt(
        contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED,
        0
    ) == 1
    if (!accessibilityEnabled) {
        return false
    }

    val enabledServices = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    if (enabledServices.isBlank()) {
        return false
    }

    val targetComponent = ComponentName(this, SmsAccessibilityService::class.java)
    return enabledServices.split(':').any { rawComponent ->
        val enabledComponent = ComponentName.unflattenFromString(rawComponent)
        if (enabledComponent != null) {
            enabledComponent.packageName.equals(targetComponent.packageName, ignoreCase = true) &&
                enabledComponent.className.equals(targetComponent.className, ignoreCase = true)
        } else {
            rawComponent.equals(targetComponent.flattenToString(), ignoreCase = true)
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)

private fun Context.openAppDetails() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$packageName"))
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
