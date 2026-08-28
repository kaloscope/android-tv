package org.kaloscope.tv.feature.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.auth.SessionRepository
import org.kaloscope.tv.data.server.ServerConnectionInfo
import org.kaloscope.tv.data.server.ServerRepository

class SavedServerDeletionCoordinatorTest {
    @Test
    fun `deletion clears token before metadata and returns remaining servers`() = runTest {
        val events = mutableListOf<String>()
        val remaining = listOf(server("remaining"))
        val coordinator = SavedServerDeletionCoordinator(
            serverRepository = FakeDeletionServerRepository(events, remaining = remaining),
            sessionRepository = FakeDeletionSessionRepository(events),
        )

        val result = coordinator.delete("target")

        assertEquals(listOf("token:target", "server:target"), events)
        assertEquals(remaining, result)
        assertEquals(SavedServerDeletionState.Idle, coordinator.state.value)
    }

    @Test
    fun `deletion in progress ignores a concurrent request`() = runTest {
        val events = mutableListOf<String>()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = FakeDeletionServerRepository(events) {
            started.complete(Unit)
            release.await()
            emptyList()
        }
        val coordinator = SavedServerDeletionCoordinator(
            serverRepository = repository,
            sessionRepository = FakeDeletionSessionRepository(events),
        )

        val first = async { coordinator.delete("first") }
        started.await()
        val second = coordinator.delete("second")

        assertNull(second)
        assertEquals(SavedServerDeletionState.Deleting("first"), coordinator.state.value)
        assertEquals(1, repository.deleteCalls)
        release.complete(Unit)
        assertEquals(emptyList<SavedServer>(), first.await())
    }

    @Test
    fun `metadata failure exposes retryable deletion error`() = runTest {
        val events = mutableListOf<String>()
        val coordinator = SavedServerDeletionCoordinator(
            serverRepository = FakeDeletionServerRepository(events) {
                error("storage unavailable")
            },
            sessionRepository = FakeDeletionSessionRepository(events),
        )

        val result = coordinator.delete("target")

        assertNull(result)
        assertEquals(listOf("token:target", "server:target"), events)
        assertEquals(SavedServerDeletionState.Failed("target"), coordinator.state.value)
    }

    @Test
    fun `clearing deletion error returns coordinator to idle`() = runTest {
        val coordinator = SavedServerDeletionCoordinator(
            serverRepository = FakeDeletionServerRepository(mutableListOf()) {
                error("storage unavailable")
            },
            sessionRepository = FakeDeletionSessionRepository(mutableListOf()),
        )
        coordinator.delete("target")

        coordinator.clearError()

        assertEquals(SavedServerDeletionState.Idle, coordinator.state.value)
    }

    @Test
    fun `cancellation resets deletion state and is rethrown`() = runTest {
        val coordinator = SavedServerDeletionCoordinator(
            serverRepository = FakeDeletionServerRepository(mutableListOf()) {
                throw CancellationException("cancelled")
            },
            sessionRepository = FakeDeletionSessionRepository(mutableListOf()),
        )

        val result = runCatching { coordinator.delete("target") }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(SavedServerDeletionState.Idle, coordinator.state.value)
    }
}

private class FakeDeletionServerRepository(
    private val events: MutableList<String>,
    private val remaining: List<SavedServer> = emptyList(),
    private val delete: suspend (String) -> List<SavedServer> = { remaining },
) : ServerRepository {
    var deleteCalls = 0

    override suspend fun testConnection(origin: String): AppResult<ServerConnectionInfo> =
        AppResult.Failure(AppError.Offline)

    override suspend fun saveServer(server: SavedServer) = Unit

    override suspend fun deleteServer(serverId: String): List<SavedServer> {
        deleteCalls += 1
        events += "server:$serverId"
        return delete(serverId)
    }

    override suspend fun setActiveServer(serverId: String) = Unit
}

private class FakeDeletionSessionRepository(
    private val events: MutableList<String>,
) : SessionRepository {
    override suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
    ): AppResult<Session> = AppResult.Failure(AppError.Offline)

    override suspend fun validate(server: SavedServer, token: String): AppResult<Session> =
        AppResult.Failure(AppError.Offline)

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
