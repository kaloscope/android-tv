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
        val playback = repository.resolvePlayback(session(), 11, result)

        val source = (playback as AppResult.Success).value
        assertEquals("video-fixture-001", source.resourceId)
        assertEquals(NetworkVideoType.Hls, source.videoType)
        assertEquals(12_500, source.danmakus.single().startMillis)
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
