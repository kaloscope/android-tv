package org.kaloscope.tv.data.server

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer

data class ServerConnectionInfo(
    val origin: String,
    val version: String,
)

/**
 * Owns verification and persistence operations for saved server endpoints.
 */
interface ServerRepository {
    suspend fun testConnection(origin: String): AppResult<ServerConnectionInfo>

    suspend fun saveServer(server: SavedServer)

    suspend fun deleteServer(serverId: String): List<SavedServer>

    suspend fun setActiveServer(serverId: String)
}
