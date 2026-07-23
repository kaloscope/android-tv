package org.kaloscope.tv.feature.login

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.data.auth.SessionRepository

class LoginCoordinatorTest {
    @Test
    fun `missing credentials do not call login`() = runBlocking {
        val repository = FakeSessionRepository()
        val coordinator = LoginCoordinator(server(), repository)

        val session = coordinator.submit()

        assertNull(session)
        assertEquals(LoginError.MissingCredentials, coordinator.state.value.error)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `successful login trims username and returns persisted session`() = runBlocking {
        val expected = session()
        val repository = FakeSessionRepository(result = AppResult.Success(expected))
        val coordinator = LoginCoordinator(server(), repository)
        coordinator.updateUsername("  tv_user ")
        coordinator.updatePassword("secret")

        val actual = coordinator.submit()

        assertEquals(expected, actual)
        assertEquals("tv_user", repository.username)
        assertEquals("secret", repository.password)
        assertEquals("", coordinator.state.value.password)
        assertFalse(coordinator.state.value.isSubmitting)
    }

    @Test
    fun `failed login clears password and exposes recoverable error`() = runBlocking {
        val error = AppError.Api(code = "login_failed", requestId = "request-id")
        val repository = FakeSessionRepository(result = AppResult.Failure(error))
        val coordinator = LoginCoordinator(server(), repository)
        coordinator.updateUsername("tv_user")
        coordinator.updatePassword("wrong")

        val session = coordinator.submit()

        assertNull(session)
        assertEquals("", coordinator.state.value.password)
        assertEquals(LoginError.Request(error), coordinator.state.value.error)
        assertFalse(coordinator.state.value.isSubmitting)
    }

    @Test
    fun `editing a field clears the previous error`() = runBlocking {
        val coordinator = LoginCoordinator(server(), FakeSessionRepository())
        coordinator.submit()

        coordinator.updateUsername("tv_user")

        assertNull(coordinator.state.value.error)
    }
}

private class FakeSessionRepository(
    private val result: AppResult<Session> = AppResult.Failure(AppError.Offline),
) : SessionRepository {
    var loginCalls = 0
    var username: String? = null
    var password: String? = null

    override suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
    ): AppResult<Session> {
        loginCalls += 1
        this.username = username
        this.password = password
        return result
    }

    override suspend fun validate(server: SavedServer, token: String): AppResult<Session> =
        AppResult.Failure(AppError.Offline)

    override suspend fun getToken(serverId: String): String? = null

    override suspend fun clearToken(serverId: String) = Unit
}

private fun server() = SavedServer(
    id = "server-id",
    name = "家庭服务器",
    origin = "http://192.168.1.2:8000",
)

private fun session() = Session(
    server = server(),
    token = "token",
    user = SessionUser(id = 1, username = "tv_user", role = "user"),
)
