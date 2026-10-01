package org.kaloscope.tv.data.search

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkMediaType
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.ResolvedNetworkResource
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.player.TranscodeResolution

class DefaultSearchRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultSearchRepository
    private lateinit var resourceRepository: DefaultNetworkResourceRepository
    private lateinit var json: Json

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val apiClientFactory = ApiClientFactory(json)
        resourceRepository = DefaultNetworkResourceRepository(apiClientFactory, json)
        repository = DefaultSearchRepository(
            apiClientFactory = apiClientFactory,
            json = json,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `catalog keeps no-auth and authenticated preview sites and hides missing auth`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = false),
                12L to CatalogSite(loginRequired = true, authName = "member"),
                13L to CatalogSite(loginRequired = true),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        val profiles = (result as AppResult.Success).value
        assertEquals(listOf(11L, 12L), profiles.map { it.indexer.id })
        assertEquals("region", profiles.first().filters.single().key)
    }

    @Test
    fun `catalog preserves available sites when another config fails`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = false),
                12L to CatalogSite(loginRequired = false, configFailureCode = 500),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertEquals(
            listOf(11L),
            (result as AppResult.Success).value.map { it.indexer.id },
        )
    }

    @Test
    fun `catalog reports session expiry even when another config succeeds`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = false),
                12L to CatalogSite(loginRequired = false, configFailureCode = 401),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertEquals(AppResult.Failure(AppError.Unauthorized), result)
    }

    @Test
    fun `catalog reports session expiry during indexer auth lookup`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = false),
                12L to CatalogSite(loginRequired = true, authFailureCode = 401),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertEquals(AppResult.Failure(AppError.Unauthorized), result)
    }

    @Test
    fun `catalog session expiry takes precedence over earlier ordinary failures`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                13L to CatalogSite(loginRequired = true),
                12L to CatalogSite(loginRequired = false, configFailureCode = 403),
                11L to CatalogSite(loginRequired = false, configFailureCode = 401),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertEquals(AppResult.Failure(AppError.Unauthorized), result)
    }

    @Test
    fun `catalog preserves available sites when another config is forbidden`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = false),
                12L to CatalogSite(loginRequired = false, configFailureCode = 403),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertEquals(
            listOf(11L),
            (result as AppResult.Success).value.map { it.indexer.id },
        )
    }

    @Test
    fun `catalog keeps source order across hidden and failed profiles`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                14L to CatalogSite(loginRequired = false),
                13L to CatalogSite(loginRequired = true),
                12L to CatalogSite(loginRequired = false, configFailureCode = 500),
                11L to CatalogSite(loginRequired = false),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertEquals(
            listOf(14L, 11L),
            (result as AppResult.Success).value.map { it.indexer.id },
        )
        assertEquals(6, server.requestCount)
    }

    @Test
    fun `catalog returns first profile error in source order`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                13L to CatalogSite(loginRequired = true),
                12L to CatalogSite(loginRequired = false, configFailureCode = 403),
                11L to CatalogSite(loginRequired = false, configFailureCode = 500),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertEquals(AppResult.Failure(AppError.Forbidden), result)
    }

    @Test
    fun `catalog carries details media and video hints`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(
                    loginRequired = false,
                    mediaType = "image",
                    videoType = "hls",
                ),
            ),
        )

        val profile = (repository.getAvailableProfiles(session()) as AppResult.Success)
            .value
            .single()

        assertEquals(NetworkMediaType.Image, profile.mediaTypeHint)
        assertEquals(NetworkVideoType.Hls, profile.videoTypeHint)
    }

    @Test
    fun `catalog fails when no site is available and one profile request fails`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = true),
                12L to CatalogSite(loginRequired = false, configFailureCode = 500),
            ),
        )

        assertTrue(repository.getAvailableProfiles(session()) is AppResult.Failure)
    }

    @Test
    fun `catalog returns empty when all candidates are unauthenticated`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = true),
                12L to CatalogSite(loginRequired = true),
            ),
        )

        val result = repository.getAvailableProfiles(session())

        assertTrue((result as AppResult.Success).value.isEmpty())
    }

    @Test
    fun `cover ratio defaults to web grid landscape`() {
        val expected = 16f / 9f

        assertEquals(expected, null.toCoverAspectRatio(), 0f)
        assertEquals(expected, "auto".toCoverAspectRatio(), 0f)
    }

    @Test
    fun `catalog keeps declared page sizes and defaults missing or invalid values`() = runTest {
        server.dispatcher = catalogDispatcher(
            mapOf(
                11L to CatalogSite(loginRequired = false, pageSize = 101),
                12L to CatalogSite(loginRequired = false, pageSize = 200),
                13L to CatalogSite(loginRequired = false, pageSize = Int.MAX_VALUE),
                14L to CatalogSite(loginRequired = false, pageSize = 0),
                15L to CatalogSite(loginRequired = false, pageSize = -1),
                16L to CatalogSite(loginRequired = false, pageSize = null),
            ),
        )

        val profiles = (repository.getAvailableProfiles(session()) as AppResult.Success).value

        assertEquals(listOf(101, 200, Int.MAX_VALUE, 20, 20, 20), profiles.map { it.pageSize })
    }

    @Test
    fun `one shot ranking search stops after one hundred results`() = runTest {
        val catalog = catalogDispatcher(
            mapOf(11L to CatalogSite(loginRequired = false, pageSize = 101)),
        )
        val items = (1..100).joinToString(",") { id ->
            """{"id":"v$id","title":"Video $id","media_type":"video"}"""
        }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.requestUrl?.encodedPath == "/_api/flow/graph/11/execute") {
                    jsonResponse("""{"status":200,"message":"","data":{"items":[$items]}}""")
                } else {
                    catalog.dispatch(request)
                }
        }
        val profile = (repository.getAvailableProfiles(session()) as AppResult.Success)
            .value.single()

        val page = (repository.search(session(), profile, "", emptyMap(), 1) as AppResult.Success)
            .value

        assertEquals(100, page.items.size)
        assertEquals(101, page.pageSize)
        assertFalse(page.hasNext)
        repeat(2) { server.takeRequest() }
        val request = json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals(JsonPrimitive(101), request["page_size"])
        assertEquals(JsonPrimitive(1), request["page_num"])
    }

    @Test
    fun `search decodes web grid metadata`() = runTest {
        server.enqueue(jsonResponse(fixture("indexer-search-success.json")))
        val profile = org.kaloscope.tv.core.model.IndexerSourceProfile(
            indexer = indexer(),
            pageSize = 20,
            keywordRequired = true,
        )

        val page = repository.search(session(), profile, "星际", emptyMap(), 1)
        val result = (page as AppResult.Success).value.items.single()

        assertEquals(4, result.ranking)
        assertEquals("1:30:00", result.misc)
        assertEquals("1GB", result.size)
    }

    @Test
    fun `malformed numeric search metadata returns invalid data and allows retry`() = runTest {
        val profile = org.kaloscope.tv.core.model.IndexerSourceProfile(
            indexer = indexer(),
            pageSize = 20,
            keywordRequired = true,
        )
        for (field in listOf("rating", "ranking")) {
            for (value in listOf("{}", "[]")) {
                server.enqueue(
                    jsonResponse(
                        """
                        {"status":200,"message":"","data":{
                          "totalPages":1,"items":[{
                            "id":"video-1","title":"Video","media_type":"video","$field":$value
                          }]
                        }}
                        """.trimIndent(),
                    ),
                )
                server.enqueue(jsonResponse(fixture("indexer-search-success.json")))
                val requestsBefore = server.requestCount

                val failed = repository.search(session(), profile, "Video", emptyMap(), 1)

                assertTrue("$field=$value", (failed as AppResult.Failure).error is AppError.InvalidData)
                assertEquals(requestsBefore + 1, server.requestCount)

                val retried = repository.search(session(), profile, "Video", emptyMap(), 1)

                val item = (retried as AppResult.Success).value.items.single()
                assertEquals("48716677", item.id)
                assertEquals(4, item.ranking)
                assertEquals(requestsBefore + 2, server.requestCount)
            }
        }
    }

    @Test
    fun `search accepts numeric resource size`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {
                  "status": 200,
                  "message": "",
                  "data": {
                    "totalPages": 1,
                    "items": [
                      {
                        "id": "novel/source-id",
                        "title": "示例小说",
                        "size": 123,
                        "media_type": "text"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val profile = org.kaloscope.tv.core.model.IndexerSourceProfile(
            indexer = indexer(),
            pageSize = 20,
            keywordRequired = false,
            mediaTypeHint = NetworkMediaType.Text,
        )

        val page = repository.search(session(), profile, "", emptyMap(), 1)

        assertTrue("numeric size should decode successfully", page is AppResult.Success)
        assertEquals("123", (page as AppResult.Success).value.items.single().size)
    }

    @Test
    fun `search and details map real resources into network playback`() = runTest {
        server.enqueue(jsonResponse(fixture("indexer-search-success.json")))
        server.enqueue(jsonResponse(fixture("indexer-details-video-success.json")))
        val profile = org.kaloscope.tv.core.model.IndexerSourceProfile(
            indexer = indexer(),
            pageSize = 20,
            keywordRequired = true,
        )

        val page = repository.search(session(), profile, "星际", emptyMap(), 1)
        val result = (page as AppResult.Success).value.items.single()
        val resolved = resourceRepository.resolveResource(
            session = session(),
            indexerId = 11,
            result = result,
            preferredDefinition = TranscodeResolution.P1080,
        )

        val source = ((resolved as AppResult.Success).value as ResolvedNetworkResource.Video).source
        assertEquals("48716677", source.resourceId)
        assertEquals(NetworkVideoType.Hls, source.videoType)
        assertEquals("1001", source.chapters.first().id)
        assertEquals("episode-2", source.chapters.last().id)
        assertEquals("287683505", source.danmakus.single().id)
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

    private fun catalogDispatcher(
        sites: Map<Long, CatalogSite>,
    ): Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = checkNotNull(request.requestUrl).encodedPath
            if (path == "/_api/flow/graph/list") {
                val items = sites.keys.joinToString(",") { id ->
                    """{"id":$id,"name":"Site $id","node_types":["search_start"],""" +
                        """"only_preview":true}"""
                }
                return jsonResponse(
                    """{"status":200,"message":"","data":{"total":${sites.size},""" +
                        """"items":[$items]}}""",
                )
            }
            val indexerId = path
                .substringAfter("/_api/flow/indexer/")
                .substringBefore("/")
                .toLongOrNull()
                ?: return MockResponse().setResponseCode(404)
            val site = sites[indexerId] ?: return MockResponse().setResponseCode(404)
            return when {
                path.endsWith("/config") && site.configFailureCode != null ->
                    MockResponse().setResponseCode(site.configFailureCode)

                path.endsWith("/config") -> jsonResponse(
                    """
                    {
                      "status": 200,
                      "message": "",
                      "data": {
                        "auth": {"login": {"required": ${site.loginRequired}}},
                        "search": {
                          "display": {"page_size": ${site.pageSize}, "cover_ratio": "2/3"},
                          "keyword": {"required": true},
                          "filters": {
                            "region": {
                              "type": "select",
                              "label": "地区",
                              "options": {"cn": "中国"}
                            }
                          }
                        },
                        "details": {
                          "specific": {
                            "media_type": ${site.mediaType?.let { "\"$it\"" } ?: "null"},
                            "video_type": ${site.videoType?.let { "\"$it\"" } ?: "null"}
                          }
                        }
                      }
                    }
                    """.trimIndent(),
                )

                path.endsWith("/auth") && site.authFailureCode != null ->
                    MockResponse().setResponseCode(site.authFailureCode)

                path.endsWith("/auth") -> {
                    val data = site.authName?.let { """{"name":"$it"}""" } ?: "null"
                    jsonResponse("""{"status":200,"message":"","data":$data}""")
                }

                else -> MockResponse().setResponseCode(404)
            }
        }
    }
}

private data class CatalogSite(
    val loginRequired: Boolean,
    val pageSize: Int? = 20,
    val authName: String? = null,
    val configFailureCode: Int? = null,
    val authFailureCode: Int? = null,
    val mediaType: String? = null,
    val videoType: String? = null,
)

private fun indexer() = NetworkIndexer(11, "星海站", null)
