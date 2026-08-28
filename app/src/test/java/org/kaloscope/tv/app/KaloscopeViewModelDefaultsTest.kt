package org.kaloscope.tv.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.app.bootstrap.BootstrapState
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.storage.ServerStore
import org.kaloscope.tv.data.auth.SessionRepository
import org.kaloscope.tv.data.server.ServerConnectionInfo
import org.kaloscope.tv.data.server.ServerRepository

class KaloscopeViewModelDefaultsTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `server setup starts with configured defaults`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = viewModel(servers = emptyList())

            advanceUntilIdle()

            assertEquals(BootstrapState.NeedsServer(emptyList()), viewModel.bootstrapState.value)
            assertEquals("Debug", viewModel.serverSetupState.value.name)
            assertEquals("https://debug.example", viewModel.serverSetupState.value.url)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `login starts with configured credentials`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val savedServer = server()
            val viewModel = viewModel(servers = listOf(savedServer))

            advanceUntilIdle()

            assertEquals(BootstrapState.NeedsLogin(savedServer), viewModel.bootstrapState.value)
            assertEquals("debug_user", viewModel.loginState.value.username)
            assertEquals("debug_password", viewModel.loginState.value.password)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(servers: List<SavedServer>) = KaloscopeViewModel(
        serverStore = DefaultsServerStore(servers),
        serverRepository = DefaultsServerRepository(),
        sessionRepository = DefaultsSessionRepository(),
        formDefaults = AppFormDefaults(
            serverName = "Debug",
            serverUrl = "https://debug.example",
            username = "debug_user",
            password = "debug_password",
        ),
    )
}

private class DefaultsServerStore(
    private val servers: List<SavedServer>,
) : ServerStore {
    override suspend fun getServers(): List<SavedServer> = servers

    override suspend fun getActiveServerId(): String? = servers.firstOrNull()?.id

    override suspend fun save(server: SavedServer) = error("Not used")

    override suspend fun delete(serverId: String): List<SavedServer> = error("Not used")

    override suspend fun setActiveServerId(serverId: String) = error("Not used")
}

private class DefaultsServerRepository : ServerRepository {
    override suspend fun testConnection(origin: String): AppResult<ServerConnectionInfo> =
        AppResult.Failure(AppError.Offline)

    override suspend fun saveServer(server: SavedServer) = error("Not used")

    override suspend fun deleteServer(serverId: String): List<SavedServer> = error("Not used")

    override suspend fun setActiveServer(serverId: String) = error("Not used")
}

private class DefaultsSessionRepository : SessionRepository {
    override suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
    ): AppResult<Session> = error("Not used")

    override suspend fun validate(
        server: SavedServer,
        token: String,
    ): AppResult<Session> = error("Not used")

    override suspend fun getToken(serverId: String): String? = null

    override suspend fun clearToken(serverId: String) = error("Not used")
}

private fun server() = SavedServer(
    id = "server-id",
    name = "Debug",
    origin = "https://debug.example",
)
