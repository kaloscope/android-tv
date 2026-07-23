package org.kaloscope.tv.app.bootstrap

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session

interface BootstrapRepository {
    suspend fun getServers(): List<SavedServer>

    suspend fun getActiveServerId(): String?

    suspend fun getToken(serverId: String): String?

    suspend fun validateSession(server: SavedServer, token: String): AppResult<Session>

    suspend fun clearToken(serverId: String)
}
