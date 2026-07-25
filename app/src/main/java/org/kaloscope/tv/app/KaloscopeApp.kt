package org.kaloscope.tv.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import org.kaloscope.tv.R
import org.kaloscope.tv.app.bootstrap.BootstrapState
import org.kaloscope.tv.app.navigation.toRootRoute
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.BackgroundRaised
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeBrand
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.Success
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.feature.login.LoginError
import org.kaloscope.tv.feature.login.LoginState
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.detail.MediaDetailViewModel
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.library.LibraryViewModel
import org.kaloscope.tv.feature.server.ServerSetupError
import org.kaloscope.tv.feature.server.ServerSetupState
import org.kaloscope.tv.feature.player.PlayerViewModel
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.search.SearchResultsState
import org.kaloscope.tv.feature.search.SearchSourceState
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.search.SearchViewModel
import org.kaloscope.tv.feature.settings.SettingsUiState
import org.kaloscope.tv.feature.settings.SettingsViewModel
import org.kaloscope.tv.core.player.PlaybackControllerFactory

@Composable
fun KaloscopeApp(
    viewModel: KaloscopeViewModel,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    detailViewModel: MediaDetailViewModel,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel,
    playbackControllerFactory: PlaybackControllerFactory,
) {
    val bootstrapState by viewModel.bootstrapState.collectAsStateWithLifecycle()
    val homeState by mainViewModel.homeState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    KaloscopeTheme {
        // Exactly one root subtree is composed to prevent hidden screens from retaining focus.
        when (val state = bootstrapState) {
            BootstrapState.Loading -> LoadingScreen()
            is BootstrapState.NeedsServer -> {
                val setupState by viewModel.serverSetupState.collectAsStateWithLifecycle()
                ServerSetupScreen(
                    savedServers = state.savedServers,
                    state = setupState,
                    onNameChange = viewModel::updateServerName,
                    onUrlChange = viewModel::updateServerUrl,
                    onTest = viewModel::testServerConnection,
                    onSave = viewModel::saveServer,
                    onSelectServer = viewModel::selectServer,
                )
            }

            is BootstrapState.NeedsLogin -> {
                val loginState by viewModel.loginState.collectAsStateWithLifecycle()
                LoginScreen(
                    server = state.server,
                    state = loginState,
                    onUsernameChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onLogin = viewModel::submitLogin,
                    onChangeServer = viewModel::showServerSelection,
                )
            }

            is BootstrapState.Ready -> {
                LaunchedEffect(state.session.server.id, state.session.user.id) {
                    mainViewModel.loadHome(state.session)
                }
                LaunchedEffect(homeState, searchState, libraryState, detailState, playerState) {
                    if (homeState.hasUnauthorized() ||
                        searchState.hasUnauthorized() ||
                        libraryState.hasUnauthorized() ||
                        detailState.hasUnauthorized() ||
                        playerState.hasUnauthorized()
                    ) {
                        viewModel.handleUnauthorized(state.session)
                    }
                }
                // Authenticated feature state must not survive a server switch.
                DisposableEffect(state.session.server.id) {
                    onDispose {
                        mainViewModel.reset()
                        searchViewModel.reset()
                        libraryViewModel.reset()
                        detailViewModel.reset()
                        playerViewModel.clearServer(state.session.server.id)
                    }
                }
                if (settingsState == SettingsUiState.Loading) {
                    LoadingScreen()
                    return@KaloscopeTheme
                }
                val activeSettings = settingsState as? SettingsUiState.Content
                val currentSettings = activeSettings?.settings ?: TvSettings()
                MainShell(
                    session = state.session,
                    homeState = homeState,
                    searchState = searchState,
                    libraryState = libraryState,
                    detailState = detailState,
                    settingsState = settingsState,
                    initialRoute = currentSettings.startPage.toRootRoute(),
                    onRefresh = { mainViewModel.loadHome(state.session, force = true) },
                    onOpenSearch = { searchViewModel.load(state.session) },
                    onSelectIndexer = { indexerId ->
                        searchViewModel.selectIndexer(state.session, indexerId)
                    },
                    onSearchQueryChange = searchViewModel::updateQuery,
                    onSearchNetwork = { searchViewModel.search(state.session) },
                    onRetrySearch = {
                        if (searchState is SearchUiState.Content) {
                            searchViewModel.retry(state.session)
                        } else {
                            searchViewModel.load(state.session, force = true)
                        }
                    },
                    onLoadMoreSearch = { searchViewModel.loadNext(state.session) },
                    onSearchResultFocused = searchViewModel::rememberFocusedResult,
                    onPlaySearchResult = { resultId ->
                        searchViewModel.play(
                            state.session,
                            resultId,
                            currentSettings,
                        )
                    },
                    onConsumeSearchPlayback = searchViewModel::consumePlaybackRequest,
                    onOpenLibrary = { libraryViewModel.load(state.session) },
                    onSelectLibrary = { libraryId ->
                        libraryViewModel.selectLibrary(state.session, libraryId)
                    },
                    onLibraryQueryChange = libraryViewModel::updateQuery,
                    onSearchLibrary = { libraryViewModel.search(state.session) },
                    onRetryLibrary = {
                        if (libraryState is LibraryUiState.Content) {
                            libraryViewModel.retryContent(state.session)
                        } else {
                            libraryViewModel.load(state.session, force = true)
                        }
                    },
                    onLoadMoreMedia = { libraryViewModel.loadNext(state.session) },
                    onMediaFocused = libraryViewModel::rememberFocusedMedia,
                    onOpenMedia = { mediaId ->
                        detailViewModel.load(state.session, mediaId)
                    },
                    onRetryDetail = { detailViewModel.retry(state.session) },
                    onSelectMediaChild = { childId ->
                        detailViewModel.selectChild(state.session, childId)
                    },
                    onRetrySettings = settingsViewModel::load,
                    onSelectSettingsSection = settingsViewModel::selectSection,
                    onPlaybackModeSetting = settingsViewModel::setPlaybackMode,
                    onTranscodeResolutionSetting =
                        settingsViewModel::setTranscodeResolution,
                    onAutoplayNextSetting = settingsViewModel::setAutoplayNext,
                    onDanmakuEnabledSetting = settingsViewModel::setDanmakuEnabled,
                    onSubtitleEnabledSetting = settingsViewModel::setSubtitleEnabled,
                    onStartPageSetting = settingsViewModel::setStartPage,
                    onTestConnection = {
                        settingsViewModel.testConnection(state.session)
                    },
                    onManageServers = viewModel::showServerSelection,
                    onLogout = viewModel::logout,
                    playerState = playerState,
                    playbackControllerFactory = playbackControllerFactory,
                    onPlayHistory = { item ->
                        playerViewModel.createFromHistory(
                            state.session,
                            item,
                            currentSettings,
                        )
                    },
                    onPlayDetail = { detail, resumePosition ->
                        val siblings = (detailState as? MediaDetailUiState.Content)
                            ?.parent
                            ?.children
                            .orEmpty()
                        playerViewModel.createFromDetail(
                            session = state.session,
                            detail = detail,
                            siblings = siblings,
                            resumePositionSeconds = resumePosition,
                            settings = currentSettings,
                        )
                    },
                    onLoadPlayer = { requestId ->
                        playerViewModel.load(state.session, requestId)
                    },
                    onPlayerProgress = { request, position, duration, reason ->
                        playerViewModel.recordProgress(
                            session = state.session,
                            request = request,
                            positionMillis = position,
                            durationMillis = duration,
                            reason = reason,
                        )
                    },
                    onSelectPlayerDefinition = { index, position ->
                        playerViewModel.selectDefinition(state.session, index, position)
                    },
                    onSwitchPlayerItem = { offset ->
                        playerViewModel.switchAdjacent(state.session, offset)
                    },
                    onClosePlayer = playerViewModel::close,
                )
            }

            is BootstrapState.ConnectionError -> ConnectionErrorScreen(
                server = state.server,
                error = state.error,
                onRetry = viewModel::retryBootstrap,
                onLoginAgain = { viewModel.useDifferentAccount(state.server) },
            )
        }
    }
}

private fun HomeUiState.hasUnauthorized(): Boolean =
    this is HomeUiState.Error && error == AppError.Unauthorized

private fun LibraryUiState.hasUnauthorized(): Boolean =
    when (this) {
        is LibraryUiState.Error -> error == AppError.Unauthorized
        is LibraryUiState.Content -> when (val itemState = items) {
            is LibraryItemsState.Error -> itemState.error == AppError.Unauthorized
            is LibraryItemsState.Content ->
                itemState.loadMoreError == AppError.Unauthorized

            else -> false
        }

        else -> false
    }

private fun SearchUiState.hasUnauthorized(): Boolean =
    when (this) {
        is SearchUiState.Error -> error == AppError.Unauthorized
        is SearchUiState.Content -> {
            val sourceUnauthorized =
                (source as? SearchSourceState.Error)?.error == AppError.Unauthorized
            val resultUnauthorized = when (val resultState = results) {
                is SearchResultsState.Error -> resultState.error == AppError.Unauthorized
                is SearchResultsState.Content ->
                    resultState.loadMoreError == AppError.Unauthorized

                else -> false
            }
            sourceUnauthorized ||
                resultUnauthorized ||
                playbackError == AppError.Unauthorized
        }

        else -> false
    }

private fun MediaDetailUiState.hasUnauthorized(): Boolean =
    when (this) {
        is MediaDetailUiState.Error -> error == AppError.Unauthorized
        is MediaDetailUiState.Content -> childError == AppError.Unauthorized
        MediaDetailUiState.Loading -> false
    }

private fun PlayerUiState.hasUnauthorized(): Boolean =
    this is PlayerUiState.Content &&
        (
            progressError == AppError.Unauthorized ||
                extraFailures.values.any { it == AppError.Unauthorized }
        )

@Composable
internal fun KaloscopeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Primary,
            background = Background,
            surface = Panel,
            surfaceVariant = PanelElevated,
            onPrimary = Background,
            onBackground = OnBackground,
            onSurface = OnBackground,
        ),
        content = content,
    )
}

@Composable
private fun LoadingScreen() {
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
    // Existing servers take focus priority; new installations start in the name field.
    LaunchedEffect(savedServers.isEmpty()) {
        if (savedServers.isEmpty()) {
            nameFocus.requestFocus()
        } else {
            savedServerFocus.requestFocus()
        }
    }

    AppFrame {
        FormPanel(
            eyebrow = stringResource(R.string.setup_eyebrow),
            title = stringResource(R.string.setup_title),
            description = stringResource(R.string.setup_description),
        ) {
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
            AppTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.server_name),
                placeholder = stringResource(R.string.server_name_hint),
                modifier = Modifier.focusRequester(nameFocus),
                onMoveDown = { urlFocus.requestFocus() },
            )
            Spacer(Modifier.height(18.dp))
            AppTextField(
                value = state.url,
                onValueChange = onUrlChange,
                label = stringResource(R.string.server_url),
                placeholder = stringResource(R.string.server_url_hint),
                modifier = Modifier.focusRequester(urlFocus),
                onMoveUp = { nameFocus.requestFocus() },
                onMoveDown = { testFocus.requestFocus() },
            )
            Spacer(Modifier.height(18.dp))
            state.error?.let {
                ErrorText(serverErrorText(it))
                Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(12.dp))
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
                    modifier = Modifier.focusRequester(testFocus),
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
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
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
private fun ConnectionErrorScreen(
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
            Box(
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
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
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
