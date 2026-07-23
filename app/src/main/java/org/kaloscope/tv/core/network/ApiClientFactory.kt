package org.kaloscope.tv.core.network

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Singleton
class ApiClientFactory @Inject constructor(
    private val json: Json,
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Binds relative API calls to one normalized Kaloscope server origin.
     */
    fun create(origin: String): KaloscopeApi =
        Retrofit.Builder()
            .baseUrl("$origin/_api/")
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(KaloscopeApi::class.java)
}
