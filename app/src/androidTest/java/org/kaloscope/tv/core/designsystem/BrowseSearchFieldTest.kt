package org.kaloscope.tv.core.designsystem

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme

class BrowseSearchFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun centerOpensFocusedEditorAndImeCommitsBeforeSearching() {
        var query by mutableStateOf("old query")
        val searchedQueries = mutableListOf<String>()
        composeRule.setContent {
            KaloscopeTheme {
                BrowseSearchField(
                    value = query,
                    hint = "搜索当前站点",
                    onValueChange = { query = it },
                    onSearch = { searchedQueries += query },
                    modifier = Modifier.width(108.dp).testTag("search-entry"),
                )
            }
        }

        composeRule.onNodeWithTag("search-entry")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertHasClickAction()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("browse-search-dialog-input")
            .assertIsFocused()
            .performTextReplacement("星海")
        composeRule.runOnIdle { assertEquals("old query", query) }
        composeRule.onNodeWithTag("browse-search-dialog-input").performImeAction()

        composeRule.onNodeWithTag("browse-search-dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("search-entry").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals("星海", query)
            assertEquals(listOf("星海"), searchedQueries)
        }
    }

    @Test
    fun cancellingDraftKeepsQueryAndRestoresTrigger() {
        var query by mutableStateOf("original")
        var searches = 0
        composeRule.setContent {
            KaloscopeTheme {
                BrowseSearchField(
                    value = query,
                    hint = "搜索当前媒体库",
                    onValueChange = { query = it },
                    onSearch = { searches += 1 },
                    modifier = Modifier.width(108.dp).testTag("search-entry"),
                )
            }
        }

        composeRule.onNodeWithTag("search-entry")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("browse-search-dialog-input")
            .performTextReplacement("discard this")
        composeRule.onNodeWithTag("browse-search-dialog-input")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithTag("browse-search-dialog-submit")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("browse-search-dialog-input").assertIsFocused()
        composeRule.onNodeWithTag("browse-search-dialog-cancel")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("browse-search-dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("search-entry").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals("original", query)
            assertEquals(0, searches)
        }
    }

    @Test
    fun backClosesEditorThenDialogWithoutSearching() {
        var searches = 0
        composeRule.setContent {
            KaloscopeTheme {
                BrowseSearchField(
                    value = "",
                    hint = "搜索当前站点",
                    onValueChange = {},
                    onSearch = { searches += 1 },
                    modifier = Modifier.width(108.dp).testTag("search-entry"),
                )
            }
        }

        composeRule.onNodeWithTag("search-entry")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("browse-search-dialog-input")
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithTag("browse-search-dialog-input")
            .assertHasClickAction()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithTag("browse-search-dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("search-entry").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, searches) }
    }
}
