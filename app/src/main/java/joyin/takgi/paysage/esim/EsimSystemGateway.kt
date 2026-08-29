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
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.se.omapi.Reader
import android.se.omapi.SEService
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager
import android.util.Log
import joyin.takgi.paysage.R
import joyin.takgi.paysage.esim.lpa.PaysageOmapiApduInterface
import joyin.takgi.paysage.esim.lpa.PaysageUsbCcidContext
import joyin.takgi.paysage.esim.lpa.PaysageUsbCcidApduInterface
import joyin.takgi.paysage.esim.lpa.PaysageLpacHttpInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.ProfileDownloadInput
import net.typeblog.lpac_jni.ProfileDownloadState
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.security.MessageDigest
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
    val isSecureElementPresent: Boolean,
    val logicalSlotIndex: Int? = null
) {
    val diagnosticKey: String
        get() = logicalSlotIndex?.let { "slot:$it:$name" } ?: "name:$name"

    val displayLabel: String
        get() = logicalSlotIndex?.let { "SIM slot ${it + 1} / $name" } ?: name
}

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

    private val channelManager by lazy {
        EuiccChannelManager(context)
    }

    @Suppress("DEPRECATION")
    fun araMAccessRuleSha1(): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        val certificate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            packageInfo.signatures?.firstOrNull()?.toByteArray()
        } ?: return ""
        return MessageDigest.getInstance("SHA-1")
            .digest(certificate)
            .joinToString(separator = "") { byte -> "%02X".format(byte) }
    }

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
        val settings = EsimSettingsStore(context).read()
        val ccidInterfaces = device.ccidInterfacesForExternalEuicc()
        if (ccidInterfaces.isEmpty()) {
            return@withContext UsbCcidAtrResult(false, "未找到 USB CCID interface。", null)
        }

        var lastFailure: UsbCcidAtrResult? = null
        for (usbInterface in ccidInterfaces) {
            val interfaceLabel = device.usbInterfaceLabel(usbInterface, ccidInterfaces.size)
            val ccidContext = PaysageUsbCcidContext(
                usbManager = usbManager,
                device = device,
                forceTpduMode = settings.forceUsbTpduMode,
                usbInterfaceId = usbInterface.id
            )
            try {
                ccidContext.connect()
                val atrHex = ccidContext.atr?.takeIf { it.isNotEmpty() }?.toHex()
                val messagePrefix = interfaceLabel.takeIf { it.isNotBlank() }?.let { "$it: " }.orEmpty()
                if (atrHex != null) {
                    return@withContext UsbCcidAtrResult(
                        success = true,
                        message = "${messagePrefix}已读取 ATR。",
                        atrHex = atrHex
                    )
                }
                lastFailure = UsbCcidAtrResult(false, "${messagePrefix}读卡器已上电，但没有返回 ATR。", null)
            } catch (error: Throwable) {
                val messagePrefix = interfaceLabel.takeIf { it.isNotBlank() }?.let { "$it: " }.orEmpty()
                lastFailure = UsbCcidAtrResult(false, messagePrefix + (error.message ?: "USB CCID 操作失败。"), null)
            } finally {
                ccidContext.close()
            }
        }
        lastFailure ?: UsbCcidAtrResult(false, "USB CCID 操作失败。", null)
    }

    suspend fun probeUsbIsdR(deviceName: String): EsimIsdRProbeResult = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(UsbManager::class.java)
            ?: return@withContext EsimIsdRProbeResult(false, "当前系统没有 USB 管理服务。", null, 0, null)
        val device = usbManager.deviceList[deviceName]
            ?: return@withContext EsimIsdRProbeResult(false, "未找到该 USB 读卡器，请重新刷新。", null, 0, null)
        val settings = EsimSettingsStore(context).read()
        val ccidInterfaces = device.ccidInterfacesForExternalEuicc()
        if (ccidInterfaces.isEmpty()) {
            return@withContext EsimIsdRProbeResult(false, "未找到 USB CCID interface。", null, 0, null)
        }

        var lastFailure: EsimIsdRProbeResult? = null
        for (usbInterface in ccidInterfaces) {
            val result = probeUsbIsdRInterface(
                usbManager = usbManager,
                device = device,
                settings = settings,
                usbInterface = usbInterface,
                ccidInterfaceCount = ccidInterfaces.size
            )
            if (result.success) {
                return@withContext result
            }
            lastFailure = result
        }
        lastFailure ?: EsimIsdRProbeResult(false, "USB CCID APDU 操作失败。", null, 0, null)
    }

    private fun probeUsbIsdRInterface(
        usbManager: UsbManager,
        device: UsbDevice,
        settings: EsimUserSettings,
        usbInterface: UsbInterface,
        ccidInterfaceCount: Int
    ): EsimIsdRProbeResult {
        val interfaceLabel = device.usbInterfaceLabel(usbInterface, ccidInterfaceCount)
        fun title(raw: String): String =
            interfaceLabel.takeIf { it.isNotBlank() }?.let { "$it / $raw" } ?: raw

        val ccidContext = PaysageUsbCcidContext(
            usbManager = usbManager,
            device = device,
            forceTpduMode = settings.forceUsbTpduMode,
            usbInterfaceId = usbInterface.id
        )
        val steps = mutableListOf<EsimApduDiagnosticStep>()
        return try {
            ccidContext.connect()
            val atrHex = ccidContext.atr?.takeIf { it.isNotEmpty() }?.toHex()
            steps += EsimApduDiagnostics.powerOn(
                success = atrHex != null,
                message = if (atrHex != null) {
                    "读卡器已上电并返回 ATR。"
                } else {
                    "读卡器已上电，但未返回 ATR。"
                },
                atrHex = atrHex
            ).copy(title = title("CCID PowerOn"))

            var lastFailure: EsimIsdRProbeResult? = null
            var lastSuccess: EsimIsdRProbeResult? = null
            val aidPlan = usbVendorAwareAidAttemptPlan(
                baseAids = settings.effectiveIsdrAids(),
                usbManager = usbManager,
                device = device,
                forceUsbTpduMode = settings.forceUsbTpduMode,
                sharedContext = ccidContext
            )
            val openedAids = mutableListOf<ByteArray>()
            for (aid in aidPlan.aids) {
                if (!aidPlan.shouldAttempt(openedAids, aid)) break
                val aidLabel = EsimApdu.aidLabel(aid)
                val apduInterface = PaysageUsbCcidApduInterface(
                    usbManager = usbManager,
                    device = device,
                    forceTpduMode = settings.forceUsbTpduMode,
                    usbInterfaceId = usbInterface.id,
                    sharedContext = ccidContext,
                    diagnosticRecorder = { stage, message ->
                        EsimPersistentDiagnostics.append(context, stage, message)
                    }
                )
                val selectBytes = runCatching {
                    apduInterface.connect()
                    apduInterface.selectAidForDiagnostics(aid)
                }.onFailure { error ->
                    val message = error.message ?: "发送 SELECT ISD-R 失败。"
                    val failedSteps = steps + EsimApduDiagnostics.failed(title("SELECT ISD-R ($aidLabel)"), message)
                    lastFailure = EsimIsdRProbeResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = 0,
                        atrHex = atrHex,
                        diagnosticSteps = failedSteps
                    )
                }.also {
                    runCatching { apduInterface.disconnect() }
                }.getOrNull() ?: continue

                val parsedResponse = runCatching {
                    EsimApdu.parseResponse(selectBytes)
                }
                if (parsedResponse.isFailure) {
                    val message = parsedResponse.exceptionOrNull()?.message ?: "APDU 响应解析失败。"
                    val failedSteps = steps + EsimApduDiagnostics.failed(title("SELECT ISD-R ($aidLabel)"), message)
                    lastFailure = EsimIsdRProbeResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = selectBytes.size,
                        atrHex = atrHex,
                        diagnosticSteps = failedSteps
                    )
                    continue
                }
                val apduResponse = parsedResponse.getOrThrow()
                val fciSummary = EsimFciAnalyzer.summarize(apduResponse.data, expectedAid = aid)?.diagnosticText
                val selectStep = EsimApduDiagnostics.selectIsdR(apduResponse, fciSummary)
                    .copy(title = title("SELECT ISD-R ($aidLabel)"))
                val result = EsimIsdRProbeResult(
                    success = apduResponse.isAccepted,
                    message = apduResponse.toIsdRMessage(),
                    statusWord = apduResponse.statusWordHex,
                    responseByteCount = apduResponse.data.size,
                    atrHex = atrHex,
                    fciSummary = fciSummary,
                    diagnosticSteps = steps + selectStep
                )
                if (apduResponse.isAccepted) {
                    lastSuccess = result
                    openedAids += aid
                }
                lastFailure = result
            }

            lastSuccess ?: lastFailure ?: EsimIsdRProbeResult(
                success = false,
                message = "没有可用于探测的 ISD-R AID。",
                statusWord = null,
                responseByteCount = 0,
                atrHex = atrHex,
                diagnosticSteps = steps + EsimApduDiagnostics.failed(title("SELECT ISD-R"), "没有可用于探测的 ISD-R AID。")
            )
        } catch (error: Throwable) {
            val message = error.message ?: "USB CCID APDU 操作失败。"
            EsimIsdRProbeResult(
                success = false,
                message = message,
                statusWord = null,
                responseByteCount = 0,
                atrHex = null,
                diagnosticSteps = steps + EsimApduDiagnostics.failed(title("USB CCID"), message)
            )
        } finally {
            ccidContext.close()
        }
    }

    suspend fun omapiReaderSummaries(): List<EsimOmapiReaderSummary> = withContext(Dispatchers.IO) {
        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
            connectOmapiService()
        } ?: return@withContext emptyList()

        try {
            omapiUiccReaderCandidates(service).map { candidate ->
                EsimOmapiReaderSummary(
                    name = candidate.name,
                    isUicc = true,
                    isSecureElementPresent = runCatching { candidate.reader.isSecureElementPresent }.getOrDefault(false),
                    logicalSlotIndex = candidate.logicalSlotIndex
                )
            }
        } finally {
            service.shutdown()
        }
    }

    suspend fun probeOmapiIsdR(
        readerName: String,
        logicalSlotIndex: Int? = null
    ): EsimIsdRProbeResult = withContext(Dispatchers.IO) {
        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
            connectOmapiService()
        } ?: return@withContext EsimIsdRProbeResult(false, "OMAPI 服务连接超时。", null, 0, null)

        var session: android.se.omapi.Session? = null
        try {
            val steps = mutableListOf<EsimApduDiagnosticStep>()
            val reader = logicalSlotIndex
                ?.let { service.findUiccReaderByLogicalSlot(it) }
                ?: service.getReaders().orEmpty().firstOrNull { reader ->
                    reader.safeOmapiName() == readerName
                }
                ?: return@withContext EsimIsdRProbeResult(false, "未找到该 OMAPI reader，请重新检测。", null, 0, null)
            val present = runCatching { reader.isSecureElementPresent }.getOrDefault(false)
            if (!present) {
                steps += EsimApduDiagnostics.skipped(
                    title = "OMAPI Reader Present",
                    message = "系统报告该 reader 当前没有可用安全元素；继续尝试打开 session，以兼容部分 ROM 的误报。"
                )
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

            val candidate = OmapiReaderCandidate(
                reader = reader,
                name = reader.safeOmapiName(),
                logicalSlotIndex = logicalSlotIndex
            )
            val settings = EsimSettingsStore(context).read()
            val aidPlan = omapiVendorAwareAidAttemptPlan(
                service = service,
                candidate = candidate,
                baseAids = settings.effectiveIsdrAids(),
                openedSession = session
            )
            var lastFailure: EsimIsdRProbeResult? = null
            var lastSuccess: EsimIsdRProbeResult? = null
            val openedAids = mutableListOf<ByteArray>()

            for (aid in aidPlan.aids) {
                if (!aidPlan.shouldAttempt(openedAids, aid)) break
                val aidLabel = EsimApdu.aidLabel(aid)
                var channel: android.se.omapi.Channel? = null
                try {
                    channel = session.openLogicalChannel(aid)
                    if (channel == null) {
                        val message = "OMAPI 未能打开 ISD-R 逻辑通道。"
                        lastFailure = EsimIsdRProbeResult(
                            success = false,
                            message = message,
                            statusWord = null,
                            responseByteCount = 0,
                            atrHex = atrHex,
                            diagnosticSteps = steps + EsimApduDiagnostics.failed("SELECT ISD-R ($aidLabel)", message)
                        )
                        continue
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
                            diagnosticSteps = steps + EsimApduDiagnostics.skipped("SELECT ISD-R ($aidLabel)", message)
                        )
                    }
                    val parsedResponse = EsimApdu.parseResponse(selectResponse)
                    val fciSummary = EsimFciAnalyzer.summarize(parsedResponse.data, expectedAid = aid)?.diagnosticText
                    val selectStep = EsimApduDiagnostics.selectIsdR(parsedResponse, fciSummary)
                        .copy(title = "SELECT ISD-R ($aidLabel)")
                    val result = EsimIsdRProbeResult(
                        success = parsedResponse.isAccepted,
                        message = parsedResponse.toIsdRMessage(),
                        statusWord = parsedResponse.statusWordHex,
                        responseByteCount = parsedResponse.data.size,
                        atrHex = atrHex,
                        fciSummary = fciSummary,
                        diagnosticSteps = steps + selectStep
                    )
                    if (parsedResponse.isAccepted) {
                        lastSuccess = result
                        openedAids += aid
                    }
                    lastFailure = result
                } catch (_: SecurityException) {
                    return@withContext EsimIsdRProbeResult(
                        success = false,
                        message = "SIM 卡槽 eUICC 的 OMAPI 访问规则拒绝 Paysage 访问 ISD-R，需要在卡内 ARA-M/访问规则中加入 Paysage 签名。",
                        statusWord = null,
                        responseByteCount = 0,
                        atrHex = atrHex,
                        diagnosticSteps = steps + EsimApduDiagnostics.failed(
                            "SELECT ISD-R ($aidLabel)",
                            "OMAPI 访问规则拒绝 Paysage 访问该 AID。"
                        )
                    )
                } catch (error: Throwable) {
                    val message = error.message ?: "OMAPI ISD-R 探测失败。"
                    lastFailure = EsimIsdRProbeResult(
                        success = false,
                        message = message,
                        statusWord = null,
                        responseByteCount = 0,
                        atrHex = atrHex,
                        diagnosticSteps = steps + EsimApduDiagnostics.failed("SELECT ISD-R ($aidLabel)", message)
                    )
                } finally {
                    channel?.close()
                }
            }

            lastSuccess ?: lastFailure ?: EsimIsdRProbeResult(
                success = false,
                message = "没有可用于探测的 ISD-R AID。",
                statusWord = null,
                responseByteCount = 0,
                atrHex = atrHex,
                diagnosticSteps = steps + EsimApduDiagnostics.failed("SELECT ISD-R", "没有可用于探测的 ISD-R AID。")
            )
        } catch (_: SecurityException) {
            EsimIsdRProbeResult(
                success = false,
                message = "SIM 卡槽 eUICC 的 OMAPI 访问规则拒绝 Paysage 访问 ISD-R，需要在卡内 ARA-M/访问规则中加入 Paysage 签名。",
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
            session?.close()
            service.shutdown()
        }
    }

    suspend fun externalProfileSummaries(): EsimExternalProfileReadResult =
        EsimIsolatedLpaClient.readExternalProfileSummaries(context)

    internal suspend fun externalProfileSourceCandidates(): List<EsimExternalProfileSource> = withContext(Dispatchers.IO) {
        val sources = mutableListOf<EsimExternalProfileSource>()
        val settings = EsimSettingsStore(context).read()
        val isdrAids = settings.effectiveIsdrAids()
        var sourceIndex = 0

        val usbManager = context.getSystemService(UsbManager::class.java)
        val usbDevices = usbManager?.deviceList.orEmpty().values
            .mapNotNull { device -> usbManager?.let { manager -> device.toCcidSummary(manager)?.let { summary -> device to summary } } }
        usbDevices.forEach { (device, summary) ->
            if (!summary.hasPermission) return@forEach
            device.ccidInterfacesForExternalEuicc().forEach { usbInterface ->
                val interfaceLabel = device.usbInterfaceLabel(usbInterface, summary.ccidInterfaceCount)
                val ccidContext = PaysageUsbCcidContext(
                    usbManager = usbManager,
                    device = device,
                    forceTpduMode = settings.forceUsbTpduMode,
                    usbInterfaceId = usbInterface.id
                )
                try {
                    usbVendorAwareAidAttemptPlan(
                        baseAids = isdrAids,
                        usbManager = usbManager,
                        device = device,
                        forceUsbTpduMode = settings.forceUsbTpduMode,
                        sharedContext = ccidContext
                    ).aids.forEach { aid ->
                        sources += EsimExternalProfileSource(
                            kind = EsimExternalProfileSourceKind.UsbCcid,
                            key = device.deviceName,
                            label = listOf(
                                summary.manufacturerName,
                                summary.productName,
                                interfaceLabel,
                                EsimApdu.aidLabel(aid)
                            ).filter { it.isNotBlank() }.joinToString(separator = " / "),
                            index = sourceIndex++,
                            isdrAidHex = EsimApdu.aidHex(aid),
                            usbInterfaceId = usbInterface.id
                        )
                    }
                } finally {
                    ccidContext.close()
                }
            }
        }

        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
            connectOmapiService()
        } ?: return@withContext sources
        try {
            omapiUiccReaderCandidates(service).forEach { candidate ->
                val logicalSlotIndex = candidate.logicalSlotIndex ?: return@forEach
                val channels = withTimeoutOrNull(EXTERNAL_PROBE_TIMEOUT_MS) {
                    channelManager.findChannelsBySlotAndPort(
                        physicalSlotId = logicalSlotIndex,
                        portId = 0,
                        logicalSlotId = logicalSlotIndex
                    )
                }.orEmpty()
                if (channels.isEmpty()) {
                    EsimPersistentDiagnostics.append(
                        context,
                        "omapi_channel_discovery_empty",
                        "slot=$logicalSlotIndex reader=${candidate.name}"
                    )
                }
                channels.forEach { channel ->
                    sources += EsimExternalProfileSource(
                        kind = EsimExternalProfileSourceKind.Omapi,
                        key = candidate.name,
                        label = candidate.labelForAid(channel.isdrAid),
                        index = sourceIndex++,
                        isdrAidHex = EsimApdu.aidHex(channel.isdrAid),
                        logicalSlotIndex = candidate.logicalSlotIndex
                    )
                }
            }
        } finally {
            runCatching { channelManager.closeAll() }
            service.shutdown()
        }

        sources.distinctBy { it.identity }
    }

    internal suspend fun externalProfileSummariesInCurrentProcess(): EsimExternalProfileReadResult = withContext(Dispatchers.IO) {
        val profiles = mutableListOf<EsimExternalProfileSummary>()
        val messages = mutableListOf<String>()
        val availableSources = mutableListOf<EsimExternalProfileSource>()

        val usbManager = context.getSystemService(UsbManager::class.java)
        val usbDevices = usbManager?.deviceList.orEmpty().values
            .mapNotNull { device -> device.toCcidSummary(usbManager!!)?.let { summary -> device to summary } }
        val esimSettings = EsimSettingsStore(context).read()
        val forceUsbTpduMode = esimSettings.forceUsbTpduMode
        val isdrAids = esimSettings.effectiveIsdrAids()
        var sourceIndex = 0
        usbDevices.forEach { (device, summary) ->
            if (!summary.hasPermission) {
                messages += context.getString(R.string.format_external_usb_reader_not_authorized, summary.productName)
                return@forEach
            }
            val ccidInterfaces = device.ccidInterfacesForExternalEuicc()
            ccidInterfaces.forEach { usbInterface ->
                val usbInterfaceId = usbInterface.id
                val interfaceLabel = device.usbInterfaceLabel(usbInterface, summary.ccidInterfaceCount)
                val ccidContext = PaysageUsbCcidContext(
                    usbManager = usbManager,
                    device = device,
                    forceTpduMode = forceUsbTpduMode,
                    usbInterfaceId = usbInterfaceId
                )
                try {
                    val aidPlan = usbVendorAwareAidAttemptPlan(
                        baseAids = isdrAids,
                        usbManager = usbManager,
                        device = device,
                        forceUsbTpduMode = forceUsbTpduMode,
                        sharedContext = ccidContext
                    )
                    val openedAids = mutableListOf<ByteArray>()
                    for (aid in aidPlan.aids) {
                        if (!aidPlan.shouldAttempt(openedAids, aid)) break
                        val source = EsimExternalProfileSource(
                            kind = EsimExternalProfileSourceKind.UsbCcid,
                            key = device.deviceName,
                            label = listOf(
                                summary.manufacturerName,
                                summary.productName,
                                interfaceLabel,
                                EsimApdu.aidLabel(aid)
                            ).filter { it.isNotBlank() }.joinToString(separator = " / "),
                            index = sourceIndex++,
                            isdrAidHex = EsimApdu.aidHex(aid),
                            usbInterfaceId = usbInterfaceId
                        )
                        val result = withTimeoutOrNull(EXTERNAL_LPA_TIMEOUT_MS) {
                            runCatching {
                                readProfilesFromLpa(
                                    source = source,
                                    apduInterface = PaysageUsbCcidApduInterface(
                                        usbManager = usbManager,
                                        device = device,
                                        forceTpduMode = forceUsbTpduMode,
                                        usbInterfaceId = usbInterfaceId,
                                        sharedContext = ccidContext,
                                        diagnosticRecorder = { stage, message ->
                                            EsimPersistentDiagnostics.append(context, stage, message)
                                        }
                                    )
                                )
                            }
                        }
                        when {
                            result == null -> {
                                messages += context.getString(R.string.format_external_lpac_read_timeout, source.label)
                                Log.w(TAG, "External eUICC USB read timeout source=${source.label}")
                            }
                            result.isSuccess -> {
                                val readProfiles = result.getOrDefault(emptyList())
                                availableSources += source
                                profiles += readProfiles
                                messages += if (readProfiles.isEmpty()) {
                                    context.getString(R.string.format_external_source_connected_no_profiles, source.label)
                                } else {
                                    context.getString(R.string.format_external_source_profiles_read, source.label, readProfiles.size)
                                }
                                openedAids += aid
                            }
                            else -> {
                                val error = result.exceptionOrNull()
                                messages += context.getString(
                                    R.string.format_external_profile_read_failed,
                                    source.label,
                                    error?.externalEuiccFailureMessage(source)
                                        ?: context.getString(R.string.message_external_profiles_read_failed)
                                )
                                Log.w(TAG, "External eUICC USB read failed source=${source.label}, error=${error?.safeLogName() ?: "Unknown"}")
                            }
                        }
                    }
                } finally {
                    ccidContext.close()
                }
            }
        }

        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
            connectOmapiService()
        }
        if (service == null) {
            messages += context.getString(R.string.message_external_omapi_service_timeout)
        } else {
            try {
                val readers = omapiUiccReaderCandidates(service)
                readers.forEach { candidate ->
                    val logicalSlotIndex = candidate.logicalSlotIndex ?: return@forEach
                    val channels = withTimeoutOrNull(EXTERNAL_LPA_TIMEOUT_MS) {
                        channelManager.findChannelsBySlotAndPort(
                            physicalSlotId = logicalSlotIndex,
                            portId = 0,
                            logicalSlotId = logicalSlotIndex
                        )
                    }.orEmpty()
                    if (channels.isEmpty()) {
                        EsimPersistentDiagnostics.append(
                            context,
                            "omapi_channel_read_empty",
                            "slot=$logicalSlotIndex reader=${candidate.name}"
                        )
                    }
                    channels.forEach { channel ->
                        val source = EsimExternalProfileSource(
                            kind = EsimExternalProfileSourceKind.Omapi,
                            key = candidate.name,
                            label = candidate.labelForAid(channel.isdrAid),
                            index = sourceIndex++,
                            isdrAidHex = EsimApdu.aidHex(channel.isdrAid),
                            logicalSlotIndex = candidate.logicalSlotIndex
                        )
                        val readProfiles = channel.lpa.profiles.mapIndexedNotNull { index, profile ->
                            if (esimSettings.showNonOperationalProfiles || profile.isVisibleInDefaultExternalProfileList()) {
                                profile.toExternalProfileSummary(source, index)
                            } else {
                                null
                            }
                        }
                        availableSources += source
                        profiles += readProfiles
                        messages += if (readProfiles.isEmpty()) {
                            context.getString(R.string.format_external_omapi_connected_no_profiles, source.label)
                        } else {
                            context.getString(R.string.format_external_omapi_profiles_read, source.label, readProfiles.size)
                        }
                    }
                }
            } finally {
                runCatching { channelManager.closeAll() }
                service.shutdown()
            }
        }

        EsimExternalProfileReadResult(
            profiles = profiles.distinctBy { "${it.source.identity}:${it.iccid}" },
            messages = messages,
            availableSources = availableSources.distinctBy { it.identity },
            summaryMessageOverride = when {
                profiles.isNotEmpty() -> context.getString(R.string.format_external_profiles_summary_read, profiles.size)
                messages.isNotEmpty() -> compactExternalProfileReadMessages(context, messages)
                else -> context.getString(R.string.message_no_external_profiles_read)
            }
        )
    }

    internal suspend fun externalProfileSummariesForSourceInCurrentProcess(
        source: EsimExternalProfileSource
    ): EsimExternalProfileReadResult = withContext(Dispatchers.IO) {
        val preflight = preflightExternalProfileSource(source)
        if (!preflight.success) {
            Log.w(
                TAG,
                "External eUICC preflight blocked source=${source.label}, reason=${preflight.message}"
            )
            return@withContext EsimExternalProfileReadResult(
                profiles = emptyList(),
                messages = listOf(
                    context.getString(
                        R.string.format_external_profile_read_failed,
                        source.label,
                        preflight.message
                    )
                ),
                availableSources = emptyList()
            )
        }

        runCatching {
            val profiles = withExternalLpaForSource(source) { lpa ->
                EsimPersistentDiagnostics.append(
                    context,
                    "lpac_eid_start",
                    "source=${source.label} aid=${source.isdrAidLabel}"
                )
                val eidSuffix = runCatching { lpa.eID.takeLast(4) }.getOrDefault("")
                EsimPersistentDiagnostics.append(
                    context,
                    "lpac_profiles_start",
                    "source=${source.label} aid=${source.isdrAidLabel} eidSuffix=${eidSuffix.ifBlank { "unknown" }}"
                )
                val showNonOperationalProfiles = EsimSettingsStore(context).read().showNonOperationalProfiles
                val readProfiles = lpa.profiles.mapIndexedNotNull { index, profile ->
                    if (showNonOperationalProfiles || profile.isVisibleInDefaultExternalProfileList()) {
                        profile.toExternalProfileSummary(source, index)
                    } else {
                        null
                    }
                }
                Log.i(
                    TAG,
                    "External eUICC isolated lpac read source=${source.label}, aid=${source.isdrAidLabel}, eidSuffix=$eidSuffix, profiles=${readProfiles.size}, unfiltered=$showNonOperationalProfiles"
                )
                EsimPersistentDiagnostics.append(
                    context,
                    "lpac_profiles_ok",
                    "source=${source.label} aid=${source.isdrAidLabel} profiles=${readProfiles.size} unfiltered=$showNonOperationalProfiles"
                )
                readProfiles
            }
            EsimExternalProfileReadResult(
                profiles = profiles,
                messages = listOf(
                    if (profiles.isEmpty()) {
                        context.getString(R.string.format_external_source_connected_no_profiles, source.label)
                    } else {
                        context.getString(R.string.format_external_source_profiles_read, source.label, profiles.size)
                    }
                ),
                availableSources = listOf(source),
                summaryMessageOverride = if (profiles.isNotEmpty()) {
                    context.getString(R.string.format_external_profiles_summary_read, profiles.size)
                } else {
                    null
                }
            )
        }.getOrElse { error ->
            EsimExternalProfileReadResult(
                profiles = emptyList(),
                messages = listOf(
                    context.getString(
                        R.string.format_external_profile_read_failed,
                        source.label,
                        error.externalEuiccFailureMessage(source)
                    )
                ),
                availableSources = emptyList()
            )
        }
    }

    private suspend fun preflightExternalProfileSource(
        source: EsimExternalProfileSource
    ): ExternalProfileSourcePreflight = withContext(Dispatchers.IO) {
        EsimPersistentDiagnostics.append(
            context,
            "preflight_start",
            "source=${source.label} identity=${source.identity} kind=${source.kind} aid=${source.isdrAidLabel}"
        )
        when (source.kind) {
            EsimExternalProfileSourceKind.UsbCcid -> ExternalProfileSourcePreflight.Success.also {
                EsimPersistentDiagnostics.append(
                    context,
                    "preflight_ok",
                    "source=${source.label} kind=${source.kind}"
                )
            }
            EsimExternalProfileSourceKind.Omapi -> preflightOmapiExternalProfileSource(source).also { result ->
                EsimPersistentDiagnostics.append(
                    context,
                    if (result.success) "preflight_ok" else "preflight_blocked",
                    "source=${source.label} kind=${source.kind} message=${result.message}"
                )
            }
        }
    }

    private suspend fun preflightOmapiExternalProfileSource(
        source: EsimExternalProfileSource
    ): ExternalProfileSourcePreflight {
        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
            connectOmapiService()
        } ?: return ExternalProfileSourcePreflight(false, "OMAPI 服务连接超时。")

        var session: android.se.omapi.Session? = null
        var channel: android.se.omapi.Channel? = null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && source.logicalSlotIndex == null) {
                return ExternalProfileSourcePreflight(false, "OMAPI source 缺少 logicalSlotIndex，请刷新 eSIM 页面后重试。")
            }
            val reader = source.logicalSlotIndex
                ?.let { service.findUiccReaderByLogicalSlot(it) }
                ?: service.getReaders().orEmpty().firstOrNull { reader ->
                    reader.safeOmapiName() == source.key
                }
                ?: return ExternalProfileSourcePreflight(false, "未找到该 OMAPI reader，请重新刷新。")

            session = reader.openSession()
            channel = session.openLogicalChannel(source.isdrAid)
                ?: return ExternalProfileSourcePreflight(false, "OMAPI 未能打开 ISD-R 逻辑通道。")

            val selectResponse = runCatching { channel.getSelectResponse() }.getOrNull()
            if (selectResponse != null && selectResponse.size >= 2) {
                val parsed = runCatching { EsimApdu.parseResponse(selectResponse) }.getOrNull()
                if (parsed != null && !parsed.isAccepted) {
                    return ExternalProfileSourcePreflight(
                        success = false,
                        message = "ISD-R SELECT 未通过，SW=${parsed.statusWordHex}。"
                    )
                }
                Log.i(
                    TAG,
                    "External eUICC OMAPI preflight passed source=${source.label}, aid=${source.isdrAidLabel}, sw=${parsed?.statusWordHex ?: "unknown"}"
                )
            } else {
                Log.i(
                    TAG,
                    "External eUICC OMAPI preflight passed source=${source.label}, aid=${source.isdrAidLabel}, no select response"
                )
            }
            ExternalProfileSourcePreflight.Success
        } catch (_: SecurityException) {
            ExternalProfileSourcePreflight(
                success = false,
                message = context.getString(R.string.message_external_omapi_access_rule_denied)
            )
        } catch (_: NoSuchElementException) {
            ExternalProfileSourcePreflight(false, "该安全元素未发现 ISD-R AID。")
        } catch (_: UnsupportedOperationException) {
            ExternalProfileSourcePreflight(false, "当前 OMAPI 实现不支持该 ISD-R 通道参数。")
        } catch (_: IllegalArgumentException) {
            ExternalProfileSourcePreflight(false, "系统未接受该 ISD-R AID 或 reader 参数。")
        } catch (_: IOException) {
            ExternalProfileSourcePreflight(false, "OMAPI 与安全元素通信失败。")
        } catch (_: IllegalStateException) {
            ExternalProfileSourcePreflight(false, "OMAPI 服务状态异常，请重新检测后再试。")
        } catch (error: Throwable) {
            ExternalProfileSourcePreflight(false, error.message ?: error.javaClass.simpleName)
        } finally {
            runCatching { channel?.close() }
            runCatching { session?.close() }
            service.shutdown()
        }
    }

    suspend fun requestDownloadExternalProfile(
        activationCode: EsimActivationCode,
        targetSourceIdentity: String? = null,
        onDownloadState: (ProfileDownloadState) -> Boolean = { true }
    ): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        if (!activationCode.isValid) {
            return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_esim_activation_code_incomplete))
        }
        if (activationCode.confirmationCodeRequired && activationCode.confirmationCode.isNullOrBlank()) {
            return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_confirmation_required))
        }

        val settings = EsimSettingsStore(context).read()
        val readResult = externalProfileSummaries()
        val source = if (targetSourceIdentity == null) {
            readResult.availableSources.singleOrNull()
        } else {
            readResult.availableSources.firstOrNull { source -> source.identity == targetSourceIdentity }
        } ?: return@withContext EsimDownloadStartResult(
            false,
            "",
            context.getString(R.string.message_external_esim_download_target_missing)
        )

        val requestId = "external-download-${System.currentTimeMillis()}"
        val downloadInput = ProfileDownloadInput(
            address = activationCode.smdpAddress.orEmpty(),
            matchingId = activationCode.matchingId,
            imei = activationCode.imei,
            confirmationCode = activationCode.confirmationCode
        )
        var handledNotifications = 0
        runCatching {
            withExternalLpaForSourceTrackingNotifications(
                source = source,
                autoHandleNotifications = settings.autoHandleExternalNotifications,
                removeHandledNotifications = settings.removeHandledExternalNotifications,
                onNotificationsHandled = { handledNotifications = it }
            ) { lpa ->
                lpa.downloadProfile(downloadInput) { state ->
                    onDownloadState(state)
                }
            }
        }.getOrElse { error ->
            Log.w(TAG, "External eUICC download failed source=${source.label}, error=${error.safeLogName()}")
            return@withContext externalOperationResult(
                started = false,
                requestId = requestId,
                message = explainExternalProfileDownloadFailure(error)
            )
        }

        externalOperationResult(
            started = true,
            requestId = requestId,
            message = withAutoNotificationSummary(
                context.getString(R.string.format_external_esim_download_success, source.label),
                handledNotifications
            )
        )
    }

    private fun explainExternalProfileDownloadFailure(error: Throwable): String {
        val downloadError = error as? LocalProfileAssistant.ProfileDownloadException
        val rawReason = downloadError?.lpaErrorReason
            ?.trim()
            .orEmpty()
            .ifBlank { error.message.orEmpty().ifBlank { error.javaClass.simpleName } }
        val reasonKey = rawReason.lowercase()
        val title = when {
            reasonKey.contains("confirm") -> context.getString(R.string.error_esim_invalid_confirmation_code)
            reasonKey.contains("activation") ||
                reasonKey.contains("matching") ||
                reasonKey.contains("smdp") ||
                reasonKey.contains("address") -> context.getString(R.string.error_esim_invalid_activation_code)
            reasonKey.contains("cert") ||
                reasonKey.contains("tls") ||
                reasonKey.contains("ssl") -> context.getString(R.string.error_esim_certificate_error)
            reasonKey.contains("memory") ||
                reasonKey.contains("space") ||
                reasonKey.contains("insufficient") -> context.getString(R.string.error_esim_euicc_insufficient_memory)
            reasonKey.contains("timeout") ||
                reasonKey.contains("network") ||
                reasonKey.contains("connect") ||
                downloadError?.lastHttpException != null -> context.getString(R.string.error_esim_connection_error)
            reasonKey.contains("cancel") -> context.getString(R.string.title_external_esim_download_cancelled)
            downloadError?.lastApduException != null ||
                downloadError?.lastApduResponse != null -> context.getString(R.string.error_esim_install_profile)
            else -> context.getString(R.string.error_esim_install_profile)
        }
        val recovery = when {
            reasonKey.contains("confirm") -> context.getString(R.string.recovery_esim_check_confirmation_code)
            reasonKey.contains("activation") ||
                reasonKey.contains("matching") ||
                reasonKey.contains("smdp") ||
                reasonKey.contains("address") -> context.getString(R.string.recovery_esim_check_activation_code)
            reasonKey.contains("cert") ||
                reasonKey.contains("tls") ||
                reasonKey.contains("ssl") -> context.getString(R.string.recovery_esim_contact_carrier)
            reasonKey.contains("memory") ||
                reasonKey.contains("space") ||
                reasonKey.contains("insufficient") -> context.getString(R.string.recovery_esim_free_external_profile_space)
            reasonKey.contains("timeout") ||
                reasonKey.contains("network") ||
                reasonKey.contains("connect") ||
                downloadError?.lastHttpException != null -> context.getString(R.string.recovery_esim_retry_stable_network)
            reasonKey.contains("cancel") -> context.getString(R.string.recovery_external_esim_download_cancelled)
            downloadError?.lastApduException != null ||
                downloadError?.lastApduResponse != null -> context.getString(R.string.recovery_esim_refresh_port)
            else -> context.getString(R.string.recovery_external_esim_download_general)
        }
        val diagnostics = buildList {
            if (rawReason.isNotBlank()) {
                add(context.getString(R.string.format_external_esim_failure_lpa_reason, rawReason))
            }
            downloadError?.lastHttpResponse?.let { response ->
                add(context.getString(R.string.format_external_esim_failure_http_status, response.rcode))
            }
            downloadError?.lastHttpException?.let { httpError ->
                add(context.getString(R.string.format_external_esim_failure_http_exception, httpError.javaClass.simpleName))
            }
            downloadError?.lastApduResponse?.takeIf { it.size >= 2 }?.let { response ->
                val sw1 = response[response.size - 2].toInt() and 0xFF
                val sw2 = response[response.size - 1].toInt() and 0xFF
                add(context.getString(R.string.format_external_esim_failure_apdu_status, sw1, sw2))
            }
            downloadError?.lastApduException?.let { apduError ->
                add(context.getString(R.string.format_external_esim_failure_apdu_exception, apduError.javaClass.simpleName))
            }
        }.joinToString(separator = "\n")

        return context.getString(
            R.string.format_external_esim_download_failed_with_recovery,
            title,
            diagnostics.ifBlank { context.getString(R.string.message_esim_no_detailed_diagnostics) },
            recovery
        )
    }

    suspend fun requestSwitchExternalProfile(identifier: String): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isBlank()) {
            return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_external_profile_id_required))
        }

        val target = findExternalProfile(cleanIdentifier)
            ?: return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_external_profile_not_found))

        val requestId = "external-switch-${System.currentTimeMillis()}"
        val settings = EsimSettingsStore(context).read()
        var handledNotifications = 0
        val switchAttempt = runExternalProfileOperationWithSourceRefresh(target, "switch") { current ->
            withExternalLpaForSourceTrackingNotifications(
                source = current.source,
                autoHandleNotifications = settings.autoHandleExternalNotifications,
                removeHandledNotifications = settings.removeHandledExternalNotifications,
                shouldHandleNotifications = { result -> result.changed },
                onNotificationsHandled = { handledNotifications = it }
            ) { lpa ->
                val refreshed = lpa.enableProfile(current.iccid, refresh = true)
                if (refreshed) {
                    ExternalProfileOperationResult(changed = true, refreshed = true)
                } else {
                    ExternalProfileOperationResult(
                        changed = lpa.enableProfile(current.iccid, refresh = false),
                        refreshed = false
                    )
                }
            }
        }.getOrElse { error ->
            Log.w(TAG, "External eUICC switch failed id=${target.commandId}, error=${error.safeLogName()}")
            return@withContext externalOperationResult(
                started = false,
                requestId = requestId,
                message = context.getString(
                    R.string.format_external_profile_switch_failed,
                    error.message ?: error.javaClass.simpleName
                )
            )
        }
        val operationTarget = switchAttempt.first
        val switchResult = switchAttempt.second

        if (switchResult.changed) {
            val verified = verifyExternalProfileState(
                source = operationTarget.source,
                iccid = operationTarget.iccid,
                expectedState = LocalProfileInfo.State.Enabled
            )
            if (!verified) {
                return@withContext externalOperationResult(
                    started = false,
                    requestId = requestId,
                    message = context.getString(R.string.message_external_profile_switch_verification_failed)
                )
            }
            externalOperationResult(
                started = true,
                requestId = requestId,
                message = withAutoNotificationSummary(
                    if (switchResult.refreshed) {
                        context.getString(R.string.format_external_profile_switch_success, operationTarget.displayName, operationTarget.commandId)
                    } else {
                        context.getString(R.string.format_external_profile_switch_success_unrefreshed, operationTarget.displayName, operationTarget.commandId)
                    },
                    handledNotifications
                )
            )
        } else {
            externalOperationResult(
                started = false,
                requestId = requestId,
                message = context.getString(R.string.message_external_profile_switch_rejected)
            )
        }
    }

    suspend fun requestDisableExternalProfile(identifier: String): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        val target = findExternalProfile(identifier)
            ?: return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_external_profile_not_found))
        val requestId = "external-disable-${System.currentTimeMillis()}"
        val settings = EsimSettingsStore(context).read()
        var handledNotifications = 0
        val disableAttempt = runExternalProfileOperationWithSourceRefresh(target, "disable") { current ->
            withExternalLpaForSourceTrackingNotifications(
                source = current.source,
                autoHandleNotifications = settings.autoHandleExternalNotifications,
                removeHandledNotifications = settings.removeHandledExternalNotifications,
                shouldHandleNotifications = { operationResult -> operationResult.changed },
                onNotificationsHandled = { handledNotifications = it }
            ) { lpa ->
                val refreshed = lpa.disableProfile(current.iccid, refresh = true)
                if (refreshed) {
                    ExternalProfileOperationResult(changed = true, refreshed = true)
                } else {
                    ExternalProfileOperationResult(
                        changed = lpa.disableProfile(current.iccid, refresh = false),
                        refreshed = false
                    )
                }
            }
        }.getOrElse { error ->
            Log.w(TAG, "External eUICC disable failed id=${target.commandId}, error=${error.safeLogName()}")
            return@withContext externalOperationResult(
                false,
                requestId,
                context.getString(R.string.format_external_profile_disable_failed, error.message ?: error.javaClass.simpleName)
            )
        }
        val operationTarget = disableAttempt.first
        val result = disableAttempt.second
        if (result.changed) {
            val verified = verifyExternalProfileState(
                source = operationTarget.source,
                iccid = operationTarget.iccid,
                expectedState = LocalProfileInfo.State.Disabled
            )
            if (!verified) {
                return@withContext externalOperationResult(
                    started = false,
                    requestId = requestId,
                    message = context.getString(R.string.message_external_profile_disable_verification_failed)
                )
            }
        }
        externalOperationResult(
            started = result.changed,
            requestId = requestId,
            message = withAutoNotificationSummary(
                when {
                    result.changed && result.refreshed -> context.getString(R.string.format_external_profile_disable_success, operationTarget.displayName)
                    result.changed -> context.getString(R.string.format_external_profile_disable_success_unrefreshed, operationTarget.displayName)
                    else -> context.getString(R.string.message_external_profile_disable_rejected)
                },
                if (result.changed) handledNotifications else 0
            )
        )
    }

    suspend fun requestRenameExternalProfile(identifier: String, nickname: String): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        val cleanNickname = nickname.trim()
        if (cleanNickname.isBlank()) {
            return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_esim_rename_blank))
        }
        val target = findExternalProfile(identifier)
            ?: return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_external_profile_not_found))
        val requestId = "external-rename-${System.currentTimeMillis()}"
        val settings = EsimSettingsStore(context).read()
        var handledNotifications = 0
        val renameAttempt = runExternalProfileOperationWithSourceRefresh(target, "rename") { current ->
            withExternalLpaForSourceTrackingNotifications(
                source = current.source,
                autoHandleNotifications = settings.autoHandleExternalNotifications,
                removeHandledNotifications = settings.removeHandledExternalNotifications,
                onNotificationsHandled = { handledNotifications = it }
            ) { lpa ->
                lpa.setNickname(current.iccid, cleanNickname)
            }
        }.getOrElse { error ->
            Log.w(TAG, "External eUICC rename failed id=${target.commandId}, error=${error.safeLogName()}")
            val message = when (error) {
                is LocalProfileAssistant.ProfileNameTooLongException -> context.getString(R.string.message_external_profile_name_too_long)
                is LocalProfileAssistant.ProfileNameIsInvalidUTF8Exception -> context.getString(R.string.message_external_profile_name_invalid)
                else -> context.getString(
                    R.string.format_external_profile_rename_failed,
                    error.message ?: error.javaClass.simpleName
                )
            }
            return@withContext externalOperationResult(false, requestId, message)
        }
        val operationTarget = renameAttempt.first
        val verified = verifyExternalProfileNickname(
            source = operationTarget.source,
            iccid = operationTarget.iccid,
            expectedNickname = cleanNickname
        )
        if (!verified) {
            return@withContext externalOperationResult(
                false,
                requestId,
                context.getString(R.string.message_external_profile_rename_verification_failed)
            )
        }
        externalOperationResult(
            true,
            requestId,
            withAutoNotificationSummary(
                context.getString(R.string.format_external_profile_rename_success, cleanNickname),
                handledNotifications
            )
        )
    }

    suspend fun requestDeleteExternalProfile(identifier: String): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        val target = findExternalProfile(identifier)
            ?: return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_external_profile_not_found))
        val requestId = "external-delete-${System.currentTimeMillis()}"
        val settings = EsimSettingsStore(context).read()
        var handledNotifications = 0
        val deleteAttempt = runExternalProfileOperationWithSourceRefresh(target, "delete") { current ->
            withExternalLpaForSourceTrackingNotifications(
                source = current.source,
                autoHandleNotifications = settings.autoHandleExternalNotifications,
                removeHandledNotifications = settings.removeHandledExternalNotifications,
                shouldHandleNotifications = { didDelete -> didDelete },
                onNotificationsHandled = { handledNotifications = it }
            ) { lpa ->
                lpa.deleteProfile(current.iccid)
            }
        }.getOrElse { error ->
            if (findExternalProfileByIccid(target.iccid) == null) {
                EsimPersistentDiagnostics.append(
                    context,
                    "external_profile_delete_confirmed_after_error",
                    "iccidSuffix=${target.iccid.takeLast(4)} error=${error.javaClass.simpleName}"
                )
                return@withContext externalOperationResult(
                    started = true,
                    requestId = requestId,
                    message = withAutoNotificationSummary(
                        context.getString(R.string.format_external_profile_delete_success, target.displayName),
                        handledNotifications
                    )
                )
            }
            Log.w(TAG, "External eUICC delete failed id=${target.commandId}, error=${error.safeLogName()}")
            return@withContext externalOperationResult(
                false,
                requestId,
                context.getString(R.string.format_external_profile_delete_failed, error.message ?: error.javaClass.simpleName)
            )
        }
        val operationTarget = deleteAttempt.first
        val deleted = deleteAttempt.second
        if (deleted) {
            val verified = verifyExternalProfileDeleted(operationTarget.source, operationTarget.iccid)
            if (!verified) {
                return@withContext externalOperationResult(
                    false,
                    requestId,
                    context.getString(R.string.message_external_profile_delete_verification_failed)
                )
            }
        }
        externalOperationResult(
            started = deleted,
            requestId = requestId,
            message = withAutoNotificationSummary(
                if (deleted) {
                    context.getString(R.string.format_external_profile_delete_success, operationTarget.displayName)
                } else {
                    context.getString(R.string.message_external_profile_delete_rejected)
                },
                if (deleted) handledNotifications else 0
            )
        )
    }

    suspend fun requestResetExternalEuicc(identifier: String): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        val target = findExternalProfile(identifier)
            ?: return@withContext EsimDownloadStartResult(false, "", context.getString(R.string.message_external_euicc_not_found))
        val requestId = "external-memory-reset-${System.currentTimeMillis()}"
        runCatching {
            withExternalLpaForSource(target.source) { lpa ->
                lpa.euiccMemoryReset()
            }
        }.getOrElse { error ->
            Log.w(TAG, "External eUICC memory reset failed source=${target.source.label}, error=${error.safeLogName()}")
            return@withContext externalOperationResult(
                false,
                requestId,
                context.getString(R.string.format_external_euicc_memory_reset_failed, error.message ?: error.javaClass.simpleName)
            )
        }
        externalOperationResult(true, requestId, context.getString(R.string.message_external_euicc_memory_reset_success))
    }

    suspend fun externalEuiccDetails(): EsimExternalEuiccDetailsResult = withContext(Dispatchers.IO) {
        val sourceResult = externalProfileSummaries()
        val details = mutableListOf<EsimExternalEuiccDetails>()
        val messages = sourceResult.messages.toMutableList()
        sourceResult.availableSources.forEach { source ->
            runCatching {
                withExternalLpaForSource(source) { lpa ->
                    val eid = runCatching { lpa.eID.trim() }.getOrNull()
                    details += EsimExternalEuiccDetails(
                        source = source,
                        maskedEid = eid?.maskEid(),
                        info = runCatching { lpa.euiccInfo2?.toExternalEuiccInfo(source, eid) }.getOrNull(),
                        notifications = runCatching {
                            lpa.notifications.map { notification ->
                                notification.toExternalNotificationSummary(source)
                            }
                        }.getOrDefault(emptyList())
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "External eUICC details failed source=${source.label}, error=${error.safeLogName()}")
                messages += context.getString(
                    R.string.format_external_euicc_details_read_failed,
                    source.label,
                    error.externalEuiccFailureMessage(source)
                )
            }
        }
        EsimExternalEuiccDetailsResult(
            details = details.distinctBy { it.source.identity },
            messages = messages.distinct(),
            summaryMessageOverride = when {
                details.isNotEmpty() -> context.getString(R.string.format_external_euicc_details_summary_read, details.size)
                messages.isNotEmpty() -> messages.distinct().joinToString(separator = context.getString(R.string.separator_semicolon))
                else -> context.getString(R.string.message_no_external_euicc_details_read)
            }
        )
    }

    suspend fun requestHandleExternalNotification(commandId: String): EsimDownloadStartResult =
        requestExternalNotificationOperation(
            commandId = commandId,
            requestPrefix = "external-notification-handle",
            successMessage = R.string.format_external_notification_handle_success
        ) { lpa, notification ->
            val handled = lpa.handleNotification(notification.seqNumber)
            if (handled && EsimSettingsStore(context).read().removeHandledExternalNotifications) {
                lpa.deleteNotification(notification.seqNumber)
            }
            handled
        }

    suspend fun requestDeleteExternalNotification(commandId: String): EsimDownloadStartResult =
        requestExternalNotificationOperation(
            commandId = commandId,
            requestPrefix = "external-notification-delete",
            successMessage = R.string.format_external_notification_delete_success
        ) { lpa, notification ->
            lpa.deleteNotification(notification.seqNumber)
        }

    suspend fun requestHandleAllExternalNotifications(sourceIdentity: String): EsimDownloadStartResult =
        requestExternalNotificationBatchOperation(
            sourceIdentity = sourceIdentity,
            requestPrefix = "external-notification-handle-all",
            emptyMessage = context.getString(R.string.message_no_external_notifications)
        ) { lpa, notifications ->
            val removeHandled = EsimSettingsStore(context).read().removeHandledExternalNotifications
            notifications.count { notification ->
                val handled = lpa.handleNotification(notification.seqNumber)
                if (handled && removeHandled) {
                    runCatching { lpa.deleteNotification(notification.seqNumber) }
                }
                handled
            }
        }

    suspend fun requestDeleteAllExternalNotifications(sourceIdentity: String): EsimDownloadStartResult =
        requestExternalNotificationBatchOperation(
            sourceIdentity = sourceIdentity,
            requestPrefix = "external-notification-delete-all",
            emptyMessage = context.getString(R.string.message_no_external_notifications)
        ) { lpa, notifications ->
            notifications.count { notification ->
                lpa.deleteNotification(notification.seqNumber)
            }
        }

    private suspend fun requestExternalNotificationOperation(
        commandId: String,
        requestPrefix: String,
        successMessage: Int,
        operation: (LocalProfileAssistant, EsimExternalEuiccNotification) -> Boolean
    ): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        val requestId = "$requestPrefix-${System.currentTimeMillis()}"
        val notifications = externalEuiccDetails().details
            .flatMap { it.notifications }
        val cleanCommandId = commandId.trim()
        val notification = notifications.firstOrNull {
            it.commandId.equals(cleanCommandId, ignoreCase = true)
        } ?: notifications.filter {
            it.legacyCommandId.equals(cleanCommandId, ignoreCase = true)
        }.singleOrNull()
            ?: return@withContext externalOperationResult(
                started = false,
                requestId = requestId,
                message = context.getString(R.string.message_external_notification_not_found)
            )

        val changed = runCatching {
            withExternalLpaForSource(notification.source) { lpa ->
                operation(lpa, notification)
            }
        }.getOrElse { error ->
            Log.w(TAG, "External eUICC notification operation failed id=${notification.commandId}, error=${error.safeLogName()}")
            return@withContext externalOperationResult(
                started = false,
                requestId = requestId,
                message = context.getString(
                    R.string.format_external_notification_operation_failed,
                    error.message ?: error.javaClass.simpleName
                )
            )
        }
        externalOperationResult(
            started = changed,
            requestId = requestId,
            message = if (changed) {
                context.getString(successMessage, notification.commandId)
            } else {
                context.getString(R.string.message_external_notification_operation_rejected)
            }
        )
    }

    private suspend fun requestExternalNotificationBatchOperation(
        sourceIdentity: String,
        requestPrefix: String,
        emptyMessage: String,
        operation: (LocalProfileAssistant, List<EsimExternalEuiccNotification>) -> Int
    ): EsimDownloadStartResult = withContext(Dispatchers.IO) {
        val requestId = "$requestPrefix-${System.currentTimeMillis()}"
        val detail = externalEuiccDetails().details.firstOrNull { detail ->
            detail.source.identity == sourceIdentity
        } ?: return@withContext externalOperationResult(
            started = false,
            requestId = requestId,
            message = context.getString(R.string.message_external_euicc_not_found)
        )
        if (detail.notifications.isEmpty()) {
            return@withContext externalOperationResult(
                started = false,
                requestId = requestId,
                message = emptyMessage
            )
        }
        val changedCount = runCatching {
            withExternalLpaForSource(detail.source) { lpa ->
                operation(lpa, detail.notifications)
            }
        }.getOrElse { error ->
            Log.w(TAG, "External eUICC notification batch operation failed source=${detail.source.label}, error=${error.safeLogName()}")
            return@withContext externalOperationResult(
                started = false,
                requestId = requestId,
                message = context.getString(
                    R.string.format_external_notification_operation_failed,
                    error.message ?: error.javaClass.simpleName
                )
            )
        }
        externalOperationResult(
            started = changedCount > 0,
            requestId = requestId,
            message = if (changedCount > 0) {
                context.getString(R.string.format_external_notifications_batch_success, changedCount)
            } else {
                context.getString(R.string.message_external_notification_operation_rejected)
            }
        )
    }

    private fun externalOperationResult(
        started: Boolean,
        requestId: String,
        message: String
    ): EsimDownloadStartResult {
        EsimDownloadResultStore(context).write(
            EsimDownloadResult(
                requestId = requestId,
                status = if (started) EsimDownloadStatus.Succeeded else EsimDownloadStatus.Failed,
                message = message,
                resultCode = null,
                detailedCode = null,
                operationCode = null,
                errorCode = null,
                smdxSubjectCode = null,
                smdxReasonCode = null,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        return EsimDownloadStartResult(started, requestId, message)
    }

    private fun Throwable.externalEuiccFailureMessage(source: EsimExternalProfileSource): String =
        if (source.kind == EsimExternalProfileSourceKind.Omapi && indicatesOmapiAccessRuleDenied()) {
            context.getString(R.string.message_external_omapi_access_rule_denied)
        } else {
            message ?: javaClass.simpleName
        }

    private fun usbVendorAwareAidAttemptPlan(
        baseAids: List<ByteArray>,
        usbManager: UsbManager,
        device: UsbDevice,
        forceUsbTpduMode: Boolean,
        sharedContext: PaysageUsbCcidContext
    ): EsimAidAttemptPlan {
        val isEstk = runCatching {
            val apduInterface = PaysageUsbCcidApduInterface(
                usbManager = usbManager,
                device = device,
                forceTpduMode = forceUsbTpduMode,
                sharedContext = sharedContext,
                diagnosticRecorder = { stage, message ->
                    EsimPersistentDiagnostics.append(context, stage, message)
                }
            )
            try {
                apduInterface.connect()
                val response = apduInterface.selectAidForDiagnostics(EsimApdu.ESTK_PRODUCT_AID)
                EsimApdu.parseResponse(response).isAccepted
            } finally {
                runCatching { apduInterface.disconnect() }
            }
        }.getOrDefault(false)

        return if (isEstk) {
            Log.i(TAG, "Detected eSTK.me USB eUICC; preferring eSTK ISD-R AIDs.")
            EsimAidAttemptPlan.estk(baseAids)
        } else {
            EsimAidAttemptPlan.default(baseAids)
        }
    }

    private fun omapiVendorAwareAidAttemptPlan(
        service: SEService,
        candidate: OmapiReaderCandidate,
        baseAids: List<ByteArray>,
        openedSession: android.se.omapi.Session? = null
    ): EsimAidAttemptPlan {
        val isEstk = if (openedSession != null) {
            runCatching {
                val channel = openedSession.openLogicalChannel(EsimApdu.ESTK_PRODUCT_AID)
                try {
                    channel != null
                } finally {
                    runCatching { channel?.close() }
                }
            }.getOrDefault(false)
        } else {
            runCatching {
                val reader = candidate.logicalSlotIndex
                    ?.let { service.findUiccReaderByLogicalSlot(it) }
                    ?: candidate.reader
                val session = reader.openSession()
                try {
                    val channel = session.openLogicalChannel(EsimApdu.ESTK_PRODUCT_AID)
                    try {
                        channel != null
                    } finally {
                        runCatching { channel?.close() }
                    }
                } finally {
                    runCatching { session.close() }
                }
            }.getOrDefault(false)
        }

        return if (isEstk) {
            Log.i(TAG, "Detected eSTK.me OMAPI eUICC reader=${candidate.name}; preferring eSTK ISD-R AIDs.")
            EsimAidAttemptPlan.estk(baseAids)
        } else {
            EsimAidAttemptPlan.default(baseAids)
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

    private suspend fun findExternalProfile(identifier: String): EsimExternalProfileSummary? {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isBlank()) return null
        val profiles = externalProfileSummaries().profiles
        profiles.firstOrNull { profile ->
            profile.commandId.equals(cleanIdentifier, ignoreCase = true)
        }?.let { return it }
        profiles.filter { profile ->
            profile.legacyCommandId.equals(cleanIdentifier, ignoreCase = true)
        }.singleOrNull()?.let { return it }
        profiles.filter { profile ->
            profile.iccid.equals(cleanIdentifier, ignoreCase = true)
        }.singleOrNull()?.let { return it }
        profiles.filter { profile ->
            profile.maskedIccid.equals(cleanIdentifier, ignoreCase = true)
        }.singleOrNull()?.let { return it }

        if (cleanIdentifier.length < 4) return null
        return profiles
            .filter { profile -> profile.iccid.endsWith(cleanIdentifier, ignoreCase = true) }
            .singleOrNull()
    }

    private suspend fun verifyExternalProfileState(
        source: EsimExternalProfileSource,
        iccid: String,
        expectedState: LocalProfileInfo.State
    ): Boolean =
        retryExternalProfileVerification {
            val directState = runCatching {
                withExternalLpaForSource(source) { lpa ->
                    lpa.profiles.firstOrNull { profile ->
                        profile.iccid.trim().equals(iccid.trim(), ignoreCase = true)
                    }?.state
                }
            }.getOrNull()
            if (directState == expectedState) {
                return@retryExternalProfileVerification true
            }

            val refreshed = findExternalProfileByIccid(iccid)
            if (refreshed != null && refreshed.source.identity != source.identity) {
                EsimPersistentDiagnostics.append(
                    context,
                    "external_profile_verify_source_refreshed",
                    "iccidSuffix=${iccid.takeLast(4)} old=${source.identity} new=${refreshed.source.identity}"
                )
            }
            refreshed?.state == expectedState.toExternalProfileState()
        }

    private suspend fun verifyExternalProfileNickname(
        source: EsimExternalProfileSource,
        iccid: String,
        expectedNickname: String
    ): Boolean =
        retryExternalProfileVerification {
            val directNickname = runCatching {
                withExternalLpaForSource(source) { lpa ->
                    lpa.profiles.firstOrNull { profile ->
                        profile.iccid.trim().equals(iccid.trim(), ignoreCase = true)
                    }?.nickName?.trim()
                }
            }.getOrNull()
            if (directNickname == expectedNickname) {
                return@retryExternalProfileVerification true
            }

            val refreshed = findExternalProfileByIccid(iccid)
            if (refreshed != null && refreshed.source.identity != source.identity) {
                EsimPersistentDiagnostics.append(
                    context,
                    "external_profile_verify_source_refreshed",
                    "iccidSuffix=${iccid.takeLast(4)} old=${source.identity} new=${refreshed.source.identity}"
                )
            }
            refreshed?.nickname?.trim() == expectedNickname
        }

    private suspend fun verifyExternalProfileDeleted(
        source: EsimExternalProfileSource,
        iccid: String
    ): Boolean =
        retryExternalProfileVerification {
            val directDeleted = runCatching {
                withExternalLpaForSource(source) { lpa ->
                    lpa.profiles.none { profile ->
                        profile.iccid.trim().equals(iccid.trim(), ignoreCase = true)
                    }
                }
            }.getOrNull()
            if (directDeleted == true) {
                return@retryExternalProfileVerification true
            }

            val refreshed = findExternalProfileByIccid(iccid)
            if (refreshed != null && refreshed.source.identity != source.identity) {
                EsimPersistentDiagnostics.append(
                    context,
                    "external_profile_verify_source_refreshed",
                    "iccidSuffix=${iccid.takeLast(4)} old=${source.identity} new=${refreshed.source.identity}"
                )
            }
            refreshed == null
        }

    private suspend fun findExternalProfileByIccid(iccid: String): EsimExternalProfileSummary? {
        val cleanIccid = iccid.trim()
        if (cleanIccid.isBlank()) return null
        return externalProfileSummaries().profiles.firstOrNull { profile ->
            profile.iccid.trim().equals(cleanIccid, ignoreCase = true)
        }
    }

    private suspend fun <T> runExternalProfileOperationWithSourceRefresh(
        target: EsimExternalProfileSummary,
        operation: String,
        block: suspend (EsimExternalProfileSummary) -> T
    ): Result<Pair<EsimExternalProfileSummary, T>> {
        val first = runCatching { target to block(target) }
        if (first.isSuccess) return first

        val firstError = first.exceptionOrNull() ?: return first
        val refreshed = findExternalProfileByIccid(target.iccid)
            ?: return Result.failure(firstError)
        if (refreshed.source.identity == target.source.identity) {
            return Result.failure(firstError)
        }

        EsimPersistentDiagnostics.append(
            context,
            "external_profile_operation_source_refreshed",
            "operation=$operation iccidSuffix=${target.iccid.takeLast(4)} old=${target.source.identity} new=${refreshed.source.identity} firstError=${firstError.javaClass.simpleName}"
        )
        return runCatching { refreshed to block(refreshed) }
    }

    private fun LocalProfileInfo.State.toExternalProfileState(): EsimExternalProfileState =
        when (this) {
            LocalProfileInfo.State.Enabled -> EsimExternalProfileState.Enabled
            LocalProfileInfo.State.Disabled -> EsimExternalProfileState.Disabled
        }

    private suspend fun retryExternalProfileVerification(
        block: suspend () -> Boolean
    ): Boolean =
        withTimeoutOrNull(EXTERNAL_PROFILE_VERIFY_TIMEOUT_MS) {
            while (true) {
                if (runCatching { block() }.getOrDefault(false)) {
                    return@withTimeoutOrNull true
                }
                delay(EXTERNAL_PROFILE_VERIFY_RETRY_DELAY_MS)
            }
            false
        } ?: false

    private suspend fun <T> withExternalLpaForSourceTrackingNotifications(
        source: EsimExternalProfileSource,
        autoHandleNotifications: Boolean,
        removeHandledNotifications: Boolean,
        shouldHandleNotifications: (T) -> Boolean = { true },
        onNotificationsHandled: (Int) -> Unit = {},
        block: (LocalProfileAssistant) -> T
    ): T {
        var latestSeqNumber = 0L
        val result = withExternalLpaForSource(source) { lpa ->
            latestSeqNumber = latestNotificationSeqNumber(lpa)
            block(lpa)
        }
        if (autoHandleNotifications && shouldHandleNotifications(result)) {
            onNotificationsHandled(
                handleNewExternalNotifications(
                    source = source,
                    afterSeqNumber = latestSeqNumber,
                    removeHandledNotifications = removeHandledNotifications
                )
            )
        }
        return result
    }

    private fun withAutoNotificationSummary(message: String, handledCount: Int): String =
        if (handledCount > 0) {
            "$message\n${context.getString(R.string.format_external_notifications_auto_handled, handledCount)}"
        } else {
            message
        }

    private fun latestNotificationSeqNumber(lpa: LocalProfileAssistant): Long =
        runCatching { lpa.notifications.maxOfOrNull { notification -> notification.seqNumber } ?: 0L }
            .getOrDefault(0L)

    private suspend fun handleNewExternalNotifications(
        source: EsimExternalProfileSource,
        afterSeqNumber: Long,
        removeHandledNotifications: Boolean
    ): Int =
        runCatching {
            withExternalLpaForSource(source) { lpa ->
                lpa.notifications
                    .filter { notification -> notification.seqNumber > afterSeqNumber }
                    .count { notification ->
                        val handled = runCatching { lpa.handleNotification(notification.seqNumber) }
                            .getOrDefault(false)
                        if (handled && removeHandledNotifications) {
                            runCatching { lpa.deleteNotification(notification.seqNumber) }
                        }
                        handled
                    }
            }
        }.onFailure { error ->
            Log.w(TAG, "External eUICC auto notification handling failed source=${source.label}, error=${error.safeLogName()}")
        }.getOrDefault(0)

    private fun readProfilesFromLpa(
        source: EsimExternalProfileSource,
        apduInterface: ApduInterface
    ): List<EsimExternalProfileSummary> =
        withExternalLpa(apduInterface, source.isdrAid) { lpa ->
            EsimPersistentDiagnostics.append(
                context,
                "lpac_eid_start",
                "source=${source.label} aid=${source.isdrAidLabel}"
            )
            val eidSuffix = runCatching { lpa.eID.takeLast(4) }.getOrDefault("")
            EsimPersistentDiagnostics.append(
                context,
                "lpac_profiles_start",
                "source=${source.label} aid=${source.isdrAidLabel} eidSuffix=${eidSuffix.ifBlank { "unknown" }}"
            )
            val showNonOperationalProfiles = EsimSettingsStore(context).read().showNonOperationalProfiles
            val profiles = lpa.profiles.mapIndexedNotNull { index, profile ->
                if (showNonOperationalProfiles || profile.isVisibleInDefaultExternalProfileList()) {
                    profile.toExternalProfileSummary(source, index)
                } else {
                    null
                }
            }
            Log.i(
                TAG,
                "External eUICC lpac read source=${source.label}, aid=${source.isdrAidLabel}, eidSuffix=$eidSuffix, profiles=${profiles.size}, unfiltered=$showNonOperationalProfiles"
            )
            EsimPersistentDiagnostics.append(
                context,
                "lpac_profiles_ok",
                "source=${source.label} aid=${source.isdrAidLabel} profiles=${profiles.size} unfiltered=$showNonOperationalProfiles"
            )
            profiles
        }

    private suspend fun <T> withExternalLpaForSource(
        source: EsimExternalProfileSource,
        block: (LocalProfileAssistant) -> T
    ): T {
        val result = withTimeoutOrNull(EXTERNAL_LPA_TIMEOUT_MS) {
            runCatching {
                when (source.kind) {
                    EsimExternalProfileSourceKind.UsbCcid -> {
                        val usbManager = context.getSystemService(UsbManager::class.java)
                            ?: error("当前系统没有 USB 管理服务。")
                        val device = usbManager.deviceList[source.key]
                            ?: error("未找到该 USB eUICC 读卡器。")
                        withExternalLpa(
                            PaysageUsbCcidApduInterface(
                                usbManager = usbManager,
                                device = device,
                                forceTpduMode = EsimSettingsStore(context).read().forceUsbTpduMode,
                                usbInterfaceId = source.usbInterfaceId,
                                diagnosticRecorder = { stage, message ->
                                    EsimPersistentDiagnostics.append(context, stage, message)
                                }
                            ),
                            source.isdrAid,
                            block
                        )
                    }
                    EsimExternalProfileSourceKind.Omapi -> {
                        val service = withTimeoutOrNull(OMAPI_CONNECT_TIMEOUT_MS) {
                            connectOmapiService()
                        } ?: error("OMAPI 服务连接超时。")
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && source.logicalSlotIndex == null) {
                                error("OMAPI source 缺少 logicalSlotIndex，请刷新 eSIM 页面后重试。")
                            }
                            withExternalLpa(
                                PaysageOmapiApduInterface(
                                    service = service,
                                    readerName = source.key,
                                    logicalSlotIndex = source.logicalSlotIndex,
                                    diagnosticRecorder = { stage, message ->
                                        EsimPersistentDiagnostics.append(context, stage, message)
                                    }
                                ),
                                source.isdrAid,
                                block
                            )
                        } finally {
                            service.shutdown()
                        }
                    }
                }
            }
        } ?: error("外置 eUICC 操作超时。")
        return result.getOrThrow()
    }

    private fun <T> withExternalLpa(
        apduInterface: ApduInterface,
        isdrAid: ByteArray,
        block: (LocalProfileAssistant) -> T
    ): T {
        var lpa: LocalProfileAssistant? = null
        var channelHandle: Int? = null
        try {
            EsimPersistentDiagnostics.append(
                context,
                "lpac_init_start",
                "aid=${EsimApdu.aidLabel(isdrAid)} apdu=${apduInterface.javaClass.simpleName}"
            )
            apduInterface.connect()
            channelHandle = apduInterface.logicalChannelOpen(isdrAid)
            lpa = LocalProfileAssistantImpl(
                isdrAid = isdrAid,
                rawApduInterface = apduInterface,
                rawHttpInterface = PaysageLpacHttpInterface()
            ).also { assistant ->
                assistant.setEs10xMss(EsimSettingsStore(context).read().es10xMss.coerceIn(1, 255).toByte())
            }
            EsimPersistentDiagnostics.append(
                context,
                "lpac_init_ok",
                "aid=${EsimApdu.aidLabel(isdrAid)} apdu=${apduInterface.javaClass.simpleName}"
            )
            EsimPersistentDiagnostics.append(
                context,
                "lpac_valid_start",
                "aid=${EsimApdu.aidLabel(isdrAid)}"
            )
            lpa.requireValidExternalEuicc()
            EsimPersistentDiagnostics.append(
                context,
                "lpac_valid_ok",
                "aid=${EsimApdu.aidLabel(isdrAid)}"
            )
            return block(lpa)
        } catch (error: Throwable) {
            EsimPersistentDiagnostics.append(
                context,
                "lpac_error",
                "aid=${EsimApdu.aidLabel(isdrAid)} error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
            )
            throw error
        } finally {
            runCatching { lpa?.close() }
            runCatching { apduInterface.disconnect() }
            EsimPersistentDiagnostics.append(
                context,
                "lpac_closed",
                "aid=${EsimApdu.aidLabel(isdrAid)} apdu=${apduInterface.javaClass.simpleName}"
            )
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

    private fun omapiUiccReaderCandidates(service: SEService): List<OmapiReaderCandidate> {
        val candidates = mutableListOf<OmapiReaderCandidate>()
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        val modemCount = telephonyManager?.activeModemCountCompat.orZero()

        repeat(modemCount) { logicalSlotIndex ->
            val reader = service.findUiccReaderByLogicalSlot(logicalSlotIndex) ?: return@repeat
            candidates += OmapiReaderCandidate(
                reader = reader,
                name = reader.safeOmapiName(),
                logicalSlotIndex = logicalSlotIndex
            )
        }

        if (candidates.isNotEmpty()) {
            EsimPersistentDiagnostics.append(
                context,
                "omapi_candidates",
                "mode=logicalSlot count=${candidates.size} readers=${candidates.joinToString { it.debugLabel }}"
            )
            return candidates.distinctBy { candidate ->
                "${candidate.logicalSlotIndex}:${candidate.name}"
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            EsimPersistentDiagnostics.append(
                context,
                "omapi_candidates",
                "mode=logicalSlot count=0 modemCount=$modemCount note=R+_does_not_fallback_to_reader_name"
            )
            return emptyList()
        }

        service.getReaders().orEmpty().forEach { reader ->
            val name = reader.safeOmapiName()
            if (name.isOmapiUiccReaderName() && candidates.none { it.name == name }) {
                candidates += OmapiReaderCandidate(
                    reader = reader,
                    name = name,
                    logicalSlotIndex = null
                )
            }
        }

        EsimPersistentDiagnostics.append(
            context,
            "omapi_candidates",
            "mode=legacyNameFallback count=${candidates.size} readers=${candidates.joinToString { it.debugLabel }}"
        )
        return candidates.distinctBy { candidate ->
            "${candidate.logicalSlotIndex ?: "name"}:${candidate.name}"
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
        private const val TAG = "EsimSystemGateway"
        private const val OMAPI_CONNECT_TIMEOUT_MS = 3_000L
        private const val EXTERNAL_PROBE_TIMEOUT_MS = 8_000L
        private const val EXTERNAL_LPA_TIMEOUT_MS = 20_000L
        private const val EXTERNAL_PROFILE_VERIFY_TIMEOUT_MS = 8_000L
        private const val EXTERNAL_PROFILE_VERIFY_RETRY_DELAY_MS = 1_200L
    }
}

private data class OmapiReaderCandidate(
    val reader: Reader,
    val name: String,
    val logicalSlotIndex: Int?
) {
    val debugLabel: String
        get() = "slot=${logicalSlotIndex ?: "legacy"} name=$name"

    fun labelForAid(aid: ByteArray): String {
        val slotLabel = logicalSlotIndex?.let { "SIM slot ${it + 1} / " }.orEmpty()
        return "$slotLabel$name / ${EsimApdu.aidLabel(aid)}"
    }
}

private data class ExternalProfileOperationResult(
    val changed: Boolean,
    val refreshed: Boolean
)

private data class ExternalProfileSourcePreflight(
    val success: Boolean,
    val message: String
) {
    companion object {
        val Success = ExternalProfileSourcePreflight(true, "")
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

private fun Throwable.safeLogName(): String =
    rootCause().javaClass.simpleName.ifBlank { "Throwable" }

private fun Throwable.indicatesOmapiAccessRuleDenied(): Boolean {
    val chain = mutableListOf<Throwable>()
    var current: Throwable? = this
    while (current != null && chain.size < 8) {
        chain += current
        current = current.cause
    }
    val text = chain.joinToString(separator = "\n") { error ->
        "${error.javaClass.simpleName}: ${error.message.orEmpty()}"
    }
    return rootCause() is SecurityException ||
        text.contains("SecurityException", ignoreCase = true) ||
        text.contains("access rule", ignoreCase = true) ||
        text.contains("access rules", ignoreCase = true) ||
        text.contains("ARA-M", ignoreCase = true) ||
        text.contains("访问规则拒绝")
}

private fun String.indicatesExternalOmapiHardBlock(): Boolean =
    contains("访问规则拒绝") ||
        contains("ARA-M", ignoreCase = true) ||
        contains("access rule", ignoreCase = true) ||
        contains("access rules", ignoreCase = true) ||
        contains("rejected Paysage", ignoreCase = true) ||
        contains("OMAPI 服务连接超时") ||
        (contains("OMAPI service", ignoreCase = true) && contains("timeout", ignoreCase = true)) ||
        contains("OMAPI 与安全元素通信失败") ||
        contains("secure element communication", ignoreCase = true)

private fun UsbDevice.toCcidSummary(usbManager: UsbManager): EsimUsbCcidReaderSummary? {
    val ccidInterfaces = ccidInterfacesForExternalEuicc().size
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

private fun UsbDevice.ccidInterfacesForExternalEuicc(): List<UsbInterface> {
    val interfaces = (0 until interfaceCount).map { getInterface(it) }
    val interfaceLevelCcid = interfaces.filter { it.interfaceClass == UsbConstants.USB_CLASS_CSCID }
    if (interfaceLevelCcid.isNotEmpty()) return interfaceLevelCcid
    if (deviceClass != UsbConstants.USB_CLASS_CSCID) return emptyList()
    return interfaces.filter { it.hasBulkInOutEndpoints() }
}

private fun UsbInterface.hasBulkInOutEndpoints(): Boolean {
    val bulkEndpoints = (0 until endpointCount)
        .map { getEndpoint(it) }
        .filter { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK }
    return bulkEndpoints.any { it.direction == UsbConstants.USB_DIR_IN } &&
        bulkEndpoints.any { it.direction == UsbConstants.USB_DIR_OUT }
}

private fun UsbDevice.usbInterfaceLabel(usbInterface: UsbInterface, ccidInterfaceCount: Int): String {
    if (ccidInterfaceCount <= 1) return ""
    val index = (0 until interfaceCount)
        .firstOrNull { getInterface(it).id == usbInterface.id }
        ?.plus(1)
    return "CCID ${index ?: usbInterface.id}"
}

private fun safeUsbString(block: () -> String?): String =
    runCatching { block().orEmpty() }.getOrDefault("")

private fun String.isOmapiUiccReaderName(): Boolean =
    contains("SIM", ignoreCase = true) || contains("UICC", ignoreCase = true)

private val TelephonyManager.activeModemCountCompat: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activeModemCount
    } else {
        @Suppress("DEPRECATION")
        phoneCount
    }

private fun Int?.orZero(): Int =
    this?.takeIf { it > 0 } ?: 0

private fun SEService.findUiccReaderByLogicalSlot(logicalSlotIndex: Int): Reader? {
    val slotNumber = logicalSlotIndex + 1
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { getUiccReader(slotNumber) }.getOrNull()
    } else {
        findUiccReaderByNameSlot(slotNumber)
    }
}

private fun SEService.findUiccReaderByNameSlot(slotNumber: Int): Reader? =
    getReaders().orEmpty().firstOrNull { reader ->
        val name = reader.safeOmapiName()
        name.equals("SIM$slotNumber", ignoreCase = true) ||
            (slotNumber == 1 && name.equals("SIM", ignoreCase = true))
    }

private fun Reader.safeOmapiName(): String =
    name.orEmpty().ifBlank { "Unnamed reader" }

private fun LocalProfileAssistant.requireValidExternalEuicc() {
    val eidResult = runCatching { eID.trim() }
    val infoResult = runCatching { euiccInfo2 }
    if (eidResult.isSuccess && eidResult.getOrNull().orEmpty().isNotBlank() && infoResult.getOrNull() != null) {
        return
    }

    if (eidResult.exceptionOrNull()?.indicatesOmapiAccessRuleDenied() == true ||
        infoResult.exceptionOrNull()?.indicatesOmapiAccessRuleDenied() == true
    ) {
        error("SIM 卡槽 eUICC 的 OMAPI 访问规则拒绝 Paysage 访问 ISD-R，需要在卡内 ARA-M/访问规则中加入 Paysage 签名。")
    }

    val eidStatus = when {
        eidResult.isFailure -> "EID=${eidResult.exceptionOrNull()?.safeLogName() ?: "Failed"}"
        eidResult.getOrNull().orEmpty().isBlank() -> "EID=Blank"
        else -> "EID=OK"
    }
    val infoStatus = when {
        infoResult.isFailure -> "EuiccInfo2=${infoResult.exceptionOrNull()?.safeLogName() ?: "Failed"}"
        infoResult.getOrNull() == null -> "EuiccInfo2=Unavailable"
        else -> "EuiccInfo2=OK"
    }
    error("外置 eUICC 通道未通过有效性检查（$eidStatus，$infoStatus）。")
}

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
