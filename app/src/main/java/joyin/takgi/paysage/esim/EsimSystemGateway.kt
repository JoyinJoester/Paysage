package joyin.takgi.paysage.esim

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.se.omapi.SEService
import android.telephony.SubscriptionManager
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager
import joyin.takgi.paysage.R
import joyin.takgi.paysage.esim.ccid.UsbCcidAtrReader
import joyin.takgi.paysage.esim.ccid.UsbCcidApduResult
import joyin.takgi.paysage.esim.ccid.UsbCcidAtrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executor
import java.util.NoSuchElementException
import kotlin.coroutines.resume

data class EsimSupportState(
    val hasTelephonySubscriptionFeature: Boolean,
    val hasEuiccFeature: Boolean,
    val hasMepFeature: Boolean,
    val hasUsbHostFeature: Boolean,
    val hasOmapiUiccFeature: Boolean,
    val euiccManagerEnabled: Boolean,
    val canOpenManagement: Boolean,
    val canOpenQrActivation: Boolean
) {
    val isReady: Boolean
        get() = hasEuiccFeature && euiccManagerEnabled && canOpenManagement

    val canRequestProfileDownload: Boolean
        get() = hasEuiccFeature && euiccManagerEnabled
}

data class EsimDownloadStartResult(
    val started: Boolean,
    val requestId: String,
    val message: String
)

data class EsimSubscriptionSummary(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val simSlotIndex: Int,
    val cardId: Int?,
    val portIndex: Int?,
    val canManage: Boolean,
    val isEmbedded: Boolean,
    val countryIso: String
)

data class EsimUsbCcidReaderSummary(
    val deviceName: String,
    val productName: String,
    val manufacturerName: String,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val interfaceCount: Int,
    val ccidInterfaceCount: Int,
    val hasPermission: Boolean
)

data class EsimOmapiReaderSummary(
    val name: String,
    val isUicc: Boolean,
    val isSecureElementPresent: Boolean
)

data class EsimIsdRProbeResult(
    val success: Boolean,
    val message: String,
    val statusWord: String?,
    val responseByteCount: Int,
    val atrHex: String?,
    val fciSummary: String? = null,
    val diagnosticSteps: List<EsimApduDiagnosticStep> = emptyList()
)

class EsimSystemGateway(private val context: Context) {
    private val packageManager = context.packageManager
    private val euiccManager = context.getSystemService(EuiccManager::class.java)

    fun supportState(): EsimSupportState {
        val hasFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC)
        return EsimSupportState(
            hasTelephonySubscriptionFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION),
            hasEuiccFeature = hasFeature,
            hasMepFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC_MEP),
            hasUsbHostFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
            hasOmapiUiccFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_SE_OMAPI_UICC),
            euiccManagerEnabled = hasFeature && euiccManager?.isEnabled == true,
            canOpenManagement = canResolve(manageIntent()),
            canOpenQrActivation = canResolve(qrActivationIntent())
        )
    }

    fun euiccInfoSummary(): EsimEuiccInfoSummary {
        val manager = euiccManager
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC) || manager == null) {
            return unavailableEuiccInfo("当前设备未声明 eUICC 功能。")
        }
        if (!manager.isEnabled) {
            return unavailableEuiccInfo("系统 eSIM 服务未启用，暂时无法读取 eUICC 信息摘要。")
        }

        val osVersion = runCatching {
            manager.euiccInfo?.osVersion?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()
        val memory = manager.safeAvailableMemory()
        val ports = manager.safePortSummaries(
            hasMepFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC_MEP)
        )
        val hasDetails = osVersion != null || memory.bytes != null || ports.isNotEmpty()

        return EsimEuiccInfoSummary(
            available = true,
            message = if (hasDetails) {
                "已通过 Android 公开 eUICC 信息接口读取设备能力摘要。"
            } else {
                "系统 eSIM 服务可用，但没有公开更多 eUICC 细节。"
            },
            osVersion = osVersion,
            memory = memory,
            ports = ports
        )
    }

    fun readEidOnUserRequest(): EsimEidReadResult {
        val manager = euiccManager ?: return EsimEidReadResult(
            status = EsimEidReadStatus.Unavailable,
            eid = null,
            message = "当前系统没有 eSIM 管理服务。"
        )
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC)) {
            return EsimEidReadResult(
                status = EsimEidReadStatus.Unavailable,
                eid = null,
                message = "当前设备未声明 eUICC 功能。"
            )
        }
        if (!manager.isEnabled) {
            return EsimEidReadResult(
                status = EsimEidReadStatus.Unavailable,
                eid = null,
                message = "系统 eSIM 服务未启用，无法读取 EID。"
            )
        }

        return try {
            val eid = manager.eid?.let(EsimEidFormatter::normalize).orEmpty()
            if (eid.isBlank()) {
                EsimEidReadResult(
                    status = EsimEidReadStatus.Unavailable,
                    eid = null,
                    message = "系统没有公开 EID。"
                )
            } else {
                EsimEidReadResult(
                    status = EsimEidReadStatus.Succeeded,
                    eid = eid,
                    message = "已从 Android eSIM 服务读取 EID，仅保留在当前界面状态中。"
                )
            }
        } catch (_: SecurityException) {
            EsimEidReadResult(
                status = EsimEidReadStatus.PermissionDenied,
                eid = null,
                message = "系统拒绝当前应用读取 EID，可能需要运营商权限或系统授权。"
            )
        } catch (_: UnsupportedOperationException) {
            EsimEidReadResult(
                status = EsimEidReadStatus.Unavailable,
                eid = null,
                message = "当前系统不支持通过公开接口读取 EID。"
            )
        } catch (_: RuntimeException) {
            EsimEidReadResult(
                status = EsimEidReadStatus.Failed,
                eid = null,
                message = "读取 EID 失败，请打开系统 eSIM 管理页查看。"
            )
        }
    }

    fun openManagement(activity: Activity): Boolean = start(activity, manageIntent())

    fun openQrActivation(activity: Activity): Boolean = start(activity, qrActivationIntent())

    fun usbCcidReaders(): List<EsimUsbCcidReaderSummary> {
        val usbManager = context.getSystemService(UsbManager::class.java) ?: return emptyList()
        return usbManager.deviceList.values
            .mapNotNull { device -> device.toCcidSummary(usbManager) }
            .sortedWith(compareBy({ it.manufacturerName }, { it.productName }, { it.deviceName }))
    }

    fun requestUsbPermission(deviceName: String): EsimDownloadStartResult {
        val usbManager = context.getSystemService(UsbManager::class.java) ?: return EsimDownloadStartResult(
            started = false,
            requestId = "",
            message = "当前系统没有 USB 管理服务。"
        )
        val device = usbManager.deviceList[deviceName] ?: return EsimDownloadStartResult(
            started = false,
            requestId = "",
            message = "未找到该 USB 读卡器，请重新刷新。"
        )
        if (usbManager.hasPermission(device)) {
            return EsimDownloadStartResult(
                started = true,
                requestId = deviceName,
                message = "USB 读卡器已授权。"
            )
        }

        val intent = Intent(context, EsimUsbPermissionReceiver::class.java)
            .setAction(EsimUsbPermissionReceiver.ACTION_USB_PERMISSION)
            .putExtra(EsimUsbPermissionReceiver.EXTRA_DEVICE_NAME, deviceName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        val pendingIntent = PendingIntent.getBroadcast(context, deviceName.hashCode(), intent, flags)
        usbManager.requestPermission(device, pendingIntent)
        return EsimDownloadStartResult(
            started = true,
            requestId = deviceName,
            message = "已请求 USB 读卡器授权，请在系统弹窗中确认。"
        )
    }

    suspend fun readUsbAtr(deviceName: String): UsbCcidAtrResult = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(UsbManager::class.java)
            ?: return@withContext UsbCcidAtrResult(false, "当前系统没有 USB 管理服务。", null)
        val device = usbManager.deviceList[deviceName]
            ?: return@withContext UsbCcidAtrResult(false, "未找到该 USB 读卡器，请重新刷新。", null)
        UsbCcidAtrReader(usbManager).readAtr(device)
    }

    suspend fun probeUsbIsdR(deviceName: String): EsimIsdRProbeResult = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(UsbManager::class.java)
            ?: return@withContext EsimIsdRProbeResult(false, "当前系统没有 USB 管理服务。", null, 0, null)
        val device = usbManager.deviceList[deviceName]
            ?: return@withContext EsimIsdRProbeResult(false, "未找到该 USB 读卡器，请重新刷新。", null, 0, null)
        UsbCcidAtrReader(usbManager).selectIsdR(device).toProbeResult()
    }

    suspend fun omapiReaderSummaries(): List<EsimOmapiReaderSummary> = withContext(Dispatchers.IO) {
        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
            connectOmapiService()
        } ?: return@withContext emptyList()

        try {
            service.getReaders().orEmpty().map { reader ->
                val name = reader.name.orEmpty()
                EsimOmapiReaderSummary(
                    name = name.ifBlank { "Unnamed reader" },
                    isUicc = name.startsWith("SIM", ignoreCase = true) ||
                        name.contains("UICC", ignoreCase = true),
                    isSecureElementPresent = runCatching { reader.isSecureElementPresent }.getOrDefault(false)
                )
            }
        } finally {
            service.shutdown()
        }
    }

    suspend fun probeOmapiIsdR(readerName: String): EsimIsdRProbeResult = withContext(Dispatchers.IO) {
        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
            connectOmapiService()
        } ?: return@withContext EsimIsdRProbeResult(false, "OMAPI 服务连接超时。", null, 0, null)

        var session: android.se.omapi.Session? = null
        var channel: android.se.omapi.Channel? = null
        try {
            val steps = mutableListOf<EsimApduDiagnosticStep>()
            val reader = service.getReaders().orEmpty().firstOrNull { reader ->
                reader.name.orEmpty().ifBlank { "Unnamed reader" } == readerName
            }
                ?: return@withContext EsimIsdRProbeResult(false, "未找到该 OMAPI reader，请重新检测。", null, 0, null)
            val present = runCatching { reader.isSecureElementPresent }.getOrDefault(false)
            if (!present) {
                return@withContext EsimIsdRProbeResult(false, "该 reader 当前没有可用安全元素。", null, 0, null)
            }

            session = reader.openSession()
            val atrHex = runCatching {
                session.getATR()?.takeIf { it.isNotEmpty() }?.toHex()
            }.getOrNull()
            steps += EsimApduDiagnostics.omapiSession(
                success = true,
                message = "已打开 OMAPI session。",
                atrHex = atrHex
            )
            channel = session.openLogicalChannel(EsimApdu.ISD_R_AID)
            if (channel == null) {
                val message = "OMAPI 未能打开 ISD-R 逻辑通道。"
                return@withContext EsimIsdRProbeResult(
                    success = false,
                    message = message,
                    statusWord = null,
                    responseByteCount = 0,
                    atrHex = atrHex,
                    diagnosticSteps = steps + EsimApduDiagnostics.failed("SELECT ISD-R", message)
                )
            }
            val selectResponse = channel.getSelectResponse()
            if (selectResponse == null) {
                val message = "已打开 ISD-R 逻辑通道，但系统未暴露 SELECT 响应。"
                return@withContext EsimIsdRProbeResult(
                    success = true,
                    message = message,
                    statusWord = null,
                    responseByteCount = 0,
                    atrHex = atrHex,
                    diagnosticSteps = steps + EsimApduDiagnostics.skipped("SELECT ISD-R", message)
                )
            }
            val parsedResponse = EsimApdu.parseResponse(selectResponse)
            val fciSummary = EsimFciAnalyzer.summarize(parsedResponse.data)?.diagnosticText
            steps += EsimApduDiagnostics.selectIsdR(parsedResponse, fciSummary)
            EsimIsdRProbeResult(
                success = parsedResponse.isAccepted,
                message = parsedResponse.toIsdRMessage(),
                statusWord = parsedResponse.statusWordHex,
                responseByteCount = parsedResponse.data.size,
                atrHex = atrHex,
                fciSummary = fciSummary,
                diagnosticSteps = steps
            )
        } catch (_: SecurityException) {
            EsimIsdRProbeResult(
                success = false,
                message = "系统 OMAPI 访问规则拒绝 Paysage 访问 ISD-R，需要设备厂商或运营商授权。",
                statusWord = null,
                responseByteCount = 0,
                atrHex = null
            )
        } catch (_: NoSuchElementException) {
            EsimIsdRProbeResult(false, "该安全元素未发现 ISD-R AID。", null, 0, null)
        } catch (_: UnsupportedOperationException) {
            EsimIsdRProbeResult(false, "当前 OMAPI 实现不支持该 ISD-R 通道参数。", null, 0, null)
        } catch (_: IllegalArgumentException) {
            EsimIsdRProbeResult(false, "系统未接受 ISD-R AID 参数。", null, 0, null)
        } catch (_: IOException) {
            EsimIsdRProbeResult(false, "OMAPI 与安全元素通信失败。", null, 0, null)
        } catch (_: IllegalStateException) {
            EsimIsdRProbeResult(false, "OMAPI 服务状态异常，请重新检测后再试。", null, 0, null)
        } finally {
            channel?.close()
            session?.close()
            service.shutdown()
        }
    }

    @SuppressLint("MissingPermission")
    fun activeSubscriptionSummaries(): List<EsimSubscriptionSummary> {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            ?: return emptyList()
        return subscriptionManager.activeSubscriptionInfoList.orEmpty().map { info ->
            info.toSubscriptionSummary(subscriptionManager)
        }
    }

    @SuppressLint("MissingPermission")
    fun accessibleSubscriptionSummaries(): List<EsimSubscriptionSummary> {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            ?: return emptyList()
        return try {
            subscriptionManager.getAccessibleSubscriptionInfoList().orEmpty()
                .filter { it.isEmbedded }
                .map { info -> info.toSubscriptionSummary(subscriptionManager) }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: UnsupportedOperationException) {
            emptyList()
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun requestProfileDownload(
        activationCode: EsimActivationCode,
        switchAfterDownload: Boolean
    ): EsimDownloadStartResult {
        val manager = euiccManager ?: return EsimDownloadStartResult(
            started = false,
            requestId = "",
            message = context.getString(R.string.message_esim_management_service_unavailable)
        )
        if (!activationCode.isValid) {
            return EsimDownloadStartResult(
                started = false,
                requestId = "",
                message = context.getString(R.string.message_esim_activation_code_incomplete)
            )
        }
        if (!manager.isEnabled) {
            return EsimDownloadStartResult(
                started = false,
                requestId = "",
                message = context.getString(R.string.message_esim_service_disabled)
            )
        }

        val requestId = "download-${System.currentTimeMillis()}"
        val subscription = DownloadableSubscription.Builder(activationCode.encoded)
            .apply {
                activationCode.confirmationCode?.let { setConfirmationCode(it) }
            }
            .build()
        val callbackIntent = EsimDownloadCallbackReceiver.pendingIntent(context, requestId)
        val resultStore = EsimDownloadResultStore(context)
        resultStore.markPending(requestId, context.getString(R.string.message_esim_result_pending))

        return try {
            manager.downloadSubscription(subscription, switchAfterDownload, callbackIntent)
            EsimDownloadStartResult(
                started = true,
                requestId = requestId,
                message = context.getString(R.string.message_esim_operation_submitted_confirmation)
            )
        } catch (_: UnsupportedOperationException) {
            val message = context.getString(R.string.message_esim_download_unsupported)
            resultStore.write(EsimDownloadResultMapper.failure(requestId, message))
            EsimDownloadStartResult(false, requestId, message)
        } catch (_: SecurityException) {
            val message = context.getString(R.string.message_esim_download_security_denied)
            resultStore.write(EsimDownloadResultMapper.failure(requestId, message))
            EsimDownloadStartResult(false, requestId, message)
        } catch (_: IllegalArgumentException) {
            val message = context.getString(R.string.message_esim_download_rejected)
            resultStore.write(EsimDownloadResultMapper.failure(requestId, message))
            EsimDownloadStartResult(false, requestId, message)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestSwitchSubscription(
        subscriptionId: Int,
        portIndex: Int? = null,
        cardId: Int? = null
    ): EsimDownloadStartResult =
        requestEuiccOperation(
            requestIdPrefix = if (portIndex == null) "switch" else "switch-port-$portIndex",
            pendingMessage = context.getString(R.string.message_esim_result_pending),
            successMessage = context.getString(R.string.message_esim_operation_submitted_confirmation),
            unsupportedMessage = context.getString(R.string.message_esim_operation_unsupported),
            securityMessage = context.getString(R.string.message_esim_operation_security_denied),
            illegalMessage = context.getString(R.string.message_esim_operation_rejected),
            cardId = cardId
        ) { manager, callback ->
            if (portIndex == null) {
                manager.switchToSubscription(subscriptionId, callback)
            } else {
                manager.switchToSubscriptionOnPort(subscriptionId, portIndex, callback)
            }
        }

    @SuppressLint("MissingPermission")
    fun requestDeleteSubscription(subscriptionId: Int, cardId: Int? = null): EsimDownloadStartResult =
        requestEuiccOperation(
            requestIdPrefix = "delete",
            pendingMessage = context.getString(R.string.message_esim_result_pending),
            successMessage = context.getString(R.string.message_esim_operation_submitted_confirmation),
            unsupportedMessage = context.getString(R.string.message_esim_operation_unsupported),
            securityMessage = context.getString(R.string.message_esim_operation_security_denied),
            illegalMessage = context.getString(R.string.message_esim_operation_rejected),
            cardId = cardId
        ) { manager, callback ->
            manager.deleteSubscription(subscriptionId, callback)
        }

    @SuppressLint("MissingPermission")
    fun requestRenameSubscription(
        subscriptionId: Int,
        nickname: String,
        cardId: Int? = null
    ): EsimDownloadStartResult {
        val cleanNickname = nickname.trim()
        if (cleanNickname.isBlank()) {
            return EsimDownloadStartResult(
                started = false,
                requestId = "",
                message = context.getString(R.string.message_esim_rename_blank)
            )
        }

        return requestEuiccOperation(
            requestIdPrefix = "rename",
            pendingMessage = context.getString(R.string.message_esim_result_pending),
            successMessage = context.getString(R.string.message_esim_operation_submitted_confirmation),
            unsupportedMessage = context.getString(R.string.message_esim_operation_unsupported),
            securityMessage = context.getString(R.string.message_esim_operation_security_denied),
            illegalMessage = context.getString(R.string.message_esim_operation_rejected),
            cardId = cardId
        ) { manager, callback ->
            manager.updateSubscriptionNickname(subscriptionId, cleanNickname, callback)
        }
    }

    private fun requestEuiccOperation(
        requestIdPrefix: String,
        pendingMessage: String,
        successMessage: String,
        unsupportedMessage: String,
        securityMessage: String,
        illegalMessage: String,
        cardId: Int? = null,
        operation: (EuiccManager, android.app.PendingIntent) -> Unit
    ): EsimDownloadStartResult {
        val manager = euiccManagerForCardId(cardId) ?: return EsimDownloadStartResult(
            started = false,
            requestId = "",
            message = context.getString(R.string.message_esim_management_service_unavailable)
        )
        if (!manager.isEnabled) {
            return EsimDownloadStartResult(
                started = false,
                requestId = "",
                message = context.getString(R.string.message_esim_service_disabled)
            )
        }

        val requestId = "$requestIdPrefix-${System.currentTimeMillis()}"
        val callbackIntent = EsimDownloadCallbackReceiver.pendingIntent(context, requestId)
        val resultStore = EsimDownloadResultStore(context)
        resultStore.markPending(requestId, pendingMessage)

        return try {
            operation(manager, callbackIntent)
            EsimDownloadStartResult(true, requestId, successMessage)
        } catch (_: UnsupportedOperationException) {
            resultStore.write(EsimDownloadResultMapper.failure(requestId, unsupportedMessage))
            EsimDownloadStartResult(false, requestId, unsupportedMessage)
        } catch (_: SecurityException) {
            resultStore.write(EsimDownloadResultMapper.failure(requestId, securityMessage))
            EsimDownloadStartResult(false, requestId, securityMessage)
        } catch (_: IllegalArgumentException) {
            resultStore.write(EsimDownloadResultMapper.failure(requestId, illegalMessage))
            EsimDownloadStartResult(false, requestId, illegalMessage)
        }
    }

    private fun euiccManagerForCardId(cardId: Int?): EuiccManager? {
        val manager = euiccManager ?: return null
        val validCardId = cardId?.takeIf { it >= 0 } ?: return manager
        return try {
            manager.createForCardId(validCardId)
        } catch (_: IllegalArgumentException) {
            manager
        } catch (_: UnsupportedOperationException) {
            manager
        } catch (_: RuntimeException) {
            manager
        }
    }

    private fun manageIntent(): Intent =
        Intent(EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS)

    private fun qrActivationIntent(): Intent =
        Intent(EuiccManager.ACTION_START_EUICC_ACTIVATION)
            .putExtra(EuiccManager.EXTRA_USE_QR_SCANNER, true)

    private fun canResolve(intent: Intent): Boolean =
        intent.resolveActivity(packageManager) != null

    private fun start(activity: Activity, intent: Intent): Boolean {
        return try {
            activity.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private suspend fun connectOmapiService(): SEService = suspendCancellableCoroutine { continuation ->
        var service: SEService? = null
        val executor = Executor { runnable -> runnable.run() }
        val listener = SEService.OnConnectedListener {
            val connectedService = service
            if (connectedService != null && continuation.isActive) {
                continuation.resume(connectedService)
            }
        }
        service = SEService(context, executor, listener)
        continuation.invokeOnCancellation {
            service?.shutdown()
        }
    }

    companion object {
        private const val OMAPI_CONNECT_TIMEOUT_MS = 3_000L
    }
}

private fun unavailableEuiccInfo(message: String): EsimEuiccInfoSummary =
    EsimEuiccInfoSummary(
        available = false,
        message = message,
        osVersion = null,
        memory = EsimEuiccInfoFormatter.unavailableMemory("无法读取 eUICC 可用空间。"),
        ports = emptyList()
    )

private fun EuiccManager.safeAvailableMemory(): EsimEuiccMemorySummary {
    val method = javaClass.methods.firstOrNull { method ->
        method.name == "getAvailableMemoryInBytes" && method.parameterTypes.isEmpty()
    } ?: return EsimEuiccInfoFormatter.memory(null)

    return try {
        val bytes = method.invoke(this) as? Long
        EsimEuiccInfoFormatter.memory(bytes)
    } catch (error: Exception) {
        val root = error.rootCause()
        EsimEuiccInfoFormatter.unavailableMemory(
            message = when (root) {
                is SecurityException -> "系统拒绝当前应用读取 eUICC 可用空间。"
                is UnsupportedOperationException -> "当前系统不支持读取 eUICC 可用空间。"
                else -> "读取 eUICC 可用空间失败。"
            }
        )
    }
}

private fun EuiccManager.safePortSummaries(hasMepFeature: Boolean): List<EsimEuiccPortSummary> {
    val method = javaClass.methods.firstOrNull { method ->
        method.name == "isSimPortAvailable" &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0] == Integer.TYPE
    } ?: return emptyList()

    val candidatePorts = if (hasMepFeature) 0..2 else 0..0
    return candidatePorts.map { portIndex ->
        try {
            val available = method.invoke(this, portIndex) as? Boolean
            val availability = if (available == true) {
                EsimEuiccPortAvailability.Available
            } else {
                EsimEuiccPortAvailability.Unavailable
            }
            EsimEuiccPortSummary(
                portIndex = portIndex,
                availability = availability,
                message = EsimEuiccInfoFormatter.portMessage(portIndex, availability)
            )
        } catch (error: Exception) {
            val root = error.rootCause()
            val message = when (root) {
                is SecurityException -> "系统拒绝读取端口 ${portIndex + 1} 状态。"
                is IllegalArgumentException -> "系统未接受端口 ${portIndex + 1} 探测。"
                is UnsupportedOperationException -> "当前系统不支持端口 ${portIndex + 1} 状态探测。"
                else -> EsimEuiccInfoFormatter.portMessage(portIndex, EsimEuiccPortAvailability.Unknown)
            }
            EsimEuiccPortSummary(
                portIndex = portIndex,
                availability = EsimEuiccPortAvailability.Unknown,
                message = message
            )
        }
    }
}

private fun EuiccManager.switchToSubscriptionOnPort(
    subscriptionId: Int,
    portIndex: Int,
    callback: PendingIntent
) {
    require(portIndex >= 0) { "Port index must not be negative" }
    val method = javaClass.methods.firstOrNull { method ->
        method.name == "switchToSubscription" &&
            method.parameterTypes.size == 3 &&
            method.parameterTypes[0] == Integer.TYPE &&
            method.parameterTypes[1] == Integer.TYPE &&
            method.parameterTypes[2] == PendingIntent::class.java
    } ?: throw UnsupportedOperationException("Port-aware eSIM switching is not supported by this Android version.")

    try {
        method.invoke(this, subscriptionId, portIndex, callback)
    } catch (error: InvocationTargetException) {
        throw error.targetException ?: error
    }
}

private fun Throwable.rootCause(): Throwable =
    if (this is InvocationTargetException && targetException != null) {
        targetException
    } else {
        this
    }

private fun UsbDevice.toCcidSummary(usbManager: UsbManager): EsimUsbCcidReaderSummary? {
    val ccidInterfaces = (0 until interfaceCount)
        .map { getInterface(it) }
        .count { it.interfaceClass == UsbConstants.USB_CLASS_CSCID }
    val deviceIsCcid = deviceClass == UsbConstants.USB_CLASS_CSCID
    if (!deviceIsCcid && ccidInterfaces == 0) return null

    return EsimUsbCcidReaderSummary(
        deviceName = deviceName,
        productName = safeUsbString { productName }.ifBlank { "USB CCID Reader" },
        manufacturerName = safeUsbString { manufacturerName }.ifBlank { "Unknown vendor" },
        vendorId = vendorId,
        productId = productId,
        deviceClass = deviceClass,
        interfaceCount = interfaceCount,
        ccidInterfaceCount = ccidInterfaces,
        hasPermission = usbManager.hasPermission(this)
    )
}

private fun UsbCcidApduResult.toProbeResult(): EsimIsdRProbeResult =
    EsimIsdRProbeResult(
        success = success,
        message = message,
        statusWord = statusWord,
        responseByteCount = responseByteCount,
        atrHex = atrHex,
        fciSummary = fciSummary,
        diagnosticSteps = diagnosticSteps
    )

private fun safeUsbString(block: () -> String?): String =
    runCatching { block().orEmpty() }.getOrDefault("")

private fun Any.toSubscriptionSummary(subscriptionManager: SubscriptionManager): EsimSubscriptionSummary {
    val info = this as android.telephony.SubscriptionInfo
    return EsimSubscriptionSummary(
        subscriptionId = info.subscriptionId,
        displayName = info.displayName?.toString().orEmpty().ifBlank { "未命名订阅" },
        carrierName = info.carrierName?.toString().orEmpty().ifBlank { "未知运营商" },
        simSlotIndex = info.simSlotIndex,
        cardId = info.safeCardId(),
        portIndex = info.safePortIndex(),
        canManage = subscriptionManager.safeCanManage(info),
        isEmbedded = info.isEmbedded,
        countryIso = info.countryIso.orEmpty().uppercase()
    )
}

private fun SubscriptionManager.safeCanManage(info: android.telephony.SubscriptionInfo): Boolean =
    try {
        canManageSubscription(info)
    } catch (_: SecurityException) {
        false
    } catch (_: UnsupportedOperationException) {
        false
    } catch (_: RuntimeException) {
        false
    }

private fun Any.safeCardId(): Int? =
    safeIntMethod("getCardId")?.takeIf { it >= 0 }

private fun Any.safePortIndex(): Int? =
    safeIntMethod("getPortIndex")?.takeIf { it >= 0 }

private fun Any.safeIntMethod(name: String): Int? {
    val method = javaClass.methods.firstOrNull { method ->
        method.name == name && method.parameterTypes.isEmpty()
    } ?: return null
    return try {
        method.invoke(this) as? Int
    } catch (_: Exception) {
        null
    }
}

private fun Int?.operationCardSuffix(): String =
    takeIf { it != null && it >= 0 }?.let { "（eUICC 卡 $it）" }.orEmpty()
