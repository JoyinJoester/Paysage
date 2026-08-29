package joyin.takgi.paysage.reliability

import joyin.takgi.paysage.reliability.root.RootKeepAliveScript
import joyin.takgi.paysage.reliability.root.RootPrivilegeCommands
import org.junit.Assert.assertTrue
import org.junit.Test

class RootEnhancementCommandsTest {

    @Test
    fun keepAliveScriptChecksPidAndRestartsService() {
        val script = RootKeepAliveScript.render(
            packageName = "joyin.takgi.paysage",
            keepAliveServiceClass = "joyin.takgi.paysage.service.SmsKeepAliveService"
        )
        assertTrue(script.startsWith("#!/system/bin/sh"))
        // 包名与服务名通过 shell 变量注入,进程存在性检查 + 拉起保活服务
        assertTrue(script.contains("PKG=joyin.takgi.paysage"))
        assertTrue(script.contains("SERVICE=joyin.takgi.paysage/joyin.takgi.paysage.service.SmsKeepAliveService"))
        assertTrue(script.contains("pidof \$PKG"))
        assertTrue(script.contains("am start-foreground-service -n \$SERVICE"))
        assertTrue(script.contains("sleep 20"))
        assertTrue(script.contains("pm list packages -e \$PKG"))
        // 包连续缺失约 10 分钟后自删脚本,避免卸载应用后留下孤儿循环
        assertTrue(script.contains("MISSING=\$((MISSING+1))"))
        assertTrue(script.contains("rm -f \"\$SCRIPT\""))
        assertTrue(script.contains("exit 0"))
    }

    @Test
    fun grantCommandsCoverPermissionsAppopsAndBatteryWhitelist() {
        val commands = RootPrivilegeCommands.buildGrantCommands(
            packageName = "joyin.takgi.paysage",
            includeNotificationPermission = true
        )
        val joined = commands.joinToString("\n")
        assertTrue(joined.contains("pm grant joyin.takgi.paysage android.permission.READ_SMS"))
        assertTrue(joined.contains("pm grant joyin.takgi.paysage android.permission.POST_NOTIFICATIONS"))
        assertTrue(joined.contains("cmd appops set joyin.takgi.paysage RUN_ANY_IN_BACKGROUND allow"))
        assertTrue(joined.contains("dumpsys deviceidle whitelist +joyin.takgi.paysage"))
    }

    @Test
    fun grantCommandsOmitNotificationPermissionBelowTiramisu() {
        val commands = RootPrivilegeCommands.buildGrantCommands(
            packageName = "joyin.takgi.paysage",
            includeNotificationPermission = false
        )
        assertTrue(commands.none { it.contains("POST_NOTIFICATIONS") })
    }
}
