package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme

class TvTextFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun focusDoesNotEnterEditingUntilCenter() {
        showField()

        composeRule.onNodeWithTag(SelectorTag)
            .assertIsFocused()
        composeRule.onNodeWithTag(EditorTag).assertDoesNotExist()

        composeRule.onNodeWithTag(SelectorTag)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag(SelectorTag).assertDoesNotExist()
        composeRule.onNodeWithTag(EditorTag).assertIsFocused()
    }

    @Test
    fun backLeavesEditingAndRestoresSelectorFocus() {
        showField()

        enterEditing()
        composeRule.onNodeWithTag(EditorTag)
            .performKeyInput { pressKey(Key.Back) }

        composeRule.onNodeWithTag(EditorTag).assertDoesNotExist()
        composeRule.onNodeWithTag(SelectorTag).assertIsFocused()
    }

    @Test
    fun downLeavesEditingAndMovesToNextControl() {
        val nextFocus = FocusRequester()
        var moves by mutableIntStateOf(0)
        showField(
            onMoveDown = {
                moves += 1
                nextFocus.requestFocus()
            },
            contentAfter = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .focusRequester(nextFocus)
                        .focusable()
                        .testTag(NextTag),
                )
            },
        )

        enterEditing()
        composeRule.onNodeWithTag(EditorTag)
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag(EditorTag).assertDoesNotExist()
        composeRule.onNodeWithTag(NextTag).assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, moves)
        }
    }

    @Test
    fun rightMovesCursorInsteadOfInvokingNavigationWhileEditing() {
        var moves by mutableIntStateOf(0)
        showField(
            initialValue = "query",
            onMoveRight = { moves += 1 },
        )

        enterEditing()
        composeRule.onNodeWithTag(EditorTag)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag(EditorTag).assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(0, moves)
        }
    }

    @Test
    fun searchImeActionSubmitsOnceAndReturnsToNavigationMode() {
        var searches by mutableIntStateOf(0)
        showField(
            imeAction = ImeAction.Search,
            onImeAction = { searches += 1 },
        )

        enterEditing()
        composeRule.onNodeWithTag(EditorTag).performImeAction()

        composeRule.onNodeWithTag(EditorTag).assertDoesNotExist()
        composeRule.onNodeWithTag(SelectorTag).assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, searches)
        }
    }

    @Test
    fun nextImeActionMovesToNextControl() {
        val nextFocus = FocusRequester()
        var moves by mutableIntStateOf(0)
        showField(
            imeAction = ImeAction.Next,
            onMoveDown = {
                moves += 1
                nextFocus.requestFocus()
            },
            contentAfter = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .focusRequester(nextFocus)
                        .focusable()
                        .testTag(NextTag),
                )
            },
        )

        enterEditing()
        composeRule.onNodeWithTag(EditorTag).performImeAction()

        composeRule.onNodeWithTag(EditorTag).assertDoesNotExist()
        composeRule.onNodeWithTag(NextTag).assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, moves)
        }
    }

    @Test
    fun passwordRemainsMaskedInNavigationMode() {
        showField(
            initialValue = "secret",
            isPassword = true,
        )

        composeRule.onNodeWithText("secret", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("••••••", useUnmergedTree = true).assertExists()
    }

    private fun showField(
        initialValue: String = "",
        isPassword: Boolean = false,
        imeAction: ImeAction = ImeAction.Done,
        onImeAction: () -> Unit = {},
        onMoveDown: (() -> Unit)? = null,
        onMoveRight: (() -> Unit)? = null,
        contentAfter: @androidx.compose.runtime.Composable () -> Unit = {},
    ) {
        val fieldFocus = FocusRequester()
        var value by mutableStateOf(initialValue)
        composeRule.setContent {
            KaloscopeTheme {
                Column {
                    TvTextField(
                        value = value,
                        onValueChange = { value = it },
                        placeholder = "Placeholder",
                        focusRequester = fieldFocus,
                        isPassword = isPassword,
                        imeAction = imeAction,
                        onImeAction = onImeAction,
                        onMoveDown = onMoveDown,
                        onMoveRight = onMoveRight,
                        selectorTestTag = SelectorTag,
                        editorTestTag = EditorTag,
                    )
                    contentAfter()
                }
            }
        }
        composeRule.runOnIdle {
            fieldFocus.requestFocus()
        }
    }

    private fun enterEditing() {
        composeRule.onNodeWithTag(SelectorTag)
            .performKeyInput { pressKey(Key.Enter) }
    }

    private companion object {
        const val SelectorTag = "tv-text-field-selector"
        const val EditorTag = "tv-text-field-editor"
        const val NextTag = "next-control"
    }
}
