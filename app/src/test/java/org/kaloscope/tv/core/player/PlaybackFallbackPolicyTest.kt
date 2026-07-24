package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFallbackPolicyTest {
    @Test
    fun `transcode mode starts with HLS while other modes start direct`() {
        assertEquals(
            PlaybackSourceKind.HlsTranscode,
            PlaybackSourcePolicy.initialSource(PlaybackMode.Transcode),
        )
        assertEquals(
            PlaybackSourceKind.Direct,
            PlaybackSourcePolicy.initialSource(PlaybackMode.Auto),
        )
        assertEquals(
            PlaybackSourceKind.Direct,
            PlaybackSourcePolicy.initialSource(PlaybackMode.Direct),
        )
    }

    @Test
    fun `auto direct playback falls back for unsupported source`() {
        assertTrue(
            PlaybackFallbackPolicy.shouldFallback(
                mode = PlaybackMode.Auto,
                sourceKind = PlaybackSourceKind.Direct,
                failure = PlaybackFailure.Source,
                fallbackAttempted = false,
            ),
        )
    }

    @Test
    fun `auto direct playback falls back for decoder failure`() {
        assertTrue(
            PlaybackFallbackPolicy.shouldFallback(
                mode = PlaybackMode.Auto,
                sourceKind = PlaybackSourceKind.Direct,
                failure = PlaybackFailure.Decoder,
                fallbackAttempted = false,
            ),
        )
    }

    @Test
    fun `direct mode never falls back`() {
        assertFalse(
            PlaybackFallbackPolicy.shouldFallback(
                mode = PlaybackMode.Direct,
                sourceKind = PlaybackSourceKind.Direct,
                failure = PlaybackFailure.Decoder,
                fallbackAttempted = false,
            ),
        )
    }

    @Test
    fun `auto does not fall back for network or access failure`() {
        for (
            failure in listOf(
                PlaybackFailure.Network,
                PlaybackFailure.Unauthorized,
                PlaybackFailure.Forbidden,
            )
        ) {
            assertFalse(
                PlaybackFallbackPolicy.shouldFallback(
                    mode = PlaybackMode.Auto,
                    sourceKind = PlaybackSourceKind.Direct,
                    failure = failure,
                    fallbackAttempted = false,
                ),
            )
        }
    }

    @Test
    fun `auto fallback happens only once`() {
        assertFalse(
            PlaybackFallbackPolicy.shouldFallback(
                mode = PlaybackMode.Auto,
                sourceKind = PlaybackSourceKind.Direct,
                failure = PlaybackFailure.Source,
                fallbackAttempted = true,
            ),
        )
        assertFalse(
            PlaybackFallbackPolicy.shouldFallback(
                mode = PlaybackMode.Auto,
                sourceKind = PlaybackSourceKind.HlsTranscode,
                failure = PlaybackFailure.Source,
                fallbackAttempted = false,
            ),
        )
    }
}
