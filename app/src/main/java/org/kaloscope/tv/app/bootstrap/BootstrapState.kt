package org.kaloscope.tv.app.bootstrap

import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session

sealed interface BootstrapState {
    data object Loading : BootstrapState

    data class NeedsServer(val savedServers: List<SavedServer>) : BootstrapState

    data class NeedsLogin(val server: SavedServer) : BootstrapState

    data class Ready(val session: Session) : BootstrapState

    data class ConnectionError(
        val server: SavedServer,
        val error: AppError,
    ) : BootstrapState
}
