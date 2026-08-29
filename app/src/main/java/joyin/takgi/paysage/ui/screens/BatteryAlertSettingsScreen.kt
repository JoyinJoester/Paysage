package joyin.takgi.paysage.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.reliability.BatteryAlertReliabilityManager
import joyin.takgi.paysage.reliability.BatteryAlertSettingsStore
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eSettingsSwitchRow
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.util.DeviceStatus
import joyin.takgi.paysage.util.DeviceStatusCollector
import java.util.Locale

@Composable
fun BatteryAlertSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { BatteryAlertSettingsStore(context) }
    var settings by remember { mutableStateOf(store.read()) }
    var currentDeviceStatus by remember { mutableStateOf(DeviceStatusCollector.collectStatus(context)) }
    var lowBatteryInput by remember(settings.lowBatteryPercent) {
        mutableStateOf(settings.lowBatteryPercent.toString())
    }
    var highTemperatureInput by remember(settings.highTemperatureCelsius) {
        mutableStateOf(settings.highTemperatureCelsius.toString())
    }
    var continuousTemperatureIntervalInput by remember(settings.continuousTemperatureIntervalMinutes) {
        mutableStateOf(settings.continuousTemperatureIntervalMinutes.toString())
    }
    var statusMessage by remember { mutableStateOf("") }

    val lowBatteryValue = lowBatteryInput.toIntOrNull()
    val highTemperatureValue = highTemperatureInput.toIntOrNull()
    val continuousTemperatureIntervalValue = continuousTemperatureIntervalInput.toIntOrNull()
    val isLowBatteryInvalid = lowBatteryInput.isBlank() ||
        lowBatteryValue !in BatteryAlertSettingsStore.MIN_LOW_BATTERY_PERCENT..BatteryAlertSettingsStore.MAX_LOW_BATTERY_PERCENT
    val isTemperatureInvalid = highTemperatureInput.isBlank() ||
        highTemperatureValue !in BatteryAlertSettingsStore.MIN_HIGH_TEMPERATURE_CELSIUS..BatteryAlertSettingsStore.MAX_HIGH_TEMPERATURE_CELSIUS
    val isContinuousTemperatureIntervalInvalid = settings.continuousTemperatureAlertsEnabled &&
        (
            continuousTemperatureIntervalInput.isBlank() ||
                continuousTemperatureIntervalValue !in BatteryAlertSettingsStore.MIN_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES..
                BatteryAlertSettingsStore.MAX_CONTINUOUS_TEMPERATURE_INTERVAL_MINUTES
            )
    val canSave = !isLowBatteryInvalid &&
        !isTemperatureInvalid &&
        !isContinuousTemperatureIntervalInvalid

    fun persist() {
        if (!canSave) {
            statusMessage = context.getString(R.string.message_battery_alert_settings_invalid)
            return
        }
        val next = settings.copy(
            lowBatteryPercent = lowBatteryValue ?: settings.lowBatteryPercent,
            highTemperatureCelsius = highTemperatureValue ?: settings.highTemperatureCelsius,
            continuousTemperatureIntervalMinutes = continuousTemperatureIntervalValue
                ?: settings.continuousTemperatureIntervalMinutes
        )
        settings = next
        store.write(next)
        currentDeviceStatus = DeviceStatusCollector.collectStatus(context)
        BatteryAlertReliabilityManager.ensureScheduled(context)
        BatteryAlertReliabilityManager.enqueueImmediateCheck(context, bypassCooldown = true)
        statusMessage = context.getString(R.string.message_battery_alert_settings_saved)
    }

    fun updateEnabled(enabled: Boolean) {
        val next = settings.copy(enabled = enabled)
        settings = next
        store.write(next)
        BatteryAlertReliabilityManager.ensureScheduled(context)
        if (enabled) {
            BatteryAlertReliabilityManager.enqueueImmediateCheck(context, bypassCooldown = true)
        }
        statusMessage = context.getString(R.string.message_battery_alert_settings_saved)
    }

    fun updateContinuousTemperatureAlerts(enabled: Boolean) {
        val next = settings.copy(continuousTemperatureAlertsEnabled = enabled)
        settings = next
        store.write(next)
        BatteryAlertReliabilityManager.ensureScheduled(context)
        if (settings.enabled && enabled) {
            BatteryAlertReliabilityManager.enqueueImmediateCheck(context)
        }
        statusMessage = context.getString(R.string.message_battery_alert_settings_saved)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.section_battery_alerts),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.detail_battery_alert_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.label_battery_alerts_enabled),
                            summary = if (settings.enabled) {
                                stringResource(R.string.summary_battery_alerts_enabled)
                            } else {
                                stringResource(R.string.summary_battery_alerts_disabled)
                            },
                            checked = settings.enabled,
                            onCheckedChange = ::updateEnabled
                        )
                        InfoLine(
                            stringResource(R.string.label_current_battery_state),
                            currentBatteryStateText(context, currentDeviceStatus)
                        )
                    }
                }
            }

            item {
                M3ePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lowBatteryInput,
                            onValueChange = { value ->
                                lowBatteryInput = value.filter(Char::isDigit).take(3)
                            },
                            enabled = settings.enabled,
                            label = { Text(stringResource(R.string.label_low_battery_threshold)) },
                            suffix = { Text("%") },
                            singleLine = true,
                            isError = isLowBatteryInvalid,
                            supportingText = {
                                Text(stringResource(R.string.helper_low_battery_threshold_range))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = highTemperatureInput,
                            onValueChange = { value ->
                                highTemperatureInput = value.filter(Char::isDigit).take(3)
                            },
                            enabled = settings.enabled,
                            label = { Text(stringResource(R.string.label_high_temperature_threshold)) },
                            suffix = { Text("°C") },
                            singleLine = true,
                            isError = isTemperatureInvalid,
                            supportingText = {
                                Text(stringResource(R.string.helper_high_temperature_threshold_range))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        M3eSettingsSwitchRow(
                            title = stringResource(R.string.label_continuous_temperature_alerts_enabled),
                            summary = if (settings.continuousTemperatureAlertsEnabled) {
                                stringResource(R.string.summary_continuous_temperature_alerts_enabled)
                            } else {
                                stringResource(R.string.summary_continuous_temperature_alerts_disabled)
                            },
                            checked = settings.continuousTemperatureAlertsEnabled,
                            enabled = settings.enabled,
                            onCheckedChange = ::updateContinuousTemperatureAlerts
                        )
                        OutlinedTextField(
                            value = continuousTemperatureIntervalInput,
                            onValueChange = { value ->
                                continuousTemperatureIntervalInput = value.filter(Char::isDigit).take(3)
                            },
                            enabled = settings.enabled && settings.continuousTemperatureAlertsEnabled,
                            label = { Text(stringResource(R.string.label_continuous_temperature_interval)) },
                            suffix = { Text(stringResource(R.string.unit_minutes)) },
                            singleLine = true,
                            isError = isContinuousTemperatureIntervalInvalid,
                            supportingText = {
                                Text(stringResource(R.string.helper_continuous_temperature_interval_range))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        M3eActionButton(
                            text = stringResource(R.string.action_save),
                            onClick = ::persist,
                            enabled = settings.enabled && canSave,
                            prominent = true,
                            icon = Icons.Default.Save,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (statusMessage.isNotBlank()) {
                item {
                    M3ePanel(modifier = Modifier.fillMaxWidth(), elevated = false) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun currentBatteryStateText(
    context: Context,
    status: DeviceStatus
): String {
    val level = if (status.batteryLevel >= 0) {
        "${status.batteryLevel}%"
    } else {
        context.getString(R.string.status_unavailable)
    }
    val temperature = if (status.batteryTemperature >= 0) {
        String.format(Locale.getDefault(), "%.1f°C", status.batteryTemperature)
    } else {
        context.getString(R.string.status_unavailable)
    }
    return context.getString(R.string.format_current_battery_state, level, temperature)
}
