package org.kaloscope.tv.core.network

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import retrofit2.HttpException

suspend fun <T> networkCall(
    json: Json,
    block: suspend () -> T,
): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: HttpException) {
        AppResult.Failure(error.toAppError(json))
    } catch (_: SocketTimeoutException) {
        AppResult.Failure(AppError.Timeout)
    } catch (_: SerializationException) {
        AppResult.Failure(AppError.InvalidData("服务器响应格式无效"))
    } catch (_: IOException) {
        AppResult.Failure(AppError.Offline)
    }

private fun HttpException.toAppError(json: Json): AppError =
    when (code()) {
        401 -> AppError.Unauthorized
        403 -> AppError.Forbidden
        404 -> AppError.NotFound
        408, 502, 503, 504 -> AppError.Timeout
        else -> {
            val errorData = runCatching {
                response()?.errorBody()?.string()?.let {
                    json.decodeFromString<ErrorData>(it)
                }
            }.getOrNull()
            AppError.Api(
                code = errorData?.message ?: "http_${code()}",
                requestId = errorData?.requestId,
            )
        }
    }
