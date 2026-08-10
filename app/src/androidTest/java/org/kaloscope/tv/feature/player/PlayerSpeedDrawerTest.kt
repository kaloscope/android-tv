package org.kaloscope.tv.feature.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.designsystem.KaloscopeButton

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

        composeRule.onNodeWithText("1.25x")
            .assertIsSelected()
            .assertIsFocused()
        composeRule.onNodeWithText("2.0x")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(2f, selected)
        }
    }

    @Test
    fun speedDrawerUsesCompactShellAndPlaybackHint() {
        setDrawer()

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val drawer = composeRule.onNodeWithTag("player-speed-drawer")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(400f * density, drawer.width, density)
        assertEquals(root.right, drawer.right, 1f)
        composeRule.onNodeWithText(
            "此处调整仅对本次播放生效，不会修改全局默认值。",
        ).assertExists()
    }

    @Test
    fun speedDrawerTrapsFocusAtAllOuterEdges() {
        setDrawer(includeFocusableBackground = true)

        composeRule.onNodeWithText("0.5x")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionUp)
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }
            .assertIsFocused()
        composeRule.onNodeWithText("2.0x")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
            .assertIsFocused()
    }

    @Test
    fun backDismissesSpeedDrawerExactlyOnce() {
        var dismissCount = 0
        setDrawer(onDismiss = { dismissCount += 1 })

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    private fun setDrawer(
        includeFocusableBackground: Boolean = false,
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                Box(Modifier.fillMaxSize()) {
                    if (includeFocusableBackground) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            KaloscopeButton(
                                onClick = {},
                                modifier = Modifier.testTag("behind-speed-top"),
                            ) {}
                            KaloscopeButton(
                                onClick = {},
                                modifier = Modifier.testTag("behind-speed-bottom"),
                            ) {}
                        }
                    }
                    PlayerSpeedDrawer(
                        speed = 1.25f,
                        onSelect = {},
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}
