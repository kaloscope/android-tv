package org.kaloscope.tv.feature.player

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.tv.material3.Text
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.PlaybackSourceKind
import org.kaloscope.tv.core.player.TranscodeQuality

class PlayerStatusUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun transcodeStatusShowsSelectedQuality() {
        composeRule.setContent {
            KaloscopeTheme {
                Text(
                    playbackModeLabel(
                        mode = PlaybackMode.Transcode,
                        sourceKind = PlaybackSourceKind.HlsTranscode,
                        quality = TranscodeQuality.High,
                    ),
                )
            }
        }

        composeRule.onNodeWithText("HLS 转码 高").assertExists()
    }
}
