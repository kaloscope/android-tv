package org.kaloscope.tv.feature.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.NetworkDefinition

class PlayerDefinitionDrawerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun currentDefinitionIsFocusedAndCenterSelectsExactIndex() {
        var selectedIndex = -1
        setDrawer(
            selectedIndex = 2,
            onSelect = { selectedIndex = it },
        )

        composeRule.onNodeWithText("1080p")
            .assertIsSelected()
            .assertIsFocused()
        composeRule.onNodeWithText("720p")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, selectedIndex)
        }
    }

    @Test
    fun emptyDefinitionDrawerStaysDismissibleAndShowsMessage() {
        var dismissCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                PlayerDefinitionDrawer(
                    definitions = emptyList(),
                    selectedIndex = null,
                    onSelect = {},
                    onDismiss = { dismissCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("暂无可用清晰度").assertExists()
        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun definitionDrawerShowsPlaybackSessionHint() {
        setDrawer(selectedIndex = 0)

        composeRule.onNodeWithText(
            "此处调整仅对本次播放生效，不会修改全局默认值。",
        ).assertExists()
    }

    @Test
    fun deepSelectedDefinitionScrollsIntoViewBeforeFocus() {
        val definitions = List(24) { index ->
            NetworkDefinition(
                label = "Quality ${index + 1}",
                url = "https://cdn.example.test/$index.m3u8",
            )
        }
        composeRule.setContent {
            KaloscopeTheme {
                PlayerDefinitionDrawer(
                    definitions = definitions,
                    selectedIndex = 19,
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Quality 20")
            .assertIsSelected()
            .assertIsFocused()
    }

    private fun setDrawer(
        selectedIndex: Int?,
        onSelect: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                PlayerDefinitionDrawer(
                    definitions = listOf(
                        NetworkDefinition("480p", "https://cdn.example.test/480.m3u8"),
                        NetworkDefinition("720p", "https://cdn.example.test/720.m3u8"),
                        NetworkDefinition("1080p", "https://cdn.example.test/1080.m3u8"),
                    ),
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                    onDismiss = {},
                )
            }
        }
    }
}
