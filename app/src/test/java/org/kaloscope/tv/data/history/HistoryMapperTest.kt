package org.kaloscope.tv.data.history

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.network.HistoryItemData
import org.kaloscope.tv.core.network.HistoryMediaData

class HistoryMapperTest {
    @Test
    fun `maps numeric string rating and clamps progress`() {
        val item = historyItem(
            percentage = 140,
            rating = JsonPrimitive("8.5"),
        )

        val mapped = item.toModel()

        checkNotNull(mapped)
        assertEquals(100, mapped.percentage)
        assertEquals(8.5, mapped.rating ?: 0.0, 0.0)
        assertEquals("启程", mapped.title)
    }

    @Test
    fun `drops records whose media was deleted`() {
        assertNull(historyItem(media = null).toModel())
    }
}

private fun historyItem(
    media: HistoryMediaData? = HistoryMediaData(
        id = 301,
        name = "S01E01.mkv",
        title = "启程",
        year = 2026,
        season = 1,
        episode = 1,
        poster = null,
        backdrop = null,
        rating = JsonPrimitive(8.5),
    ),
    percentage: Int = 63,
    rating: JsonPrimitive = JsonPrimitive(8.5),
) = HistoryItemData(
    id = 401,
    updatedAt = "2026-07-23T08:00:00Z",
    relId = 301,
    position = 1694,
    percentage = percentage,
    media = media?.copy(rating = rating),
)
