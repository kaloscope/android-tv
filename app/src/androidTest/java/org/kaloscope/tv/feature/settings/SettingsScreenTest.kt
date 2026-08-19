package org.kaloscope.tv.feature.settings

import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeQuality

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadingUsesCenteredIndicator() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Loading,
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithTag("settings-loading-indicator").assertExists()
        composeRule.onNodeWithText("正在加载设置").assertDoesNotExist()
    }

    @Test
    fun settingsMenuUsesApprovedLabelsInOrder() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(TvSettings()),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }
        val labels = listOf(
            "播放设置",
            "弹幕设置",
            "字幕设置",
            "阅读设置",
            "外观与行为",
            "服务器与账户",
        )

        val verticalPositions = labels.map { label ->
            composeRule.onNode(hasClickAction() and hasText(label))
                .fetchSemanticsNode()
                .boundsInRoot
                .top
        }

        assertEquals(verticalPositions.sorted(), verticalPositions)
        composeRule.onNode(hasClickAction() and hasText("外观", substring = false))
            .assertDoesNotExist()
    }

    @Test
    fun playbackCategoryShowsAndSelectsTranscodeQuality() {
        var selectedQuality: TranscodeQuality? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(TvSettings()),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = { selectedQuality = it },
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(
            hasClickAction() and hasText("转码质量") and hasText("中"),
        )
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("transcode-quality-option-high").assertExists()
        composeRule.onNodeWithTag("transcode-quality-option-medium").assertIsFocused()
        composeRule.onNodeWithTag("transcode-quality-option-low").assertExists()
        composeRule.onNodeWithTag("transcode-quality-option-high")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(TranscodeQuality.High, selectedQuality)
        }
    }

    @Test
    fun readingCategoryUsesApprovedGroupLabels() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Reading,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithTag("reading-default-settings").assertExists()
        composeRule.onNodeWithText("通用").assertExists()
        composeRule.onNodeWithText("图片").assertExists()
        composeRule.onNodeWithText("文本").assertExists()
        composeRule.onNodeWithText("图片阅读").assertDoesNotExist()
        composeRule.onNodeWithText("文本阅读").assertDoesNotExist()
        composeRule.onNode(hasClickAction() and hasText("阅读模式")).assertExists()
        composeRule.onNode(hasClickAction() and hasText("背景")).assertExists()
    }

    @Test
    fun readingThemeDialogShowsAColorSwatchForEveryTheme() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Reading,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasClickAction() and hasText("背景"))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        val themeTags = listOf(
            "white",
            "cream",
            "sepia",
            "lightgray",
            "green",
            "dark",
            "slate",
            "black",
        )
        themeTags.forEachIndexed { index, themeTag ->
            val option = composeRule.onNodeWithTag("reader-theme-option-$themeTag")
                .assertIsFocused()
            composeRule.onNodeWithTag(
                testTag = "reader-theme-swatch-$themeTag",
                useUnmergedTree = true,
            ).assertExists()
            if (index != themeTags.lastIndex) {
                option.performKeyInput { pressKey(Key.DirectionDown) }
            }
        }
    }

    @Test
    fun readingThemeDialogUsesWebUiLabels() {
        setSettingsContent(TvSettings(), SettingsSection.Reading)

        composeRule.onNode(hasClickAction() and hasText("背景"))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        val themes = listOf(
            "white" to "纯白",
            "cream" to "奶油",
            "sepia" to "护眼",
            "lightgray" to "浅灰",
            "green" to "豆绿",
            "dark" to "深色",
            "slate" to "蓝灰",
            "black" to "夜间",
        )
        themes.forEachIndexed { index, (themeTag, label) ->
            val option = composeRule.onNodeWithTag("reader-theme-option-$themeTag")
                .assertIsFocused()
                .assertTextEquals(label)
            if (index != themes.lastIndex) {
                option.performKeyInput { pressKey(Key.DirectionDown) }
            }
        }
    }

    @Test
    fun readingFontDialogUsesBuiltInFontFamilyLabels() {
        setSettingsContent(TvSettings(), SettingsSection.Reading)

        composeRule.onNode(hasClickAction() and hasText("字体"))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        val labels = listOf("默认", "SansSerif", "Serif", "Cursive", "Monospace")
        labels.forEachIndexed { index, label ->
            val option = composeRule.onNode(
                hasClickAction() and hasTextExactly(label) and isFocused(),
            ).assertExists()
            if (index != labels.lastIndex) {
                option.performKeyInput { pressKey(Key.DirectionDown) }
            }
        }
    }

    @Test
    fun readingGroupLabelsUseReadableForegroundColor() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Reading,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        listOf("通用", "图片", "文本").forEach { label ->
            assertEquals(OnBackground, textLayoutFor(label).layoutInput.style.color)
        }
    }

    @Test
    fun readingSettingsSeparateParenthesizedUnitsFromValues() {
        setSettingsContent(TvSettings(), SettingsSection.Reading)

        listOf(
            listOf("字号", "(sp)", "28", "28 sp"),
            listOf("段间距", "(em)", "1.0", "1.0 em"),
            listOf("左右留白", "(dp)", "48", "48 dp"),
        ).forEach { (title, unit, value, combinedValue) ->
            val row = composeRule.onNode(hasClickAction() and hasText(title))
                .performScrollTo()

            row.assert(hasText(unit, substring = false))
                .assert(hasText(value, substring = false))
            composeRule.onNodeWithText(combinedValue, useUnmergedTree = true)
                .assertDoesNotExist()
        }

        composeRule.onNode(hasClickAction() and hasText("字号")).performScrollTo()
        val title = textLayoutFor("字号")
        val unit = textLayoutFor("(sp)")

        assertEquals(12f, unit.layoutInput.style.fontSize.value, 0f)
        assertEquals(FontWeight.Light, unit.layoutInput.style.fontWeight)
        assertTrue(unit.layoutInput.style.fontSize.value < title.layoutInput.style.fontSize.value)
        assertTrue(unit.layoutInput.style.color.alpha < title.layoutInput.style.color.alpha)
    }

    @Test
    fun readingFontSizeAdjustsOnlyAfterEnteringAdjustmentMode() {
        var updatedFontSize: Int? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Reading,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTextReaderSettings = { updatedFontSize = it.fontSizeSp },
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val fontSizeRow = composeRule.onNode(hasClickAction() and hasText("字号"))
        fontSizeRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.runOnIdle { assertEquals(30, updatedFontSize) }
    }

    @Test
    fun danmakuOpacityUsesAdjustmentModeAndCanonicalStep() {
        var updatedOpacity: Int? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(
                            danmaku = DanmakuSettings(opacityPercent = 50),
                        ),
                        section = SettingsSection.Danmaku,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = { updatedOpacity = it.opacityPercent },
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val opacityRow = composeRule.onNode(
            hasClickAction() and hasText("弹幕透明度"),
        )
        opacityRow.performScrollTo()
        composeRule.onNodeWithTag(
            testTag = "danmaku-opacity-decrease",
            useUnmergedTree = true,
        ).assertIsEnabled()
        composeRule.onNodeWithTag(
            testTag = "danmaku-opacity-increase",
            useUnmergedTree = true,
        ).assertIsEnabled()

        opacityRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.runOnIdle { assertEquals(null, updatedOpacity) }

        opacityRow
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.runOnIdle { assertEquals(75, updatedOpacity) }
    }

    @Test
    fun readingAdjustmentModeUsesAccentSurfaceWhileFocused() {
        composeRule.mainClock.autoAdvance = false
        setSettingsContent(TvSettings(), SettingsSection.Reading)

        val fontSizeRow = composeRule.onNode(hasClickAction() and hasText("字号"))
        fontSizeRow.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(500)
        assertCenterColor(
            label = "focused adjustment row",
            expected = AndroidColor.rgb(0xE8, 0xED, 0xF4),
            actual = fontSizeRow.captureToImage().asAndroidBitmap(),
        )

        fontSizeRow.performKeyInput { pressKey(Key.Enter) }
        composeRule.mainClock.advanceTimeBy(500)
        assertCenterColor(
            label = "active adjustment row",
            expected = AndroidColor.rgb(0x28, 0x35, 0x5F),
            actual = fontSizeRow.captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun readerMinimumRendersDecreaseArrowDisabled() {
        setSettingsContent(
            settings = TvSettings(
                textReader = TextReaderSettings(fontSizeSp = 20),
            ),
            section = SettingsSection.Reading,
        )

        val fontSizeRow = composeRule.onNode(hasClickAction() and hasText("字号"))
        fontSizeRow.performScrollTo().assertIsEnabled()

        val decrease = composeRule.onNodeWithTag(
            testTag = "reader-font-size-decrease",
            useUnmergedTree = true,
        ).assertIsNotEnabled().fetchSemanticsNode().boundsInRoot
        val increase = composeRule.onNodeWithTag(
            testTag = "reader-font-size-increase",
            useUnmergedTree = true,
        ).assertIsEnabled().fetchSemanticsNode().boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        listOf(decrease, increase).forEach { arrow ->
            assertEquals(14f * density, arrow.width, 0.5f)
            assertEquals(18f * density, arrow.height, 0.5f)
        }
    }

    @Test
    fun allReaderAdjustmentRowsDisableDecreaseAtMinimums() {
        setSettingsContent(
            settings = TvSettings(
                textReader = TextReaderSettings(
                    fontSizeSp = 20,
                    lineHeight = 1.4f,
                    paragraphSpacingEm = 0f,
                    horizontalPaddingDp = 0,
                ),
            ),
            section = SettingsSection.Reading,
        )

        listOf(
            "reader-font-size",
            "reader-line-height",
            "reader-paragraph-spacing",
            "reader-horizontal-padding",
        ).forEach { tagPrefix ->
            composeRule.onNodeWithTag(
                testTag = "$tagPrefix-decrease",
                useUnmergedTree = true,
            ).assertIsNotEnabled()
            composeRule.onNodeWithTag(
                testTag = "$tagPrefix-increase",
                useUnmergedTree = true,
            ).assertIsEnabled()
        }
    }

    @Test
    fun readerMinimumDoesNotInvokeDecrease() {
        var updates = 0
        setSettingsContent(
            settings = TvSettings(
                textReader = TextReaderSettings(fontSizeSp = 20),
            ),
            section = SettingsSection.Reading,
            onTextReaderSettings = { updates += 1 },
        )

        val fontSizeRow = composeRule.onNode(hasClickAction() and hasText("字号"))
        fontSizeRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.DirectionLeft)
            }

        composeRule.runOnIdle { assertEquals(0, updates) }
        fontSizeRow.assertIsFocused().assertIsSelected()
    }

    @Test
    fun subtitleMaximumRendersIncreaseArrowDisabled() {
        setSettingsContent(
            settings = TvSettings(
                subtitle = SubtitleSettings(fontScalePercent = 200),
            ),
            section = SettingsSection.Subtitle,
        )

        composeRule.onNode(hasClickAction() and hasText("字幕字号"))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag(
            testTag = "subtitle-font-scale-decrease",
            useUnmergedTree = true,
        ).assertIsEnabled()
        composeRule.onNodeWithTag(
            testTag = "subtitle-font-scale-increase",
            useUnmergedTree = true,
        ).assertIsNotEnabled()
    }

    @Test
    fun allSubtitleAdjustmentRowsDisableIncreaseAtMaximums() {
        setSettingsContent(
            settings = TvSettings(
                subtitle = SubtitleSettings(
                    timeOffsetSeconds = 3_600f,
                    fontScalePercent = 200,
                    verticalPositionPercent = 15,
                ),
            ),
            section = SettingsSection.Subtitle,
        )

        listOf(
            "subtitle-font-scale",
            "subtitle-vertical-position",
            "subtitle-time-offset",
        ).forEach { tagPrefix ->
            composeRule.onNodeWithTag(
                testTag = "$tagPrefix-decrease",
                useUnmergedTree = true,
            ).assertIsEnabled()
            composeRule.onNodeWithTag(
                testTag = "$tagPrefix-increase",
                useUnmergedTree = true,
            ).assertIsNotEnabled()
        }
    }

    @Test
    fun subtitleMaximumDoesNotInvokeIncrease() {
        var updates = 0
        setSettingsContent(
            settings = TvSettings(
                subtitle = SubtitleSettings(fontScalePercent = 200),
            ),
            section = SettingsSection.Subtitle,
            onSubtitleSettings = { updates += 1 },
        )

        val fontScaleRow = composeRule.onNode(hasClickAction() and hasText("字幕字号"))
        fontScaleRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.DirectionRight)
            }

        composeRule.runOnIdle { assertEquals(0, updates) }
        fontScaleRow.assertIsFocused().assertIsSelected()
    }

    @Test
    fun appearanceAndBehaviorSelectsAccentAndRestoresRowFocus() {
        var selectedAccent: AccentColor? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Behavior,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onAccentColor = { selectedAccent = it },
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val accentRow = composeRule.onNode(hasClickAction() and hasText("强调色"))
        val startPageRow = composeRule.onNode(hasClickAction() and hasText("默认启动页"))
        assertTrue(
            accentRow.fetchSemanticsNode().boundsInRoot.top <
                startPageRow.fetchSemanticsNode().boundsInRoot.top,
        )
        composeRule.onNode(
            hasClickAction() and
                hasAnyDescendant(hasText("蓝色")) and
                hasAnyDescendant(hasTestTag("choice-setting-indicator")),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNode(hasClickAction() and hasText("蓝色  ›")).assertDoesNotExist()
        accentRow
            .assertExists()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("accent-option-blue")
            .assertIsSelected()
            .assertIsFocused()
        for (accent in AccentColor.entries) {
            composeRule.onNodeWithTag(
                testTag = "accent-swatch-${accent.name.lowercase()}",
                useUnmergedTree = true,
            )
                .assertExists()
        }
        val accentLabels = mapOf(
            AccentColor.Blue to "蓝色",
            AccentColor.Purple to "紫色",
            AccentColor.Orange to "橙色",
            AccentColor.Yellow to "黄色",
            AccentColor.Green to "绿色",
        )
        for ((accent, label) in accentLabels) {
            composeRule.onNodeWithTag("accent-option-${accent.name.lowercase()}")
                .assertTextEquals(label)
        }
        composeRule.onNodeWithTag("accent-option-purple")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        accentRow.assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(AccentColor.Purple, selectedAccent)
        }
    }

    @Test
    fun appearanceAccentDialogBackDismissesWithoutChangingSelection() {
        var selectedAccent: AccentColor? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(accentColor = AccentColor.Green),
                        section = SettingsSection.Behavior,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onAccentColor = { selectedAccent = it },
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val accentRow = composeRule.onNode(hasClickAction() and hasText("强调色"))
        accentRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("accent-option-green").assertIsFocused()

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("accent-option-green").assertDoesNotExist()
        accentRow.assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(null, selectedAccent)
        }
    }

    @Test
    fun savingStateKeepsAccentChoiceInteractive() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Behavior,
                        isSaving = true,
                    ),
                    requestInitialFocus = false,
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onAccentColor = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val accentRow = composeRule.onNode(hasClickAction() and hasText("强调色"))
        accentRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("accent-option-blue")
            .assertExists()
            .assertIsFocused()
    }

    @Test
    fun playbackCategoryHasInitialFocusAndChoiceUpdatesSetting() {
        var selectedMode: PlaybackMode? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(TvSettings()),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = { selectedMode = it },
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasClickAction() and hasText("播放设置")).assertIsFocused()
        composeRule.onNodeWithText("默认播放模式")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("playback-mode-option-auto")
            .assertIsSelected()
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
            .assertIsFocused()
        composeRule.onNodeWithTag("playback-mode-option-direct")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(PlaybackMode.Direct, selectedMode)
        }
    }

    @Test
    fun settingsMenuUsesLocalIconsForEverySection() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(TvSettings()),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        listOf(
            "settings-section-icon-playback",
            "settings-section-icon-danmaku",
            "settings-section-icon-subtitle",
            "settings-section-icon-behavior",
            "settings-section-icon-server-account",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun menuSelectionAndSettingFocusRemainIndependent() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Playback,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasClickAction() and hasText("播放设置")).assertIsSelected()
        composeRule.onNodeWithText("默认播放模式")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.onNode(hasClickAction() and hasText("播放设置"))
            .assertIsSelected()
            .assertIsNotFocused()
    }

    @Test
    fun focusingNextMenuCategorySelectsItWithoutCenter() {
        var state by mutableStateOf(
            SettingsUiState.Content(
                settings = TvSettings(),
                section = SettingsSection.Playback,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = state,
                    onRetry = {},
                    onSelectSection = { section ->
                        state = state.copy(section = section)
                    },
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasClickAction() and hasText("播放设置"))
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNode(hasClickAction() and hasText("弹幕设置"))
            .assertIsFocused()
            .assertIsSelected()
        composeRule.onNodeWithText("默认开启弹幕").assertExists()
    }

    @Test
    fun movingLeftReturnsToTheSelectedMenuCategory() {
        var state by mutableStateOf(
            SettingsUiState.Content(
                settings = TvSettings(),
                section = SettingsSection.Danmaku,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = state,
                    onRetry = {},
                    onSelectSection = { section ->
                        state = state.copy(section = section)
                    },
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("默认开启弹幕")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNode(hasClickAction() and hasText("弹幕设置"))
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun serverCategoryExposesRealAccountActions() {
        var manages = 0
        var tests = 0
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.ServerAccount,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = { tests += 1 },
                    onManageServers = { manages += 1 },
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("当前服务器").assertDoesNotExist()
        composeRule.onNodeWithText("当前账号").assertDoesNotExist()
        composeRule.onNodeWithText("家庭服务器").assertExists()
        composeRule.onNodeWithText("http://127.0.0.1:8000").assertExists()
        composeRule.onNodeWithText("tv_user").assertExists()
        composeRule.onNode(
            hasClickAction() and
                hasText("测试连接") and
                hasText("http://127.0.0.1:8000"),
        ).assertExists()
        composeRule.onNode(
            hasClickAction() and
                hasText("切换或添加服务器") and
                hasText("家庭服务器"),
        ).assertExists()
        composeRule.onNode(
            hasClickAction() and hasText("退出登录") and hasText("tv_user"),
        ).assertExists()
        composeRule.onNodeWithText("切换或添加服务器")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("测试连接")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, manages)
            assertEquals(1, tests)
        }
    }

    @Test
    fun logoutUsernameUsesReadableForegroundColor() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.ServerAccount,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        assertEquals(OnBackground, textLayoutFor("tv_user").layoutInput.style.color)
    }

    @Test
    fun serverActionsScrollIntoViewWithDpadAtMainShellHeight() {
        var manages = 0
        var logouts = 0
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
                    SettingsScreen(
                        session = session(),
                        state = SettingsUiState.Content(
                            settings = TvSettings(),
                            section = SettingsSection.ServerAccount,
                        ),
                        onRetry = {},
                        onSelectSection = {},
                        onPlaybackMode = {},
                        onTranscodeQuality = {},
                        onAutoplayNext = {},
                        onDanmakuSettings = {},
                        onSubtitleSettings = {},
                        onStartPage = {},
                        onTestConnection = {},
                        onManageServers = { manages += 1 },
                        onLogout = { logouts += 1 },
                    )
                }
            }
        }

        composeRule.onNodeWithText("测试连接")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("切换或添加服务器")
            .assertIsFocused()
            .assertIsDisplayed()
            .performKeyInput { pressKey(Key.Enter) }
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("退出登录")
            .assertIsFocused()
            .assertIsDisplayed()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, manages)
            assertEquals(1, logouts)
        }
    }

    @Test
    fun logoutRequiresConfirmationAndRestoresFocusAfterCancel() {
        var logouts = 0
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.ServerAccount,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = { logouts += 1 },
                )
            }
        }
        val logout = composeRule.onNode(
            hasClickAction() and hasText("退出登录") and hasText("tv_user"),
        )

        logout
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("kaloscope-confirm-dialog").assertExists()
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("kaloscope-confirm-dialog").assertDoesNotExist()
        logout.assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, logouts) }

        logout.performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("confirm-dialog-cancel")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle { assertEquals(1, logouts) }
    }

    @Test
    fun loadErrorFocusesRetryAndInvokesCallback() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Error(AppError.Offline),
                    onRetry = { retries += 1 },
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("重试")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }

    @Test
    fun playbackSettingsFitMainShellViewport() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
                    SettingsScreen(
                        session = session(),
                        state = SettingsUiState.Content(TvSettings()),
                        onRetry = {},
                        onSelectSection = {},
                        onPlaybackMode = {},
                        onTranscodeQuality = {},
                        onAutoplayNext = {},
                        onDanmakuSettings = {},
                        onSubtitleSettings = {},
                        onStartPage = {},
                        onTestConnection = {},
                        onManageServers = {},
                        onLogout = {},
                    )
                }
            }
        }

        val viewport = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val firstRow = composeRule.onNodeWithText("默认播放模式")
            .fetchSemanticsNode()
            .boundsInRoot
        val lastRow = composeRule.onNodeWithText("自动播放下一集")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(lastRow.left >= viewport.left)
        assertTrue(lastRow.right <= viewport.right)
        assertTrue(lastRow.top >= viewport.top)
        assertTrue(lastRow.bottom <= viewport.bottom)
        assertTrue(lastRow.height >= firstRow.height)

        listOf(
            "自动播放下一集",
            "当前内容结束且存在下一集时自动继续。",
        ).forEach { text ->
            val layoutResults = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    it(layoutResults)
                }
            assertFalse(
                "$text must not overflow its text bounds",
                layoutResults.single().hasVisualOverflow,
            )
        }

        composeRule.onNodeWithText("设置").assertDoesNotExist()
        composeRule.onNodeWithText("仅保存在这台设备上").assertDoesNotExist()

        composeRule.onNodeWithText("默认播放模式")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
            }
        composeRule.onNodeWithText("自动播放下一集").assertIsFocused()
    }

    @Test
    fun sectionHeaderScrollsWithOverflowingOptions() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
                    SettingsScreen(
                        session = session(),
                        state = SettingsUiState.Content(
                            settings = TvSettings(),
                            section = SettingsSection.Danmaku,
                        ),
                        onRetry = {},
                        onSelectSection = {},
                        onPlaybackMode = {},
                        onTranscodeQuality = {},
                        onAutoplayNext = {},
                        onDanmakuSettings = {},
                        onSubtitleSettings = {},
                        onStartPage = {},
                        onTestConnection = {},
                        onManageServers = {},
                        onLogout = {},
                    )
                }
            }
        }
        val header = composeRule.onNode(
            hasText("弹幕设置") and !hasClickAction(),
        )
        val initialHeaderTop = header.getUnclippedBoundsInRoot().top

        composeRule.onNodeWithText("默认开启弹幕")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                repeat(5) { pressKey(Key.DirectionDown) }
            }

        composeRule.onNodeWithText("屏蔽类型")
            .assertIsFocused()
            .assertIsDisplayed()
        val scrolledHeaderTop = header.getUnclippedBoundsInRoot().top
        assertTrue(
            "Expected the section header to scroll with its options",
            scrolledHeaderTop < initialHeaderTop,
        )
    }

    @Test
    fun standaloneBooleanSettingsUseSwitchStateWithoutSelectedRowsOrLabels() {
        var state by mutableStateOf(SettingsUiState.Content(TvSettings()))
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = state,
                    requestInitialFocus = false,
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        listOf(
            SettingsSection.Playback to "自动播放下一集",
            SettingsSection.Danmaku to "默认开启弹幕",
            SettingsSection.Subtitle to "默认开启字幕",
        ).forEach { (section, title) ->
            composeRule.runOnIdle { state = state.copy(section = section) }

            composeRule.onNode(hasClickAction() and hasText(title))
                .assertIsNotSelected()
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.On,
                    ),
                )
            composeRule.onNodeWithTag(
                testTag = "setting-switch-indicator",
                useUnmergedTree = true,
            ).assertExists()
            composeRule.onNodeWithText("开启", useUnmergedTree = true).assertDoesNotExist()
            composeRule.onNodeWithText("关闭", useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun centerMovesBooleanSwitchThumbWhileKeepingRowFocus() {
        var settings by mutableStateOf(TvSettings(autoplayNext = false))
        var updates = 0
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(settings),
                    requestInitialFocus = false,
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {
                        updates += 1
                        settings = settings.copy(autoplayNext = it)
                    },
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val row = composeRule.onNode(hasClickAction() and hasText("自动播放下一集"))
        val thumb = composeRule.onNodeWithTag(
            testTag = "setting-switch-thumb",
            useUnmergedTree = true,
        )
        row.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ToggleableState,
                ToggleableState.Off,
            ),
        )
        composeRule.onNodeWithTag(
            testTag = "setting-switch-indicator",
            useUnmergedTree = true,
        ).assert(!hasClickAction())
        val uncheckedThumbLeft = thumb.getUnclippedBoundsInRoot().left

        row.performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsFocused()

        composeRule.runOnIdle { assertEquals(1, updates) }
        row.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ToggleableState,
                ToggleableState.On,
            ),
        )
        val checkedThumbLeft = thumb.getUnclippedBoundsInRoot().left
        assertTrue(checkedThumbLeft > uncheckedThumbLeft)
    }

    @Test
    fun danmakuCategoryShowsDefaultsAndUpdatesTheWholeModel() {
        var updatedSettings: DanmakuSettings? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Danmaku,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = { updatedSettings = it },
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("默认开启弹幕").assertExists()
        composeRule.onNodeWithText("弹幕字号").assertExists()
        composeRule.onNodeWithText("滚动速度").assertExists()
        composeRule.onNodeWithText("默认开启弹幕")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.runOnIdle {
            assertEquals(null, updatedSettings)
        }

        composeRule.onNodeWithText("默认开启弹幕")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(false, updatedSettings?.enabled)
        }
    }

    @Test
    fun danmakuBlockDialogBatchesSharedOptionsUntilBack() {
        var updatedSettings: DanmakuSettings? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Danmaku,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = { updatedSettings = it },
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val blockRow = composeRule.onNode(hasClickAction() and hasText("屏蔽类型"))
        blockRow
            .assertExists()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag(
            testTag = "settings-danmaku-block-scroll-checkbox-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "settings-danmaku-block-top-checkbox-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag("settings-danmaku-block-scroll")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsSelected()
        composeRule.onNodeWithTag("settings-danmaku-block-top")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsSelected()

        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertExists()
        composeRule.runOnIdle {
            assertEquals(null, updatedSettings)
        }

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertDoesNotExist()
        blockRow.assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(
                setOf(DanmakuDisplayMode.Bottom),
                updatedSettings?.visibleModes,
            )
            assertEquals(false, updatedSettings?.blockColored)
        }
        composeRule.onNodeWithText("滚动弹幕").assertDoesNotExist()
        composeRule.onNodeWithText("顶部弹幕").assertDoesNotExist()
        composeRule.onNodeWithText("底部弹幕").assertDoesNotExist()
    }

    @Test
    fun subtitleCategoryShowsDefaultsAndUpdatesTheWholeModel() {
        var updatedSettings: SubtitleSettings? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Subtitle,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = { updatedSettings = it },
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("默认开启字幕").assertExists()
        composeRule.onNodeWithText("首选语言").assertExists()
        composeRule.onNode(
            hasClickAction() and
                hasAnyDescendant(hasText("首选语言")) and
                hasAnyDescendant(hasTestTag("choice-setting-indicator")),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithText("显示样式").assertExists()
        composeRule.onNodeWithText("字幕字号").assertExists()
        composeRule.onNodeWithText("垂直位置").assertExists()
        composeRule.onNodeWithText("默认开启字幕")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.Enter)
                repeat(5) { pressKey(Key.DirectionDown) }
            }
        composeRule.onNodeWithText("时间偏移")
            .assertIsFocused()
            .assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(false, updatedSettings?.enabled)
        }
    }

    @Test
    fun adjustableSubtitleRowLeftReturnsToSelectedMenuWithoutChangingValue() {
        var state by mutableStateOf(
            SettingsUiState.Content(
                settings = TvSettings(
                    subtitle = SubtitleSettings(fontScalePercent = 100),
                ),
                section = SettingsSection.Subtitle,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = state,
                    requestInitialFocus = false,
                    onRetry = {},
                    onSelectSection = { state = state.copy(section = it) },
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = { subtitle ->
                        state = state.copy(
                            settings = state.settings.copy(subtitle = subtitle),
                        )
                    },
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("字幕字号")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNode(hasClickAction() and hasText("字幕设置")).assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(100, state.settings.subtitle.fontScalePercent)
        }
    }

    @Test
    fun subtitleAdjustmentModeConsumesDirectionsUntilCenterOrBackExits() {
        var systemBacks = 0
        var state by mutableStateOf(
            SettingsUiState.Content(
                settings = TvSettings(
                    subtitle = SubtitleSettings(fontScalePercent = 100),
                ),
                section = SettingsSection.Subtitle,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                BackHandler { systemBacks += 1 }
                SettingsScreen(
                    session = session(),
                    state = state,
                    requestInitialFocus = false,
                    onRetry = {},
                    onSelectSection = { state = state.copy(section = it) },
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = { subtitle ->
                        state = state.copy(
                            settings = state.settings.copy(subtitle = subtitle),
                        )
                    },
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val fontScaleRow = composeRule.onNodeWithText("字幕字号")
        fontScaleRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsFocused()
            .assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        fontScaleRow.assertIsFocused().assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(95, state.settings.subtitle.fontScalePercent)
        }

        fontScaleRow
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsNotSelected()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNode(hasClickAction() and hasText("字幕设置")).assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(95, state.settings.subtitle.fontScalePercent)
        }

        fontScaleRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsSelected()
            .performKeyInput { pressKey(Key.Back) }
            .assertIsFocused()
            .assertIsNotSelected()
        composeRule.runOnIdle {
            assertEquals(0, systemBacks)
        }

        fontScaleRow.performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNode(hasClickAction() and hasText("字幕设置")).assertIsFocused()
    }

    @Test
    fun subtitleLanguageBackLeavesEditingBeforeDismissingDialog() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Subtitle,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("首选语言")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("subtitle-language-selector").assertIsFocused()
        composeRule.onNodeWithTag("subtitle-language-editor").assertDoesNotExist()

        composeRule.onNodeWithTag("subtitle-language-selector")
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("subtitle-language-editor")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Back) }

        composeRule.onNodeWithTag("subtitle-language-editor").assertDoesNotExist()
        composeRule.onNodeWithTag("subtitle-language-selector").assertIsFocused()
        composeRule.onNodeWithText("保存").assertExists()

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("保存").assertDoesNotExist()
        composeRule.onNodeWithText("首选语言").assertIsFocused()
    }

    private fun textLayoutFor(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(results)
            }
        return results.single()
    }

    private fun setSettingsContent(
        settings: TvSettings,
        section: SettingsSection,
        onTextReaderSettings: (TextReaderSettings) -> Unit = {},
        onSubtitleSettings: (SubtitleSettings) -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = settings,
                        section = section,
                    ),
                    requestInitialFocus = false,
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeQuality = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = onSubtitleSettings,
                    onStartPage = {},
                    onTextReaderSettings = onTextReaderSettings,
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }
    }
}

private fun assertCenterColor(
    label: String,
    expected: Int,
    actual: android.graphics.Bitmap,
    tolerance: Int = 3,
) {
    val actualColor = actual.getPixel(actual.width / 2, actual.height / 2)
    val channelDifferences = listOf(
        kotlin.math.abs(AndroidColor.red(expected) - AndroidColor.red(actualColor)),
        kotlin.math.abs(AndroidColor.green(expected) - AndroidColor.green(actualColor)),
        kotlin.math.abs(AndroidColor.blue(expected) - AndroidColor.blue(actualColor)),
    )
    assertTrue(
        "$label expected ${Integer.toHexString(expected)} but was " +
            Integer.toHexString(actualColor),
        channelDifferences.all { it <= tolerance },
    )
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
