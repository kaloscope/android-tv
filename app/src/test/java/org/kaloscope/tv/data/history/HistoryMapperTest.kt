package org.kaloscope.tv.data.history

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
            rating = "8.5",
        )

        val mapped = item.toModel()

        checkNotNull(mapped)
        assertEquals(100, mapped.percentage)
        assertEquals(8.5, mapped.rating ?: 0.0, 0.0)
        assertEquals("启程", mapped.title)
        assertEquals("/media/tv/S01E01.mkv", mapped.path)
    }

    @Test
    fun `drops records whose media was deleted`() {
        assertNull(historyItem(media = null).toModel())
    }

    @Test
    fun `uses parent metadata for history display while retaining episode media`() {
        val item = Json {
            ignoreUnknownKeys = true
        }.decodeFromString<HistoryItemData>(
            """
            {
              "id": 401,
              "updated_at": "2026-07-23T08:00:00Z",
              "rel_id": 301,
              "position": 1694,
              "percentage": 63,
              "media": {
                "id": 301,
                "path": "/media/tv/Stellar Archive/S01E01.mkv",
                "name": "S01E01.mkv",
                "title": "启程",
                "year": 2021,
                "season": 1,
                "episode": 1,
                "poster": "/images/episode-poster.webp",
                "backdrop": "/images/episode-backdrop.webp",
                "rating": "7.1",
                "parent": {
                  "id": 201,
                  "path": "/media/tv/Stellar Archive",
                  "name": "Stellar Archive",
                  "title": "群星档案",
                  "year": 2026,
                  "poster": "/images/series-poster.webp",
                  "backdrop": "/images/series-backdrop.webp",
                  "rating": "8.8"
                }
              }
            }
            """.trimIndent(),
        )

        val mapped = item.toModel()

        checkNotNull(mapped)
        assertEquals(301L, mapped.mediaId)
        assertEquals("启程", mapped.title)
        assertEquals(2026, mapped.year)
        assertEquals("/images/series-poster.webp", mapped.posterPath)
        assertEquals("/images/series-backdrop.webp", mapped.backdropPath)
        assertEquals(8.8, mapped.rating ?: 0.0, 0.0)
        assertEquals("群星档案", mapped.parentTitle)
    }

    @Test
    fun `falls back to episode fields when parent display metadata is missing`() {
        val item = historyItem(
            media = HistoryMediaData(
                id = 301,
                path = "/media/tv/S01E01.mkv",
                name = "S01E01.mkv",
                title = "启程",
                year = 2021,
                season = 1,
                episode = 1,
                poster = "/images/episode-poster.webp",
                backdrop = "/images/episode-backdrop.webp",
                rating = "7.1",
                parent = HistoryMediaData(
                    id = 201,
                    name = "群星档案",
                    year = null,
                    poster = " ",
                    backdrop = null,
                    rating = "unrated",
                ),
            ),
        )

        val mapped = item.toModel()

        checkNotNull(mapped)
        assertEquals("群星档案", mapped.parentTitle)
        assertEquals(2021, mapped.year)
        assertEquals("/images/episode-poster.webp", mapped.posterPath)
        assertEquals("/images/episode-backdrop.webp", mapped.backdropPath)
        assertEquals(7.1, mapped.rating ?: 0.0, 0.0)
    }
}

private fun historyItem(
    media: HistoryMediaData? = HistoryMediaData(
        id = 301,
        path = "/media/tv/S01E01.mkv",
        name = "S01E01.mkv",
        title = "启程",
        year = 2026,
        season = 1,
        episode = 1,
        poster = null,
        backdrop = null,
        rating = "8.5",
    ),
    percentage: Int = 63,
    rating: String? = null,
) = HistoryItemData(
    id = 401,
    updatedAt = "2026-07-23T08:00:00Z",
    relId = 301,
    position = 1694,
    percentage = percentage,
    media = media?.let { source ->
        rating?.let { source.copy(rating = it) } ?: source
    },
)
