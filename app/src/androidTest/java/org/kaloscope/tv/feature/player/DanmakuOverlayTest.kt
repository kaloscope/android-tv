package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.media3.exoplayer.ExoPlayer
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.DanmakuSettings

class DanmakuOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var player: ExoPlayer? = null

    @After
    fun tearDown() {
        composeRule.runOnIdle {
            player?.release()
            player = null
        }
    }

    @Test
    fun akDanmakuHostIsDisplayed() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                val context = LocalContext.current
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().also {
                        player = it
                    }
                }
                AkDanmakuOverlay(
                    player = exoPlayer,
                    comments = listOf(
                        DanmakuComment(
                            id = "comment",
                            text = "AkDanmaku comment",
                            mode = "scroll",
                            color = null,
                            startMillis = 1_000,
                        ),
                    ),
                    settings = DanmakuSettings(),
                )
            }
        }

        composeRule.onNodeWithTag("ak-danmaku-overlay").assertIsDisplayed()
    }
}
