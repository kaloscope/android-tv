package org.kaloscope.tv.app.bootstrap

import javax.inject.Inject
import javax.inject.Singleton
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.storage.ServerStore
import org.kaloscope.tv.data.auth.SessionRepository

@Singleton
class DefaultBootstrapRepository @Inject constructor(
    private val serverStore: ServerStore,
    private val sessionRepository: SessionRepository,
) : BootstrapRepository {
    override suspend fun getServers(): List<SavedServer> =
        serverStore.getServers()

    override suspend fun getActiveServerId(): String? =
        serverStore.getActiveServerId()

    override suspend fun getToken(serverId: String): String? =
        sessionRepository.getToken(serverId)

    override suspend fun validateSession(
        server: SavedServer,
        token: String,
    ): AppResult<Session> = sessionRepository.validate(server, token)

    override suspend fun clearToken(serverId: String) {
        sessionRepository.clearToken(serverId)
    }
}
