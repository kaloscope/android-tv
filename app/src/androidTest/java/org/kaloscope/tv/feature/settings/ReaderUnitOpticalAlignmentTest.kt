package org.kaloscope.tv.feature.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TvSettings

class ReaderUnitOpticalAlignmentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readingSettingUnitSitsOneDpAboveTitleBaseline() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Reading,
                    ),
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

        composeRule.onNode(hasClickAction() and hasText("字号")).performScrollTo()
        val titleNode = composeRule.onNodeWithText("字号", useUnmergedTree = true)
        val unitNode = composeRule.onNodeWithText("(sp)", useUnmergedTree = true)
        val title = textLayoutFor("字号")
        val unit = textLayoutFor("(sp)")
        val titleBaseline = titleNode.fetchSemanticsNode().boundsInRoot.top + title.firstBaseline
        val unitBaseline = unitNode.fetchSemanticsNode().boundsInRoot.top + unit.firstBaseline
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertEquals(1f * density, titleBaseline - unitBaseline, 0.5f)
    }

    private fun textLayoutFor(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(results)
            }
        return results.single()
    }

    private fun session() = Session(
        server = SavedServer("server-id", "Test Server", "http://127.0.0.1:8000"),
        token = "token",
        user = SessionUser(1, "tv_user", "user"),
    )
}
