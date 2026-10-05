package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.kaloscope.tv.R

@Composable
fun BrowseSearchField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val internalFocus = remember { FocusRequester() }
    val fieldFocus = focusRequester ?: internalFocus
    var dialogOpen by remember { mutableStateOf(false) }
    var restoreFocus by remember { mutableStateOf(false) }

    LaunchedEffect(dialogOpen) {
        if (!dialogOpen && restoreFocus) {
            withFrameNanos { }
            fieldFocus.requestFocus()
            restoreFocus = false
        }
    }
    fun closeDialog() {
        // Keep root navigation on the caller while the dialog window is removed.
        fieldFocus.requestFocus()
        restoreFocus = true
        dialogOpen = false
    }

    KaloscopeButton(
        onClick = { dialogOpen = true },
        modifier = modifier
            .height(BrowseLayoutTokens.SearchControlHeight)
            .focusRequester(fieldFocus)
            .semantics { contentDescription = hint },
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_search),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .testTag("browse-search-field-icon"),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = value.ifEmpty { stringResource(R.string.search_action) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("browse-search-field-label"),
            )
        }
    }
    if (dialogOpen) {
        BrowseSearchDialog(
            value = value,
            title = hint,
            onDismiss = ::closeDialog,
            onSubmit = { query ->
                closeDialog()
                onValueChange(query)
                onSearch()
            },
        )
    }
}

@Composable
private fun BrowseSearchDialog(
    value: String,
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(value) }
    val inputFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ModalScrim)
                .testTag("browse-search-dialog"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .background(PanelElevated, RoundedCornerShape(22.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    color = OnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                TvSearchField(
                    value = draft,
                    hint = stringResource(R.string.search_keyword_hint),
                    onValueChange = { draft = it },
                    onSearch = { onSubmit(draft) },
                    onBack = onDismiss,
                    focusRequester = inputFocus,
                    initiallyEditing = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties {
                            up = FocusRequester.Cancel
                            down = submitFocus
                        }
                        .testTag("browse-search-dialog-input"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    KaloscopeButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("browse-search-dialog-cancel")
                            .focusProperties { up = inputFocus },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    KaloscopeButton(
                        onClick = { onSubmit(draft) },
                        modifier = Modifier
                            .focusRequester(submitFocus)
                            .focusProperties { up = inputFocus }
                            .testTag("browse-search-dialog-submit"),
                    ) {
                        Text(stringResource(R.string.search_action))
                    }
                }
            }
        }
    }
}
