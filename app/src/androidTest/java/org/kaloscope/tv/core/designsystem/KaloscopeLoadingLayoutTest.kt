package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KaloscopeLoadingLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun indicatorStaysCenteredAndInsideSmallTvViewport() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(180.dp)
                    .testTag("loading-host"),
            ) {
                KaloscopeLoadingLayout(testTag = "loading")
            }
        }

        val host = composeRule.onNodeWithTag("loading-host")
            .fetchSemanticsNode()
            .boundsInRoot
        val indicator = composeRule.onNodeWithTag("loading-indicator")
            .assert(
                SemanticsMatcher.keyIsDefined(
                    SemanticsProperties.ProgressBarRangeInfo,
                ),
            )
            .fetchSemanticsNode()
            .boundsInRoot
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(abs(indicator.center.x - host.center.x) <= tolerance)
        assertTrue(abs(indicator.center.y - host.center.y) <= tolerance)
        assertTrue(indicator.left >= host.left)
        assertTrue(indicator.top >= host.top)
        assertTrue(indicator.right <= host.right)
        assertTrue(indicator.bottom <= host.bottom)
    }

    @Test
    fun optionalStatusMessageIsPlacedBelowTheIndicator() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .width(640.dp)
                    .height(360.dp),
            ) {
                KaloscopeLoadingLayout(
                    testTag = "loading",
                    message = "正在获取资源…",
                )
            }
        }

        val indicator = composeRule.onNodeWithTag("loading-indicator")
            .fetchSemanticsNode()
            .boundsInRoot
        val message = composeRule.onNodeWithText("正在获取资源…")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(message.top > indicator.bottom)
    }
}
