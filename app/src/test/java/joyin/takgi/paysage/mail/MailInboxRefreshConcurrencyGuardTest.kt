package joyin.takgi.paysage.mail

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class MailInboxRefreshConcurrencyGuardTest {
    @Test
    fun serializesOverlappingRefreshBlocks() = runBlocking {
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondAttempted = CompletableDeferred<Unit>()
        val secondEntered = AtomicBoolean(false)
        val order = mutableListOf<String>()

        val first = async(Dispatchers.Default) {
            MailInboxRefreshConcurrencyGuard.runSerialized {
                order.add("first-start")
                firstEntered.complete(Unit)
                releaseFirst.await()
                order.add("first-end")
            }
        }
        firstEntered.await()

        val second = async(Dispatchers.Default) {
            secondAttempted.complete(Unit)
            MailInboxRefreshConcurrencyGuard.runSerialized {
                secondEntered.set(true)
                order.add("second-start")
            }
        }
        secondAttempted.await()

        val enteredBeforeRelease = withTimeoutOrNull(100) {
            while (!secondEntered.get()) {
                yield()
            }
            true
        } ?: false
        assertFalse(enteredBeforeRelease)

        releaseFirst.complete(Unit)
        first.await()
        second.await()

        assertTrue(secondEntered.get())
        assertEquals(listOf("first-start", "first-end", "second-start"), order)
    }
}
