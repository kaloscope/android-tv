package org.kaloscope.tv.feature.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack

class PlayerSubtitleSettingsDrawerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedTrackHasInitialFocusAndSettingsStayInSessionModel() {
        var selectedTrackId: String? = "zh"
        var settings by mutableStateOf(SubtitleSettings())
        composeRule.setContent {
            KaloscopeTheme {
                PlayerSubtitleSettingsDrawer(
                    tracks = tracks(),
                    selectedTrackId = selectedTrackId,
                    settings = settings,
                    onSelectTrack = { selectedTrackId = it },
                    onChangeSettings = { settings = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("简体中文").assertIsFocused()
        composeRule.onNodeWithText("字幕字号")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.runOnIdle {
            assertEquals(105, settings.fontScalePercent)
        }
    }

    @Test
    fun disabledSubtitlesFocusOffAndCenterSelectsTrack() {
        var selectedTrackId: String? = null
        composeRule.setContent {
            KaloscopeTheme {
                PlayerSubtitleSettingsDrawer(
                    tracks = tracks(),
                    selectedTrackId = selectedTrackId,
                    settings = SubtitleSettings(enabled = false),
                    onSelectTrack = { selectedTrackId = it },
                    onChangeSettings = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("关闭字幕").assertIsFocused()
        composeRule.onNodeWithText("English")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals("en", selectedTrackId)
        }
    }

    private fun tracks() = listOf(
        SubtitleTrack("zh", "简体中文", "/zh.vtt", "zh-CN"),
        SubtitleTrack("en", "English", "/en.vtt", "en"),
    )
}
