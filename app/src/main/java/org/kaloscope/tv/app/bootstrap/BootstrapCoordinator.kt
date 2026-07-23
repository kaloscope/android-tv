package org.kaloscope.tv.app.bootstrap

import kotlinx.coroutines.CancellationException
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult

class BootstrapCoordinator(
    private val repository: BootstrapRepository,
) {
    suspend fun resolve(): BootstrapState {
        try {
            val servers = repository.getServers()
            if (servers.isEmpty()) {
                return BootstrapState.NeedsServer(emptyList())
            }

            val activeServerId = repository.getActiveServerId()
            val activeServer = servers.firstOrNull { it.id == activeServerId } ?: servers.first()
            val token = repository.getToken(activeServer.id)
            if (token.isNullOrBlank()) {
                return BootstrapState.NeedsLogin(activeServer)
            }

            return when (val result = repository.validateSession(activeServer, token)) {
                is AppResult.Success -> BootstrapState.Ready(result.value)
                is AppResult.Failure -> {
                    if (result.error == AppError.Unauthorized) {
                        repository.clearToken(activeServer.id)
                        BootstrapState.NeedsLogin(activeServer)
                    } else {
                        BootstrapState.ConnectionError(activeServer, result.error)
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        }
    }
}
