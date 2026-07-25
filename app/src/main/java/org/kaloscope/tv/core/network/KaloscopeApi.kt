package org.kaloscope.tv.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import org.kaloscope.tv.data.media.remote.DanmakuWrapperData
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response
import org.kaloscope.tv.data.media.remote.MediaItemData
import org.kaloscope.tv.data.media.remote.MediaLibraryData
import org.kaloscope.tv.data.media.remote.MediaPageData
import org.kaloscope.tv.data.media.remote.SubtitleTrackData
import org.kaloscope.tv.data.search.remote.IndexerAuthData
import org.kaloscope.tv.data.search.remote.IndexerConfigData
import org.kaloscope.tv.data.search.remote.IndexerDetailsRequestData
import org.kaloscope.tv.data.search.remote.IndexerPageData
import org.kaloscope.tv.data.search.remote.IndexerResourceData
import org.kaloscope.tv.data.search.remote.IndexerResourcePageData

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

    @GET("flow/graph/list")
    suspend fun getIndexers(
        @Header("Authorization") authorization: String,
        @Query("page_num") pageNumber: Int = 0,
        @Query("ordering") ordering: String = "name",
        @Query("category") category: String = "indexer",
        @Query("states") states: List<String> = listOf("modified", "published"),
    ): ApiEnvelope<IndexerPageData>

    @GET("flow/indexer/{indexerId}/config")
    suspend fun getIndexerConfig(
        @Header("Authorization") authorization: String,
        @Path("indexerId") indexerId: Long,
    ): ApiEnvelope<IndexerConfigData>

    @GET("flow/indexer/{indexerId}/auth")
    suspend fun getIndexerAuth(
        @Header("Authorization") authorization: String,
        @Path("indexerId") indexerId: Long,
    ): NullableApiEnvelope<IndexerAuthData>

    @POST("flow/graph/{indexerId}/execute")
    suspend fun executeIndexerSearch(
        @Header("Authorization") authorization: String,
        @Path("indexerId") indexerId: Long,
        @Body body: JsonObject,
    ): ApiEnvelope<IndexerResourcePageData>

    @POST("flow/graph/{indexerId}/execute")
    suspend fun executeIndexerDetails(
        @Header("Authorization") authorization: String,
        @Path("indexerId") indexerId: Long,
        @Body body: IndexerDetailsRequestData,
    ): NullableApiEnvelope<IndexerResourceData>

    @POST("subtitle/tracks")
    suspend fun getSubtitleTracks(
        @Header("Authorization") authorization: String,
        @Body body: MediaResourceData,
    ): ApiEnvelope<List<SubtitleTrackData>>

    @POST("danmaku/match")
    suspend fun getDanmakus(
        @Header("Authorization") authorization: String,
        @Body body: MediaResourceData,
    ): ApiEnvelope<DanmakuWrapperData>

    @POST("user/history/record")
    suspend fun recordVideoProgress(
        @Header("Authorization") authorization: String,
        @Body body: HistoryRecordData,
    ): Response<Unit>
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
data class NullableApiEnvelope<T>(
    @SerialName("request_id")
    val requestId: String? = null,
    val status: Int,
    val message: String = "",
    val data: T? = null,
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
    val path: String = "",
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
data class MediaResourceData(
    val path: String,
)

@Serializable
data class HistoryRecordData(
    @SerialName("rel_type")
    val relationType: String,
    @SerialName("rel_id")
    val relationId: Long,
    val position: Long,
    val percentage: Int,
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

internal fun <T> NullableApiEnvelope<T>.dataOrThrow(): T? {
    if (status != 200) {
        throw SerializationException("Unexpected envelope status")
    }
    return data
}
