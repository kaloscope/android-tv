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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.feature.player.PlayerUiState

data class PlaybackStatus(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val error: Boolean = false,
    val cues: List<Cue> = emptyList(),
)

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackController internal constructor(
    context: Context,
    private val session: Session,
    private val content: PlayerUiState.Content,
    private val onProgress: (Long, Long, ProgressReason) -> Unit,
) {
    private val mutableStatus = MutableStateFlow(PlaybackStatus())
    private val listener = object : Player.Listener {
        override fun onEvents(
            player: Player,
            events: Player.Events,
        ) {
            mutableStatus.value = mutableStatus.value.copy(
                isPlaying = player.isPlaying,
                playbackState = player.playbackState,
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
            mutableStatus.value = mutableStatus.value.copy(error = true)
            record(ProgressReason.Error)
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
        player.setMediaItem(buildMediaItem(), content.request.resumePositionSeconds.orZero() * 1_000)
        player.prepare()
        player.playWhenReady = true
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
        mutableStatus.value = mutableStatus.value.copy(error = false)
        player.prepare()
        player.play()
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

    private fun buildMediaItem(): MediaItem {
        val subtitles = content.subtitles.map { track ->
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
            .setMediaId(content.request.mediaId.toString())
            .setUri(PlaybackSourceResolver.directStreamUrl(session, content.request.path))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(content.request.title)
                    .build(),
            )
            .setSubtitleConfigurations(subtitles)
            .build()
    }

    private fun record(reason: ProgressReason) {
        onProgress(player.currentPosition, player.duration, reason)
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

    private companion object {
        const val SEEK_INCREMENT_MILLIS = 10_000L
    }
}

class PlaybackControllerFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun create(
        session: Session,
        content: PlayerUiState.Content,
        onProgress: (Long, Long, ProgressReason) -> Unit,
    ): PlaybackController = PlaybackController(
        context = context,
        session = session,
        content = content,
        onProgress = onProgress,
    )
}
