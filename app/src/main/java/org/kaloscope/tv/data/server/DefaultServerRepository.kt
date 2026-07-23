package org.kaloscope.tv.data.server

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.core.storage.ServerStore

@Singleton
class DefaultServerRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverStore: ServerStore,
    private val json: Json,
) : ServerRepository {
    override suspend fun testConnection(origin: String): AppResult<String> =
        networkCall(json) {
            apiClientFactory.create(origin).getVersion().dataOrThrow().version
        }

    override suspend fun saveServer(server: SavedServer) {
        serverStore.save(server)
    }

    override suspend fun setActiveServer(serverId: String) {
        serverStore.setActiveServerId(serverId)
    }
}
