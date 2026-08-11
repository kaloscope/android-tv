package org.kaloscope.tv.feature.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
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
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.model.DanmakuBlockPolicy
import org.kaloscope.tv.core.model.DanmakuBlockType
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.SubtitleDisplayMode
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

        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertExists()
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    DanmakuBlockType.Scroll,
                    DanmakuBlockType.Top,
                ),
                DanmakuBlockPolicy.selected(harness.danmakuSettings),
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

        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertDoesNotExist()
        composeRule.onNodeWithTag("player-settings-block-types").assertIsFocused()
    }

    @Test
    fun sectionHeadingsAreSkippedByVerticalFocus() {
        val harness = DrawerHarness()
        setDrawer(harness)

        composeRule.onNodeWithTag("player-subtitle-offset-reset")
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

    @Test
    fun combinedSettingsKeepVerticalBoundaryFocusInsideDrawer() {
        val harness = DrawerHarness()
        setDrawer(harness, includeFocusableBackground = true)

        assertTopBoundaryStaysOn("简体中文")
        scrollBlockRowIntoView()
        assertBottomBoundaryStaysOnBlockRow()
    }

    @Test
    fun subtitleOnlySettingsKeepVerticalBoundaryFocusInsideDrawer() {
        val harness = DrawerHarness()
        setDrawer(
            harness = harness,
            includeDanmakuSettings = false,
            includeFocusableBackground = true,
        )

        assertTopBoundaryStaysOn("简体中文")
        composeRule.onNodeWithTag("player-subtitle-offset-reset")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
            .assertIsFocused()
    }

    @Test
    fun danmakuOnlySettingsKeepVerticalBoundaryFocusInsideDrawer() {
        val harness = DrawerHarness()
        setDrawer(
            harness = harness,
            subtitleTracks = emptyList(),
            includeFocusableBackground = true,
        )

        assertTopBoundaryStaysOn("弹幕字号")
        scrollBlockRowIntoView()
        assertBottomBoundaryStaysOnBlockRow()
    }

    @Test
    fun subtitleFontScaleUpperBoundaryDisablesIncreaseWithoutUpdating() {
        val harness = DrawerHarness(
            initialSubtitleSettings = SubtitleSettings(fontScalePercent = 200),
        )
        setDrawer(harness)
        scrollRowIntoView("player-subtitle-font-scale-row")

        composeRule.onNodeWithTag(
            testTag = "player-subtitle-font-scale-increase",
            useUnmergedTree = true,
        ).assertIsNotEnabled()
        composeRule.onNodeWithTag("player-subtitle-font-scale-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.Enter)
            }

        composeRule.runOnIdle {
            assertEquals(200, harness.subtitleSettings.fontScalePercent)
            assertEquals(0, harness.subtitleUpdateCount)
        }
    }

    @Test
    fun centerDoesNotAdjustSubtitleFontScale() {
        val harness = DrawerHarness()
        setDrawer(harness)
        scrollRowIntoView("player-subtitle-font-scale-row")

        composeRule.onNodeWithTag("player-subtitle-font-scale-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(100, harness.subtitleSettings.fontScalePercent)
            assertEquals(0, harness.subtitleUpdateCount)
        }
    }

    @Test
    fun centerDoesNotAdjustSubtitleOffset() {
        val harness = DrawerHarness(
            initialSubtitleSettings = SubtitleSettings(timeOffsetSeconds = 0.5f),
        )
        setDrawer(harness)
        scrollRowIntoView("player-subtitle-offset-row")

        composeRule.onNodeWithTag("player-subtitle-offset-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(0.5f, harness.subtitleSettings.timeOffsetSeconds)
            assertEquals(0, harness.subtitleUpdateCount)
        }
    }

    @Test
    fun subtitleDisplayModeOpensDialogAndRestoresRowFocus() {
        val harness = DrawerHarness()
        setDrawer(harness)
        scrollRowIntoView("player-subtitle-display-mode-row")

        val row = composeRule.onNodeWithTag("player-subtitle-display-mode-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }
            .assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, harness.subtitleUpdateCount) }

        row.performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("player-subtitle-display-mode-option-stroke")
            .assertIsFocused()
            .performKeyInput {
                pressKey(Key.DirectionDown)
                pressKey(Key.Enter)
            }

        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertDoesNotExist()
        row.assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(SubtitleDisplayMode.Background, harness.subtitleSettings.displayMode)
            assertEquals(1, harness.subtitleUpdateCount)
        }
    }

    @Test
    fun danmakuOpacityUsesCanonicalHorizontalStepAndIgnoresCenter() {
        val harness = DrawerHarness().apply {
            danmakuSettings = DanmakuSettings(opacityPercent = 50)
        }
        setDrawer(harness)
        scrollRowIntoView("player-danmaku-opacity-row")

        composeRule.onNodeWithTag(
            testTag = "player-danmaku-opacity-decrease",
            useUnmergedTree = true,
        ).assertIsEnabled()
        composeRule.onNodeWithTag(
            testTag = "player-danmaku-opacity-increase",
            useUnmergedTree = true,
        ).assertIsEnabled()
        composeRule.onNodeWithTag("player-danmaku-opacity-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.Enter)
            }

        composeRule.runOnIdle {
            assertEquals(75, harness.danmakuSettings.opacityPercent)
            assertEquals(1, harness.danmakuUpdateCount)
        }
    }

    @Test
    fun explicitSubtitleOffsetResetKeepsDrawerOpenAndFocused() {
        val harness = DrawerHarness(
            initialSubtitleSettings = SubtitleSettings(timeOffsetSeconds = 0.5f),
        )
        setDrawer(harness)
        scrollRowIntoView("player-subtitle-offset-reset")

        composeRule.onNodeWithTag("player-subtitle-offset-reset")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsFocused()

        composeRule.onNodeWithTag("player-settings-drawer").assertExists()
        composeRule.runOnIdle {
            assertEquals(0f, harness.subtitleSettings.timeOffsetSeconds)
            assertEquals(1, harness.subtitleUpdateCount)
        }
    }

    @Test
    fun drawerShowsPlaybackSessionHint() {
        setDrawer(DrawerHarness())

        composeRule.onNodeWithText(
            "此处调整仅对本次播放生效，不会修改全局默认值。",
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "player-session-settings-hint-icon",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "player-session-settings-hint-text",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun deepSelectedSubtitleTrackScrollsIntoViewBeforeFocus() {
        val tracks = List(24) { index ->
            SubtitleTrack(
                id = "track-$index",
                label = "Track ${index + 1}",
                url = "/track-$index.vtt",
                language = "lang-$index",
            )
        }
        setDrawer(
            harness = DrawerHarness(initialSelectedTrackId = "track-19"),
            subtitleTracks = tracks,
        )

        composeRule.onNodeWithText("Track 20")
            .assertIsSelected()
            .assertIsFocused()
    }

    private fun assertTopBoundaryStaysOn(label: String) {
        composeRule.onNodeWithText(label)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionUp) }
            .assertIsFocused()
    }

    private fun assertBottomBoundaryStaysOnBlockRow() {
        composeRule.onNodeWithTag("player-settings-block-types")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
            .assertIsFocused()
    }

    private fun scrollBlockRowIntoView() {
        composeRule.onNodeWithTag("player-settings-list")
            .performScrollToNode(hasTestTag("player-settings-block-types"))
    }

    private fun scrollRowIntoView(tag: String) {
        composeRule.onNodeWithTag("player-settings-list")
            .performScrollToNode(hasTestTag(tag))
    }

    private fun setDrawer(
        harness: DrawerHarness,
        subtitleTracks: List<SubtitleTrack> = listOf(
            SubtitleTrack("zh", "简体中文", "/zh.vtt", "zh-CN"),
            SubtitleTrack("en", "English", "/en.vtt", "en"),
        ),
        includeDanmakuSettings: Boolean = true,
        includeFocusableBackground: Boolean = false,
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                Box {
                    if (includeFocusableBackground) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            KaloscopeButton(
                                onClick = {},
                                modifier = Modifier.testTag("behind-drawer-top"),
                            ) {
                                androidx.tv.material3.Text("Behind top")
                            }
                            KaloscopeButton(
                                onClick = {},
                                modifier = Modifier.testTag("behind-drawer-bottom"),
                            ) {
                                androidx.tv.material3.Text("Behind bottom")
                            }
                        }
                    }
                    PlayerSettingsDrawer(
                        subtitleTracks = subtitleTracks,
                        selectedSubtitleTrackId = harness.selectedTrackId,
                        subtitleSettings = harness.subtitleSettings,
                        danmakuSettings = harness.danmakuSettings.takeIf {
                            includeDanmakuSettings
                        },
                        onSelectSubtitleTrack = { harness.selectedTrackId = it },
                        onChangeSubtitleSettings = {
                            harness.subtitleUpdateCount += 1
                            harness.subtitleSettings = it
                        },
                        onChangeDanmakuSettings = {
                            harness.danmakuUpdateCount += 1
                            harness.danmakuSettings = it
                        },
                        onDismiss = { harness.dismissCount += 1 },
                    )
                }
            }
        }
    }

    private class DrawerHarness(
        initialSubtitleSettings: SubtitleSettings = SubtitleSettings(),
        initialSelectedTrackId: String? = "zh",
    ) {
        var selectedTrackId by mutableStateOf(initialSelectedTrackId)
        var subtitleSettings by mutableStateOf(initialSubtitleSettings)
        var danmakuSettings by mutableStateOf(DanmakuSettings())
        var subtitleUpdateCount = 0
        var danmakuUpdateCount = 0
        var dismissCount = 0
    }
}
