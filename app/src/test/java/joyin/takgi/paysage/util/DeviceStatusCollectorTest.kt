package joyin.takgi.paysage.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceStatusCollectorTest {
    @Test
    fun uptimePartsSplitDaysHoursMinutesAndSeconds() {
        val uptimeMillis =
            2L * 24 * 60 * 60 * 1_000 +
                3L * 60 * 60 * 1_000 +
                4L * 60 * 1_000 +
                5L * 1_000 +
                999L

        assertEquals(
            UptimeParts(days = 2, hours = 3, minutes = 4, seconds = 5),
            DeviceStatusCollector.uptimeParts(uptimeMillis)
        )
    }

    @Test
    fun negativeUptimeIsReportedAsZero() {
        assertEquals(
            UptimeParts(days = 0, hours = 0, minutes = 0, seconds = 0),
            DeviceStatusCollector.uptimeParts(-1L)
        )
    }

    @Test
    fun uptimePartsHandleUnitBoundaries() {
        assertEquals(
            UptimeParts(days = 0, hours = 0, minutes = 0, seconds = 59),
            DeviceStatusCollector.uptimeParts(59_999L)
        )
        assertEquals(
            UptimeParts(days = 0, hours = 0, minutes = 1, seconds = 0),
            DeviceStatusCollector.uptimeParts(60_000L)
        )
        assertEquals(
            UptimeParts(days = 1, hours = 0, minutes = 0, seconds = 0),
            DeviceStatusCollector.uptimeParts(24L * 60 * 60 * 1_000)
        )
    }
}
