package org.kaloscope.tv.core.storage

import org.kaloscope.tv.core.model.SavedServer

/**
 * Stores user-approved servers and the active server selection.
 */
interface ServerStore {
    suspend fun getServers(): List<SavedServer>

    suspend fun save(server: SavedServer)

    suspend fun delete(serverId: String): List<SavedServer>

    suspend fun getActiveServerId(): String?

    suspend fun setActiveServerId(serverId: String)
}
