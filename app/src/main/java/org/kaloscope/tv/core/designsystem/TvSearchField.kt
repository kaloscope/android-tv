package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun TvSearchField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val selectionColors = remember {
        TextSelectionColors(
            handleColor = Primary,
            backgroundColor = Primary.copy(alpha = 0.35f),
        )
    }

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .onFocusChanged { focused = it.isFocused }
                .background(
                    color = if (focused) {
                        Primary.copy(alpha = 0.12f).compositeOver(PanelElevated)
                    } else {
                        Panel.copy(alpha = 0.88f)
                    },
                    shape = shape,
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) Primary else Outline,
                    shape = shape,
                )
                .padding(horizontal = 18.dp, vertical = 13.dp),
            textStyle = TextStyle(
                color = OnBackground,
                fontSize = 16.sp,
            ),
            singleLine = true,
            cursorBrush = SolidColor(PrimarySoft),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = hint,
                            color = if (focused) OnBackground.copy(alpha = 0.7f) else Muted,
                            fontSize = 16.sp,
                        )
                    }
                    field()
                }
            },
        )
    }
}
