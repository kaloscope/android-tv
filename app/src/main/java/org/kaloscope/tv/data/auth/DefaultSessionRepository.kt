package org.kaloscope.tv.data.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.LoginData
import org.kaloscope.tv.core.network.UserData
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.core.storage.SessionStore

@Singleton
class DefaultSessionRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val sessionStore: SessionStore,
    private val json: Json,
) : SessionRepository {
    override suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
    ): AppResult<Session> {
        val result = networkCall(json) {
            apiClientFactory.create(server.origin)
                .login(username, password)
                .dataOrThrow()
                .also { login ->
                    if (login.token.isBlank() || login.user.id <= 0) {
                        throw SerializationException("Invalid login data")
                    }
                }
        }
        return when (result) {
            is AppResult.Success -> {
                // Persist only tokens from structurally valid login responses.
                sessionStore.setToken(server.id, result.value.token)
                AppResult.Success(result.value.toSession(server))
            }

            is AppResult.Failure -> result
        }
    }

    override suspend fun validate(server: SavedServer, token: String): AppResult<Session> {
        val result = networkCall(json) {
            // The client is bound to this server origin before adding its token.
            apiClientFactory.create(server.origin)
                .getCurrentUser("Token $token")
                .dataOrThrow()
                .also { user ->
                    if (user.id <= 0) {
                        throw SerializationException("Invalid current user")
                    }
                }
        }
        return when (result) {
            is AppResult.Success -> AppResult.Success(
                Session(
                    server = server,
                    token = token,
                    user = result.value.toModel(),
                ),
            )

            is AppResult.Failure -> result
        }
    }

    override suspend fun getToken(serverId: String): String? =
        sessionStore.getToken(serverId)

    override suspend fun clearToken(serverId: String) {
        sessionStore.clearToken(serverId)
    }
}

private fun LoginData.toSession(server: SavedServer) = Session(
    server = server,
    token = token,
    user = user.toModel(),
)

private fun UserData.toModel() = SessionUser(
    id = id,
    username = username,
    role = role,
)
