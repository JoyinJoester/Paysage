package joyin.takgi.paysage.reliability.root

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicReference

data class RootResult(
    val exitCode: Int,
    val output: String
) {
    val success: Boolean get() = exitCode == 0
}

/**
 * Root shell 执行器。
 *
 * 优先复用一条持久 su 会话(命令写入 stdin,读到结束标记为止),
 * 避免每条命令都重新走 su 授权与进程创建,让保活/授权操作足够灵敏;
 * 会话不可用时回退为逐条 `su -c`。
 */
object RootShell {

    private val session = AtomicReference<Process?>(null)
    private val mutex = Mutex()
    private var commandSeq = 0

    suspend fun isAvailable(): Boolean =
        runCatching {
            val result = exec("id", timeoutMs = 4_000L)
            result.success && result.output.contains("uid=0")
        }.getOrDefault(false)

    suspend fun exec(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): RootResult =
        mutex.withLock {
            runCatching { execViaSession(command, timeoutMs) }.getOrElse {
                // 会话可能已死,重开一次;仍失败则回退逐条 su -c
                closeSession()
                runCatching { execViaSession(command, timeoutMs) }
                    .getOrElse { execViaSuC(command, timeoutMs) }
            }
        }

    fun close() {
        closeSession()
    }

    private fun execViaSession(command: String, timeoutMs: Long): RootResult {
        var process = session.get() ?: openSession().also { session.set(it) }
        if (!process.isAlive) {
            closeSession()
            process = openSession().also { session.set(it) }
        }
        commandSeq += 1
        val marker = "$MARKER_PREFIX${commandSeq}_"
        val writer = OutputStreamWriter(process.outputStream, Charsets.UTF_8)
        writer.write("$command\n")
        writer.write("echo $marker\$?\n")
        writer.flush()
        val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
        return readUntilMarker(reader, marker, timeoutMs)
    }

    private fun openSession(): Process =
        ProcessBuilder("su")
            .redirectErrorStream(true)
            .start()

    private fun closeSession() {
        session.getAndSet(null)?.destroy()
    }

    private fun readUntilMarker(reader: BufferedReader, marker: String, timeoutMs: Long): RootResult {
        val deadline = System.currentTimeMillis() + timeoutMs
        val lines = StringBuilder()
        while (System.currentTimeMillis() < deadline) {
            val line = if (reader.ready()) reader.readLine() else {
                Thread.sleep(20)
                continue
            }
            if (line.startsWith(marker)) {
                return RootResult(
                    exitCode = line.removePrefix(marker).trim().toIntOrNull() ?: -1,
                    output = lines.toString().trim()
                )
            }
            lines.appendLine(line)
        }
        // 超时视为会话异常,重建
        closeSession()
        return RootResult(exitCode = -1, output = lines.toString().trim())
    }

    private fun execViaSuC(command: String, timeoutMs: Long): RootResult {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(true)
            .start()
        val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
        val output = StringBuilder()
        val deadline = System.currentTimeMillis() + timeoutMs
        val finished = false
        while (!finished && System.currentTimeMillis() < deadline) {
            while (reader.ready()) {
                reader.readLine()?.let { output.appendLine(it) }
            }
            if (process.isAlive) {
                Thread.sleep(20)
            } else {
                break
            }
        }
        if (process.isAlive) {
            process.destroy()
        }
        @Suppress("ControlFlowWithEmptyBody")
        while (reader.ready()) {
            output.appendLine(reader.readLine())
        }
        val exit = runCatching { process.exitValue() }.getOrDefault(-1)
        return RootResult(exit, output.toString().trim())
    }

    const val MARKER_PREFIX = "__PAYSAGE_DONE_"
    const val DEFAULT_TIMEOUT_MS = 8_000L
}
