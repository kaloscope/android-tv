package org.kaloscope.tv.data.server

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer

interface ServerRepository {
    suspend fun testConnection(origin: String): AppResult<String>

    suspend fun saveServer(server: SavedServer)

    suspend fun setActiveServer(serverId: String)
}
