package org.kaloscope.tv.feature.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme

class PlayerSpeedDrawerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun currentSpeedHasFocusAndSelectionUsesExactValue() {
        var selected = 0f
        composeRule.setContent {
            KaloscopeTheme {
                PlayerSpeedDrawer(
                    speed = 1.25f,
                    onSelect = { selected = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("1.25x").assertIsFocused()
        composeRule.onNodeWithText("2.0x")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(2f, selected)
        }
    }
}
