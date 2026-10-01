package org.kaloscope.tv.data.search

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.NetworkChapter
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
    fun `search items inherit missing video type but keep explicit values`() {
        val model = IndexerResourcePageData(
            items = listOf(
                resource(id = "hint", title = "配置类型", mediaType = "video"),
                resource(id = "item", title = "资源类型", mediaType = "video")
                    .copy(videoType = "mp4"),
                resource(id = "unknown", title = "未知类型", mediaType = "video")
                    .copy(videoType = "custom"),
            ),
        ).toModel(
            pageNumber = 1,
            pageSize = 20,
            videoTypeHint = NetworkVideoType.Dash,
        )

        assertEquals(
            listOf(
                NetworkVideoType.Dash,
                NetworkVideoType.Mp4,
                NetworkVideoType.Unknown,
            ),
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
    fun `search numeric metadata rejects structured values`() {
        for (value in listOf(JsonObject(emptyMap()), JsonArray(emptyList()))) {
            for (item in listOf(
                resource("v1", "Video", "video").copy(rating = value),
                resource("v1", "Video", "video").copy(ranking = value),
            )) {
                assertThrows(SerializationException::class.java) {
                    IndexerResourcePageData(items = listOf(item))
                        .toModel(pageNumber = 1, pageSize = 20)
                }
            }
        }
    }

    @Test
    fun `search ratings preserve scalar and missing value semantics`() {
        val cases = listOf(
            null to null,
            JsonNull to null,
            JsonPrimitive("") to null,
            JsonPrimitive("not-a-rating") to null,
            JsonPrimitive(true) to null,
            JsonPrimitive("8.6") to 8.6,
            JsonPrimitive(7.5) to 7.5,
        )
        val model = IndexerResourcePageData(
            items = cases.mapIndexed { index, (rating, _) ->
                resource("v$index", "Video $index", "video").copy(rating = rating)
            },
        ).toModel(pageNumber = 1, pageSize = 20)

        assertEquals(cases.map { it.second }, model.items.map { it.rating })
    }

    @Test
    fun `search ranking normalizes web grid values`() {
        val rankings = listOf(
            JsonPrimitive("3"),
            JsonPrimitive(2.6),
            JsonPrimitive(0),
            JsonPrimitive(101),
            JsonPrimitive("not-a-rank"),
            null,
            JsonNull,
            JsonPrimitive(true),
            JsonPrimitive(""),
            JsonPrimitive("NaN"),
            JsonPrimitive("Infinity"),
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
            listOf(3, 3, null, null, null, null, null, null, null, null, null),
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
    fun `search total takes precedence and stops at the final full page`() {
        val page = IndexerResourcePageData(
            total = 100,
            totalPages = 10,
            items = (1..20).map { resource("v$it", "Video $it", "video") },
        )

        assertTrue(page.toModel(pageNumber = 4, pageSize = 20).hasNext)
        assertFalse(page.toModel(pageNumber = 5, pageSize = 20).hasNext)
        assertFalse(page.copy(total = 0).toModel(pageNumber = 1, pageSize = 20).hasNext)
    }

    @Test
    fun `search uses total pages when the total is absent`() {
        val page = IndexerResourcePageData(totalPages = 5)

        assertTrue(page.toModel(pageNumber = 4, pageSize = 20).hasNext)
        assertFalse(page.toModel(pageNumber = 5, pageSize = 20).hasNext)
    }

    @Test
    fun `simple search pagination uses raw result count before filtering`() {
        val page = IndexerResourcePageData(
            items = (1..20).map {
                resource("v$it", "Video $it", if (it == 20) "audio" else "video")
            },
        )

        val model = page.toModel(pageNumber = 2, pageSize = 20)

        assertEquals(19, model.items.size)
        assertTrue(model.hasNext)
    }

    @Test
    fun `zero total pages uses simple pagination like the web client`() {
        val page = IndexerResourcePageData(
            totalPages = 0,
            items = (1..20).map { resource("v$it", "Video $it", "video") },
        )

        assertTrue(page.toModel(pageNumber = 1, pageSize = 20).hasNext)
    }

    @Test
    fun `large configured page size cannot overflow total comparison`() {
        val page = IndexerResourcePageData(total = Int.MAX_VALUE)

        assertFalse(page.toModel(pageNumber = 2, pageSize = Int.MAX_VALUE).hasNext)
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
        assertNull(source.selectedChapterIndex)
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
        assertEquals(0, source.selectedChapterIndex)
    }

    @Test
    fun `resolved video identifies its chapter after invalid entries are filtered`() {
        val playbackUrl = "https://cdn.example/ep-2.m3u8"
        for (useDefinition in listOf(false, true)) {
            val source = IndexerResourceData(
                id = " ep-2 ",
                title = "Episode 2",
                mediaType = "video",
                url = if (useDefinition) null else playbackUrl,
                definitions = if (useDefinition) {
                    listOf(IndexerDefinitionData(playbackUrl, JsonPrimitive("1080P")))
                } else {
                    emptyList()
                },
                chapters = listOf(
                    IndexerChapterData(null, null, "Unavailable", null),
                    IndexerChapterData("ep-1", null, "Episode 1", null),
                    IndexerChapterData(" ep-2 ", null, "Episode 2", null),
                    IndexerChapterData("ep-3", null, "Episode 3", null),
                ),
            ).toPlaybackSource(
                indexerId = 11,
                fallbackTitle = "Series",
                preferredDefinition = TranscodeResolution.P1080,
            )

            checkNotNull(source)
            assertEquals(playbackUrl, source.url)
            assertEquals(listOf("ep-1", "ep-2", "ep-3"), source.chapters.map { it.id })
            assertEquals("useDefinition=$useDefinition", 1, source.selectedChapterIndex)
        }
    }

    @Test
    fun `missing preferred quality falls back to first valid definition`() {
        val source = IndexerResourceData(
            id = "v1",
            title = "视频",
            mediaType = "video",
            url = "https://cdn.example/master.m3u8",
            videoType = "hls",
            definitions = listOf(
                IndexerDefinitionData(url = " ", definition = JsonPrimitive("1080P")),
                IndexerDefinitionData(
                    url = "https://cdn.example/720.m3u8",
                    definition = JsonPrimitive("720P"),
                ),
                IndexerDefinitionData(
                    url = "https://cdn.example/480.m3u8",
                    definition = JsonPrimitive("480P"),
                ),
            ),
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "备用",
            preferredDefinition = TranscodeResolution.Original,
        )

        checkNotNull(source)
        assertEquals(listOf("720P", "480P"), source.definitions.map { it.label })
        assertEquals(0, source.selectedDefinitionIndex)
        assertEquals("https://cdn.example/720.m3u8", source.url)
    }

    @Test
    fun `invalid definition label fails even when definition URL is missing`() {
        for (label in listOf(JsonObject(emptyMap()), JsonArray(emptyList()))) {
            val resource = resource("v1", "Video", "video").copy(
                url = "https://cdn.example/video.mp4",
                definitions = listOf(IndexerDefinitionData(definition = label)),
            )

            assertThrows(SerializationException::class.java) {
                resource.toPlaybackSource(
                    indexerId = 11,
                    fallbackTitle = "Fallback",
                    preferredDefinition = TranscodeResolution.P1080,
                )
            }
        }
    }

    @Test
    fun `definition labels keep primitive values and skip empty values`() {
        val labels = listOf(
            null,
            JsonNull,
            JsonPrimitive("  "),
            JsonPrimitive(" 1080P "),
            JsonPrimitive(720),
            JsonPrimitive(true),
        )
        val source = resource("v1", "Video", "video").copy(
            definitions = labels.mapIndexed { index, label ->
                IndexerDefinitionData(
                    url = "https://cdn.example/quality-$index.m3u8",
                    definition = label,
                )
            },
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "Fallback",
            preferredDefinition = TranscodeResolution.P1080,
        )

        checkNotNull(source)
        assertEquals(listOf("1080P", "720", "true"), source.definitions.map { it.label })
        assertEquals(
            listOf(
                "https://cdn.example/quality-3.m3u8",
                "https://cdn.example/quality-4.m3u8",
                "https://cdn.example/quality-5.m3u8",
            ),
            source.definitions.map { it.url },
        )
        assertEquals(0, source.selectedDefinitionIndex)
        assertEquals("https://cdn.example/quality-3.m3u8", source.url)
    }

    @Test
    fun `missing preferred DASH quality still applies HEVC preference`() {
        val source = IndexerResourceData(
            id = "v1",
            title = "视频",
            mediaType = "video",
            videoType = "dash",
            definitions = listOf(
                IndexerDefinitionData(
                    url = "https://cdn.example/480-avc.mpd",
                    definition = JsonPrimitive("480P AVC"),
                ),
                IndexerDefinitionData(
                    url = "https://cdn.example/480-hevc.mpd",
                    definition = JsonPrimitive("480P HEVC"),
                ),
            ),
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "备用",
            preferredDefinition = TranscodeResolution.P1080,
            preferHevcForDash = true,
        )

        checkNotNull(source)
        assertEquals(1, source.selectedDefinitionIndex)
        assertEquals("https://cdn.example/480-hevc.mpd", source.url)
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
    fun `directory fallback starts at first direct chapter even when another id matches`() {
        val source = IndexerResourceData(
            id = "ep-2",
            title = "Series",
            mediaType = "video",
            url = " ",
            chapters = listOf(
                IndexerChapterData(null, "https://cdn.example/ep-1.m3u8", "Episode 1", null),
                IndexerChapterData("ep-2", "https://cdn.example/ep-2.m3u8", "Episode 2", null),
            ),
        ).toPlaybackSource(
            indexerId = 11,
            fallbackTitle = "Series",
            preferredDefinition = TranscodeResolution.P1080,
        )

        checkNotNull(source)
        assertEquals("https://cdn.example/ep-1.m3u8", source.url)
        assertEquals(0, source.selectedChapterIndex)
    }

    @Test
    fun `chapters retain source order duplicates and volume title fallback`() {
        val chapters = IndexerResourceData(
            chapters = listOf(
                IndexerChapterData(" ep-2 ", null, " Chapter 2 ", " Volume A "),
                IndexerChapterData(null, " /chapter-1.mp4 ", " ", " Volume B "),
                IndexerChapterData("ep-2", null, "Duplicate", null),
                IndexerChapterData(null, " ", "Missing source", null),
                IndexerChapterData("ep-3", null, " ", " "),
            ),
        ).toChapters()

        assertEquals(
            listOf(
                NetworkChapter("ep-2", null, "Chapter 2", "Volume A"),
                NetworkChapter(null, "/chapter-1.mp4", "Volume B", "Volume B"),
                NetworkChapter("ep-2", null, "Duplicate", null),
            ),
            chapters,
        )
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
