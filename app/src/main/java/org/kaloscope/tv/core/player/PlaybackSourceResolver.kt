package org.kaloscope.tv.core.player

import androidx.media3.common.MimeTypes
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.kaloscope.tv.core.model.Session

data class ResolvedPlaybackSource(
    val url: String,
    val mimeType: String?,
)

object PlaybackSourceResolver {
    fun directStreamUrl(
        session: Session,
        path: String,
    ): String = streamUrl(session, path, PlaybackSourceKind.Direct, TranscodeResolution.P1080)

    fun localMediaSource(
        session: Session,
        path: String,
        sourceKind: PlaybackSourceKind,
        resolution: TranscodeResolution,
    ): ResolvedPlaybackSource {
        return ResolvedPlaybackSource(
            url = streamUrl(session, path, sourceKind, resolution),
            // The redirect target is HLS, but the initial stream URL has no file extension.
            mimeType = MimeTypes.APPLICATION_M3U8.takeIf {
                sourceKind == PlaybackSourceKind.HlsTranscode
            },
        )
    }

    fun resolveServerResource(
        session: Session,
        rawUrl: String,
    ): String =
        when {
            rawUrl.startsWith("/") -> "${session.server.origin}$rawUrl"
            else -> rawUrl
        }

    private fun streamUrl(
        session: Session,
        path: String,
        sourceKind: PlaybackSourceKind,
        resolution: TranscodeResolution,
    ): String {
        val builder = "${session.server.origin}/_api/media/stream"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("path", path)
        if (sourceKind == PlaybackSourceKind.HlsTranscode) {
            builder
                .addQueryParameter("transcode", "true")
                .addQueryParameter("quality", "medium")
                .addQueryParameter("resolution", resolution.queryValue)
        }
        return builder.build().toString()
    }
}
