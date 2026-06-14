package joyin.takgi.paysage.mail

import org.junit.Assert.assertEquals
import org.junit.Test

class MailCommandRecordRetentionTest {
    @Test
    fun keepsThirtyDaysOfCommandRecords() {
        val now = 40L * 24L * 60L * 60L * 1000L

        assertEquals(
            10L * 24L * 60L * 60L * 1000L,
            MailCommandRecordRetention.cutoffMillis(now)
        )
    }

    @Test
    fun cutoffNeverGoesNegative() {
        assertEquals(0L, MailCommandRecordRetention.cutoffMillis(1_000L))
    }
}

