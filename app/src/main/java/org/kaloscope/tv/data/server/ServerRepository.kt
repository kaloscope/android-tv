package org.kaloscope.tv.data.server

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer

/**
 * Owns verification and persistence operations for saved server endpoints.
 */
interface ServerRepository {
    suspend fun testConnection(origin: String): AppResult<String>

    suspend fun saveServer(server: SavedServer)

    suspend fun deleteServer(serverId: String): List<SavedServer>

    suspend fun setActiveServer(serverId: String)
}
