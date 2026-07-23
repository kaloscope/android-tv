package org.kaloscope.tv.feature.login

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.auth.SessionRepository

sealed interface LoginError {
    data object MissingCredentials : LoginError

    data class Request(val error: AppError) : LoginError
}

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: LoginError? = null,
)

class LoginCoordinator(
    private val server: SavedServer,
    private val repository: SessionRepository,
) {
    private val mutableState = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = mutableState.asStateFlow()

    fun updateUsername(value: String) {
        mutableState.value = mutableState.value.copy(username = value, error = null)
    }

    fun updatePassword(value: String) {
        mutableState.value = mutableState.value.copy(password = value, error = null)
    }

    suspend fun submit(): Session? {
        val current = mutableState.value
        if (current.isSubmitting) {
            return null
        }
        if (current.username.isBlank() || current.password.isBlank()) {
            mutableState.value = current.copy(error = LoginError.MissingCredentials)
            return null
        }

        mutableState.value = current.copy(isSubmitting = true, error = null)
        try {
            return when (
                val result = repository.login(
                    server = server,
                    username = current.username.trim(),
                    password = current.password,
                )
            ) {
                is AppResult.Success -> {
                    mutableState.value = mutableState.value.copy(
                        password = "",
                        isSubmitting = false,
                    )
                    result.value
                }

                is AppResult.Failure -> {
                    mutableState.value = mutableState.value.copy(
                        password = "",
                        isSubmitting = false,
                        error = LoginError.Request(result.error),
                    )
                    null
                }
            }
        } catch (error: CancellationException) {
            mutableState.value = mutableState.value.copy(
                password = "",
                isSubmitting = false,
            )
            throw error
        }
    }
}
