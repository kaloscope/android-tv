package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuComment

class DanmakuOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scrollingCommentMovesLeftAsPlaybackAdvances() {
        var positionMillis by mutableLongStateOf(3_000)
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                DanmakuOverlay(
                    comments = listOf(
                        DanmakuComment(
                            id = "moving",
                            text = "Moving comment",
                            mode = "scroll",
                            color = null,
                            startMillis = 1_000,
                        ),
                    ),
                    positionMillis = positionMillis,
                    isPlaying = false,
                )
            }
        }
        val initialLeft = composeRule
            .onNodeWithTag("danmaku-comment-moving")
            .fetchSemanticsNode()
            .boundsInRoot
            .left

        composeRule.runOnIdle {
            positionMillis = 5_000
        }

        val laterLeft = composeRule
            .onNodeWithTag("danmaku-comment-moving")
            .fetchSemanticsNode()
            .boundsInRoot
            .left
        assertTrue(
            "Expected comment to move left: initial=$initialLeft later=$laterLeft",
            laterLeft < initialLeft,
        )
    }
}
