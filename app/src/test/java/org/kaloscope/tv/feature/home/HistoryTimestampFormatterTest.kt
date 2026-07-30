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
    fun previousLocalDayUsesYesterdayLabel() {
        assertEquals(
            "昨天",
            formatHistoryUpdatedAt(
                value = timestampForLocalDayOffset(-1),
                todayLabel = "今天",
                yesterdayLabel = "昨天",
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun currentLocalDayUsesTodayLabel() {
        assertEquals(
            "今天",
            formatHistoryUpdatedAt(
                value = timestampForLocalDayOffset(0),
                todayLabel = "今天",
                yesterdayLabel = "昨天",
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun olderTimestampOmitsClockTime() {
        assertEquals(
            "2026/07/27",
            formatHistoryUpdatedAt(
                value = "2026-07-27T08:00:00.123456+02:30",
                todayLabel = "今天",
                yesterdayLabel = "昨天",
                timeZone = shanghai,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun absentOrInvalidTimestampIsOmitted() {
        assertNull(formatHistoryUpdatedAt(null, "今天", "昨天", shanghai, Locale.US))
        assertNull(formatHistoryUpdatedAt(" ", "今天", "昨天", shanghai, Locale.US))
        assertNull(
            formatHistoryUpdatedAt(
                "not-a-date",
                "今天",
                "昨天",
                shanghai,
                Locale.US,
            ),
        )
        assertNull(
            formatHistoryUpdatedAt(
                "2026-02-30T08:00:00Z",
                "今天",
                "昨天",
                shanghai,
                Locale.US,
            ),
        )
    }

    private fun timestampForLocalDayOffset(offset: Int): String {
        val calendar = Calendar.getInstance(shanghai).apply {
            add(Calendar.DAY_OF_YEAR, offset)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = shanghai
        }.format(calendar.time)
    }
}
