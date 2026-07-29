package org.kaloscope.tv.feature.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.data.auth.SessionRepository
import org.kaloscope.tv.data.server.ServerRepository

sealed interface SavedServerDeletionState {
    data object Idle : SavedServerDeletionState

    data class Deleting(val serverId: String) : SavedServerDeletionState

    data class Failed(val serverId: String) : SavedServerDeletionState
}

class SavedServerDeletionCoordinator(
    private val serverRepository: ServerRepository,
    private val sessionRepository: SessionRepository,
) {
    private val mutableState =
        MutableStateFlow<SavedServerDeletionState>(SavedServerDeletionState.Idle)
    val state: StateFlow<SavedServerDeletionState> = mutableState.asStateFlow()

    suspend fun delete(serverId: String): List<SavedServer>? {
        if (mutableState.value is SavedServerDeletionState.Deleting) {
            return null
        }
        mutableState.value = SavedServerDeletionState.Deleting(serverId)
        try {
            // Credentials are removed first so a partial failure cannot leave an orphan token.
            sessionRepository.clearToken(serverId)
            return serverRepository.deleteServer(serverId).also {
                mutableState.value = SavedServerDeletionState.Idle
            }
        } catch (error: CancellationException) {
            mutableState.value = SavedServerDeletionState.Idle
            throw error
        } catch (_: Exception) {
            mutableState.value = SavedServerDeletionState.Failed(serverId)
            return null
        }
    }

    fun clearError() {
        if (mutableState.value is SavedServerDeletionState.Failed) {
            mutableState.value = SavedServerDeletionState.Idle
        }
    }
}
