package org.kaloscope.tv.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.feature.server.ServerSetupState

class ServerSetupFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun serverSetupShowsTwoStepProgress() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithText("配置服务器").assertExists()
        composeRule.onNodeWithText("登录账户").assertExists()
        composeRule.onNodeWithText("连接到你的服务器").assertExists()
    }

    @Test
    fun successfulConnectionPrefixesVersionWithV() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(
                        name = "家庭服务器",
                        url = "http://192.168.1.2:8000",
                        verifiedOrigin = "http://192.168.1.2:8000",
                        serverVersion = "1.0.0",
                    ),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithText("v1.0.0", substring = true).assertExists()
    }

    @Test
    fun testConnectionLabelIsHorizontallyCentered() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        val buttonCenter = composeRule.onNodeWithText("测试连接")
            .fetchSemanticsNode()
            .boundsInRoot
            .center
            .x
        val labelCenter = composeRule.onNodeWithText(
            text = "测试连接",
            useUnmergedTree = true,
        ).fetchSemanticsNode()
            .boundsInRoot
            .center
            .x

        assertTrue(abs(buttonCenter - labelCenter) < 1f)
    }

    @Test
    fun serverNameEntersEditModeOnlyAfterClick() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithTag("server-name-editor").assertDoesNotExist()
        composeRule.onNodeWithTag("server-name-selector")
            .assertIsFocused()
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("server-name-editor").assertExists()
        composeRule.onNodeWithText("例如：家庭服务器").assertIsFocused()
    }

    @Test
    fun serverPlaceholderKeepsSameAppearanceWhenFocusChanges() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        val focusedPlaceholder = composeRule.onNodeWithText(
            text = "例如：家庭服务器",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()
        composeRule.onNodeWithTag("server-name-selector")
            .performKeyInput { pressKey(Key.DirectionDown) }
        val unfocusedPlaceholder = composeRule.onNodeWithText(
            text = "例如：家庭服务器",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        assertTrue(focusedPlaceholder.sameAs(unfocusedPlaceholder))
    }

    @Test
    fun serverPlaceholderKeepsSameAppearanceWhenEditing() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        val navigationLayoutResults = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(
            text = "例如：家庭服务器",
            useUnmergedTree = true,
        ).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(navigationLayoutResults)
        }
        composeRule.onNodeWithTag("server-name-selector")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeBy(600)
        val editingLayoutResults = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(
            text = "例如：家庭服务器",
            useUnmergedTree = true,
        ).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(editingLayoutResults)
        }

        assertEquals(
            navigationLayoutResults.single().layoutInput.style,
            editingLayoutResults.single().layoutInput.style,
        )
    }

    @Test
    fun serverEditorKeepsHeightAfterTyping() {
        val state = mutableStateOf(ServerSetupState())
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = state.value,
                    onNameChange = { state.value = state.value.copy(name = it) },
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithTag("server-name-selector")
            .performSemanticsAction(SemanticsActions.OnClick)
        val placeholderHeight = composeRule.onNodeWithTag("server-name-editor")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
        composeRule.onNodeWithTag("server-name-editor").performTextInput("家庭服务器")
        val valueHeight = composeRule.onNodeWithTag("server-name-editor")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

        assertTrue(
            "Placeholder height was $placeholderHeight px; value height was $valueHeight px",
            abs(placeholderHeight - valueHeight) < 1f,
        )
    }

    @Test
    fun serverEditorUsesWhiteCursor() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithTag("server-name-selector")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeBy(100)
        val editor = composeRule.onNodeWithTag("server-name-editor")
            .captureToImage()
            .asAndroidBitmap()
        val hasWhitePixel = (0 until editor.width).any { x ->
            (0 until editor.height).any { y ->
                val pixel = editor.getPixel(x, y)
                android.graphics.Color.red(pixel) > 245 &&
                    android.graphics.Color.green(pixel) > 245 &&
                    android.graphics.Color.blue(pixel) > 245
            }
        }

        assertTrue(hasWhitePixel)
    }

    @Test
    fun serverNameReturnsToNavigationModeOnBack() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithTag("server-name-selector")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("server-name-editor")
            .performKeyInput { pressKey(Key.Back) }

        composeRule.onNodeWithTag("server-name-editor").assertDoesNotExist()
        composeRule.onNodeWithTag("server-name-selector").assertIsFocused()
    }

    @Test
    fun dpadDownMovesFromServerNameToUrl() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithText("例如：家庭服务器")
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithText("例如：http://192.168.1.2:8000").assertIsFocused()
    }
}
