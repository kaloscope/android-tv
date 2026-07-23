package org.kaloscope.tv.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data object Unauthorized : AppError

    data object Forbidden : AppError

    data object NotFound : AppError

    data object Timeout : AppError

    data object Offline : AppError

    data class Api(
        val code: String?,
        val requestId: String?,
    ) : AppError

    data class InvalidData(val context: String) : AppError
}
