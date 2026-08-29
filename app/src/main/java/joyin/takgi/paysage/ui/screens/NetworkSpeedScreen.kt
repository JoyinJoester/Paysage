package joyin.takgi.paysage.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import joyin.takgi.paysage.ui.components.M3eActionButton
import joyin.takgi.paysage.ui.components.M3ePanel
import joyin.takgi.paysage.ui.components.M3eTopBar
import joyin.takgi.paysage.util.NetworkSpeedMonitor
import joyin.takgi.paysage.util.PublicIpChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import java.util.Locale

/**
 * 网速仪表盘:参考 FlClash 的仪表盘逻辑。
 * 采样只读 /proc/net/dev 内核计数器(被动、零网络流量,不影响网速),
 * 采样协程绑定本页面,离开页面即停止检测。
 */
@Composable
fun NetworkSpeedScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monitor = remember { NetworkSpeedMonitor() }
    val snapshot by monitor.state.collectAsState()

    var publicIp by remember { mutableStateOf("") }
    var publicIpStatus by remember { mutableStateOf("") }
    var lanIps by remember { mutableStateOf<List<String>>(emptyList()) }

    // 采样协程绑定本页面生命周期:离开页面协程被取消,自动停止检测
    LaunchedEffect(Unit) {
        launch { monitor.run() }
        // NetworkInterface 枚举是阻塞 I/O,放 IO 线程避免主线程卡顿
        lanIps = withContext(Dispatchers.IO) { collectLanAddresses() }
        publicIpStatus = context.getString(R.string.status_public_ip_loading)
        PublicIpChecker.fetch()
            .onSuccess {
                publicIp = it
                publicIpStatus = ""
            }
            .onFailure {
                publicIp = ""
                publicIpStatus = context.getString(R.string.status_public_ip_failed)
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            M3eTopBar(
                title = stringResource(R.string.screen_network_speed_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    Text(
                        text = stringResource(
                            R.string.summary_counted_interfaces,
                            snapshot.countedInterfaces.joinToString(", ").ifBlank { "—" }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            M3ePanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.title_session_traffic),
                        style = MaterialTheme.typography.titleMedium,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                M3ePanel(modifier = Modifier.weight(1f)) {
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
                M3ePanel(modifier = Modifier.weight(1f)) {
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

            M3eActionButton(
                text = stringResource(R.string.action_refresh_public_ip),
                icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        publicIp = ""
                        publicIpStatus = context.getString(R.string.status_public_ip_loading)
                        PublicIpChecker.fetch()
                            .onSuccess { result ->
                                publicIp = result
                                publicIpStatus = ""
                            }
                            .onFailure {
                                publicIpStatus = context.getString(R.string.status_public_ip_failed)
                            }
                    }
                }
            )

            Text(
                text = stringResource(R.string.summary_network_speed_page),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
