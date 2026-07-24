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
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.player.TranscodeResolution

class DefaultSearchRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultSearchRepository
    private lateinit var json: Json

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        repository = DefaultSearchRepository(ApiClientFactory(json), json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `profile blocks source when required web auth is absent`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"status":200,"message":"","data":{"auth":{"login":{"required":true}},""" +
                    """"search":{"display":{"page_size":30},"keyword":{"required":true}}}}""",
            ),
        )
        server.enqueue(jsonResponse("""{"status":200,"message":"","data":null}"""))

        val result = repository.getProfile(session(), indexer())

        val profile = (result as AppResult.Success).value
        assertEquals(30, profile.pageSize)
        assertTrue(profile.webAuthRequired)
        assertEquals("/_api/flow/indexer/11/config", server.takeRequest().path)
        assertEquals("/_api/flow/indexer/11/auth", server.takeRequest().path)
    }

    @Test
    fun `search and details map real resources into network playback`() = runTest {
        server.enqueue(jsonResponse(fixture("indexer-search-success.json")))
        server.enqueue(jsonResponse(fixture("indexer-details-video-success.json")))
        val profile = org.kaloscope.tv.core.model.IndexerSourceProfile(
            indexer = indexer(),
            pageSize = 20,
            keywordRequired = true,
            webAuthRequired = false,
        )

        val page = repository.search(session(), profile, "星际", 1)
        val result = (page as AppResult.Success).value.items.single()
        val playback = repository.resolvePlayback(
            session(),
            11,
            result,
            TranscodeResolution.P1080,
        )

        val source = (playback as AppResult.Success).value
        assertEquals("video-fixture-001", source.resourceId)
        assertEquals(NetworkVideoType.Hls, source.videoType)
        assertEquals(12_500, source.danmakus.single().startMillis)
    }

    @Test
    fun `details re-resolves first id-only chapter`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"status":200,"message":"","data":{""" +
                    """"id":"series-1","title":"Series","media_type":"video",""" +
                    """"video_type":"dash","chapters":[{"id":"episode-1",""" +
                    """"title":"Episode 1"}]}}""",
            ),
        )
        server.enqueue(
            jsonResponse(
                """{"status":200,"message":"","data":{""" +
                    """"id":"series-1","title":"Episode 1","media_type":"video",""" +
                    """"video_type":"dash","url":"https://cdn.example/episode-1.mpd"}}""",
            ),
        )

        val playback = repository.resolvePlayback(
            session = session(),
            indexerId = 11,
            result = NetworkSearchResult(
                id = "series-1",
                title = "Series",
                coverPath = null,
                rating = null,
                category = null,
                uploader = null,
                uploadedAt = null,
            ),
            preferredDefinition = TranscodeResolution.P1080,
        )

        val source = (playback as AppResult.Success).value
        assertEquals("https://cdn.example/episode-1.mpd", source.url)
        assertEquals(0, source.selectedChapterIndex)
        server.takeRequest()
        val chapterRequest = server.takeRequest()
        assertTrue(chapterRequest.body.readUtf8().contains(""""chapter_id":"episode-1""""))
    }

    @Test
    fun `direct chapter does not retain previous episode definitions or danmakus`() = runTest {
        val current = org.kaloscope.tv.core.model.NetworkPlaybackSource(
            indexerId = 11,
            resourceId = "series-1",
            title = "Episode 1",
            url = "https://cdn.example/episode-1.m3u8",
            videoType = NetworkVideoType.Hls,
            danmakus = listOf(
                org.kaloscope.tv.core.model.DanmakuComment(
                    id = "old",
                    text = "Old episode",
                    mode = "scroll",
                    color = null,
                    startMillis = 1_000,
                ),
            ),
            definitions = listOf(
                org.kaloscope.tv.core.model.NetworkDefinition(
                    "1080P",
                    "https://cdn.example/episode-1-1080.m3u8",
                ),
            ),
            chapters = listOf(
                org.kaloscope.tv.core.model.NetworkChapter(
                    "ep-1",
                    null,
                    "Episode 1",
                    null,
                ),
                org.kaloscope.tv.core.model.NetworkChapter(
                    null,
                    "https://cdn.example/episode-2.m3u8",
                    "Episode 2",
                    null,
                ),
            ),
            selectedDefinitionIndex = 0,
            selectedChapterIndex = 0,
        )

        val result = repository.resolveChapter(
            session(),
            current,
            1,
            TranscodeResolution.P1080,
        )

        val next = (result as AppResult.Success).value
        assertTrue(next.definitions.isEmpty())
        assertTrue(next.danmakus.isEmpty())
        assertEquals(1, next.selectedChapterIndex)
    }

    private fun session() = Session(
        server = SavedServer(
            id = "server-id",
            name = "Home",
            origin = server.url("/").toString().removeSuffix("/"),
        ),
        token = "fixture-token",
        user = SessionUser(1, "tv", "user"),
    )

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fixtures/api/$name")).readText()

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

private fun indexer() = NetworkIndexer(11, "星海站", null)
