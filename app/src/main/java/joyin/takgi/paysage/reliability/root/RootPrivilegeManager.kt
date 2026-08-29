package joyin.takgi.paysage.reliability.root

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Root 一键授权:静默授予危险权限、解除后台限制、加入电池优化白名单。
 * 命令构造是纯逻辑便于测试,执行走 [RootShell]。
 */
object RootPrivilegeCommands {

    val DANGEROUS_PERMISSIONS = listOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_CALL_LOG",
        "android.permission.CAMERA"
    )

    fun buildGrantCommands(packageName: String, includeNotificationPermission: Boolean): List<String> =
        buildList {
            val permissions = DANGEROUS_PERMISSIONS +
                if (includeNotificationPermission) listOf("android.permission.POST_NOTIFICATIONS") else emptyList()
            permissions.forEach { permission ->
                add("pm grant $packageName $permission")
            }
            // 解除后台与前台服务限制,保证保活链路不被 appops 拦截
            add("cmd appops set $packageName RUN_IN_BACKGROUND allow")
            add("cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow")
            add("cmd appops set $packageName START_FOREGROUND allow")
            add("dumpsys deviceidle whitelist +$packageName")
        }
}

object RootPrivilegeManager {

    suspend fun grantAll(context: Context): Pair<Int, List<String>> {
        val appContext = context.applicationContext
        val commands = RootPrivilegeCommands.buildGrantCommands(
            packageName = appContext.packageName,
            includeNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
        var succeeded = 0
        val failures = mutableListOf<String>()
        commands.forEach { command ->
            val result = RootShell.exec(command)
            if (result.success) {
                succeeded += 1
            } else {
                failures.add(command)
            }
        }
        val store = RootSettingsStore(appContext)
        store.lastGrantTimestamp = System.currentTimeMillis()
        return succeeded to failures
    }

    /** 已在系统层授予的危险权限检查结果(不走 root,普通查询即可) */
    fun missingPermissions(context: Context): List<String> {
        val permissions = RootPrivilegeCommands.DANGEROUS_PERMISSIONS +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf("android.permission.POST_NOTIFICATIONS")
            } else {
                emptyList()
            }
        return permissions.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
    }
}
