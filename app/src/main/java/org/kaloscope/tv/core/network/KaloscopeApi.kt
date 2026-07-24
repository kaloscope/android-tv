package org.kaloscope.tv.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import org.kaloscope.tv.data.media.remote.MediaItemData
import org.kaloscope.tv.data.media.remote.MediaLibraryData
import org.kaloscope.tv.data.media.remote.MediaPageData

interface KaloscopeApi {
    @GET("system/version")
    suspend fun getVersion(): ApiEnvelope<VersionData>

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
    ): ApiEnvelope<LoginData>

    @GET("auth/current")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String,
    ): ApiEnvelope<UserData>

    @GET("user/history/list")
    suspend fun getVideoHistory(
        @Header("Authorization") authorization: String,
        @Query("rel_type") relationType: String = "video",
        @Query("page_num") pageNumber: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("ordering") ordering: String = "-updated_at",
    ): ApiEnvelope<HistoryListData>

    @GET("media/lib/list")
    suspend fun getMediaLibraries(
        @Header("Authorization") authorization: String,
    ): ApiEnvelope<List<MediaLibraryData>>

    @GET("media/list")
    suspend fun getMediaPage(
        @Header("Authorization") authorization: String,
        @Query("page_num") pageNumber: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("lib_id") libraryId: Long,
        @Query("keyword") keyword: String? = null,
    ): ApiEnvelope<MediaPageData>

    @GET("media/{mediaId}")
    suspend fun getMediaDetail(
        @Header("Authorization") authorization: String,
        @Path("mediaId") mediaId: Long,
    ): ApiEnvelope<MediaItemData>
}

@Serializable
data class ApiEnvelope<T>(
    @SerialName("request_id")
    val requestId: String? = null,
    val status: Int,
    val message: String = "",
    val data: T,
)

@Serializable
data class VersionData(
    val version: String = "",
)

@Serializable
data class LoginData(
    val token: String,
    val user: UserData,
)

@Serializable
data class UserData(
    val id: Long,
    val username: String,
    val role: String,
)

@Serializable
data class HistoryListData(
    val total: Int = 0,
    val items: List<HistoryItemData> = emptyList(),
)

@Serializable
data class HistoryItemData(
    val id: Long,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("rel_id")
    val relId: Long,
    val position: Long = 0,
    val percentage: Int = 0,
    val media: HistoryMediaData? = null,
)

@Serializable
data class HistoryMediaData(
    val id: Long,
    val name: String = "",
    val title: String? = null,
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: String? = null,
)

@Serializable
internal data class ErrorData(
    @SerialName("request_id")
    val requestId: String? = null,
    val message: String? = null,
)

internal fun <T> ApiEnvelope<T>.dataOrThrow(): T {
    // The backend mirrors HTTP status in every successful JSON envelope.
    if (status != 200) {
        throw SerializationException("Unexpected envelope status")
    }
    return data
}
