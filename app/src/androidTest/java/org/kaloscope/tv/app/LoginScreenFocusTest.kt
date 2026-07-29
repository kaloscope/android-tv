package org.kaloscope.tv.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.feature.login.LoginState

class LoginScreenFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginShowsSecondWizardStepAsActive() {
        showLogin()

        composeRule.onNodeWithTag("setup-step-1").assertIsNotSelected()
        composeRule.onNodeWithTag("setup-step-2").assertIsSelected()
    }

    @Test
    fun usernameStartsInNavigationMode() {
        showLogin()

        composeRule.onNodeWithTag("login-username-selector")
            .assertIsFocused()
            .assertHasClickAction()
        composeRule.onNodeWithTag("login-username-editor").assertDoesNotExist()
    }

    @Test
    fun usernameReturnsToNavigationModeOnBack() {
        showLogin()

        composeRule.onNodeWithTag("login-username-selector")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("login-username-editor")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Back) }

        composeRule.onNodeWithTag("login-username-editor").assertDoesNotExist()
        composeRule.onNodeWithTag("login-username-selector").assertIsFocused()
    }

    @Test
    fun usernameImeNextMovesToPassword() {
        showLogin()

        composeRule.onNodeWithTag("login-username-selector")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("login-username-editor").performImeAction()

        composeRule.onNodeWithTag("login-username-editor").assertDoesNotExist()
        composeRule.onNodeWithTag("login-password-selector").assertIsFocused()
    }

    @Test
    fun dpadMovesThroughLoginControls() {
        showLogin()

        composeRule.onNodeWithTag("login-username-selector")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithTag("login-password-selector").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("登录").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithText("更换服务器").assertIsFocused()
    }

    @Test
    fun submittingLoginKeepsButtonFocusAndIgnoresRepeatCenter() {
        val state = mutableStateOf(
            LoginState(
                username = "tv_user",
                password = "secret",
            ),
        )
        var loginRequests = 0
        composeRule.setContent {
            KaloscopeTheme {
                LoginScreen(
                    server = SavedServer(
                        id = "demo",
                        name = "Demo",
                        origin = "https://demo.example",
                    ),
                    state = state.value,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLogin = {
                        loginRequests += 1
                        state.value = state.value.copy(isSubmitting = true)
                    },
                    onChangeServer = {},
                )
            }
        }

        composeRule.onNodeWithText("登录")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithText("正在登录…")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, loginRequests)
        }
    }

    @Test
    fun loginButtonsUseEqualWidths() {
        showLogin()

        val loginWidth = composeRule.onNodeWithText("登录")
            .fetchSemanticsNode()
            .boundsInRoot
            .width
        val changeServerWidth = composeRule.onNodeWithText("更换服务器")
            .fetchSemanticsNode()
            .boundsInRoot
            .width

        assertTrue(
            "Login width was $loginWidth px; change-server width was $changeServerWidth px",
            abs(loginWidth - changeServerWidth) <= 1f,
        )
    }

    @Test
    fun passwordStaysMaskedInNavigationMode() {
        showLogin(LoginState(password = "secret"))

        composeRule.onNodeWithText("secret", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("••••••", useUnmergedTree = true).assertExists()
    }

    private fun showLogin(state: LoginState = LoginState()) {
        composeRule.setContent {
            KaloscopeTheme {
                LoginScreen(
                    server = SavedServer(
                        id = "demo",
                        name = "Demo",
                        origin = "https://demo.example",
                    ),
                    state = state,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLogin = {},
                    onChangeServer = {},
                )
            }
        }
    }
}
