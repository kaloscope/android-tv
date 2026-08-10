package org.kaloscope.tv.data.search

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.NetworkChapter
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.NetworkMediaType
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.ResolvedNetworkResource
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.player.NetworkVideoCodecSupport
import org.kaloscope.tv.core.player.TranscodeResolution

class DefaultNetworkResourceRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultNetworkResourceRepository
    private lateinit var json: Json

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        repository = DefaultNetworkResourceRepository(ApiClientFactory(json), json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `details type overrides catalog hint and resolves text array`() = runTest {
        server.enqueue(
            response(
                """
                {"status":200,"message":"","data":{
                  "id":"book-1","title":"Chapter One","media_type":"text",
                  "text":["First paragraph","Second paragraph"],
                  "chapters":[{"id":"c1","title":"Chapter One","volume":"Volume A"}]
                }}
                """.trimIndent(),
            ),
        )

        val resolved = repository.resolveResource(
            session = session(),
            indexerId = 11,
            result = result("book-1", NetworkMediaType.Image),
            preferredDefinition = TranscodeResolution.P1080,
        )

        val text = (resolved as AppResult.Success).value as ResolvedNetworkResource.Text
        assertEquals("First paragraph\n\nSecond paragraph", text.content.text)
        assertEquals("c1", text.content.chapters.single().id)
    }

    @Test
    fun `video details use catalog video type when response omits it`() = runTest {
        server.enqueue(
            response(
                """
                {"status":200,"message":"","data":{
                  "id":"video-1","title":"Video","media_type":"video",
                  "url":"<MPD><Period /></MPD>"
                }}
                """.trimIndent(),
            ),
        )

        val resolved = repository.resolveResource(
            session = session(),
            indexerId = 11,
            result = result(
                id = "video-1",
                type = NetworkMediaType.Video,
                videoTypeHint = NetworkVideoType.Dash,
            ),
            preferredDefinition = TranscodeResolution.P1080,
        )

        val video = (resolved as AppResult.Success).value as ResolvedNetworkResource.Video
        assertEquals(NetworkVideoType.Dash, video.source.videoType)
    }

    @Test
    fun `software AVC capability selects matching HEVC DASH definition`() = runTest {
        server.enqueue(
            response(
                """
                {
                  "status": 200,
                  "message": "",
                  "data": {
                    "id": "video-1",
                    "title": "Video",
                    "media_type": "video",
                    "video_type": "dash",
                    "definitions": [
                      {"url": "https://media.example/avc.mpd", "definition": "480P AVC"},
                      {"url": "https://media.example/hevc.mpd", "definition": "480P HEVC"}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val compatibleRepository = DefaultNetworkResourceRepository(
            apiClientFactory = ApiClientFactory(json),
            json = json,
            videoCodecSupport = NetworkVideoCodecSupport { true },
        )

        val resolved = compatibleRepository.resolveResource(
            session = session(),
            indexerId = 11,
            result = result(
                id = "video-1",
                type = NetworkMediaType.Video,
                videoTypeHint = NetworkVideoType.Dash,
            ),
            preferredDefinition = TranscodeResolution.P480,
        )

        val source = ((resolved as AppResult.Success).value as ResolvedNetworkResource.Video).source
        assertEquals("https://media.example/hevc.mpd", source.url)
        assertEquals(1, source.selectedDefinitionIndex)
    }

    @Test
    fun `details re-resolves first id-only chapter`() = runTest {
        server.enqueue(
            response(
                """{"status":200,"message":"","data":{""" +
                    """"id":"series-1","title":"Series","media_type":"video",""" +
                    """"video_type":"dash","chapters":[{"id":"episode-1",""" +
                    """"title":"Episode 1"}]}}""",
            ),
        )
        server.enqueue(
            response(
                """{"status":200,"message":"","data":{""" +
                    """"id":"series-1","title":"Episode 1","media_type":"video",""" +
                    """"video_type":"dash","url":"https://cdn.example/episode-1.mpd"}}""",
            ),
        )

        val resolved = repository.resolveResource(
            session = session(),
            indexerId = 11,
            result = result(
                id = "series-1",
                type = NetworkMediaType.Video,
                videoTypeHint = NetworkVideoType.Dash,
            ),
            preferredDefinition = TranscodeResolution.P1080,
        )

        val source = ((resolved as AppResult.Success).value as ResolvedNetworkResource.Video).source
        assertEquals("https://cdn.example/episode-1.mpd", source.url)
        assertEquals(0, source.selectedChapterIndex)
        server.takeRequest()
        val chapterRequest = server.takeRequest()
        assertTrue(chapterRequest.body.readUtf8().contains(""""chapter_id":"episode-1""""))
    }

    @Test
    fun `direct chapter does not retain previous episode definitions or danmakus`() = runTest {
        val current = NetworkPlaybackSource(
            indexerId = 11,
            resourceId = "series-1",
            title = "Episode 1",
            url = "https://cdn.example/episode-1.m3u8",
            videoType = NetworkVideoType.Hls,
            danmakus = listOf(
                DanmakuComment(
                    id = "old",
                    text = "Old episode",
                    mode = "scroll",
                    color = null,
                    startMillis = 1_000,
                ),
            ),
            definitions = listOf(
                NetworkDefinition(
                    "1080P",
                    "https://cdn.example/episode-1-1080.m3u8",
                ),
            ),
            chapters = listOf(
                NetworkChapter("ep-1", null, "Episode 1", null),
                NetworkChapter(
                    null,
                    "https://cdn.example/episode-2.m3u8",
                    "Episode 2",
                    null,
                ),
            ),
            selectedDefinitionIndex = 0,
            selectedChapterIndex = 0,
        )

        val result = repository.resolveVideoChapter(
            session = session(),
            source = current,
            chapterIndex = 1,
            preferredDefinition = TranscodeResolution.P1080,
        )

        val next = (result as AppResult.Success).value
        assertTrue(next.definitions.isEmpty())
        assertTrue(next.danmakus.isEmpty())
        assertEquals(1, next.selectedChapterIndex)
    }

    @Test
    fun `image resolution falls back to first valid chapter once`() = runTest {
        server.enqueue(
            response(
                """
                {"status":200,"message":"","data":{
                  "id":"comic-1","title":"Comic","media_type":"image",
                  "chapters":[
                    {"id":null,"title":"Broken"},
                    {"id":"c1","title":"Chapter One","volume":"Volume A"},
                    {"id":"c1","title":"Duplicate","volume":"Volume A"}
                  ]
                }}
                """.trimIndent(),
            ),
        )
        server.enqueue(
            response(
                """
                {"status":200,"message":"","data":{
                  "id":"comic-1","title":"Chapter One","media_type":"image",
                  "images":[" one.jpg ","one.jpg","two.jpg"],"image_count":5
                }}
                """.trimIndent(),
            ),
        )

        val resolved = repository.resolveResource(
            session = session(),
            indexerId = 11,
            result = result("comic-1", NetworkMediaType.Image),
            preferredDefinition = TranscodeResolution.P1080,
        )

        val image = (resolved as AppResult.Success).value as ResolvedNetworkResource.Image
        assertEquals(listOf("one.jpg", "two.jpg"), image.content.images)
        assertEquals(5, image.content.imageCount)
        assertEquals(listOf("c1"), image.content.chapters.map { it.id })
        assertEquals(0, image.content.selectedChapterIndex)
        assertEquals(2, server.requestCount)
        server.takeRequest()
        assertTrue(server.takeRequest().body.readUtf8().contains("\"chapter_id\":\"c1\""))
    }

    @Test
    fun `reader chapter request preserves source chapters and selects requested chapter`() = runTest {
        server.enqueue(
            response(
                """
                {"status":200,"message":"","data":{
                  "id":"book-1","title":"Chapter Two","media_type":"text","text":"Body"
                }}
                """.trimIndent(),
            ),
        )
        val current = ReaderTextContent.network(
            indexerId = 11,
            resourceId = "book-1",
            title = "Chapter One",
            text = "Old",
            chapters = listOf(
                ReaderChapter("c1", "Chapter One"),
                ReaderChapter("c2", "Chapter Two"),
            ),
            selectedChapterIndex = 0,
        )

        val result = repository.resolveReaderChapter(session(), current, 1)

        val next = (result as AppResult.Success).value as ReaderTextContent
        assertEquals("Body", next.text)
        assertEquals(current.chapters, next.chapters)
        assertEquals(1, next.selectedChapterIndex)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"chapter_id\":\"c2\""))
    }

    @Test
    fun `image page starts after loaded count deduplicates and detects exhaustion`() = runTest {
        server.enqueue(
            response(
                """
                {"status":200,"message":"","data":{
                  "id":"comic-1","media_type":"image",
                  "images":["two.jpg","three.jpg","three.jpg"],"image_count":3
                }}
                """.trimIndent(),
            ),
        )
        val current = ReaderImageContent.network(
            indexerId = 11,
            resourceId = "comic-1",
            chapterId = "c1",
            title = "Chapter One",
            images = listOf("one.jpg", "two.jpg"),
            imageCount = 5,
            chapters = listOf(ReaderChapter("c1", "Chapter One")),
            selectedChapterIndex = 0,
        )

        val result = repository.loadImagePage(session(), current)

        val page = (result as AppResult.Success).value
        assertEquals(listOf("three.jpg"), page.images)
        assertEquals(3, page.imageCount)
        assertTrue(page.exhausted)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"chapter_id\":\"c1\""))
        assertTrue(body.contains("\"page\":3"))
    }

    private fun result(
        id: String,
        type: NetworkMediaType,
        videoTypeHint: NetworkVideoType = NetworkVideoType.Unknown,
    ) = NetworkSearchResult(
        id = id,
        title = "Result",
        coverPath = null,
        rating = null,
        category = null,
        uploader = null,
        uploadedAt = null,
        mediaType = type,
        videoTypeHint = videoTypeHint,
    )

    private fun session() = Session(
        server = SavedServer(
            id = "server-id",
            name = "Home",
            origin = server.url("/").toString().removeSuffix("/"),
        ),
        token = "fixture-token",
        user = SessionUser(1, "tv", "user"),
    )

    private fun response(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
