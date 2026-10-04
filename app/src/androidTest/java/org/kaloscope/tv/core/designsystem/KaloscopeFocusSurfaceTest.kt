package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.test.assertContentCardFocusOutline
import org.kaloscope.tv.test.captureToImage

class KaloscopeFocusSurfaceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gridOutlineRestoresAfterInterruptedFocusAndDefaultSurfaceKeepsItsStyle() {
        var gridClicks = 0
        var defaultClicks = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KaloscopeFocusSurface(
                        onClick = { gridClicks += 1 },
                        shape = RoundedCornerShape(15.dp),
                        focusedContainerColor = ContentCardFocused,
                        focusScale = BrowseLayoutTokens.GridCardFocusScale,
                        focusScaleEdgeClearance = BrowseLayoutTokens.GridCardFocusEdgeClearance,
                        variant = KaloscopeFocusSurfaceVariant.GridCard,
                        modifier = Modifier.width(160.dp).testTag("grid-card"),
                    ) {
                        Box(Modifier.fillMaxWidth().height(100.dp).testTag("grid-card-content"))
                    }
                    KaloscopeFocusSurface(
                        onClick = { defaultClicks += 1 },
                        shape = RoundedCornerShape(15.dp),
                        focusedContainerColor = ContentCardFocused,
                        modifier = Modifier.width(160.dp).testTag("default-surface"),
                    ) {
                        Box(Modifier.fillMaxWidth().height(100.dp))
                    }
                }
            }
        }

        val grid = composeRule.onNodeWithTag("grid-card")
        val defaultSurface = composeRule.onNodeWithTag("default-surface")
        val content = composeRule.onNodeWithTag("grid-card-content", useUnmergedTree = true)
        defaultSurface.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(240)
        val restingContent = content.fetchSemanticsNode().boundsInRoot
        grid.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(240)
        val focusedContent = content.fetchSemanticsNode().boundsInRoot
        assertEquals(restingContent.width * 1.02f, focusedContent.width, 1f)
        assertEquals(restingContent.height * 1.02f, focusedContent.height, 1f)
        grid.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(48)
        defaultSurface.assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.mainClock.advanceTimeBy(600)

        grid.assertIsFocused()
        assertContentCardFocusOutline(
            label = "Restored grid card",
            bitmap = grid.captureToImage().asAndroidBitmap(),
            density = composeRule.density.density,
        )
        grid.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle {
            assertEquals(1, gridClicks)
            assertEquals(0, defaultClicks)
        }

        grid.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(600)
        defaultSurface.assertIsFocused()
        assertEquals(restingContent, content.fetchSemanticsNode().boundsInRoot)
        assertContentCardFocusOutline(
            label = "Blurred grid card",
            bitmap = grid.captureToImage().asAndroidBitmap(),
            density = composeRule.density.density,
            visible = false,
        )
        assertContentCardFocusOutline(
            label = "Default surface",
            bitmap = defaultSurface.captureToImage().asAndroidBitmap(),
            density = composeRule.density.density,
            visible = false,
        )
    }
}
