package org.kaloscope.tv.feature.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuTextSize

class PlayerDanmakuSettingsDrawerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstRowHasFocusAndAdjustmentsStayInTheProvidedSessionModel() {
        var settings by mutableStateOf(DanmakuSettings())
        composeRule.setContent {
            KaloscopeTheme {
                PlayerDanmakuSettingsDrawer(
                    settings = settings,
                    onChange = { settings = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("开启弹幕").assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("弹幕字号")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.runOnIdle {
            assertEquals(false, settings.enabled)
            assertEquals(DanmakuTextSize.Large, settings.textSize)
        }
    }
}
