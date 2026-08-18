package org.kaloscope.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.kaloscope.tv.app.bootstrap.BootstrapState
import org.kaloscope.tv.app.navigation.toRootRoute
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.detail.MediaDetailViewModel
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.library.LibraryViewModel
import org.kaloscope.tv.feature.player.PlayerViewModel
import org.kaloscope.tv.feature.reader.ReaderViewModel
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.search.SearchViewModel
import org.kaloscope.tv.feature.settings.SettingsUiState
import org.kaloscope.tv.feature.settings.SettingsViewModel

@Composable
fun KaloscopeApp(
    viewModel: KaloscopeViewModel,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    detailViewModel: MediaDetailViewModel,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel,
    readerViewModel: ReaderViewModel,
    playbackControllerFactory: PlaybackControllerFactory,
) {
    val bootstrapState by viewModel.bootstrapState.collectAsStateWithLifecycle()
    val homeState by mainViewModel.homeState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = (settingsState as? SettingsUiState.Content)
        ?.settings
        ?.accentColor
        ?: AccentColor.Blue

    KaloscopeTheme(accentColor = accentColor) {
        // Exactly one root subtree is composed to prevent hidden screens from retaining focus.
        when (val state = bootstrapState) {
            BootstrapState.Loading -> LoadingScreen()
            is BootstrapState.NeedsServer -> {
                val setupState by viewModel.serverSetupState.collectAsStateWithLifecycle()
                val deletionState by viewModel.serverDeletionState.collectAsStateWithLifecycle()
                ServerSetupScreen(
                    savedServers = state.savedServers,
                    state = setupState,
                    deletionState = deletionState,
                    onNameChange = viewModel::updateServerName,
                    onUrlChange = viewModel::updateServerUrl,
                    onTest = viewModel::testServerConnection,
                    onSave = viewModel::saveServer,
                    onSelectServer = viewModel::selectServer,
                    onDeleteServer = viewModel::deleteServer,
                    onClearDeletionError = viewModel::clearServerDeletionError,
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
                LaunchedEffect(
                    homeState,
                    searchState,
                    libraryState,
                    detailState,
                    playerState,
                    readerState,
                ) {
                    if (homeState.hasUnauthorized() ||
                        searchState.hasUnauthorized() ||
                        libraryState.hasUnauthorized() ||
                        detailState.hasUnauthorized() ||
                        playerState.hasUnauthorized() ||
                        readerState.hasUnauthorized()
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
                        readerViewModel.clearServer(state.session.server.id)
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
                    homeActions = HomeActions(
                        refresh = { mainViewModel.loadHome(state.session, force = true) },
                        play = { item ->
                            playerViewModel.createFromHistory(
                                state.session,
                                item,
                                currentSettings,
                            )
                        },
                    ),
                    searchActions = SearchActions(
                        open = { searchViewModel.load(state.session) },
                        refreshIndexers = {
                            searchViewModel.load(state.session, force = true)
                        },
                        selectIndexer = { indexerId ->
                            searchViewModel.selectIndexer(state.session, indexerId)
                        },
                        updateQuery = searchViewModel::updateQuery,
                        search = { searchViewModel.search(state.session) },
                        retry = {
                            if (searchState is SearchUiState.Content) {
                                searchViewModel.retry(state.session)
                            } else {
                                searchViewModel.load(state.session, force = true)
                            }
                        },
                        loadMore = { searchViewModel.loadNext(state.session) },
                        rememberFocusedResult = searchViewModel::rememberFocusedResult,
                        rememberGridViewport = searchViewModel::rememberGridViewport,
                        play = { resultId ->
                            searchViewModel.play(
                                state.session,
                                resultId,
                                currentSettings,
                            )
                        },
                        cancelResolution = searchViewModel::cancelResolution,
                        openFilters = searchViewModel::openFilters,
                        dismissFilters = searchViewModel::dismissFilters,
                        applyFilters = { values: Map<String, SearchFilterValue> ->
                            searchViewModel.applyFilters(state.session, values)
                        },
                        clearFilters = {
                            searchViewModel.clearFilters(state.session)
                        },
                        consumeDestination = searchViewModel::consumeDestination,
                    ),
                    libraryActions = LibraryActions(
                        open = { libraryViewModel.load(state.session) },
                        select = { libraryId ->
                            libraryViewModel.selectLibrary(state.session, libraryId)
                        },
                        updateQuery = libraryViewModel::updateQuery,
                        search = { libraryViewModel.search(state.session) },
                        retry = {
                            if (libraryState is LibraryUiState.Content) {
                                libraryViewModel.retryContent(state.session)
                            } else {
                                libraryViewModel.load(state.session, force = true)
                            }
                        },
                        loadMore = { libraryViewModel.loadNext(state.session) },
                        rememberFocusedMedia = libraryViewModel::rememberFocusedMedia,
                        rememberGridViewport = libraryViewModel::rememberGridViewport,
                    ),
                    detailActions = DetailActions(
                        open = { mediaId ->
                            detailViewModel.load(state.session, mediaId)
                        },
                        retry = { detailViewModel.retry(state.session) },
                        rememberFocusedChild = detailViewModel::rememberFocusedChild,
                        rememberChildViewport = detailViewModel::rememberChildViewport,
                        playParent = { detail, resumePosition ->
                            val siblings = (detailState as? MediaDetailUiState.Content)
                                ?.parent
                                ?.children
                                .orEmpty()
                            playerViewModel.createFromDetail(
                                session = state.session,
                                detail = detail,
                                siblings = siblings,
                                parentTitle = (detailState as? MediaDetailUiState.Content)
                                    ?.parent
                                    ?.takeIf { it.id != detail.id }
                                    ?.title,
                                resumePositionSeconds = resumePosition,
                                settings = currentSettings,
                            )
                        },
                        playChild = playChild@{ child, resumePosition ->
                            val content = detailState as? MediaDetailUiState.Content
                                ?: return@playChild null
                            playerViewModel.createFromSummary(
                                session = state.session,
                                summary = child,
                                siblings = content.parent.children,
                                parentTitle = content.parent.title,
                                resumePositionSeconds = resumePosition,
                                settings = currentSettings,
                            )
                        },
                    ),
                    settingsActions = SettingsActions(
                        retry = settingsViewModel::load,
                        selectSection = settingsViewModel::selectSection,
                        setPlaybackMode = settingsViewModel::setPlaybackMode,
                        setTranscodeQuality = settingsViewModel::setTranscodeQuality,
                        setAutoplayNext = settingsViewModel::setAutoplayNext,
                        setAccentColor = settingsViewModel::setAccentColor,
                        setDanmaku = settingsViewModel::setDanmakuSettings,
                        setSubtitles = settingsViewModel::setSubtitleSettings,
                        setStartPage = settingsViewModel::setStartPage,
                        setReaderChapterOrder =
                            settingsViewModel::setReaderChapterOrder,
                        setImageReaderSettings =
                            settingsViewModel::setImageReaderSettings,
                        setTextReaderSettings =
                            settingsViewModel::setTextReaderSettings,
                        testConnection = {
                            settingsViewModel.testConnection(state.session)
                        },
                        manageServers = viewModel::showServerSelection,
                        logout = viewModel::logout,
                    ),
                    playerState = playerState,
                    playbackControllerFactory = playbackControllerFactory,
                    playerActions = PlayerActions(
                        load = { requestId ->
                            playerViewModel.load(state.session, requestId)
                        },
                        recordProgress = { request, position, duration, reason ->
                            playerViewModel.recordProgress(
                                session = state.session,
                                request = request,
                                positionMillis = position,
                                durationMillis = duration,
                                reason = reason,
                                // Refresh only after persistence completes to avoid an exit race.
                                onSaved = {
                                    mainViewModel.loadHome(state.session, force = true)
                                },
                            )
                        },
                        selectDefinition = { index, position ->
                            playerViewModel.selectDefinition(state.session, index, position)
                        },
                        switchItem = { offset ->
                            playerViewModel.switchAdjacent(state.session, offset)
                        },
                        retryExtra = { extra ->
                            playerViewModel.retryExtra(state.session, extra)
                        },
                        close = playerViewModel::close,
                    ),
                    readerState = readerState,
                    readerActions = ReaderActions(
                        load = { requestId ->
                            readerViewModel.load(requestId, state.session)
                        },
                        selectChapter = { chapterIndex ->
                            readerViewModel.selectChapter(state.session, chapterIndex)
                        },
                        loadMoreImages = {
                            readerViewModel.loadMoreImages(state.session)
                        },
                        setImageSettings = readerViewModel::updateImageSettings,
                        setTextSettings = readerViewModel::updateTextSettings,
                        setChapterOrder = readerViewModel::updateChapterOrder,
                        dismissChapterError = readerViewModel::dismissChapterError,
                        dismissPageError = readerViewModel::dismissPageError,
                        close = readerViewModel::close,
                    ),
                )
            }

            is BootstrapState.ConnectionError -> ConnectionErrorScreen(
                server = state.server,
                error = state.error,
                onRetry = viewModel::retryBootstrap,
                onSwitchServer = viewModel::showServerSelection,
            )
        }
    }
}
