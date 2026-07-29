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
import org.kaloscope.tv.app.bootstrap.BootstrapRepository
import org.kaloscope.tv.app.bootstrap.BootstrapState
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.auth.SessionRepository
import org.kaloscope.tv.data.server.ServerRepository

class KaloscopeViewModelDeletionTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `successful deletion publishes remaining servers at root`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val target = server("target")
            val remaining = listOf(server("remaining"))
            val events = mutableListOf<String>()
            val viewModel = KaloscopeViewModel(
                bootstrapRepository = EmptyBootstrapRepository(),
                serverRepository = DeletionServerRepository(events, remaining),
                sessionRepository = DeletionSessionRepository(events),
            )
            advanceUntilIdle()

            viewModel.deleteServer(target)
            advanceUntilIdle()

            assertEquals(listOf("token:target", "server:target"), events)
            assertEquals(
                BootstrapState.NeedsServer(remaining),
                viewModel.bootstrapState.value,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class EmptyBootstrapRepository : BootstrapRepository {
    override suspend fun getServers(): List<SavedServer> = emptyList()

    override suspend fun getActiveServerId(): String? = null

    override suspend fun getToken(serverId: String): String? = null

    override suspend fun validateSession(
        server: SavedServer,
        token: String,
    ): AppResult<Session> = AppResult.Failure(AppError.Offline)

    override suspend fun clearToken(serverId: String) = Unit
}

private class DeletionServerRepository(
    private val events: MutableList<String>,
    private val remaining: List<SavedServer>,
) : ServerRepository {
    override suspend fun testConnection(origin: String): AppResult<String> =
        AppResult.Failure(AppError.Offline)

    override suspend fun saveServer(server: SavedServer) = Unit

    override suspend fun deleteServer(serverId: String): List<SavedServer> {
        events += "server:$serverId"
        return remaining
    }

    override suspend fun setActiveServer(serverId: String) = Unit
}

private class DeletionSessionRepository(
    private val events: MutableList<String>,
) : SessionRepository {
    override suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
    ): AppResult<Session> = AppResult.Failure(AppError.Offline)

    override suspend fun validate(
        server: SavedServer,
        token: String,
    ): AppResult<Session> = AppResult.Failure(AppError.Offline)

    override suspend fun getToken(serverId: String): String? = null

    override suspend fun clearToken(serverId: String) {
        events += "token:$serverId"
    }
}

private fun server(id: String) = SavedServer(
    id = id,
    name = "Server $id",
    origin = "https://$id.example",
)
