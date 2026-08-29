package joyin.takgi.paysage.xposed

import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * LSPosed 模块入口:hook com.android.phone 里短信入站处理,在系统
 * 收到短信的第一现场取到内容并发给 Paysage。任何 hook 失败都只记日志,
 * 不影响电话进程;广播通道失败时仍有广播/观察器/无障碍/root 兜底。
 */
class PaysageXposedModule(base: io.github.libxposed.api.XposedContext, param: ModuleLoadedParam) :
    XposedModule(base, param) {

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != TARGET_PACKAGE) return
        hookInboundSmsHandler(param.classLoader)
    }

    private fun hookInboundSmsHandler(classLoader: ClassLoader) {
        try {
            val handlerClass = Class.forName(
                "com.android.internal.telephony.InboundSmsHandler",
                false,
                classLoader
            )
            var hooked = 0
            for (method in handlerClass.declaredMethods) {
                val parameterTypes = method.parameterTypes
                val isSmsDispatch = (method.name == "dispatchIntent" ||
                    method.name == "dispatchSmsDeliveryBroadcast") &&
                    parameterTypes.isNotEmpty() &&
                    parameterTypes[0] == Intent::class.java
                if (!isSmsDispatch) continue

                runCatching {
                    method.isAccessible = true
                    hookBefore(method) { callback ->
                        runCatching {
                            forwardFromIntent(callback.args.filterIsInstance<Intent>().firstOrNull())
                        }
                    }
                }.onSuccess { hooked++ }
            }
            log("hooked $hooked dispatch methods in com.android.phone")
        } catch (error: Throwable) {
            log("failed to hook InboundSmsHandler", error)
        }
    }

    private fun forwardFromIntent(intent: Intent?) {
        intent ?: return
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {
            return
        }
        val messages = runCatching {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        }.getOrNull() ?: return

        // 同一 intent 内的多段按发送方拼接,跨 intent 的续段交给应用内分段合并
        messages
            .filter { it.displayOriginatingAddress?.isNotBlank() == true }
            .groupBy { it.displayOriginatingAddress.orEmpty() }
            .forEach { (sender, parts) ->
                val content = parts.joinToString(separator = "") {
                    it.displayMessageBody.orEmpty()
                }.trim()
                if (content.isBlank()) return@forEach
                val broadcast = Intent(ACTION_XPOSED_SMS)
                    .setPackage(APP_PACKAGE)
                    .putExtra(EXTRA_SENDER, sender)
                    .putExtra(EXTRA_CONTENT, content)
                    .putExtra(
                        EXTRA_TIMESTAMP,
                        parts.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                    )
                // 只允许持有电话权限的系统组件伪造,防止第三方应用注入假短信
                sendBroadcast(broadcast, BRIDGE_PERMISSION)
            }
    }

    companion object {
        const val TAG = "PaysageXposed"
        const val TARGET_PACKAGE = "com.android.phone"
        const val APP_PACKAGE = "joyin.takgi.paysage"
        const val ACTION_XPOSED_SMS = "$APP_PACKAGE.action.XPOSED_SMS"
        const val BRIDGE_PERMISSION = "android.permission.MODIFY_PHONE_STATE"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_TIMESTAMP = "timestamp"
    }
}
