package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPresentationTest {
    @Test
    fun `quality control uses selected definition without playback source`() {
        assertEquals(
            "480P 清晰 HEVC",
            playerQualityControlLabel(
                playbackModeLabel = "网络资源",
                selectedDefinitionLabel = "480P 清晰 HEVC",
            ),
        )
    }

    @Test
    fun `quality control falls back when selected definition is blank`() {
        assertEquals(
            "网络资源",
            playerQualityControlLabel(
                playbackModeLabel = "网络资源",
                selectedDefinitionLabel = "   ",
            ),
        )
    }
}
