package org.kaloscope.tv.core.player

import androidx.media3.common.PlaybackException

enum class PlaybackMode {
    Auto,
    Direct,
    Transcode,
}

enum class PlaybackSourceKind {
    Direct,
    HlsTranscode,
    Network,
}

enum class TranscodeResolution(
    val queryValue: String,
) {
    Original("original"),
    P1080("1080p"),
    P720("720p"),
    P480("480p"),
}

enum class TranscodeQuality(
    val queryValue: String,
) {
    Low("low"),
    Medium("medium"),
    High("high"),
}

enum class PlaybackFailure {
    Source,
    Decoder,
    Network,
    Unauthorized,
    Forbidden,
    MissingMedia,
    Unknown,
}

object PlaybackSourcePolicy {
    fun initialSource(mode: PlaybackMode): PlaybackSourceKind =
        if (mode == PlaybackMode.Transcode) {
            PlaybackSourceKind.HlsTranscode
        } else {
            PlaybackSourceKind.Direct
        }
}

object PlaybackFallbackPolicy {
    fun shouldFallback(
        mode: PlaybackMode,
        sourceKind: PlaybackSourceKind,
        failure: PlaybackFailure,
        fallbackAttempted: Boolean,
    ): Boolean =
        mode == PlaybackMode.Auto &&
            sourceKind == PlaybackSourceKind.Direct &&
            !fallbackAttempted &&
            (failure == PlaybackFailure.Source || failure == PlaybackFailure.Decoder)
}

object PlaybackFailureClassifier {
    fun classify(
        errorCode: Int,
        httpStatus: Int? = null,
    ): PlaybackFailure {
        if (httpStatus != null) {
            return when {
                httpStatus == 401 -> PlaybackFailure.Unauthorized
                httpStatus == 403 -> PlaybackFailure.Forbidden
                httpStatus == 404 -> PlaybackFailure.MissingMedia
                httpStatus == 408 || httpStatus >= 500 -> PlaybackFailure.Network
                else -> PlaybackFailure.Unknown
            }
        }
        return when (errorCode) {
            PlaybackException.ERROR_CODE_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> PlaybackFailure.Network

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            -> PlaybackFailure.MissingMedia

            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            -> PlaybackFailure.Source

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
            -> PlaybackFailure.Decoder

            else -> PlaybackFailure.Unknown
        }
    }
}
