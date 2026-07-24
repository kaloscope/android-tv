package org.kaloscope.tv.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SubtitleTrack

data class PlaybackStatus(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val sourceKind: PlaybackSourceKind,
    val fallbackInProgress: Boolean = false,
    val failure: PlaybackFailure? = null,
    val cues: List<Cue> = emptyList(),
)

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackController internal constructor(
    context: Context,
    private val session: Session,
    private val request: PlaybackRequest,
    private val subtitles: List<SubtitleTrack>,
    private val onProgress: (Long, Long, ProgressReason) -> Unit,
) {
    private var sourceKind = when (request) {
        is PlaybackRequest.LocalMedia -> PlaybackSourcePolicy.initialSource(request.playbackMode)
        is PlaybackRequest.NetworkVideo -> PlaybackSourceKind.Network
    }
    private var fallbackAttempted = false
    private val mutableStatus = MutableStateFlow(PlaybackStatus(sourceKind = sourceKind))
    private val listener = object : Player.Listener {
        override fun onEvents(
            player: Player,
            events: Player.Events,
        ) {
            mutableStatus.value = mutableStatus.value.copy(
                isPlaying = player.isPlaying,
                playbackState = player.playbackState,
                fallbackInProgress = mutableStatus.value.fallbackInProgress &&
                    player.playbackState != Player.STATE_READY,
            )
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
                player.playbackState == Player.STATE_READY
            ) {
                record(ProgressReason.Started)
            }
        }

        override fun onCues(cueGroup: CueGroup) {
            mutableStatus.value = mutableStatus.value.copy(cues = cueGroup.cues)
        }

        override fun onPlayerError(error: PlaybackException) {
            record(ProgressReason.Error)
            val failure = PlaybackFailureClassifier.classify(
                errorCode = error.errorCode,
                httpStatus = error.findHttpStatus(),
            )
            val localRequest = request as? PlaybackRequest.LocalMedia
            if (localRequest != null &&
                PlaybackFallbackPolicy.shouldFallback(
                    mode = localRequest.playbackMode,
                    sourceKind = sourceKind,
                    failure = failure,
                    fallbackAttempted = fallbackAttempted,
                )
            ) {
                // Auto may replace direct playback once while keeping the viewer's position.
                fallbackAttempted = true
                sourceKind = PlaybackSourceKind.HlsTranscode
                mutableStatus.value = mutableStatus.value.copy(
                    sourceKind = sourceKind,
                    fallbackInProgress = true,
                    failure = null,
                )
                startSource(sourceKind, currentPositionMillis())
            } else {
                mutableStatus.value = mutableStatus.value.copy(
                    fallbackInProgress = false,
                    failure = failure,
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && player.playbackState == Player.STATE_READY) {
                record(ProgressReason.Paused)
            }
        }
    }

    val player: ExoPlayer
    val status: StateFlow<PlaybackStatus> = mutableStatus.asStateFlow()
    private val mediaSession: MediaSession

    init {
        val dataSourceFactory = OkHttpDataSource.Factory(authenticatedClient(session))
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setSeekBackIncrementMs(SEEK_INCREMENT_MILLIS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MILLIS)
            .build()
        mediaSession = MediaSession.Builder(context, player).build()
        player.addListener(listener)
        startSource(sourceKind, request.resumePositionMillis())
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekBy(offsetMillis: Long) {
        val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + offsetMillis).coerceIn(0, duration))
        record(ProgressReason.Seeked)
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
    }

    fun retry() {
        mutableStatus.value = mutableStatus.value.copy(
            fallbackInProgress = sourceKind == PlaybackSourceKind.HlsTranscode,
            failure = null,
        )
        startSource(sourceKind, currentPositionMillis())
    }

    fun recordPeriodicProgress() {
        record(ProgressReason.Periodic)
    }

    fun release() {
        record(ProgressReason.Exit)
        player.removeListener(listener)
        mediaSession.release()
        player.release()
    }

    private fun startSource(
        target: PlaybackSourceKind,
        positionMillis: Long,
    ) {
        player.setMediaItem(buildMediaItem(target), positionMillis)
        player.prepare()
        player.playWhenReady = true
    }

    private fun buildMediaItem(target: PlaybackSourceKind): MediaItem {
        val source = when (request) {
            is PlaybackRequest.LocalMedia -> PlaybackSourceResolver.localMediaSource(
                session = session,
                path = request.path,
                sourceKind = target,
                resolution = request.transcodeResolution,
            )

            is PlaybackRequest.NetworkVideo -> PlaybackSourceResolver.networkMediaSource(
                session = session,
                rawUrl = request.source.url,
                videoType = request.source.videoType,
            )
        }
        val subtitleConfigurations = subtitles.map { track ->
            MediaItem.SubtitleConfiguration.Builder(
                Uri.parse(PlaybackSourceResolver.resolveServerResource(session, track.url)),
            )
                .setId(track.id)
                .setLabel(track.label)
                .setLanguage(track.language)
                .setMimeType(MimeTypes.TEXT_VTT)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }
        return MediaItem.Builder()
            .setMediaId(
                when (request) {
                    is PlaybackRequest.LocalMedia -> request.mediaId.toString()
                    is PlaybackRequest.NetworkVideo -> request.source.resourceId
                },
            )
            .setUri(source.url)
            .setMimeType(source.mimeType)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(request.title)
                    .build(),
            )
            .setSubtitleConfigurations(subtitleConfigurations)
            .build()
    }

    private fun record(reason: ProgressReason) {
        onProgress(player.currentPosition, player.duration, reason)
    }

    private fun currentPositionMillis(): Long =
        player.currentPosition.takeIf { it >= 0 }
            ?: request.resumePositionMillis()

    private fun Throwable.findHttpStatus(): Int? {
        var current: Throwable? = this
        repeat(MAX_CAUSE_DEPTH) {
            if (current is HttpDataSource.InvalidResponseCodeException) {
                return current.responseCode
            }
            current = current?.cause
        }
        return null
    }

    private fun authenticatedClient(session: Session): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                // Media3 may follow resource URLs, so authentication is decided per request.
                val authenticated = if (
                    org.kaloscope.tv.core.network.OriginAuthPolicy.shouldAttachToken(
                        session.server.origin,
                        request.url.toString(),
                    )
                ) {
                    request.newBuilder()
                        .header("Authorization", "Token ${session.token}")
                        .build()
                } else {
                    request
                }
                chain.proceed(authenticated)
            }
            .build()

    private fun Long?.orZero(): Long = this?.coerceAtLeast(0) ?: 0

    private fun PlaybackRequest.resumePositionMillis(): Long =
        when (this) {
            is PlaybackRequest.LocalMedia -> resumePositionSeconds.orZero() * 1_000
            is PlaybackRequest.NetworkVideo -> 0
        }

    private companion object {
        const val SEEK_INCREMENT_MILLIS = 10_000L
        const val MAX_CAUSE_DEPTH = 8
    }
}

class PlaybackControllerFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun create(
        session: Session,
        request: PlaybackRequest,
        subtitles: List<SubtitleTrack>,
        onProgress: (Long, Long, ProgressReason) -> Unit,
    ): PlaybackController = PlaybackController(
        context = context,
        session = session,
        request = request,
        subtitles = subtitles,
        onProgress = onProgress,
    )
}
