package org.kaloscope.tv.feature.home

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryTimestampFormatterTest {
    private val shanghai = TimeZone.getTimeZone("GMT+08:00")

    @Test
    fun utcTimestampUsesTheDeviceTimeZone() {
        assertEquals(
            "2026/07/27 16:00:00",
            formatHistoryUpdatedAt(
                value = "2026-07-27T08:00:00Z",
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun fractionalOffsetTimestampUsesTheDeviceTimeZone() {
        assertEquals(
            "2026/07/27 13:30:00",
            formatHistoryUpdatedAt(
                value = "2026-07-27T08:00:00.123456+02:30",
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun absentOrInvalidTimestampIsOmitted() {
        assertNull(formatHistoryUpdatedAt(null, shanghai, Locale.US))
        assertNull(formatHistoryUpdatedAt(" ", shanghai, Locale.US))
        assertNull(formatHistoryUpdatedAt("not-a-date", shanghai, Locale.US))
        assertNull(
            formatHistoryUpdatedAt(
                "2026-02-30T08:00:00Z",
                shanghai,
                Locale.US,
            ),
        )
    }
}
