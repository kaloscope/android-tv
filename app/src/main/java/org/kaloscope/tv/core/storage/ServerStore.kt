package org.kaloscope.tv.core.storage

import org.kaloscope.tv.core.model.SavedServer

interface ServerStore {
    suspend fun getServers(): List<SavedServer>

    suspend fun save(server: SavedServer)

    suspend fun getActiveServerId(): String?

    suspend fun setActiveServerId(serverId: String)
}
