package org.kaloscope.tv.feature.home

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val HistoryTimestampPattern = Regex(
    """^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.\d{1,9})?(Z|[+-]\d{2}:?\d{2})$""",
)

internal fun formatHistoryUpdatedAt(
    value: String?,
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): String? {
    val match = value?.trim()?.let(HistoryTimestampPattern::matchEntire)
        ?: return null
    val offset = match.groupValues[2]
        .replace("Z", "+0000")
        .replace(":", "")
    val normalized = match.groupValues[1] + offset
    val parser = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ssZ",
        Locale.US,
    ).apply {
        isLenient = false
    }
    val position = ParsePosition(0)
    val parsed = parser.parse(normalized, position) ?: return null
    if (position.index != normalized.length) {
        return null
    }

    return SimpleDateFormat("yyyy/MM/dd HH:mm:ss", locale).apply {
        this.timeZone = timeZone
    }.format(parsed)
}
