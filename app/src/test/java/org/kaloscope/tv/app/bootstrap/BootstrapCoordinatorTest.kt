package org.kaloscope.tv.app.bootstrap

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.storage.ServerStore
import org.kaloscope.tv.data.auth.SessionRepository

class BootstrapCoordinatorTest {
    @Test
    fun `requires a server when none are saved`() = runBlocking {
        val data = FakeBootstrapData()

        val state = BootstrapCoordinator(data, data).resolve()

        assertEquals(BootstrapState.NeedsServer(emptyList()), state)
    }

    @Test
    fun `requires login when active server has no token`() = runBlocking {
        val server = savedServer()
        val data = FakeBootstrapData(servers = listOf(server))

        val state = BootstrapCoordinator(data, data).resolve()

        assertEquals(BootstrapState.NeedsLogin(server), state)
    }

    @Test
    fun `enters ready state after validating a saved session`() = runBlocking {
        val server = savedServer()
        val session = session(server)
        val data = FakeBootstrapData(
            servers = listOf(server),
            token = "saved-token",
            validation = AppResult.Success(session),
        )

        val state = BootstrapCoordinator(data, data).resolve()

        assertEquals(BootstrapState.Ready(session), state)
        assertFalse(data.tokenCleared)
    }

    @Test
    fun `clears only an unauthorized token and returns to login`() = runBlocking {
        val server = savedServer()
        val data = FakeBootstrapData(
            servers = listOf(server),
            token = "expired-token",
            validation = AppResult.Failure(AppError.Unauthorized),
        )

        val state = BootstrapCoordinator(data, data).resolve()

        assertEquals(BootstrapState.NeedsLogin(server), state)
        assertTrue(data.tokenCleared)
    }

    @Test
    fun `keeps token when validation fails because server is offline`() = runBlocking {
        val server = savedServer()
        val data = FakeBootstrapData(
            servers = listOf(server),
            token = "saved-token",
            validation = AppResult.Failure(AppError.Offline),
        )

        val state = BootstrapCoordinator(data, data).resolve()

        assertEquals(BootstrapState.ConnectionError(server, AppError.Offline), state)
        assertFalse(data.tokenCleared)
    }
}

private class FakeBootstrapData(
    private val servers: List<SavedServer> = emptyList(),
    private val activeServerId: String? = servers.firstOrNull()?.id,
    private val token: String? = null,
    private val validation: AppResult<Session> = AppResult.Failure(AppError.Offline),
) : ServerStore, SessionRepository {
    var tokenCleared = false

    override suspend fun getServers(): List<SavedServer> = servers

    override suspend fun save(server: SavedServer) = error("Not used")

    override suspend fun delete(serverId: String): List<SavedServer> = error("Not used")

    override suspend fun getActiveServerId(): String? = activeServerId

    override suspend fun setActiveServerId(serverId: String) = error("Not used")

    override suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
    ): AppResult<Session> = error("Not used")

    override suspend fun getToken(serverId: String): String? = token

    override suspend fun validate(server: SavedServer, token: String): AppResult<Session> =
        validation

    override suspend fun clearToken(serverId: String) {
        tokenCleared = true
    }
}

private fun savedServer() = SavedServer(
    id = "server-id",
    name = "家庭服务器",
    origin = "http://192.168.1.2:8000",
)

private fun session(server: SavedServer) = Session(
    server = server,
    token = "saved-token",
    user = SessionUser(id = 1, username = "tv_user", role = "user"),
)
