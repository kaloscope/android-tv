package org.kaloscope.tv.feature.server

import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.data.server.InvalidServerUrl
import org.kaloscope.tv.data.server.ServerRepository
import org.kaloscope.tv.data.server.ServerUrlNormalizer

sealed interface ServerSetupError {
    data object InvalidUrl : ServerSetupError

    data object SaveFailed : ServerSetupError

    data class Connection(val error: AppError) : ServerSetupError
}

data class ServerSetupState(
    val name: String = "",
    val url: String = "",
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val error: ServerSetupError? = null,
    val verifiedOrigin: String? = null,
    val serverVersion: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && verifiedOrigin != null && !isTesting && !isSaving
}

class ServerSetupCoordinator(
    private val repository: ServerRepository,
    private val createServerId: () -> String,
    initialName: String = "",
    initialUrl: String = "",
) {
    private val initialState = ServerSetupState(
        name = initialName,
        url = initialUrl,
    )
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<ServerSetupState> = mutableState.asStateFlow()

    fun reset() {
        mutableState.value = initialState
    }

    fun updateName(value: String) {
        mutableState.value = mutableState.value.copy(
            name = value,
            error = null,
        )
    }

    fun updateUrl(value: String) {
        // A connection proof is valid only for the exact origin that was tested.
        mutableState.value = mutableState.value.copy(
            url = value,
            error = null,
            verifiedOrigin = null,
            serverVersion = null,
        )
    }

    suspend fun testConnection() {
        val current = mutableState.value
        if (current.isTesting || current.isSaving) {
            return
        }

        val origin = try {
            ServerUrlNormalizer.normalize(current.url)
        } catch (_: InvalidServerUrl) {
            mutableState.value = current.copy(error = ServerSetupError.InvalidUrl)
            return
        }

        mutableState.value = current.copy(isTesting = true, error = null)
        try {
            mutableState.value = when (val result = repository.testConnection(origin)) {
                is AppResult.Success -> {
                    val connection = result.value
                    mutableState.value.copy(
                        name = mutableState.value.name.ifBlank {
                            URI(connection.origin).host.removeSurrounding("[", "]")
                        },
                        url = connection.origin,
                        isTesting = false,
                        verifiedOrigin = connection.origin,
                        serverVersion = connection.version,
                    )
                }

                is AppResult.Failure -> mutableState.value.copy(
                    isTesting = false,
                    verifiedOrigin = null,
                    serverVersion = null,
                    error = ServerSetupError.Connection(result.error),
                )
            }
        } catch (error: CancellationException) {
            mutableState.value = mutableState.value.copy(isTesting = false)
            throw error
        }
    }

    suspend fun save(): SavedServer? {
        val current = mutableState.value
        // Never persist an address that has not passed the public version check.
        val origin = current.verifiedOrigin ?: return null
        if (!current.canSave) {
            return null
        }

        val server = SavedServer(
            id = createServerId(),
            name = current.name.trim(),
            origin = origin,
        )
        mutableState.value = current.copy(isSaving = true)
        try {
            repository.saveServer(server)
            repository.setActiveServer(server.id)
            mutableState.value = mutableState.value.copy(isSaving = false)
            return server
        } catch (error: CancellationException) {
            mutableState.value = mutableState.value.copy(isSaving = false)
            throw error
        } catch (_: Exception) {
            // Keep the verified draft available so the user can retry the local write.
            mutableState.value = mutableState.value.copy(
                isSaving = false,
                error = ServerSetupError.SaveFailed,
            )
            return null
        }
    }
}
