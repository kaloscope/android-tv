package org.kaloscope.tv.core.player

import androidx.media3.common.MimeTypes
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.ByteString.Companion.encodeUtf8
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.NetworkVideoType

data class ResolvedPlaybackSource(
    val url: String,
    val mimeType: String?,
)

object PlaybackSourceResolver {
    fun localMediaSource(
        session: Session,
        path: String,
        sourceKind: PlaybackSourceKind,
        quality: TranscodeQuality,
    ): ResolvedPlaybackSource {
        return ResolvedPlaybackSource(
            url = streamUrl(session, path, sourceKind, quality),
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

    fun networkMediaSource(
        session: Session,
        rawUrl: String,
        videoType: NetworkVideoType,
    ): ResolvedPlaybackSource {
        val resolvedUrl = if (videoType == NetworkVideoType.Dash && rawUrl.isInlineDash()) {
            // Inline manifests still need an absolute base for authenticated API segments.
            val manifest = rawUrl.replace(
                INLINE_API_BASE,
                "$1${session.server.origin}/_api/",
            )
            val encoded = manifest.encodeUtf8().base64()
            "data:${MimeTypes.APPLICATION_MPD};base64,$encoded"
        } else {
            resolveServerResource(session, rawUrl)
        }
        return ResolvedPlaybackSource(
            url = resolvedUrl,
            mimeType = when (videoType) {
                NetworkVideoType.Hls -> MimeTypes.APPLICATION_M3U8
                NetworkVideoType.Dash -> MimeTypes.APPLICATION_MPD
                NetworkVideoType.Mp4 -> MimeTypes.VIDEO_MP4
                NetworkVideoType.Unknown -> null
            },
        )
    }

    private fun streamUrl(
        session: Session,
        path: String,
        sourceKind: PlaybackSourceKind,
        quality: TranscodeQuality,
    ): String {
        val builder = "${session.server.origin}/_api/media/stream"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("path", path)
        if (sourceKind == PlaybackSourceKind.HlsTranscode) {
            builder
                .addQueryParameter("transcode", "true")
                .addQueryParameter("quality", quality.queryValue)
        }
        return builder.build().toString()
    }

    private fun String.isInlineDash(): Boolean = INLINE_DASH.containsMatchIn(this)

    private val INLINE_DASH = Regex(
        pattern = """^\s*(?:<\?xml[\s\S]*?\?>\s*)?<MPD[\s>]""",
        option = RegexOption.IGNORE_CASE,
    )
    private val INLINE_API_BASE = Regex(
        pattern = """(<BaseURL>\s*)/_api/""",
        option = RegexOption.IGNORE_CASE,
    )
}
