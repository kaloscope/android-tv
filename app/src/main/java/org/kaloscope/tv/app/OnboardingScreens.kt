package org.kaloscope.tv.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.BackgroundRaised
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeBrand
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.Success
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.feature.login.LoginError
import org.kaloscope.tv.feature.login.LoginState
import org.kaloscope.tv.feature.server.ServerSetupError
import org.kaloscope.tv.feature.server.ServerSetupState

@Composable
internal fun LoadingScreen() {
    AppFrame {
        Text(
            text = stringResource(R.string.loading),
            color = Muted,
            fontSize = 22.sp,
        )
    }
}

@Composable
internal fun ServerSetupScreen(
    savedServers: List<SavedServer>,
    state: ServerSetupState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onSelectServer: (SavedServer) -> Unit,
) {
    val savedServerFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    val testFocus = remember { FocusRequester() }
    // Existing servers take focus priority; new installations start in navigation mode.
    LaunchedEffect(savedServers.isEmpty()) {
        if (savedServers.isEmpty()) {
            nameFocus.requestFocus()
        } else {
            savedServerFocus.requestFocus()
        }
    }

    AppFrame {
        SetupWizardPanel(
            title = stringResource(R.string.setup_title),
            description = stringResource(R.string.setup_description),
        ) {
            Text(
                text = stringResource(R.string.setup_form_title),
                color = Muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(20.dp))
            if (savedServers.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.saved_servers),
                    color = OnBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                savedServers.forEachIndexed { index, server ->
                    PrimaryButton(
                        text = "${server.name}  ${server.origin}",
                        enabled = true,
                        onClick = { onSelectServer(server) },
                        modifier = if (index == 0) {
                            Modifier.focusRequester(savedServerFocus)
                        } else {
                            Modifier
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.add_another_server),
                    color = Primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(14.dp))
            }
            ServerTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.server_name),
                placeholder = stringResource(R.string.server_name_hint),
                focusRequester = nameFocus,
                selectorTestTag = "server-name-selector",
                editorTestTag = "server-name-editor",
                onMoveDown = { urlFocus.requestFocus() },
            )
            Spacer(Modifier.height(12.dp))
            ServerTextField(
                value = state.url,
                onValueChange = onUrlChange,
                label = stringResource(R.string.server_url),
                placeholder = stringResource(R.string.server_url_hint),
                focusRequester = urlFocus,
                selectorTestTag = "server-url-selector",
                editorTestTag = "server-url-editor",
                onMoveUp = { nameFocus.requestFocus() },
                onMoveDown = { testFocus.requestFocus() },
            )
            Spacer(Modifier.height(12.dp))
            state.error?.let {
                ErrorText(serverErrorText(it))
                Spacer(Modifier.height(10.dp))
            }
            state.verifiedOrigin?.let {
                Text(
                    text = stringResource(
                        R.string.connection_success,
                        state.serverVersion.orEmpty(),
                    ),
                    color = Success,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PrimaryButton(
                    text = if (state.isTesting) {
                        stringResource(R.string.testing)
                    } else {
                        stringResource(R.string.test_connection)
                    },
                    enabled = !state.isTesting && !state.isSaving,
                    onClick = onTest,
                    modifier = Modifier
                        .focusRequester(testFocus)
                        .weight(1f),
                )
                if (state.verifiedOrigin != null) {
                    PrimaryButton(
                        text = if (state.isSaving) {
                            stringResource(R.string.saving)
                        } else {
                            stringResource(R.string.save_continue)
                        },
                        enabled = state.canSave,
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoginScreen(
    server: SavedServer,
    state: LoginState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onChangeServer: () -> Unit,
) {
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val loginFocus = remember { FocusRequester() }
    LaunchedEffect(server.id) { usernameFocus.requestFocus() }

    AppFrame {
        FormPanel(
            eyebrow = stringResource(R.string.login_eyebrow),
            title = stringResource(R.string.login_title),
            description = stringResource(R.string.login_server, server.name, server.origin),
        ) {
            AppTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.username),
                placeholder = stringResource(R.string.username_hint),
                modifier = Modifier.focusRequester(usernameFocus),
                onMoveDown = { passwordFocus.requestFocus() },
            )
            Spacer(Modifier.height(18.dp))
            AppTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_hint),
                isPassword = true,
                modifier = Modifier.focusRequester(passwordFocus),
                onMoveUp = { usernameFocus.requestFocus() },
                onMoveDown = { loginFocus.requestFocus() },
            )
            Spacer(Modifier.height(18.dp))
            state.error?.let {
                ErrorText(loginErrorText(it))
                Spacer(Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PrimaryButton(
                    text = if (state.isSubmitting) {
                        stringResource(R.string.logging_in)
                    } else {
                        stringResource(R.string.login)
                    },
                    enabled = !state.isSubmitting,
                    onClick = onLogin,
                    modifier = Modifier.focusRequester(loginFocus),
                )
                PrimaryButton(
                    text = stringResource(R.string.change_server),
                    enabled = !state.isSubmitting,
                    onClick = onChangeServer,
                )
            }
        }
    }
}

@Composable
internal fun ConnectionErrorScreen(
    server: SavedServer,
    error: AppError,
    onRetry: () -> Unit,
    onLoginAgain: () -> Unit,
) {
    AppFrame {
        FormPanel(
            eyebrow = stringResource(R.string.connection_error_eyebrow),
            title = stringResource(R.string.connection_error_title),
            description = stringResource(R.string.connection_error_server, server.name),
        ) {
            ErrorText(appErrorText(error))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.retry),
                    enabled = true,
                    onClick = onRetry,
                )
                PrimaryButton(
                    text = stringResource(R.string.login_again),
                    enabled = true,
                    onClick = onLoginAgain,
                )
            }
        }
    }
}

@Composable
private fun SetupWizardPanel(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(0.86f)) {
            SetupProgress()
            Spacer(Modifier.height(22.dp))
            Text(
                text = title,
                color = OnBackground,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = description,
                color = Muted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
        }
        Column(
            modifier = Modifier
                .weight(1.14f)
                .heightIn(max = 390.dp)
                .background(Panel.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
                .border(1.dp, Outline, RoundedCornerShape(22.dp))
                .testTag("onboarding-panel")
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
            content = content,
        )
    }
}

@Composable
private fun SetupProgress() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SetupStep(
            number = "1",
            label = stringResource(R.string.setup_step_server),
            active = true,
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .width(24.dp)
                .height(1.dp)
                .background(Outline),
        )
        SetupStep(
            number = "2",
            label = stringResource(R.string.setup_step_login),
            active = false,
        )
    }
}

@Composable
private fun SetupStep(
    number: String,
    label: String,
    active: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (active) Primary else BackgroundRaised,
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    color = if (active) Primary else Outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = if (active) Color.White else Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = label,
            color = if (active) OnBackground else Muted,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun AppFrame(content: @Composable () -> Unit) {
    KaloscopeBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 72.dp, vertical = 48.dp),
        ) {
            KaloscopeBrand(
                name = stringResource(R.string.app_name),
                caption = stringResource(R.string.tv_experience),
            )
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                content()
            }
        }
    }
}

@Composable
private fun FormPanel(
    eyebrow: String,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(72.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.width(390.dp)) {
            Text(
                text = eyebrow,
                color = Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = OnBackground,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = description,
                color = Muted,
                fontSize = 17.sp,
                lineHeight = 25.sp,
            )
        }
        Column(
            modifier = Modifier
                .width(520.dp)
                .heightIn(max = 720.dp)
                .background(Panel.copy(alpha = 0.86f), RoundedCornerShape(22.dp))
                .border(1.dp, Outline, RoundedCornerShape(22.dp))
                .testTag("onboarding-panel")
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            content = content,
        )
    }
}

@Composable
private fun ServerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    focusRequester: FocusRequester,
    selectorTestTag: String,
    editorTestTag: String,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    var editing by remember { mutableStateOf(false) }
    var restoreSelectorFocus by remember { mutableStateOf(false) }
    var selectorFocused by remember { mutableStateOf(false) }
    val editorFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val exitEditing = {
        keyboardController?.hide()
        restoreSelectorFocus = true
        editing = false
    }

    LaunchedEffect(editing, restoreSelectorFocus) {
        if (editing) {
            editorFocus.requestFocus()
            keyboardController?.show()
        } else if (restoreSelectorFocus) {
            restoreSelectorFocus = false
            focusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = OnBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(7.dp))
        if (editing) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = OnBackground, fontSize = 17.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(editorFocus)
                    .testTag(editorTestTag)
                    .onPreInterceptKeyBeforeSoftKeyboard { event ->
                        if (event.key == Key.Back) {
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
                        } else {
                            when (event.key) {
                                Key.Back -> {
                                    exitEditing()
                                    true
                                }

                                Key.DirectionUp -> onMoveUp?.let { moveFocus ->
                                    keyboardController?.hide()
                                    editing = false
                                    moveFocus()
                                    true
                                } ?: false

                                Key.DirectionDown -> onMoveDown?.let { moveFocus ->
                                    keyboardController?.hide()
                                    editing = false
                                    moveFocus()
                                    true
                                } ?: false

                                else -> false
                            }
                        }
                    }
                    .background(BackgroundRaised, RoundedCornerShape(12.dp))
                    .border(2.dp, Primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Muted,
                                fontSize = 17.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        } else {
            Button(
                onClick = { editing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(selectorTestTag)
                    .onFocusChanged { selectorFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            false
                        } else {
                            when (event.key) {
                                Key.DirectionCenter,
                                Key.Enter,
                                Key.NumPadEnter,
                                -> {
                                    editing = true
                                    true
                                }

                                Key.DirectionUp -> onMoveUp?.let {
                                    it()
                                    true
                                } ?: false

                                Key.DirectionDown -> onMoveDown?.let {
                                    it()
                                    true
                                } ?: false

                                else -> false
                            }
                        }
                    }
                    .border(
                        width = if (selectorFocused) 2.dp else 1.dp,
                        color = if (selectorFocused) OnBackground else Outline,
                        shape = RoundedCornerShape(12.dp),
                    ),
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.colors(
                    containerColor = BackgroundRaised,
                    contentColor = if (value.isEmpty()) Muted else OnBackground,
                    focusedContainerColor = BackgroundRaised,
                    focusedContentColor = OnBackground,
                ),
                scale = ButtonDefaults.scale(focusedScale = 1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = value.ifEmpty { placeholder },
                    fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = OnBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = OnBackground, fontSize = 18.sp),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        when (event.key) {
                            Key.DirectionUp -> onMoveUp?.let {
                                it()
                                true
                            } ?: false

                            Key.DirectionDown -> onMoveDown?.let {
                                it()
                                true
                            } ?: false

                            else -> false
                        }
                    }
                }
                .onFocusChanged { focused = it.isFocused }
                .background(BackgroundRaised, RoundedCornerShape(12.dp))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) OnBackground else Outline,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Muted,
                            fontSize = 18.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF272D40),
            contentColor = OnBackground,
            focusedContainerColor = Primary,
            focusedContentColor = Color.White,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.03f),
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = Danger,
        fontSize = 16.sp,
    )
}

@Composable
private fun serverErrorText(error: ServerSetupError): String =
    when (error) {
        ServerSetupError.InvalidName -> stringResource(R.string.error_server_name)
        ServerSetupError.InvalidUrl -> stringResource(R.string.error_server_url)
        ServerSetupError.SaveFailed -> stringResource(R.string.error_server_save)
        is ServerSetupError.Connection -> appErrorText(error.error)
    }

@Composable
private fun loginErrorText(error: LoginError): String =
    when (error) {
        LoginError.MissingCredentials -> stringResource(R.string.error_credentials)
        is LoginError.Request -> appErrorText(error.error)
    }

@Composable
private fun appErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }
