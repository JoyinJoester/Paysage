package joyin.takgi.paysage.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import joyin.takgi.paysage.R
import joyin.takgi.paysage.reliability.SmsReliabilityManager
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar

@Composable
fun PermissionManagementScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTick += 1
    }

    fun refresh() {
        refreshTick += 1
        statusMessage = context.getString(R.string.permission_status_refreshed)
    }

    fun requestRuntimePermissions(permissions: List<String>) {
        val pending = permissions
            .filter { context.hasRuntimePermission(it).not() }
            .toTypedArray()
        if (pending.isNotEmpty()) {
            permissionLauncher.launch(pending)
        } else {
            refresh()
        }
    }

    val permissions = remember(refreshTick) {
        buildPermissionItems(context)
    }
    val grantedCount = permissions.count { it.granted }
    val availableCount = permissions.count { it.available }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_permission_management_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                M3ePanel(modifier = Modifier.fillMaxWidth(), prominent = grantedCount == availableCount) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.permission_overview_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                R.string.permission_overview_format,
                                grantedCount,
                                availableCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(
                count = permissions.size,
                key = { permissions[it].id }
            ) { index ->
                val item = permissions[index]
                PermissionCard(
                    item = item,
                    onClick = {
                        when (item.action) {
                            PermissionAction.RequestRuntime -> requestRuntimePermissions(item.permissions)
                            PermissionAction.OpenAccessibility -> {
                                context.startSettingsSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                            PermissionAction.OpenNotificationListener -> {
                                context.startSettingsSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                            PermissionAction.OpenBattery -> {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    SmsReliabilityManager.openBatteryOptimizationWizard(activity)
                                } else {
                                    context.startSettingsSafely(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                            PermissionAction.OpenAppDetails -> {
                                context.openAppDetailsSafely()
                            }
                        }
                    }
                )
            }

            if (statusMessage.isNotBlank()) {
                item {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    onClick: () -> Unit
) {
    M3ePanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (!item.available) {
                                stringResource(R.string.permission_status_unavailable)
                            } else if (item.granted) {
                                stringResource(R.string.permission_status_granted)
                            } else {
                                stringResource(R.string.permission_status_missing)
                            }
                        )
                    }
                )
            }
            M3eActionButton(
                text = if (item.granted || !item.available) {
                    stringResource(R.string.action_view_settings)
                } else {
                    item.actionLabel
                },
                onClick = onClick,
                enabled = item.available,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class PermissionItem(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val permissions: List<String> = emptyList(),
    val granted: Boolean,
    val available: Boolean = true,
    val action: PermissionAction,
    val actionLabel: String
)

private enum class PermissionAction {
    RequestRuntime,
    OpenAccessibility,
    OpenNotificationListener,
    OpenBattery,
    OpenAppDetails
}

private fun buildPermissionItems(context: Context): List<PermissionItem> {
    val notificationAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    return listOf(
        PermissionItem(
            id = "sms",
            title = context.getString(R.string.permission_sms_title),
            summary = context.getString(R.string.permission_sms_summary),
            icon = Icons.Default.Sms,
            permissions = listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
            granted = context.hasRuntimePermission(Manifest.permission.RECEIVE_SMS) &&
                context.hasRuntimePermission(Manifest.permission.READ_SMS),
            action = PermissionAction.RequestRuntime,
            actionLabel = context.getString(R.string.action_request_permission)
        ),
        PermissionItem(
            id = "notification",
            title = context.getString(R.string.permission_notification_title),
            summary = context.getString(R.string.permission_notification_summary),
            icon = Icons.Default.Notifications,
            permissions = if (notificationAvailable) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList(),
            granted = !notificationAvailable ||
                context.hasRuntimePermission(Manifest.permission.POST_NOTIFICATIONS),
            available = notificationAvailable,
            action = PermissionAction.RequestRuntime,
            actionLabel = context.getString(R.string.action_request_permission)
        ),
        PermissionItem(
            id = "phone_state",
            title = context.getString(R.string.permission_phone_state_title),
            summary = context.getString(R.string.permission_phone_state_summary),
            icon = Icons.Default.Security,
            permissions = listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG),
            granted = context.hasRuntimePermission(Manifest.permission.READ_PHONE_STATE) &&
                context.hasRuntimePermission(Manifest.permission.READ_CALL_LOG),
            action = PermissionAction.RequestRuntime,
            actionLabel = context.getString(R.string.action_request_permission)
        ),
        PermissionItem(
            id = "accessibility",
            title = context.getString(R.string.permission_accessibility_title),
            summary = context.getString(R.string.permission_accessibility_summary),
            icon = Icons.Default.Accessibility,
            granted = context.isAccessibilityEnabled(),
            action = PermissionAction.OpenAccessibility,
            actionLabel = context.getString(R.string.action_open_accessibility_settings)
        ),
        PermissionItem(
            id = "notification_listener",
            title = context.getString(R.string.permission_notification_listener_title),
            summary = context.getString(R.string.permission_notification_listener_summary),
            icon = Icons.Default.NotificationsActive,
            granted = context.isNotificationListenerEnabled(),
            action = PermissionAction.OpenNotificationListener,
            actionLabel = context.getString(R.string.action_open_notification_listener_settings)
        ),
        PermissionItem(
            id = "battery",
            title = context.getString(R.string.permission_battery_title),
            summary = context.getString(R.string.permission_battery_summary),
            icon = Icons.Default.BatterySaver,
            granted = SmsReliabilityManager.isIgnoringBatteryOptimizations(context),
            action = PermissionAction.OpenBattery,
            actionLabel = context.getString(R.string.action_open_battery_settings)
        )
    )
}

private fun Context.hasRuntimePermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isNotificationListenerEnabled(): Boolean =
    androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(this)
        .contains(packageName)

private fun Context.isAccessibilityEnabled(): Boolean {
    val enabledServices = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabledServices.contains(packageName, ignoreCase = true)
}

private fun Context.openAppDetailsSafely() {
    startSettingsSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$packageName")),
        Intent(Settings.ACTION_APPLICATION_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
}

private fun Context.startSettingsSafely(vararg intents: Intent): Boolean {
    val activity = findActivity()
    val launchContext: Context = activity ?: this
    return intents.any { intent ->
        val launchIntent = Intent(intent)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { launchContext.startActivity(launchIntent) }.isSuccess
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
