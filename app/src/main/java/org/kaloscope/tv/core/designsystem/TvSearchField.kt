package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

@Composable
fun TvSearchField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Search,
) {
    TvTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = hint,
        modifier = modifier,
        imeAction = imeAction,
        onImeAction = onSearch,
        onMoveUp = onMoveUp,
        onMoveRight = onMoveRight,
    )
}
