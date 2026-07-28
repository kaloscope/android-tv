package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    focusRequester: FocusRequester? = null,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    selectorTestTag: String? = null,
    editorTestTag: String? = null,
) {
    val internalFocus = remember { FocusRequester() }
    val fieldFocus = focusRequester ?: internalFocus
    val field: @Composable (Modifier) -> Unit = { fieldModifier ->
        TvTextFieldSurface(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = fieldModifier,
            fieldFocus = fieldFocus,
            isPassword = isPassword,
            imeAction = imeAction,
            onImeAction = onImeAction,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onMoveLeft = onMoveLeft,
            onMoveRight = onMoveRight,
            selectorTestTag = selectorTestTag,
            editorTestTag = editorTestTag,
        )
    }

    if (label == null) {
        field(modifier)
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = OnBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(7.dp))
            field(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TvTextFieldSurface(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    fieldFocus: FocusRequester,
    isPassword: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMoveLeft: (() -> Unit)?,
    onMoveRight: (() -> Unit)?,
    selectorTestTag: String?,
    editorTestTag: String?,
) {
    var editing by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            ),
        )
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(12.dp)
    val fieldTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = OnBackground,
        fontSize = 17.sp,
        lineHeight = 20.sp,
    )
    val placeholderTextStyle = fieldTextStyle.copy(color = Subtle)

    val enterEditing = {
        fieldValue = fieldValue.copy(selection = TextRange(fieldValue.text.length))
        editing = true
    }
    val exitEditing = {
        keyboardController?.hide()
        editing = false
    }
    val exitToward: (FocusDirection, (() -> Unit)?) -> Unit = { direction, move ->
        keyboardController?.hide()
        editing = false
        if (move == null) {
            focusManager.moveFocus(direction)
        } else {
            move()
        }
    }
    val finishImeAction = {
        when (imeAction) {
            ImeAction.Next -> exitToward(FocusDirection.Down, onMoveDown)
            else -> {
                exitEditing()
                onImeAction()
            }
        }
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(
                text = value,
                selection = TextRange(
                    start = fieldValue.selection.start.coerceIn(0, value.length),
                    end = fieldValue.selection.end.coerceIn(0, value.length),
                ),
            )
        }
    }
    LaunchedEffect(editing) {
        if (editing) {
            // The field keeps focus while becoming editable; wait for its input session to start.
            withFrameNanos { }
            keyboardController?.show()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }

    val visibleFieldValue = if (isPassword && !editing) {
        fieldValue.copy(text = "•".repeat(fieldValue.text.length))
    } else {
        fieldValue
    }
    BasicTextField(
        value = visibleFieldValue,
        onValueChange = { updated ->
            if (editing) {
                fieldValue = updated
                onValueChange(updated.text)
            }
        },
        readOnly = !editing,
        singleLine = true,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        textStyle = fieldTextStyle,
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { finishImeAction() },
            onGo = { finishImeAction() },
            onNext = { finishImeAction() },
            onPrevious = { finishImeAction() },
            onSearch = { finishImeAction() },
            onSend = { finishImeAction() },
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .focusRequester(fieldFocus)
            .optionalTestTag(if (editing) editorTestTag else selectorTestTag)
            .onFocusChanged { fieldFocused = it.isFocused }
            .onPreInterceptKeyBeforeSoftKeyboard { event ->
                if (editing && event.key == Key.Back) {
                    if (event.type == KeyEventType.KeyDown) {
                        exitEditing()
                    }
                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else if (editing) {
                    when (event.key) {
                        Key.Back -> {
                            exitEditing()
                            true
                        }

                        Key.DirectionUp -> {
                            exitToward(FocusDirection.Up, onMoveUp)
                            true
                        }

                        Key.DirectionDown -> {
                            exitToward(FocusDirection.Down, onMoveDown)
                            true
                        }

                        else -> false
                    }
                } else {
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter,
                        -> {
                            enterEditing()
                            true
                        }

                        Key.DirectionUp -> onMoveUp?.let { move ->
                            move()
                            true
                        } ?: false

                        Key.DirectionDown -> onMoveDown?.let { move ->
                            move()
                            true
                        } ?: false

                        Key.DirectionLeft -> onMoveLeft?.let { move ->
                            move()
                            true
                        } ?: false

                        Key.DirectionRight -> onMoveRight?.let { move ->
                            move()
                            true
                        } ?: false

                        else -> false
                    }
                }
            }
            .semantics {
                if (!editing) {
                    role = Role.Button
                    onClick {
                        enterEditing()
                        true
                    }
                }
            }
            .background(BackgroundRaised, shape)
            .border(
                width = if (editing || fieldFocused) 2.dp else 1.dp,
                color = when {
                    editing -> Primary
                    fieldFocused -> OnBackground
                    else -> Outline
                },
                shape = shape,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (fieldValue.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = placeholderTextStyle,
                    )
                }
                innerTextField()
            }
        },
    )
}

private fun Modifier.optionalTestTag(tag: String?): Modifier =
    if (tag == null) {
        this
    } else {
        testTag(tag)
    }
