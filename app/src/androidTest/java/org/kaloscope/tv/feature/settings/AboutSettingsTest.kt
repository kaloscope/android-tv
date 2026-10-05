package org.kaloscope.tv.feature.settings

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kaloscope.tv.BuildConfig
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.UpdateFailure
import org.kaloscope.tv.core.model.AppUpdateRelease
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TvSettings

@RunWith(AndroidJUnit4::class)
class AboutSettingsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun aboutShowsVersionAndRemoteCanReachCheckThenReturnToMenu() {
        var checks = 0
        content(actions = AppUpdateActions(check = { checks++ }))
        composeRule.onNodeWithTag("settings-section-icon-about", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("当前版本：${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-section-about").assertIsFocused()
        composeRule.onNodeWithTag("settings-section-about").performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("about-link-feedback").assertIsFocused()
        composeRule.onNodeWithTag("about-link-feedback").performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("about-link-project").assertIsFocused()
        composeRule.onNodeWithTag("about-link-project").performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("update-check").assertIsFocused()
        composeRule.onNodeWithTag("update-check").performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(1, checks) }
        composeRule.onNodeWithTag("update-check").performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithTag("settings-section-about").assertIsFocused()
    }

    @Test
    fun updateConfirmationTrapsFocusAndBackRestoresCheckButton() {
        var state by mutableStateOf(AppUpdateUiState())
        var downloads = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestSettingsScreen(
                    state,
                    AppUpdateActions(
                        check = { state = AppUpdateUiState(AppUpdatePhase.Available, release, confirmationOpen = true) },
                        dismissConfirmation = { state = state.copy(confirmationOpen = false) },
                        confirmDownload = { downloads++ },
                    ),
                )
            }
        }
        activate("update-check")
        composeRule.onNodeWithTag("confirm-dialog-cancel").assertIsFocused()
        composeRule.onNodeWithTag("confirm-dialog-cancel").performKeyInput {
            pressKey(Key.DirectionUp)
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionLeft)
        }
        composeRule.onNodeWithTag("confirm-dialog-cancel").assertIsFocused()
        pressBack()
        composeRule.onNodeWithTag("update-check").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, downloads) }
    }

    @Test
    fun downloadRequiresConfirmAndExposesProgressCancellationAndRetryError() {
        var state by mutableStateOf(AppUpdateUiState(AppUpdatePhase.Available, release))
        var downloads = 0
        var cancellations = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestSettingsScreen(
                    state,
                    AppUpdateActions(
                        promptDownload = { state = state.copy(confirmationOpen = true) },
                        confirmDownload = {
                            downloads++
                            state = state.copy(phase = AppUpdatePhase.Downloading, progress = 42, confirmationOpen = false)
                        },
                        cancel = {
                            cancellations++
                            state = state.copy(phase = AppUpdatePhase.Available, error = AppError.Update(UpdateFailure.Integrity))
                        },
                    ),
                )
            }
        }
        activate("update-download")
        composeRule.runOnIdle { assertEquals(0, downloads) }
        composeRule.onNodeWithTag("confirm-dialog-cancel").performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("confirm-dialog-confirm").assertIsFocused()
        composeRule.onNodeWithTag("confirm-dialog-confirm").performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("update-download").assertTextContains("正在下载并校验：42%")
        composeRule.onNodeWithTag("update-download").assertIsFocused()
        composeRule.onNodeWithTag("update-download").performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("update-download").assertTextContains("安装包不完整或校验失败，请重新下载。")
        composeRule.onNodeWithText("下载并安装").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, downloads)
            assertEquals(1, cancellations)
        }
    }

    @Test
    fun navigatingAwayCancelsPendingOperation() {
        var left = 0
        content(
            state = AppUpdateUiState(AppUpdatePhase.Downloading, release, progress = 10),
            actions = AppUpdateActions(leave = { left++ }),
        )
        composeRule.onNodeWithTag("settings-section-serveraccount")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.runOnIdle { assertEquals(1, left) }
    }

    @Test
    fun manualInstallNoticeContainsFocusAndBackRestoresSaveButton() {
        var state by mutableStateOf(AppUpdateUiState(
            phase = AppUpdatePhase.Ready,
            release = release,
            savedDownloadName = "kaloscope-tv-0.3.23.apk",
            manualInstallNoticeOpen = true,
        ))
        composeRule.setContent {
            KaloscopeTheme {
                TestSettingsScreen(
                    state,
                    AppUpdateActions(
                        dismissManualInstallNotice = { state = state.copy(manualInstallNoticeOpen = false) },
                    ),
                )
            }
        }
        composeRule.onNodeWithTag("kaloscope-confirm-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("知道了").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm-dialog-cancel").assertDoesNotExist()
        composeRule.onNodeWithTag("confirm-dialog-confirm").assertIsFocused()
        composeRule.onNodeWithTag("confirm-dialog-confirm").performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionUp)
            pressKey(Key.DirectionDown)
        }
        composeRule.onNodeWithTag("confirm-dialog-confirm").assertIsFocused()
        pressBack()
        composeRule.onNodeWithTag("kaloscope-confirm-dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("update-save-downloads").assertIsFocused()
        composeRule.onNodeWithTag("update-save-downloads").assertTextContains("Download/kaloscope-tv-0.3.23.apk")
    }

    @Test
    fun projectLinksShowQrAndBackRestoresTheirTrigger() {
        content()
        composeRule.onNodeWithText("项目与支持").assertIsDisplayed()
        composeRule.onNodeWithText("© 2026 Kaloscope · MIT License").assertIsDisplayed()
        listOf(
            "project" to "https://github.com/kaloscope/android-tv",
            "releases" to "https://github.com/kaloscope/android-tv/releases",
            "feedback" to "https://github.com/kaloscope/android-tv/issues",
            "license" to "https://github.com/kaloscope/android-tv/blob/main/LICENSE",
        ).forEach { (tag, url) ->
            activate("about-link-$tag")
            composeRule.onNodeWithTag("about-link-qr").assertIsDisplayed()
            composeRule.onNodeWithText(url).assertIsDisplayed()
            composeRule.onNodeWithTag("about-link-close").assertIsFocused()
            composeRule.onNodeWithTag("about-link-close").performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionUp)
                pressKey(Key.DirectionDown)
            }
            composeRule.onNodeWithTag("about-link-close").assertIsFocused()
            pressBack()
            composeRule.onNodeWithTag("about-link-panel").assertDoesNotExist()
            composeRule.onNodeWithTag("about-link-$tag").assertIsFocused()
        }
    }

    private fun activate(tag: String) {
        composeRule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
    }

    private fun content(
        state: AppUpdateUiState = AppUpdateUiState(),
        actions: AppUpdateActions = AppUpdateActions(),
    ) {
        composeRule.setContent { KaloscopeTheme { TestSettingsScreen(state, actions) } }
    }

    @androidx.compose.runtime.Composable
    private fun TestSettingsScreen(state: AppUpdateUiState, actions: AppUpdateActions) {
        var section by androidx.compose.runtime.remember { mutableStateOf(SettingsSection.About) }
        SettingsScreen(
            session = Session(SavedServer("sample", "Sample", "https://example.com"), "sample-token", SessionUser(1, "Sample", "user")),
            state = SettingsUiState.Content(TvSettings(), section),
            updateState = state,
            updateActions = actions,
            onRetry = {},
            onSelectSection = { section = it },
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

    private val release = AppUpdateRelease("0.3.23", "https://example.com/app.apk", 3, "a".repeat(64), null)
}
