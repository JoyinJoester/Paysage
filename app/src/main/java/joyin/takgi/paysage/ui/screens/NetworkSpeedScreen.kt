package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import joyin.takgi.paysage.R
import joyin.takgi.paysage.ui.navigation.PaysageBottomBar
import joyin.takgi.paysage.reliability.ForwardAccountAlertSender
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3eAlertDialog
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.util.DeviceStatus
import joyin.takgi.paysage.util.DeviceStatusCollector
import joyin.takgi.paysage.util.LatencyProbe
import joyin.takgi.paysage.util.NetworkSpeedMonitor
import joyin.takgi.paysage.util.NetworkSpeedSnapshot
import joyin.takgi.paysage.util.PublicIpChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import java.util.Locale

/**
 * 网速仪表盘:参考 FlClash 的仪表盘逻辑。
 * 采样读内核字节计数器(被动、零网络流量,不影响网速),
 * 采样协程绑定本页面,离开页面即停止检测。
 */
@Composable
fun NetworkSpeedScreen(
    onBackClick: (() -> Unit)? = null,
    selectedTab: Int? = null,
    onTabSelected: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monitor = remember { NetworkSpeedMonitor() }
    val snapshot by monitor.state.collectAsState()

    var publicIp by remember { mutableStateOf("") }
    var publicIpStatus by remember { mutableStateOf("") }
    var lanIps by remember { mutableStateOf<List<String>>(emptyList()) }
    var deviceStatus by remember { mutableStateOf<DeviceStatus?>(null) }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var latencyMeasuring by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var showPushDialog by remember { mutableStateOf(false) }
    val alertSender = remember { ForwardAccountAlertSender(context) }

    fun refreshPublicIp() {
        scope.launch {
            publicIp = ""
            publicIpStatus = context.getString(R.string.status_public_ip_loading)
            PublicIpChecker.fetch()
                .onSuccess {
                    publicIp = it
                    publicIpStatus = ""
                }
                .onFailure {
                    publicIp = ""
                    publicIpStatus = context.getString(
                        R.string.status_public_ip_failed_reason,
                        it.message?.take(120) ?: context.getString(R.string.status_public_ip_failed)
                    )
                }
        }
    }

    fun pushTo(useEmail: Boolean, useTelegram: Boolean) {
        scope.launch {
            val report = buildStatusReport(context, snapshot, deviceStatus, lanIps, publicIp, latencyMs)
            val ok = alertSender.sendTo(
                subject = "Paysage ${context.getString(R.string.screen_network_speed_title)}",
                body = report,
                useEmail = useEmail,
                useTelegram = useTelegram
            )
            statusMessage = context.getString(
                if (ok) R.string.message_push_done else R.string.message_push_failed
            )
        }
    }

    fun onPushClick() {
        scope.launch {
            val (emailConfigured, telegramConfigured) = alertSender.configuredTargets()
            when {
                !emailConfigured && !telegramConfigured ->
                    statusMessage = context.getString(R.string.message_push_no_channel)
                emailConfigured && telegramConfigured -> showPushDialog = true
                else -> pushTo(useEmail = emailConfigured, useTelegram = telegramConfigured)
            }
        }
    }

    // 采样协程绑定本页面生命周期:离开页面协程被取消,自动停止检测
    LaunchedEffect(Unit) {
        launch { monitor.run() }
        // NetworkInterface 枚举是阻塞 I/O,放 IO 线程避免主线程卡顿
        lanIps = withContext(Dispatchers.IO) { collectLanAddresses() }
        // 电池/温度每 3 秒刷新一次,同样仅页面停留期间
        launch {
            while (true) {
                deviceStatus = withContext(Dispatchers.IO) {
                    DeviceStatusCollector.collectStatus(context)
                }
                delay(3_000L)
            }
        }
        // 延迟每 10 秒探测一次(generate_204,几十字节)
        launch {
            while (true) {
                latencyMeasuring = true
                latencyMs = LatencyProbe.measure()
                latencyMeasuring = false
                delay(10_000L)
            }
        }
        refreshPublicIp()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selectedTab != null && onTabSelected != null) {
                PaysageBottomBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }
        },
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_network_speed_title),
                navigationIcon = if (onBackClick != null) {
                    {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                } else {
                    null
                },
                actions = {
                    IconButton(onClick = ::refreshPublicIp) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh_public_ip)
                        )
                    }
                    IconButton(onClick = ::onPushClick) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = stringResource(R.string.action_push_status)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
            M3ePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.title_net_speed),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "↑ ${formatBytesPerSec(snapshot.uploadBytesPerSec)}  ↓ ${formatBytesPerSec(snapshot.downloadBytesPerSec)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    SpeedCurve(
                        history = snapshot.history,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }
            }

            item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                M3ePanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.title_session_traffic),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TrafficLine(
                            label = stringResource(R.string.label_upload),
                            value = formatBytes(snapshot.uploadTotalBytes)
                        )
                        TrafficLine(
                            label = stringResource(R.string.label_download),
                            value = formatBytes(snapshot.downloadTotalBytes)
                        )
                    }
                }
                M3ePanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.title_latency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                latencyMeasuring -> stringResource(R.string.status_latency_measuring)
                                latencyMs != null -> "$latencyMs ms"
                                else -> stringResource(R.string.status_latency_timeout)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = latencyColor(latencyMs)
                        )
                    }
                }
            }
            }

            item {
            M3ePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.title_battery_status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val status = deviceStatus
                    if (status == null) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        TrafficLine(
                            label = stringResource(R.string.label_battery_level),
                            value = "${status.batteryLevel}%"
                        )
                        TrafficLine(
                            label = stringResource(R.string.label_battery_temperature),
                            value = String.format(Locale.US, "%.1f ℃", status.batteryTemperature)
                        )
                        TrafficLine(
                            label = stringResource(R.string.label_battery_voltage),
                            value = String.format(Locale.US, "%.2f V", status.batteryVoltage)
                        )
                        TrafficLine(
                            label = stringResource(R.string.label_charging),
                            value = stringResource(
                                if (status.isCharging) R.string.value_charging_yes else R.string.value_charging_no
                            )
                        )
                    }
                }
            }
            }

            item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                M3ePanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.title_lan_ip),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = lanIps.joinToString("\n").ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                M3ePanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.title_public_ip),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = publicIp.ifBlank {
                                publicIpStatus.ifBlank { "—" }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (publicIpStatus.isBlank()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            }

            item {
            Text(
                text = stringResource(R.string.summary_network_speed_page),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            }
        }
    }

    if (showPushDialog) {
        PushTargetDialog(
            onDismiss = { showPushDialog = false },
            onConfirm = { useEmail, useTelegram -> pushTo(useEmail, useTelegram) }
        )
    }
}

@Composable
private fun PushTargetDialog(
    onDismiss: () -> Unit,
    onConfirm: (useEmail: Boolean, useTelegram: Boolean) -> Unit
) {
    var useEmail by remember { mutableStateOf(true) }
    var useTelegram by remember { mutableStateOf(true) }

    M3eAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_push_status_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogCheckboxRow(
                    label = stringResource(R.string.push_target_telegram),
                    checked = useTelegram,
                    onCheckedChange = { useTelegram = it }
                )
                DialogCheckboxRow(
                    label = stringResource(R.string.push_target_email),
                    checked = useEmail,
                    onCheckedChange = { useEmail = it }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    onDismiss()
                    if (useEmail || useTelegram) {
                        onConfirm(useEmail, useTelegram)
                    }
                }
            ) { Text(stringResource(R.string.action_send)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DialogCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun buildStatusReport(
    context: android.content.Context,
    snapshot: NetworkSpeedSnapshot,
    deviceStatus: DeviceStatus?,
    lanIps: List<String>,
    publicIp: String,
    latencyMs: Long?
): String = buildString {
    appendLine(context.getString(R.string.screen_network_speed_title))
    appendLine()
    appendLine("${context.getString(R.string.label_upload)}: ${formatBytesPerSec(snapshot.uploadBytesPerSec)}")
    appendLine("${context.getString(R.string.label_download)}: ${formatBytesPerSec(snapshot.downloadBytesPerSec)}")
    appendLine(
        "${context.getString(R.string.title_session_traffic)}: " +
            "↑${formatBytes(snapshot.uploadTotalBytes)} / ↓${formatBytes(snapshot.downloadTotalBytes)}"
    )
    appendLine("${context.getString(R.string.title_latency)}: ${latencyMs?.toString() ?: "-"} ms")
    deviceStatus?.let { status ->
        appendLine("${context.getString(R.string.label_battery_level)}: ${status.batteryLevel}%")
        appendLine(
            "${context.getString(R.string.label_battery_temperature)}: " +
                String.format(java.util.Locale.US, "%.1f ℃", status.batteryTemperature)
        )
        appendLine(
            "${context.getString(R.string.label_charging)}: " +
                context.getString(
                    if (status.isCharging) R.string.value_charging_yes else R.string.value_charging_no
                )
        )
    }
    appendLine("${context.getString(R.string.title_lan_ip)}: ${lanIps.joinToString().ifBlank { "-" }}")
    appendLine("${context.getString(R.string.title_public_ip)}: ${publicIp.ifBlank { "-" }}")
    appendLine()
    appendLine(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
}

@Composable
private fun SpeedCurve(history: List<Long>, modifier: Modifier = Modifier) {
    val strokeColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val baselineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val baseline = 1024f
        val maxValue = (history.maxOrNull() ?: 0L).coerceAtLeast(baseline.toLong()).toFloat() * 1.2f
        val width = size.width
        val height = size.height

        drawLine(
            color = baselineColor,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1.dp.toPx()
        )
        if (history.size < 2) return@Canvas

        val stepX = width / (history.size - 1)
        val points = history.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - (value.toFloat() / maxValue) * (height - 8.dp.toPx()) - 4.dp.toPx()
            Offset(x, y)
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, height)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(listOf(fillColor, androidx.compose.ui.graphics.Color.Transparent)),
        )
        val strokePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        drawPath(strokePath, color = strokeColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun TrafficLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun collectLanAddresses(): List<String> =
    runCatching {
        val addresses = mutableSetOf<String>()
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching emptyList()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            networkInterface.inetAddresses.asSequence()
                .filter { it.isSiteLocalAddress && it.hostAddress?.contains(":") == false }
                .forEach { addresses.add(it.hostAddress.orEmpty()) }
        }
        addresses.filter { it.isNotBlank() }.sorted()
    }.getOrDefault(emptyList())

private fun latencyColor(latencyMs: Long?): androidx.compose.ui.graphics.Color = when {
    latencyMs == null -> androidx.compose.ui.graphics.Color(0xFFF87171)
    latencyMs < 200 -> androidx.compose.ui.graphics.Color(0xFF4ADE80)
    latencyMs < 500 -> androidx.compose.ui.graphics.Color(0xFFFBBF24)
    else -> androidx.compose.ui.graphics.Color(0xFFF87171)
}

private fun formatBytesPerSec(bytesPerSec: Long): String {
    if (bytesPerSec < 1024) return "$bytesPerSec B/s"
    return "${formatBytes(bytesPerSec)}/s"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}
