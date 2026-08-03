package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerAuxiliaryControlPolicyTest {
    @Test
    fun `visible controls keep approved order and omit unavailable actions`() {
        assertEquals(
            listOf(
                PlayerAuxiliaryControl.Subtitle,
                PlayerAuxiliaryControl.Speed,
                PlayerAuxiliaryControl.Settings,
            ),
            PlayerAuxiliaryControlPolicy.visibleControls(
                subtitles = PlayerActionUiState(enabled = true),
                danmakus = PlayerActionUiState(enabled = false),
                quality = PlayerActionUiState(enabled = false),
                settings = PlayerActionUiState(enabled = true),
            ),
        )
    }
}
