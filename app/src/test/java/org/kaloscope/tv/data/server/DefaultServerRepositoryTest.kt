package org.kaloscope.tv.data.server

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.storage.ServerStore

class DefaultServerRepositoryTest {
    private lateinit var sourceServer: MockWebServer
    private lateinit var targetServer: MockWebServer
    private lateinit var repository: DefaultServerRepository

    @Before
    fun setUp() {
        sourceServer = MockWebServer()
        targetServer = MockWebServer()
        sourceServer.start()
        targetServer.start()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        repository = DefaultServerRepository(
            apiClientFactory = ApiClientFactory(json),
            serverStore = UnusedServerStore(),
            json = json,
        )
    }

    @After
    fun tearDown() {
        sourceServer.shutdown()
        targetServer.shutdown()
    }

    @Test
    fun `connection result includes the verified origin and version`() = runTest {
        sourceServer.enqueue(versionResponse("1.2.3"))
        val origin = sourceServer.url("/").toString().removeSuffix("/")

        val result = repository.testConnection(origin)

        assertEquals(
            AppResult.Success(
                ServerConnectionInfo(
                    origin = origin,
                    version = "1.2.3",
                ),
            ),
            result,
        )
    }

    @Test
    fun `connection test rejects a redirect to another origin`() = runTest {
        sourceServer.enqueue(
            MockResponse()
                .setResponseCode(308)
                .addHeader("Location", targetServer.url("/_api/system/version")),
        )
        targetServer.enqueue(versionResponse("1.2.3"))
        val origin = sourceServer.url("/").toString().removeSuffix("/")

        val result = repository.testConnection(origin)

        assertEquals(
            AppResult.Failure(AppError.InvalidData("server_redirect")),
            result,
        )
    }

    private fun versionResponse(version: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(
            """{"request_id":"test","status":200,"message":"","data":{"version":"$version"}}""",
        )
}

private class UnusedServerStore : ServerStore {
    override suspend fun getServers(): List<SavedServer> = error("Not used")

    override suspend fun save(server: SavedServer) = error("Not used")

    override suspend fun delete(serverId: String): List<SavedServer> = error("Not used")

    override suspend fun getActiveServerId(): String? = error("Not used")

    override suspend fun setActiveServerId(serverId: String) = error("Not used")
}
