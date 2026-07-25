package org.kaloscope.tv.core.network

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.data.search.remote.IndexerDetailsRequestData

class KaloscopeApiContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: KaloscopeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiClientFactory(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        ).create(server.url("/").toString().removeSuffix("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `version uses public system endpoint`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"request_id":"test","status":200,"message":"","data":{"version":"1.2.3"}}""",
            ),
        )

        val response = api.getVersion()

        assertEquals("1.2.3", response.data.version)
        assertEquals("/_api/system/version", server.takeRequest().path)
    }

    @Test
    fun `login sends form fields and parses fixture`() = runTest {
        server.enqueue(jsonResponse(fixture("auth-login-success.json")))

        val response = api.login("tv user", "secret+value")
        val request = server.takeRequest()

        assertEquals("/_api/auth/login", request.path)
        assertEquals("POST", request.method)
        assertTrue(
            request.getHeader("Content-Type")
                .orEmpty()
                .startsWith("application/x-www-form-urlencoded"),
        )
        assertEquals("username=tv+user&password=secret%2Bvalue", request.body.readUtf8())
        assertEquals("fixture-token-not-for-production", response.data.token)
        assertEquals("tv_user", response.data.user.username)
    }

    @Test
    fun `current user sends token header`() = runTest {
        server.enqueue(jsonResponse(fixture("auth-current-success.json")))

        val response = api.getCurrentUser("Token fixture-token")
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/auth/current", request.path)
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals(1L, response.data.id)
    }

    @Test
    fun `history requests recent videos and parses media`() = runTest {
        server.enqueue(jsonResponse(fixture("history-video-list-success.json")))

        val response = api.getVideoHistory("Token fixture-token")
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals(
            "/_api/user/history/list?rel_type=video&page_num=1&page_size=20&ordering=-updated_at",
            request.path,
        )
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals(301L, response.data.items.single().media?.id)
        assertEquals(63, response.data.items.single().percentage)
        assertEquals("8.5", response.data.items.single().media?.rating)
    }

    @Test
    fun `library list uses authenticated unpaged endpoint`() = runTest {
        server.enqueue(jsonResponse(fixture("media-library-list-success.json")))

        val response = api.getMediaLibraries("Token fixture-token")
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/media/lib/list", request.path)
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals(21L, response.data.single().id)
        assertEquals("tv_show", response.data.single().libraryType)
    }

    @Test
    fun `media page sends library pagination and omits empty keyword`() = runTest {
        server.enqueue(jsonResponse(fixture("media-list-success.json")))

        val response = api.getMediaPage(
            authorization = "Token fixture-token",
            libraryId = 21,
        )
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals(
            "/_api/media/list?page_num=1&page_size=20&lib_id=21",
            request.path,
        )
        assertEquals(201L, response.data.items.single().id)
    }

    @Test
    fun `media page sends confirmed keyword and requested page`() = runTest {
        server.enqueue(jsonResponse(fixture("media-list-success.json")))

        api.getMediaPage(
            authorization = "Token fixture-token",
            pageNumber = 2,
            libraryId = 21,
            keyword = "群 星",
        )
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals(
            "/_api/media/list?page_num=2&page_size=20&lib_id=21&keyword=%E7%BE%A4%20%E6%98%9F",
            request.path,
        )
    }

    @Test
    fun `media detail uses stable media id route`() = runTest {
        server.enqueue(jsonResponse(fixture("media-detail-success.json")))

        val response = api.getMediaDetail(
            authorization = "Token fixture-token",
            mediaId = 201,
        )
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/media/201", request.path)
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals("群星档案", response.data.title)
        assertEquals(301L, response.data.children.single().id)
    }

    @Test
    fun `indexer list requests all searchable published sources`() = runTest {
        server.enqueue(jsonResponse(fixture("indexer-list-success.json")))

        val response = api.getIndexers("Token fixture-token")
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals(
            "/_api/flow/graph/list?page_num=0&ordering=name&category=indexer" +
                "&states=modified&states=published",
            request.path,
        )
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals(11L, response.data.items.single().id)
    }

    @Test
    fun `indexer configuration and nullable auth use their dedicated endpoints`() = runTest {
        server.enqueue(jsonResponse(fixture("indexer-config-success.json")))
        server.enqueue(
            jsonResponse(
                """{"request_id":"auth","status":200,"message":"","data":null}""",
            ),
        )

        val config = api.getIndexerConfig("Token fixture-token", 11)
        val auth = api.getIndexerAuth("Token fixture-token", 11)

        assertTrue(config.data.search?.keyword?.required == true)
        assertEquals(null, auth.data)
        assertEquals("/_api/flow/indexer/11/config", server.takeRequest().path)
        assertEquals("/_api/flow/indexer/11/auth", server.takeRequest().path)
    }

    @Test
    fun `indexer search sends confirmed query and parses video resources`() = runTest {
        server.enqueue(jsonResponse(fixture("indexer-search-success.json")))

        val response = api.executeIndexerSearch(
            authorization = "Token fixture-token",
            indexerId = 11,
            body = buildJsonObject {
                put("\$start", "search_start")
                put("page_num", 1)
                put("page_size", 20)
                put("keyword", "星际 回声")
                put("mobile", false)
                put("region", "cn")
                put(
                    "genre",
                    JsonArray(listOf(JsonPrimitive("sci-fi"), JsonPrimitive("drama"))),
                )
            },
        )
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/flow/graph/11/execute", request.path)
        assertEquals(
            """{"${'$'}start":"search_start","page_num":1,"page_size":20,""" +
                """"keyword":"星际 回声","mobile":false,"region":"cn",""" +
                """"genre":["sci-fi","drama"]}""",
            request.body.readUtf8(),
        )
        assertEquals("48716677", response.data.items.single().id)
    }

    @Test
    fun `indexer details sends TV capability data and parses playback source`() = runTest {
        server.enqueue(jsonResponse(fixture("indexer-details-video-success.json")))

        val response = api.executeIndexerDetails(
            authorization = "Token fixture-token",
            indexerId = 11,
            body = IndexerDetailsRequestData(
                resourceId = "video-fixture-001",
            ),
        )
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/flow/graph/11/execute", request.path)
        assertEquals(
            """{"${'$'}start":"details_start","id":"video-fixture-001","chapter_id":null,""" +
                """"dash_supported":true,"ua":{"device":{"type":"smarttv"},""" +
                """"os":{"name":"Android"},"navigator":{"platform":"Android","maxTouchPoints":0}}}""",
            request.body.readUtf8(),
        )
        assertEquals("hls", response.data?.videoType)
        assertEquals(2, response.data?.definitions?.size)
        assertEquals("48716677", response.data?.id)
        assertEquals("1001", response.data?.chapters?.first()?.id)
        assertEquals("episode-2", response.data?.chapters?.last()?.id)
        assertEquals("287683505", response.data?.danmakus?.single()?.id)
    }

    @Test
    fun `subtitle tracks send media path and parse vtt source`() = runTest {
        server.enqueue(jsonResponse(fixture("subtitle-tracks-success.json")))

        val response = api.getSubtitleTracks(
            authorization = "Token fixture-token",
            body = MediaResourceData("/media/video.mkv"),
        )
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/subtitle/tracks", request.path)
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals("""{"path":"/media/video.mkv"}""", request.body.readUtf8())
        assertEquals("zh-CN", response.data.first().language)
    }

    @Test
    fun `danmaku match parses millisecond start`() = runTest {
        server.enqueue(jsonResponse(fixture("danmaku-match-success.json")))

        val response = api.getDanmakus(
            authorization = "Token fixture-token",
            body = MediaResourceData("/media/video.mkv"),
        )

        assertEquals(12_500L, response.data.comments.first().start)
    }

    @Test
    fun `history progress sends video relation and accepts empty response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        api.recordVideoProgress(
            authorization = "Token fixture-token",
            body = HistoryRecordData(
                relationType = "video",
                relationId = 301,
                position = 42,
                percentage = 35,
            ),
        )
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/user/history/record", request.path)
        assertEquals(
            """{"rel_type":"video","rel_id":301,"position":42,"percentage":35}""",
            request.body.readUtf8(),
        )
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fixtures/api/$name")).readText()

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
