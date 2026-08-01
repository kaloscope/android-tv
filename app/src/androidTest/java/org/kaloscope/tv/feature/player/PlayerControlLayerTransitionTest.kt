package org.kaloscope.tv.feature.player

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerControlLayerTransitionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun controlsFadeOutBeforeTheLayerIsRemoved() {
        lateinit var setLayer: (PlayerControlLayer) -> Unit
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            var layer by remember { mutableStateOf(PlayerControlLayer.Controls) }
            setLayer = { layer = it }
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black)
                        .testTag("transition-stage"),
                ) {
                    AnimatedPlayerControlLayer(layer = layer) { renderedLayer ->
                        if (renderedLayer == PlayerControlLayer.Controls) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                                    .testTag("controls-layer"),
                            )
                        }
                    }
                }
            }
        }

        assertEquals(255, centerRedChannel("transition-stage"))
        composeRule.runOnIdle { setLayer(PlayerControlLayer.Hidden) }
        composeRule.mainClock.advanceTimeBy(100)

        val halfwayRed = centerRedChannel("transition-stage")
        assertTrue("Expected a partially faded overlay, red=$halfwayRed", halfwayRed in 1..254)
        composeRule.onAllNodesWithTag("controls-layer").assertCountEquals(1)

        composeRule.mainClock.advanceTimeBy(140)
        assertEquals(0, centerRedChannel("transition-stage"))
        composeRule.onAllNodesWithTag("controls-layer").assertCountEquals(0)
    }

    @Test
    fun previewAndControlsCrossfadeThroughTheSameTransition() {
        lateinit var setLayer: (PlayerControlLayer) -> Unit
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            var layer by remember { mutableStateOf(PlayerControlLayer.Preview) }
            setLayer = { layer = it }
            MaterialTheme {
                AnimatedPlayerControlLayer(layer = layer) { renderedLayer ->
                    when (renderedLayer) {
                        PlayerControlLayer.Hidden -> Unit
                        PlayerControlLayer.Preview -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("preview-layer"),
                        )

                        PlayerControlLayer.Controls -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("controls-layer"),
                        )
                    }
                }
            }
        }

        composeRule.runOnIdle { setLayer(PlayerControlLayer.Controls) }
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onAllNodesWithTag("preview-layer").assertCountEquals(1)
        composeRule.onAllNodesWithTag("controls-layer").assertCountEquals(1)

        composeRule.mainClock.advanceTimeBy(140)
        composeRule.onAllNodesWithTag("preview-layer").assertCountEquals(0)
        composeRule.onAllNodesWithTag("controls-layer").assertCountEquals(1)
    }

    private fun centerRedChannel(tag: String): Int {
        val bitmap = composeRule.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        return AndroidColor.red(pixel)
    }
}
