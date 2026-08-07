package org.kaloscope.tv.data.search

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.NetworkMediaType
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.data.search.remote.IndexerChapterData
import org.kaloscope.tv.data.search.remote.IndexerDanmakuData
import org.kaloscope.tv.data.search.remote.IndexerData
import org.kaloscope.tv.data.search.remote.IndexerDefinitionData
import org.kaloscope.tv.data.search.remote.IndexerFilterData
import org.kaloscope.tv.data.search.remote.IndexerPageData
import org.kaloscope.tv.data.search.remote.IndexerResourceData
import org.kaloscope.tv.data.search.remote.IndexerResourcePageData
import org.kaloscope.tv.data.search.remote.IndexerSearchConfigData

class SearchMapperTest {
    @Test
    fun `indexers keep only real preview searchable sources`() {
        val page = IndexerPageData(
            items = listOf(
                IndexerData(
                    id = 11,
                    name = "  星海站  ",
                    icon = "/icon.png",
                    nodeTypes = listOf("search_start"),
                    onlyPreview = true,
                ),
                IndexerData(
                    id = 12,
                    name = "下载站",
                    nodeTypes = listOf("search_start"),
                    onlyPreview = false,
                ),
                IndexerData(
                    id = 13,
                    name = "无搜索",
                    nodeTypes = listOf("details_start"),
                    onlyPreview = true,
                ),
                IndexerData(
                    id = 0,
                    name = "无效",
                    nodeTypes = listOf("search_start"),
                    onlyPreview = true,
                ),
            ),
        )

        val indexers = page.toModels()

        assertEquals(1, indexers.size)
        assertEquals(11L, indexers.single().id)
        assertEquals("星海站", indexers.single().name)
    }

    @Test
    fun `filter mapper accepts declared types and rejects invalid definitions`() {
        val filters = IndexerSearchConfigData(
            filters = linkedMapOf(
                "alias" to IndexerFilterData(type = "text", label = "别名"),
                "mode" to IndexerFilterData(
                    type = "radio",
                    options = linkedMapOf("movie" to "电影"),
                ),
                "region" to IndexerFilterData(
                    type = "checkbox",
                    options = linkedMapOf("cn" to "中国", "jp" to "日本"),
                ),
                "source" to IndexerFilterData(
                    type = "select",
                    options = linkedMapOf("web" to "网络"),
                ),
                "release_at" to IndexerFilterData(type = "datetime"),
                "page_num" to IndexerFilterData(type = "text"),
                "unknown" to IndexerFilterData(type = "range"),
                "empty_options" to IndexerFilterData(type = "select"),
            ),
        ).toFilterDefinitions()

        assertEquals(
            listOf("alias", "mode", "region", "source", "release_at"),
            filters.map { it.key },
        )
        assertEquals(
            listOf(
                SearchFilterType.Text,
                SearchFilterType.Radio,
                SearchFilterType.Checkbox,
                SearchFilterType.Select,
                SearchFilterType.DateTime,
            ),
            filters.map { it.type },
        )
        assertEquals(listOf("cn", "jp"), filters[2].options.map { it.value })
    }

    @Test
    fun `search page keeps supported media and removes audio and invalid resources`() {
        val page = IndexerResourcePageData(
            total = 42,
            items = listOf(
                resource(id = "v1", title = "视频", mediaType = "video"),
                resource(id = "v2", title = "兼容视频", mediaType = null),
                resource(id = "i1", title = "图片", mediaType = "image"),
                resource(id = "t1", title = "文本", mediaType = "text"),
                resource(id = "a1", title = "音频", mediaType = "audio"),
                resource(id = null, title = "无标识", mediaType = "video"),
            ),
        )

        val model = page.toModel(pageNumber = 1, pageSize = 20)

        assertEquals(listOf("v1", "v2", "i1", "t1"), model.items.map { it.id })
        assertEquals(
            listOf(
                NetworkMediaType.Video,
                NetworkMediaType.Video,
                NetworkMediaType.Image,
                NetworkMediaType.Text,
            ),
            model.items.map { it.mediaType },
        )
        assertEquals(8.6, model.items.first().rating)
        assertTrue(model.hasNext)
    }

    @Test
    fun `item media type wins profile hint and missing type uses hint`() {
        val model = IndexerResourcePageData(
            items = listOf(
                resource(id = "text", title = "文本", mediaType = "text"),
                resource(id = "hint", title = "图片", mediaType = null),
                resource(id = "audio", title = "音频", mediaType = "audio"),
            ),
        ).toModel(
            pageNumber = 1,
            pageSize = 20,
            mediaTypeHint = NetworkMediaType.Image,
        )

        assertEquals(listOf("text", "hint"), model.items.map { it.id })
        assertEquals(
            listOf(NetworkMediaType.Text, NetworkMediaType.Image),
            model.items.map { it.mediaType },
        )

        val audioProfile = IndexerResourcePageData(
            items = listOf(resource(id = "missing", title = "音频", mediaType = null)),
        ).toModel(
            pageNumber = 1,
            pageSize = 20,
            mediaTypeHint = NetworkMediaType.Audio,
        )
        assertTrue(audioProfile.items.isEmpty())
    }

    @Test
    fun `search items use video hint unless the item overrides it`() {
        val model = IndexerResourcePageData(
            items = listOf(
                resource(id = "hint", title = "配置类型", mediaType = "video"),
                resource(id = "item", title = "资源类型", mediaType = "video")
                    .copy(videoType = "mp4"),
            ),
        ).toModel(
            pageNumber = 1,
            pageSize = 20,
            videoTypeHint = NetworkVideoType.Dash,
        )

        assertEquals(
            listOf(NetworkVideoType.Dash, NetworkVideoType.Mp4),
            model.items.map { it.videoTypeHint },
        )
    }

    @Test
    fun `reader payload parses string array images and image count`() {
        val parser = Json { ignoreUnknownKeys = true }
        val stringText = parser.decodeFromString<IndexerResourceData>(
            """{"id":"t1","text":"first\nsecond"}""",
        )
        val arrayText = parser.decodeFromString<IndexerResourceData>(
            """{"id":"t2","text":["first","second"]}""",
        )
        val image = parser.decodeFromString<IndexerResourceData>(
            """{"id":"i1","images":["one.jpg","two.jpg"],"image_count":8}""",
        )

        assertEquals("first\nsecond", stringText.toTextBody())
        assertEquals("first\n\nsecond", arrayText.toTextBody())
        assertEquals(listOf("one.jpg", "two.jpg"), image.images)
        assertEquals(8, image.imageCount)
    }

    @Test
    fun `image count accepts numeric workflow string`() {
        val parser = Json { ignoreUnknownKeys = true }
        val image = parser.decodeFromString<IndexerResourceData>(
            """{"id":"i1","images":["one.jpg"],"image_count":"8"}""",
        )

        assertEquals(8, image.imageCount)
    }

    @Test
    fun `search result maps web grid metadata`() {
        val model = IndexerResourcePageData(
            items = listOf(
                IndexerResourceData(
                    id = "v1",
                    title = "视频",
                    mediaType = "video",
                    rating = JsonPrimitive(8.6),
                    ranking = JsonPrimitive(2),
                    category = " 电影 ",
                    misc = " 1:30:00 ",
                    uploader = " Admin ",
                    uploadedAt = " 10 Hours Ago ",
                    size = " 1GB ",
                ),
            ),
        ).toModel(pageNumber = 1, pageSize = 20)

        val result = model.items.single()
        assertEquals(2, result.ranking)
        assertEquals(8.6, result.rating)
        assertEquals("电影", result.category)
        assertEquals("1:30:00", result.misc)
        assertEquals("Admin", result.uploader)
        assertEquals("10 Hours Ago", result.uploadedAt)
        assertEquals("1GB", result.size)
    }

    @Test
    fun `search ranking normalizes web grid values`() {
        val rankings = listOf(
            JsonPrimitive("3"),
            JsonPrimitive(2.6),
            JsonPrimitive(0),
            JsonPrimitive(101),
            JsonPrimitive("not-a-rank"),
        )
        val model = IndexerResourcePageData(
            items = rankings.mapIndexed { index, ranking ->
                IndexerResourceData(
                    id = "v$index",
                    title = "视频$index",
                    mediaType = "video",
                    ranking = ranking,
                )
            },
        ).toModel(pageNumber = 1, pageSize = 20)

        assertEquals(
            listOf(3, 3, null, null, null),
            model.items.map { it.ranking },
        )
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
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "备用",
            preferredDefinition = TranscodeResolution.P1080,
        )

        checkNotNull(source)
        assertEquals("/_api/media/proxy?id=1", source.url)
        assertEquals(NetworkVideoType.Hls, source.videoType)
        assertEquals("Ready", source.danmakus.single().text)
    }

    @Test
    fun `preferred definition is selected and chapter metadata is retained`() {
        val source = IndexerResourceData(
            id = "v1",
            title = "视频",
            mediaType = "video",
            url = "https://cdn.example/master.m3u8",
            videoType = "hls",
            definitions = listOf(
                IndexerDefinitionData(
                    url = "https://cdn.example/720.m3u8",
                    definition = JsonPrimitive("720P"),
                ),
                IndexerDefinitionData(
                    url = "https://cdn.example/1080.m3u8",
                    definition = JsonPrimitive(1080),
                ),
            ),
            chapters = listOf(
                IndexerChapterData("ep-1", null, "第 1 集", "第 1 季"),
                IndexerChapterData(null, "https://cdn.example/ep-2.m3u8", "第 2 集", null),
            ),
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "备用",
            preferredDefinition = TranscodeResolution.P1080,
        )

        checkNotNull(source)
        assertEquals("https://cdn.example/1080.m3u8", source.url)
        assertEquals("1080", source.selectedDefinition?.label)
        assertEquals(listOf("第 1 集", "第 2 集"), source.chapters.map { it.title })
    }

    @Test
    fun `DASH mapping prefers matching HEVC definition when requested`() {
        val source = IndexerResourceData(
            id = "v1",
            title = "视频",
            mediaType = "video",
            videoType = "dash",
            definitions = listOf(
                IndexerDefinitionData(
                    url = "<MPD><Representation codecs=\"avc1.640033\" /></MPD>",
                    definition = JsonPrimitive("480P 清晰"),
                ),
                IndexerDefinitionData(
                    url = "<MPD><Representation codecs=\"hvc1.1.6.L120.90\" /></MPD>",
                    definition = JsonPrimitive("480P 清晰 HEVC"),
                ),
            ),
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "备用",
            preferredDefinition = TranscodeResolution.P480,
            preferHevcForDash = true,
        )

        checkNotNull(source)
        assertEquals(1, source.selectedDefinitionIndex)
        assertEquals("480P 清晰 HEVC", source.selectedDefinition?.label)
        assertTrue(source.url.contains("hvc1"))
    }

    @Test
    fun `first direct chapter becomes playable when top level source is absent`() {
        val source = IndexerResourceData(
            id = "v1",
            title = "视频",
            mediaType = "video",
            videoType = "dash",
            chapters = listOf(
                IndexerChapterData(
                    id = null,
                    url = "https://cdn.example/ep-1.mpd",
                    title = "第 1 集",
                ),
            ),
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "备用",
            preferredDefinition = TranscodeResolution.P1080,
        )

        checkNotNull(source)
        assertEquals("https://cdn.example/ep-1.mpd", source.url)
        assertEquals(NetworkVideoType.Dash, source.videoType)
        assertEquals(0, source.selectedChapterIndex)
    }

    @Test
    fun `details without any playable source are rejected`() {
        assertNull(
            resource("v1", "视频", "video").toPlaybackSource(
                indexerId = 11,
                fallbackTitle = "备用",
                preferredDefinition = TranscodeResolution.P1080,
            ),
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
