package org.kaloscope.tv.core.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFailureClassifierTest {
    @Test
    fun `parsing and decoder errors are recoverable by transcoding`() {
        assertEquals(
            PlaybackFailure.Source,
            PlaybackFailureClassifier.classify(
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            ),
        )
        assertEquals(
            PlaybackFailure.Decoder,
            PlaybackFailureClassifier.classify(
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            ),
        )
    }

    @Test
    fun `connection errors do not trigger transcoding`() {
        assertEquals(
            PlaybackFailure.Network,
            PlaybackFailureClassifier.classify(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            ),
        )
    }

    @Test
    fun `http authorization status takes priority over the media error code`() {
        assertEquals(
            PlaybackFailure.Unauthorized,
            PlaybackFailureClassifier.classify(
                errorCode = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                httpStatus = 401,
            ),
        )
    }

    @Test
    fun `http forbidden status remains a permission failure`() {
        assertEquals(
            PlaybackFailure.Forbidden,
            PlaybackFailureClassifier.classify(
                errorCode = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                httpStatus = 403,
            ),
        )
    }
}
