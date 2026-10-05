package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction

@Composable
fun TvSearchField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Search,
    initiallyEditing: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    TvTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = hint,
        modifier = modifier,
        focusRequester = focusRequester,
        imeAction = imeAction,
        onImeAction = onSearch,
        onBack = onBack,
        onMoveUp = onMoveUp,
        onMoveRight = onMoveRight,
        initiallyEditing = initiallyEditing,
    )
}
