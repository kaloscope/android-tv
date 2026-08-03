package org.kaloscope.tv.feature.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack

class PlayerSettingsDrawerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingTrackImmediatelyReturnsThatTrack() {
        val harness = DrawerHarness()
        setDrawer(harness)

        composeRule.onNodeWithText("English")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle { assertEquals("en", harness.selectedTrackId) }
    }

    @Test
    fun blockMenuKeepsOpenWhileMultipleOptionsToggle() {
        val harness = DrawerHarness()
        setDrawer(harness)
        scrollBlockRowIntoView()

        composeRule.onNodeWithTag("player-settings-block-types")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("滚动")
            .assertIsFocused()
            .performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.DirectionDown)
            }
        composeRule.onNodeWithText("顶部")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("player-settings-block-menu").assertExists()
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    PlayerDanmakuBlockOption.Scroll,
                    PlayerDanmakuBlockOption.Top,
                ),
                PlayerDanmakuBlockPolicy.selected(harness.danmakuSettings),
            )
        }
    }

    @Test
    fun backClosesBlockMenuBeforeDrawerAndRestoresRowFocus() {
        val harness = DrawerHarness()
        setDrawer(harness)
        scrollBlockRowIntoView()

        composeRule.onNodeWithTag("player-settings-block-types")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("滚动")
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("player-settings-block-menu").assertDoesNotExist()
        composeRule.onNodeWithTag("player-settings-block-types").assertIsFocused()
    }

    @Test
    fun sectionHeadingsAreSkippedByVerticalFocus() {
        val harness = DrawerHarness()
        setDrawer(harness)

        composeRule.onNodeWithText("时间偏移")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithText("弹幕字号").assertIsFocused()
    }

    @Test
    fun collapsedBlockSummaryUsesNamesForNoneAndAllSelections() {
        val harness = DrawerHarness()
        setDrawer(harness)
        scrollBlockRowIntoView()
        composeRule.onNodeWithText("未屏蔽").assertExists()

        composeRule.runOnIdle {
            harness.danmakuSettings = DanmakuSettings(
                visibleModes = emptySet(),
                blockColored = true,
            )
        }

        composeRule.onNodeWithText("滚动、顶部、底部、彩色").assertExists()
        composeRule.onNodeWithText("全部屏蔽").assertDoesNotExist()
    }

    private fun scrollBlockRowIntoView() {
        composeRule.onNodeWithTag("player-settings-list")
            .performScrollToNode(hasTestTag("player-settings-block-types"))
    }

    private fun setDrawer(harness: DrawerHarness) {
        composeRule.setContent {
            KaloscopeTheme {
                PlayerSettingsDrawer(
                    subtitleTracks = listOf(
                        SubtitleTrack("zh", "简体中文", "/zh.vtt", "zh-CN"),
                        SubtitleTrack("en", "English", "/en.vtt", "en"),
                    ),
                    selectedSubtitleTrackId = harness.selectedTrackId,
                    subtitleSettings = harness.subtitleSettings,
                    danmakuSettings = harness.danmakuSettings,
                    onSelectSubtitleTrack = { harness.selectedTrackId = it },
                    onChangeSubtitleSettings = { harness.subtitleSettings = it },
                    onChangeDanmakuSettings = { harness.danmakuSettings = it },
                    onDismiss = { harness.dismissCount += 1 },
                )
            }
        }
    }

    private class DrawerHarness {
        var selectedTrackId by mutableStateOf<String?>("zh")
        var subtitleSettings by mutableStateOf(SubtitleSettings())
        var danmakuSettings by mutableStateOf(DanmakuSettings())
        var dismissCount = 0
    }
}
