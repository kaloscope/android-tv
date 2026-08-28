package org.kaloscope.tv.data.server

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.core.storage.ServerStore
import retrofit2.HttpException

@Singleton
class DefaultServerRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverStore: ServerStore,
    private val json: Json,
) : ServerRepository {
    override suspend fun testConnection(origin: String): AppResult<ServerConnectionInfo> {
        val probeResult = networkCall(json) {
            val response = apiClientFactory.create(origin).getVersion()
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            val envelope = response.body()
                ?: throw SerializationException("Missing server version")
            ServerVersionProbe(
                version = envelope.dataOrThrow().version,
                finalUrl = response.raw().request.url,
            )
        }

        return when (probeResult) {
            is AppResult.Success -> {
                val verifiedOrigin = ServerConnectionOriginPolicy.resolve(
                    requestedOrigin = origin,
                    finalUrl = probeResult.value.finalUrl,
                ) ?: return AppResult.Failure(AppError.InvalidData("server_redirect"))
                AppResult.Success(
                    ServerConnectionInfo(
                        origin = verifiedOrigin,
                        version = probeResult.value.version,
                    ),
                )
            }

            is AppResult.Failure -> probeResult
        }
    }

    override suspend fun saveServer(server: SavedServer) {
        serverStore.save(server)
    }

    override suspend fun deleteServer(serverId: String): List<SavedServer> =
        serverStore.delete(serverId)

    override suspend fun setActiveServer(serverId: String) {
        serverStore.setActiveServerId(serverId)
    }
}

private data class ServerVersionProbe(
    val version: String,
    val finalUrl: HttpUrl,
)
