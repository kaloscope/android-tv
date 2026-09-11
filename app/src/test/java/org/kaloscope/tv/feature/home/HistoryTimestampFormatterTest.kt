package org.kaloscope.tv.feature.home

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryTimestampFormatterTest {
    private val shanghai = TimeZone.getTimeZone("GMT+08:00")

    @Test
    fun previousLocalDayIncludesFullDateAndClockTime() {
        val timestamp = timestampForLocalDayOffset(-1)
        assertEquals(
            "${timestamp.take(10).replace('-', '/')} 12:34:56",
            formatHistoryUpdatedAt(
                value = timestamp,
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun currentLocalDayIncludesFullDateAndClockTime() {
        val timestamp = timestampForLocalDayOffset(0)
        assertEquals(
            "${timestamp.take(10).replace('-', '/')} 12:34:56",
            formatHistoryUpdatedAt(
                value = timestamp,
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun olderTimestampIncludesClockTimeInLocalTimeZone() {
        assertEquals(
            "2026/07/27 13:35:09",
            formatHistoryUpdatedAt(
                value = "2026-07-27T08:05:09.123456+02:30",
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun utcTimestampRollsOverToNextLocalYear() {
        assertEquals(
            "2027/01/01 00:05:09",
            formatHistoryUpdatedAt(
                value = "2026-12-31T16:05:09Z",
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun absentOrInvalidTimestampIsOmitted() {
        assertNull(formatHistoryUpdatedAt(null, shanghai, Locale.US))
        assertNull(formatHistoryUpdatedAt(" ", shanghai, Locale.US))
        assertNull(
            formatHistoryUpdatedAt(
                "not-a-date",
                shanghai,
                Locale.US,
            ),
        )
        assertNull(
            formatHistoryUpdatedAt(
                "2026-02-30T08:00:00Z",
                shanghai,
                Locale.US,
            ),
        )
    }

    private fun timestampForLocalDayOffset(offset: Int): String {
        val calendar = Calendar.getInstance(shanghai).apply {
            add(Calendar.DAY_OF_YEAR, offset)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 34)
            set(Calendar.SECOND, 56)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = shanghai
        }.format(calendar.time)
    }
}
