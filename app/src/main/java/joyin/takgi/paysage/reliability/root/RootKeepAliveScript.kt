package joyin.takgi.paysage.reliability.root

import android.content.Context
import java.io.File

/**
 * Magisk service.d 看门狗脚本:开机后周期检查进程,不在就拉起保活服务。
 * 脚本内容渲染是纯逻辑便于测试,安装/卸载经 [RootShell] 落盘。
 */
object RootKeepAliveScript {

    const val SCRIPT_PATH = "/data/adb/service.d/paysage_keepalive.sh"
    private const val CHECK_INTERVAL_SECONDS = 20

    fun render(packageName: String, keepAliveServiceClass: String): String = """
        #!/system/bin/sh
        # Paysage root keep-alive watchdog (installed by the app)
        PKG=$packageName
        SERVICE=$packageName/$keepAliveServiceClass
        while true; do
            if pm list packages -e ${'$'}PKG 2>/dev/null | grep -q ${'$'}PKG; then
                if ! pidof ${'$'}PKG >/dev/null 2>&1; then
                    am start-foreground-service -n ${'$'}SERVICE >/dev/null 2>&1
                fi
            fi
            sleep $CHECK_INTERVAL_SECONDS
        done
    """.trimIndent()

    suspend fun install(context: Context): RootResult {
        val appContext = context.applicationContext
        val staged = File(appContext.cacheDir, "paysage_keepalive.sh")
        staged.writeText(
            render(
                packageName = appContext.packageName,
                keepAliveServiceClass = "joyin.takgi.paysage.service.SmsKeepAliveService"
            )
        )
        val result = RootShell.exec(
            "mkdir -p /data/adb/service.d && cp ${staged.absolutePath} $SCRIPT_PATH " +
                "&& chmod 700 $SCRIPT_PATH && chown 0:0 $SCRIPT_PATH"
        )
        staged.delete()
        return result
    }

    suspend fun remove(): RootResult =
        RootShell.exec("rm -f $SCRIPT_PATH")

    suspend fun installed(): Boolean =
        RootShell.exec("test -f $SCRIPT_PATH && echo yes").output.contains("yes")
}
