package org.kaloscope.tv.data.search

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.data.search.remote.IndexerDanmakuData
import org.kaloscope.tv.data.search.remote.IndexerData
import org.kaloscope.tv.data.search.remote.IndexerPageData
import org.kaloscope.tv.data.search.remote.IndexerResourceData
import org.kaloscope.tv.data.search.remote.IndexerResourcePageData

class SearchMapperTest {
    @Test
    fun `indexers keep only real searchable sources`() {
        val page = IndexerPageData(
            items = listOf(
                IndexerData(11, "  星海站  ", "/icon.png", listOf("search_start")),
                IndexerData(12, "无搜索", null, listOf("details_start")),
                IndexerData(0, "无效", null, listOf("search_start")),
            ),
        )

        val indexers = page.toModels()

        assertEquals(1, indexers.size)
        assertEquals(11L, indexers.single().id)
        assertEquals("星海站", indexers.single().name)
    }

    @Test
    fun `search page removes non video and invalid resources`() {
        val page = IndexerResourcePageData(
            total = 42,
            items = listOf(
                resource(id = "v1", title = "视频", mediaType = "video"),
                resource(id = "v2", title = "兼容视频", mediaType = null),
                resource(id = "a1", title = "音频", mediaType = "audio"),
                resource(id = null, title = "无标识", mediaType = "video"),
            ),
        )

        val model = page.toModel(pageNumber = 1, pageSize = 20)

        assertEquals(listOf("v1", "v2"), model.items.map { it.id })
        assertEquals(8.6, model.items.first().rating)
        assertTrue(model.hasNext)
    }

    @Test
    fun `simple search page stops when page is short`() {
        val model = IndexerResourcePageData(
            items = listOf(resource("v1", "视频", "video")),
        ).toModel(pageNumber = 2, pageSize = 20)

        assertFalse(model.hasNext)
        assertNull(model.total)
    }

    @Test
    fun `details map top level source and valid danmakus`() {
        val source = IndexerResourceData(
            id = "v1",
            title = "视频",
            mediaType = "video",
            url = " /_api/media/proxy?id=1 ",
            videoType = "hls",
            danmakus = listOf(
                IndexerDanmakuData("d1", " Ready ", "scroll", "#FFFFFF", 12_500),
                IndexerDanmakuData("d2", "", "scroll", null, 10),
            ),
        ).toPlaybackSource(indexerId = 11, fallbackTitle = "备用")

        checkNotNull(source)
        assertEquals("/_api/media/proxy?id=1", source.url)
        assertEquals(NetworkVideoType.Hls, source.videoType)
        assertEquals("Ready", source.danmakus.single().text)
    }

    @Test
    fun `details without playable top level source are rejected`() {
        assertNull(
            resource("v1", "视频", "video")
                .toPlaybackSource(indexerId = 11, fallbackTitle = "备用"),
        )
    }
}

private fun resource(
    id: String?,
    title: String?,
    mediaType: String?,
) = IndexerResourceData(
    id = id,
    title = title,
    mediaType = mediaType,
    rating = JsonPrimitive(8.6),
)
