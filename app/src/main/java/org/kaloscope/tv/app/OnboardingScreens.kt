package org.kaloscope.tv.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.collectLatest
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.BackgroundRaised
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeBrand
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeConfirmDialog
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlTone
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.Success
import org.kaloscope.tv.core.designsystem.TvTextField
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.feature.login.LoginError
import org.kaloscope.tv.feature.login.LoginState
import org.kaloscope.tv.feature.server.SavedServerDeletionState
import org.kaloscope.tv.feature.server.ServerSetupError
import org.kaloscope.tv.feature.server.ServerSetupState
import org.kaloscope.tv.feature.server.ServerUrlDraft
import org.kaloscope.tv.feature.server.ServerUrlScheme

private val SetupProgressOutline = Color(0x33FFFFFF)
private val SavedServerControlHeight = 48.dp

private sealed interface ServerSetupFocusTarget {
    data object Name : ServerSetupFocusTarget

    data class Server(val id: String) : ServerSetupFocusTarget
}

@Composable
internal fun LoadingScreen() {
    KaloscopeBackground {
        KaloscopeLoadingLayout("app-loading")
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
    deletionState: SavedServerDeletionState = SavedServerDeletionState.Idle,
    onDeleteServer: (SavedServer) -> Unit = {},
    onClearDeletionError: () -> Unit = {},
) {
    val accentPalette = LocalAccentPalette.current
    val nameFocus = remember { FocusRequester() }
    val urlSchemeFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    val testFocus = remember { FocusRequester() }
    val saveFocus = remember { FocusRequester() }
    val panelScrollState = rememberScrollState()
    val statusVisible = state.error != null || state.verifiedOrigin != null
    val serverIds = savedServers.map(SavedServer::id)
    val serverFocusRequesters = remember(serverIds) {
        serverIds.associateWith { FocusRequester() }
    }
    val deleteFocusRequesters = remember(serverIds) {
        serverIds.associateWith { FocusRequester() }
    }
    var revealedServerId by remember { mutableStateOf<String?>(null) }
    var pendingDeletion by remember { mutableStateOf<SavedServer?>(null) }
    var pendingDeletionIndex by remember { mutableStateOf<Int?>(null) }
    var focusAfterDeleteAction by remember {
        mutableStateOf<ServerSetupFocusTarget?>(null)
    }
    var deleteFocusToRestore by remember { mutableStateOf<String?>(null) }
    var focusAfterDeletion by remember { mutableStateOf<ServerSetupFocusTarget?>(null) }
    var wasTesting by remember { mutableStateOf(state.isTesting) }

    // Existing servers take focus priority; new installations start in navigation mode.
    LaunchedEffect(savedServers.isEmpty()) {
        withFrameNanos { }
        if (savedServers.isEmpty()) {
            nameFocus.requestFocus()
        } else {
            serverFocusRequesters[savedServers.first().id]?.requestFocus()
        }
    }
    LaunchedEffect(revealedServerId, serverIds) {
        revealedServerId?.let { serverId ->
            withFrameNanos { }
            deleteFocusRequesters[serverId]?.requestFocus()
        }
    }
    LaunchedEffect(focusAfterDeleteAction, serverIds) {
        focusAfterDeleteAction?.let { target ->
            withFrameNanos { }
            when (target) {
                ServerSetupFocusTarget.Name -> nameFocus.requestFocus()
                is ServerSetupFocusTarget.Server ->
                    serverFocusRequesters[target.id]?.requestFocus()
            }
            focusAfterDeleteAction = null
        }
    }
    LaunchedEffect(deleteFocusToRestore, serverIds) {
        deleteFocusToRestore?.let { serverId ->
            withFrameNanos { }
            deleteFocusRequesters[serverId]?.requestFocus()
            deleteFocusToRestore = null
        }
    }
    LaunchedEffect(serverIds, pendingDeletion?.id, pendingDeletionIndex) {
        val target = pendingDeletion ?: return@LaunchedEffect
        val deletedIndex = pendingDeletionIndex ?: return@LaunchedEffect
        if (target.id in serverIds) {
            return@LaunchedEffect
        }
        focusAfterDeletion = if (serverIds.isEmpty()) {
            ServerSetupFocusTarget.Name
        } else {
            ServerSetupFocusTarget.Server(
                serverIds[deletedIndex.coerceAtMost(serverIds.lastIndex)],
            )
        }
        pendingDeletion = null
        pendingDeletionIndex = null
        revealedServerId = null
    }
    LaunchedEffect(focusAfterDeletion, serverIds) {
        val target = focusAfterDeletion ?: return@LaunchedEffect
        withFrameNanos { }
        when (target) {
            ServerSetupFocusTarget.Name -> nameFocus.requestFocus()
            is ServerSetupFocusTarget.Server ->
                serverFocusRequesters[target.id]?.requestFocus()
        }
        focusAfterDeletion = null
    }
    LaunchedEffect(statusVisible) {
        if (statusVisible) {
            // Status rows change the panel height and must remain visible with the actions.
            snapshotFlow { panelScrollState.maxValue }.collectLatest { maxValue ->
                panelScrollState.animateScrollTo(maxValue)
            }
        }
    }
    LaunchedEffect(state.isTesting, state.verifiedOrigin) {
        val testSucceeded =
            wasTesting && !state.isTesting && state.verifiedOrigin != null
        wasTesting = state.isTesting
        if (testSucceeded) {
            // The save action is conditional, so wait until it is attached before focusing it.
            withFrameNanos { }
            saveFocus.requestFocus()
        }
    }
    BackHandler(enabled = pendingDeletion == null && revealedServerId != null) {
        focusAfterDeleteAction = revealedServerId?.let {
            ServerSetupFocusTarget.Server(it)
        }
        revealedServerId = null
    }

    AppFrame {
        SetupWizardPanel(
            title = stringResource(R.string.setup_title),
            description = stringResource(R.string.setup_description),
            activeStep = 1,
            scrollState = panelScrollState,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SavedServerButton(
                            server = server,
                            onClick = { onSelectServer(server) },
                            modifier = Modifier
                                .weight(1f)
                                .height(SavedServerControlHeight)
                                .testTag("saved-server-${server.id}")
                                .focusRequester(serverFocusRequesters.getValue(server.id))
                                .onPreviewKeyEvent { event ->
                                    if (event.key != Key.DirectionRight) {
                                        return@onPreviewKeyEvent false
                                    }
                                    if (event.type == KeyEventType.KeyDown) {
                                        if (revealedServerId == server.id) {
                                            deleteFocusRequesters[server.id]?.requestFocus()
                                        } else {
                                            revealedServerId = server.id
                                        }
                                    }
                                    true
                                },
                        )
                        AnimatedVisibility(
                            visible = revealedServerId == server.id,
                            enter = fadeIn(
                                tween(KaloscopeMotion.FocusMillis),
                            ) + slideInHorizontally(
                                animationSpec = tween(KaloscopeMotion.FocusMillis),
                                initialOffsetX = { it / 2 },
                            ),
                            exit = fadeOut(
                                tween(KaloscopeMotion.FocusMillis),
                            ) + slideOutHorizontally(
                                animationSpec = tween(KaloscopeMotion.FocusMillis),
                                targetOffsetX = { it / 2 },
                            ),
                        ) {
                            val deleteDescription = stringResource(
                                R.string.delete_server_description,
                                server.name,
                            )
                            KaloscopeIconButton(
                                onClick = {
                                    onClearDeletionError()
                                    pendingDeletion = server
                                },
                                tone = KaloscopeControlTone.Danger,
                                modifier = Modifier
                                    .size(SavedServerControlHeight)
                                    .testTag("delete-server-${server.id}")
                                    .semantics {
                                        contentDescription = deleteDescription
                                    }
                                    .focusRequester(
                                        deleteFocusRequesters.getValue(server.id),
                                    )
                                    .onPreviewKeyEvent { event ->
                                        when (event.key) {
                                            Key.DirectionLeft -> {
                                                if (event.type == KeyEventType.KeyDown) {
                                                    focusAfterDeleteAction =
                                                        ServerSetupFocusTarget.Server(server.id)
                                                    revealedServerId = null
                                                }
                                                true
                                            }

                                            Key.DirectionUp -> {
                                                if (
                                                    event.type == KeyEventType.KeyDown &&
                                                    index > 0
                                                ) {
                                                    focusAfterDeleteAction =
                                                        ServerSetupFocusTarget.Server(
                                                            savedServers[index - 1].id,
                                                        )
                                                    revealedServerId = null
                                                }
                                                true
                                            }

                                            Key.DirectionDown -> {
                                                if (event.type == KeyEventType.KeyDown) {
                                                    focusAfterDeleteAction =
                                                        savedServers.getOrNull(index + 1)
                                                            ?.let {
                                                                ServerSetupFocusTarget.Server(
                                                                    it.id,
                                                                )
                                                            }
                                                            ?: ServerSetupFocusTarget.Name
                                                    revealedServerId = null
                                                }
                                                true
                                            }

                                            else -> false
                                        }
                                    },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.add_another_server),
                    color = accentPalette.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(14.dp))
            }
            TvTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.server_name),
                placeholder = stringResource(R.string.server_name_hint),
                focusRequester = nameFocus,
                imeAction = ImeAction.Next,
                selectorTestTag = "server-name-selector",
                editorTestTag = "server-name-editor",
                onMoveDown = { urlFocus.requestFocus() },
            )
            Spacer(Modifier.height(12.dp))
            ServerUrlField(
                url = state.url,
                onUrlChange = onUrlChange,
                schemeFocus = urlSchemeFocus,
                addressFocus = urlFocus,
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
                    // Keep the focused test action enabled or Compose moves D-pad focus.
                    enabled = !state.isSaving,
                    onClick = {
                        if (!state.isTesting) {
                            onTest()
                        }
                    },
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
                        // Keep the focused action eligible for D-pad focus while saving.
                        enabled = state.canSave || state.isSaving,
                        onClick = {
                            if (state.canSave) {
                                onSave()
                            }
                        },
                        modifier = Modifier
                            .focusRequester(saveFocus)
                            .weight(1f),
                    )
                }
            }
        }
    }

    pendingDeletion?.let { server ->
        val isDeleting = deletionState is SavedServerDeletionState.Deleting &&
            deletionState.serverId == server.id
        val deletionFailed = deletionState is SavedServerDeletionState.Failed &&
            deletionState.serverId == server.id
        KaloscopeConfirmDialog(
            title = stringResource(R.string.delete_server_title),
            message = stringResource(R.string.delete_server_message, server.name),
            cancelLabel = stringResource(R.string.cancel),
            confirmLabel = stringResource(R.string.delete),
            confirmTone = KaloscopeControlTone.Danger,
            busy = isDeleting,
            errorMessage = if (deletionFailed) {
                stringResource(R.string.delete_server_failed)
            } else {
                null
            },
            onDismiss = {
                onClearDeletionError()
                pendingDeletion = null
                pendingDeletionIndex = null
                deleteFocusToRestore = server.id
            },
            onConfirm = {
                val serverIndex = savedServers.indexOfFirst {
                    it.id == server.id
                }
                if (serverIndex >= 0) {
                    pendingDeletionIndex = serverIndex
                    onDeleteServer(server)
                }
            },
        )
    }
}

@Composable
private fun ServerUrlField(
    url: String,
    onUrlChange: (String) -> Unit,
    schemeFocus: FocusRequester,
    addressFocus: FocusRequester,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val draft = ServerUrlDraft.from(url)
    val schemeLabel = stringResource(
        when (draft.scheme) {
            ServerUrlScheme.Http -> R.string.server_scheme_http
            ServerUrlScheme.Https -> R.string.server_scheme_https
        },
    )
    val schemeDescription = stringResource(
        R.string.server_scheme_description,
        schemeLabel,
    )

    Text(
        text = stringResource(R.string.server_url),
        color = OnBackground,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(7.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KaloscopeButton(
            onClick = {
                val nextScheme = when (draft.scheme) {
                    ServerUrlScheme.Http -> ServerUrlScheme.Https
                    ServerUrlScheme.Https -> ServerUrlScheme.Http
                }
                onUrlChange(draft.replaceScheme(nextScheme))
            },
            modifier = Modifier
                .width(104.dp)
                .height(48.dp)
                .focusRequester(schemeFocus)
                .testTag("server-url-scheme")
                .semantics { contentDescription = schemeDescription }
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionUp -> {
                            if (event.type == KeyEventType.KeyDown) onMoveUp()
                            true
                        }

                        Key.DirectionDown -> {
                            if (event.type == KeyEventType.KeyDown) onMoveDown()
                            true
                        }

                        Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown) {
                                addressFocus.requestFocus()
                            }
                            true
                        }

                        else -> false
                    }
                },
            variant = KaloscopeControlVariant.Filled,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = schemeLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        TvTextField(
            value = draft.address,
            onValueChange = { onUrlChange(draft.replaceAddress(it)) },
            placeholder = stringResource(R.string.server_url_hint),
            modifier = Modifier.weight(1f),
            focusRequester = addressFocus,
            imeAction = ImeAction.Next,
            selectorTestTag = "server-url-selector",
            editorTestTag = "server-url-editor",
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onMoveLeft = { schemeFocus.requestFocus() },
        )
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
        SetupWizardPanel(
            title = stringResource(R.string.login_title),
            description = stringResource(R.string.login_server, server.name, server.origin),
            activeStep = 2,
        ) {
            Text(
                text = stringResource(R.string.login_form_title),
                color = Muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(20.dp))
            TvTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.username),
                placeholder = stringResource(R.string.username_hint),
                focusRequester = usernameFocus,
                imeAction = ImeAction.Next,
                selectorTestTag = "login-username-selector",
                editorTestTag = "login-username-editor",
                onMoveDown = { passwordFocus.requestFocus() },
            )
            Spacer(Modifier.height(12.dp))
            TvTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_hint),
                isPassword = true,
                focusRequester = passwordFocus,
                imeAction = ImeAction.Next,
                selectorTestTag = "login-password-selector",
                editorTestTag = "login-password-editor",
                onMoveUp = { usernameFocus.requestFocus() },
                onMoveDown = { loginFocus.requestFocus() },
            )
            Spacer(Modifier.height(12.dp))
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
                    // Keep the focused action eligible for D-pad focus while submitting.
                    enabled = true,
                    onClick = {
                        if (!state.isSubmitting) {
                            onLogin()
                        }
                    },
                    modifier = Modifier
                        .focusRequester(loginFocus)
                        .weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.change_server),
                    enabled = !state.isSubmitting,
                    onClick = onChangeServer,
                    modifier = Modifier.weight(1f),
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
    onSwitchServer: () -> Unit,
) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        retryFocus.requestFocus()
    }

    AppFrame {
        FormPanel(
            title = stringResource(R.string.connection_error_title),
            description = stringResource(R.string.connection_error_server, server.name),
        ) {
            ErrorText(appErrorText(error))
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.retry),
                    enabled = true,
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(retryFocus),
                )
                PrimaryButton(
                    text = stringResource(R.string.switch_server),
                    enabled = true,
                    onClick = onSwitchServer,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SetupWizardPanel(
    title: String,
    description: String,
    activeStep: Int,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(0.86f)) {
            SetupProgress(activeStep)
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
                .verticalScroll(scrollState)
                .padding(26.dp),
            content = content,
        )
    }
}

@Composable
private fun SetupProgress(activeStep: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SetupStep(
            number = "1",
            label = stringResource(R.string.setup_step_server),
            active = activeStep == 1,
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .width(24.dp)
                .height(1.dp)
                .background(SetupProgressOutline),
        )
        SetupStep(
            number = "2",
            label = stringResource(R.string.setup_step_login),
            active = activeStep == 2,
        )
    }
}

@Composable
private fun SetupStep(
    number: String,
    label: String,
    active: Boolean,
) {
    val accentPalette = LocalAccentPalette.current
    Row(
        modifier = Modifier
            .testTag("setup-step-$number")
            .semantics { selected = active },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (active) accentPalette.primary else BackgroundRaised,
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    color = if (active) accentPalette.primary else SetupProgressOutline,
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
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        variant = KaloscopeControlVariant.Filled,
        size = KaloscopeControlSize.Compact,
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
private fun SavedServerButton(
    server: SavedServer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeButton(
        onClick = onClick,
        modifier = modifier,
        variant = KaloscopeControlVariant.Filled,
        size = KaloscopeControlSize.Compact,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            val nameMaxWidth = maxWidth * 0.4f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = server.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.widthIn(max = nameMaxWidth),
                )
                Text(
                    text = server.origin,
                    color = LocalContentColor.current.copy(alpha = 0.68f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
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
