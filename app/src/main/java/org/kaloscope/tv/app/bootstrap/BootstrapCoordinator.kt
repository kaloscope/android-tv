package org.kaloscope.tv.app.bootstrap

import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.storage.ServerStore
import org.kaloscope.tv.data.auth.SessionRepository

class BootstrapCoordinator(
    private val serverStore: ServerStore,
    private val sessionRepository: SessionRepository,
) {
    suspend fun resolve(): BootstrapState {
        val servers = serverStore.getServers()
        if (servers.isEmpty()) {
            return BootstrapState.NeedsServer(emptyList())
        }

        val activeServerId = serverStore.getActiveServerId()
        val activeServer = servers.firstOrNull { it.id == activeServerId } ?: servers.first()
        val token = sessionRepository.getToken(activeServer.id)
        if (token.isNullOrBlank()) {
            return BootstrapState.NeedsLogin(activeServer)
        }

        return when (val result = sessionRepository.validate(activeServer, token)) {
            is AppResult.Success -> BootstrapState.Ready(result.value)
            is AppResult.Failure -> {
                // Only authentication failures invalidate a stored session.
                if (result.error == AppError.Unauthorized) {
                    sessionRepository.clearToken(activeServer.id)
                    BootstrapState.NeedsLogin(activeServer)
                } else {
                    BootstrapState.ConnectionError(activeServer, result.error)
                }
            }
        }
    }
}
