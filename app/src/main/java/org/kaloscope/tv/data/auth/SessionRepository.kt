package org.kaloscope.tv.data.auth

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session

/**
 * Keeps authentication operations and tokens scoped to a single saved server.
 */
interface SessionRepository {
    suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
    ): AppResult<Session>

    suspend fun validate(server: SavedServer, token: String): AppResult<Session>

    suspend fun getToken(serverId: String): String?

    suspend fun clearToken(serverId: String)
}
