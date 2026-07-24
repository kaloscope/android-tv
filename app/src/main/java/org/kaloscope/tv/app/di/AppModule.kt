package org.kaloscope.tv.app.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.kaloscope.tv.app.bootstrap.BootstrapRepository
import org.kaloscope.tv.app.bootstrap.DefaultBootstrapRepository
import org.kaloscope.tv.core.storage.AndroidKeystoreTokenCipher
import org.kaloscope.tv.core.storage.PreferencesServerStore
import org.kaloscope.tv.core.storage.SecureSessionStore
import org.kaloscope.tv.core.storage.ServerStore
import org.kaloscope.tv.core.storage.SessionStore
import org.kaloscope.tv.core.storage.TokenCipher
import org.kaloscope.tv.data.auth.DefaultSessionRepository
import org.kaloscope.tv.data.auth.SessionRepository
import org.kaloscope.tv.data.history.DefaultHistoryRepository
import org.kaloscope.tv.data.history.HistoryRepository
import org.kaloscope.tv.data.media.DefaultMediaRepository
import org.kaloscope.tv.data.media.MediaRepository
import org.kaloscope.tv.data.search.DefaultSearchRepository
import org.kaloscope.tv.data.search.SearchRepository
import org.kaloscope.tv.data.server.DefaultServerRepository
import org.kaloscope.tv.data.server.ServerRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    abstract fun bindServerStore(implementation: PreferencesServerStore): ServerStore

    @Binds
    abstract fun bindSessionStore(implementation: SecureSessionStore): SessionStore

    @Binds
    abstract fun bindTokenCipher(implementation: AndroidKeystoreTokenCipher): TokenCipher

    @Binds
    abstract fun bindServerRepository(implementation: DefaultServerRepository): ServerRepository

    @Binds
    abstract fun bindSessionRepository(implementation: DefaultSessionRepository): SessionRepository

    @Binds
    abstract fun bindHistoryRepository(
        implementation: DefaultHistoryRepository,
    ): HistoryRepository

    @Binds
    abstract fun bindMediaRepository(
        implementation: DefaultMediaRepository,
    ): MediaRepository

    @Binds
    abstract fun bindSearchRepository(
        implementation: DefaultSearchRepository,
    ): SearchRepository

    @Binds
    abstract fun bindBootstrapRepository(
        implementation: DefaultBootstrapRepository,
    ): BootstrapRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvides {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader = ImageLoader.Builder(context)
        .components {
            // Authentication is attached per request after validating its origin.
            add(OkHttpNetworkFetcherFactory(callFactory = { OkHttpClient() }))
        }
        .build()
}
