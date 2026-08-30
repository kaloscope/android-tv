package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.test.captureToImage

class TvSearchFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun focusWaitsForCenterBeforeCreatingEditor() {
        composeRule.setContent {
            KaloscopeTheme {
                TvSearchField(
                    value = "",
                    hint = "搜索",
                    onValueChange = {},
                    onSearch = {},
                    modifier = Modifier.testTag("search-field"),
                )
            }
        }

        composeRule.onNodeWithTag("search-field")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .assertHasClickAction()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)

        composeRule.onNodeWithTag("search-field")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("search-field").assertIsFocused()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
    }

    @Test
    fun rightNavigatesOnlyBeforeEditing() {
        var rightMoves = 0
        composeRule.setContent {
            KaloscopeTheme {
                TvSearchField(
                    value = "query",
                    hint = "搜索",
                    onValueChange = {},
                    onSearch = {},
                    onMoveRight = { rightMoves += 1 },
                    modifier = Modifier.testTag("search-field"),
                )
            }
        }

        composeRule.onNodeWithTag("search-field")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.runOnIdle {
            assertEquals(1, rightMoves)
        }

        composeRule.onNodeWithTag("search-field")
            .performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.DirectionRight)
            }
            .assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, rightMoves)
        }
    }

    @Test
    fun hintKeepsSameAppearanceWhenFocusChanges() {
        composeRule.setContent {
            KaloscopeTheme {
                Column {
                    TvSearchField(
                        value = "",
                        hint = "搜索",
                        onValueChange = {},
                        onSearch = {},
                        modifier = Modifier.testTag("search-field"),
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .focusable()
                            .testTag("other-focus"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("search-field")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        val focusedHint = composeRule.onNodeWithText(
            text = "搜索",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()
        composeRule.onNodeWithTag("other-focus")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        val unfocusedHint = composeRule.onNodeWithText(
            text = "搜索",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        val textPixelsMatch = (4 until focusedHint.width).all { x ->
            (0 until focusedHint.height).all { y ->
                focusedHint.getPixel(x, y) == unfocusedHint.getPixel(x, y)
            }
        }
        assertTrue(textPixelsMatch)
    }

    @Test
    fun cursorIsWhite() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                TvSearchField(
                    value = "",
                    hint = "",
                    onValueChange = {},
                    onSearch = {},
                    modifier = Modifier.testTag("search-field"),
                )
            }
        }

        composeRule.onNodeWithTag("search-field")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.mainClock.advanceTimeBy(100)
        val field = composeRule.onNodeWithTag("search-field")
            .captureToImage()
            .asAndroidBitmap()
        val hasWhitePixel = (0 until field.width).any { x ->
            (0 until field.height).any { y ->
                val pixel = field.getPixel(x, y)
                android.graphics.Color.red(pixel) > 245 &&
                    android.graphics.Color.green(pixel) > 245 &&
                    android.graphics.Color.blue(pixel) > 245
            }
        }

        assertTrue(hasWhitePixel)
    }
}
