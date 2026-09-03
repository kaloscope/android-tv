package org.kaloscope.tv.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.kaloscope.tv.R
import org.kaloscope.tv.app.navigation.HomeRoute
import org.kaloscope.tv.app.navigation.LibraryRoute
import org.kaloscope.tv.app.navigation.MediaDetailRoute
import org.kaloscope.tv.app.navigation.PlayerRoute
import org.kaloscope.tv.app.navigation.ReaderRoute
import org.kaloscope.tv.app.navigation.SearchRoute
import org.kaloscope.tv.app.navigation.SettingsRoute
import org.kaloscope.tv.app.navigation.handleMainBack
import org.kaloscope.tv.app.navigation.openMediaDetail
import org.kaloscope.tv.app.navigation.openPlayer
import org.kaloscope.tv.app.navigation.openReader
import org.kaloscope.tv.app.navigation.openSettings
import org.kaloscope.tv.app.navigation.selectRoot
import org.kaloscope.tv.core.designsystem.BrowseLayoutTokens
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeConfirmDialog
import org.kaloscope.tv.core.designsystem.KaloscopeControlTone
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.KaloscopePlaybackLoadingLayout
import org.kaloscope.tv.core.designsystem.ServerBackdrop
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.network.ServerImagePolicy
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackPreparationStage
import org.kaloscope.tv.feature.detail.MediaDetailScreen
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeBackdropPresentation
import org.kaloscope.tv.feature.home.HomeScreen
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryBackdropPresentation
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerScreen
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.reader.ReaderScreen
import org.kaloscope.tv.feature.reader.ReaderUiState
import org.kaloscope.tv.feature.search.SearchScreen
import org.kaloscope.tv.feature.search.SearchPendingDestination
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.settings.SettingsScreen
import org.kaloscope.tv.feature.settings.SettingsUiState

@Composable
internal fun MainShell(
    session: Session,
    homeState: HomeUiState,
    searchState: SearchUiState = SearchUiState.Loading,
    libraryState: LibraryUiState,
    detailState: MediaDetailUiState,
    homeActions: HomeActions,
    searchActions: SearchActions,
    libraryActions: LibraryActions,
    detailActions: DetailActions,
    settingsState: SettingsUiState = SettingsUiState.Content(TvSettings()),
    initialRoute: NavKey = HomeRoute,
    settingsActions: SettingsActions,
    playerState: PlayerUiState = PlayerUiState.Loading(),
    playbackControllerFactory: PlaybackControllerFactory? = null,
    playerActions: PlayerActions,
    onExit: () -> Unit,
    readerState: ReaderUiState = ReaderUiState.Idle,
    readerActions: ReaderActions = ReaderActions(),
) {
    // Saved start-page changes take effect only when a new authenticated shell is created.
    val launchRoute = remember(session.server.id, session.user.id) { initialRoute }
    val backStack = rememberNavBackStack(launchRoute)
    val homeFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val libraryFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val searchContentEntryFocus = remember { FocusRequester() }
    val libraryContentEntryFocus = remember { FocusRequester() }
    val selectedSettingsSectionFocus = remember { FocusRequester() }
    var restoreMediaId by remember { mutableStateOf<Long?>(null) }
    var currentRoute by remember {
        mutableStateOf<NavKey>(backStack.lastOrNull() ?: HomeRoute)
    }
    var destinationEntryKeepsTopFocus by remember {
        mutableStateOf(false)
    }
    var exitConfirmationOpen by remember {
        mutableStateOf(false)
    }
    var restoringSearchFocusAfterPlayer by remember {
        mutableStateOf(false)
    }
    var pendingReaderCloseRequestId by remember {
        mutableStateOf<String?>(null)
    }
    var homeBackdrop by remember(session.server.id) {
        mutableStateOf<HomeBackdropPresentation?>(null)
    }
    var libraryBackdrop by remember(session.server.id) {
        mutableStateOf<LibraryBackdropPresentation?>(null)
    }
    val resumePositionsByMediaId = remember(homeState) {
        (homeState as? HomeUiState.Content)
            ?.items
            .orEmpty()
            .associate { it.mediaId to it.positionSeconds }
    }
    val searchPlaybackPreparing = currentRoute == SearchRoute &&
        (searchState as? SearchUiState.Content)?.resolvingResultId != null

    LaunchedEffect(currentRoute, pendingReaderCloseRequestId) {
        val requestId = pendingReaderCloseRequestId
        if (currentRoute == SearchRoute && requestId != null) {
            // Draw and present Search loading before releasing the outgoing Reader state.
            withFrameNanos { }
            withFrameNanos { }
            readerActions.close(requestId)
            searchActions.cancelResolution()
            pendingReaderCloseRequestId = null
        }
    }

    // TV launchers do not guarantee an initial Compose focus owner.
    LaunchedEffect(launchRoute) {
        when (launchRoute) {
            SearchRoute -> {
                destinationEntryKeepsTopFocus = true
                searchActions.open()
                searchFocus.requestFocus()
            }

            LibraryRoute -> {
                destinationEntryKeepsTopFocus = true
                libraryActions.open()
                libraryFocus.requestFocus()
            }

            SettingsRoute -> destinationEntryKeepsTopFocus = false
            else -> {
                destinationEntryKeepsTopFocus = true
                homeFocus.requestFocus()
            }
        }
    }

    fun selectRoot(route: NavKey) {
        backStack.selectRoot(route)
        currentRoute = route
    }

    fun navigateToMediaDetail(mediaId: Long) {
        destinationEntryKeepsTopFocus = false
        restoreMediaId = null
        backStack.openMediaDetail(mediaId)
        currentRoute = MediaDetailRoute(mediaId)
        detailActions.open(mediaId)
    }

    fun navigateToPlayer(requestId: String) {
        backStack.openPlayer(requestId)
        currentRoute = PlayerRoute(requestId)
        playerActions.load(requestId)
    }

    fun activateTopDestination(
        route: NavKey,
        keepTopBarFocus: Boolean,
    ) {
        restoringSearchFocusAfterPlayer = false
        if (route == currentRoute) {
            return
        }
        destinationEntryKeepsTopFocus = keepTopBarFocus
        when (route) {
            HomeRoute -> {
                restoreMediaId = null
                selectRoot(HomeRoute)
            }

            SearchRoute -> {
                selectRoot(SearchRoute)
                searchActions.open()
            }

            LibraryRoute -> {
                restoreMediaId = null
                selectRoot(LibraryRoute)
                libraryActions.open()
            }

            SettingsRoute -> {
                backStack.openSettings()
                currentRoute = SettingsRoute
            }
        }
    }

    fun goBack() {
        val leavingRoute = currentRoute
        val returnRoute = backStack.getOrNull(backStack.lastIndex - 1) ?: HomeRoute
        val returningToSearchFromFullscreen =
            (leavingRoute is PlayerRoute || leavingRoute is ReaderRoute) &&
                returnRoute == SearchRoute
        val readerReturningToSearch =
            leavingRoute is ReaderRoute && returningToSearchFromFullscreen
        // Releasing player focus can briefly focus Home before Search restores its result.
        restoringSearchFocusAfterPlayer = returningToSearchFromFullscreen
        if (returningToSearchFromFullscreen && !readerReturningToSearch) {
            // Keep Search covered until the fullscreen destination is ready,
            // then reveal it on pop.
            searchActions.cancelResolution()
        }
        if (leavingRoute is MediaDetailRoute &&
            returnRoute in setOf(HomeRoute, LibraryRoute)
        ) {
            // Publish the restore target before the previous entry becomes active again.
            restoreMediaId = leavingRoute.mediaId
        }
        if (leavingRoute is PlayerRoute) {
            playerActions.close(leavingRoute.requestId)
        }
        if (leavingRoute is ReaderRoute) {
            if (readerReturningToSearch) {
                pendingReaderCloseRequestId = leavingRoute.requestId
            } else {
                readerActions.close(leavingRoute.requestId)
            }
        }
        backStack.handleMainBack()
        currentRoute = backStack.lastOrNull() ?: HomeRoute
    }

    BackHandler(
        enabled = currentRoute == HomeRoute ||
            currentRoute == SearchRoute ||
            currentRoute == LibraryRoute ||
            currentRoute == SettingsRoute,
    ) {
        if (currentRoute == SearchRoute && searchActions.cancelResolution()) {
            destinationEntryKeepsTopFocus = false
        } else {
            exitConfirmationOpen = true
        }
    }

    KaloscopeBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = ::goBack,
                transitionSpec = {
                    mainContentTransform(initialState.key, targetState.key)
                },
                popTransitionSpec = {
                    mainContentTransform(initialState.key, targetState.key)
                },
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        RootDestinationFrame(
                            backdrop = {
                                homeBackdrop?.let { backdrop ->
                                    RootFullscreenBackdrop(
                                        session = session,
                                        path = backdrop.path,
                                        testTag = "home-fullscreen-backdrop",
                                    )
                                }
                            },
                        ) {
                            HomeScreen(
                                session = session,
                                state = homeState,
                                onRefresh = homeActions.refresh,
                                restoreMediaId = restoreMediaId,
                                topNavigationFocusRequester = homeFocus,
                                onOpenLibrary = {
                                    activateTopDestination(
                                        LibraryRoute,
                                        keepTopBarFocus = false,
                                    )
                                },
                                onOpenSearch = {
                                    activateTopDestination(
                                        SearchRoute,
                                        keepTopBarFocus = false,
                                    )
                                },
                                onOpenMedia = ::navigateToMediaDetail,
                                onPlayHistory = { item ->
                                    destinationEntryKeepsTopFocus = false
                                    restoreMediaId = item.mediaId
                                    homeActions.play(item)?.let(::navigateToPlayer)
                                },
                                onBackdropChanged = { homeBackdrop = it },
                            )
                        }
                    }
                    entry<SearchRoute> {
                        val searchContent = searchState as? SearchUiState.Content
                        val pendingDestination = searchContent?.pendingDestination
                        LaunchedEffect(pendingDestination) {
                            when (val destination = pendingDestination) {
                                is SearchPendingDestination.Player -> {
                                    val requestId = destination.requestId
                                    destinationEntryKeepsTopFocus = false
                                    navigateToPlayer(requestId)
                                    searchActions.consumeDestination(requestId)
                                }

                                is SearchPendingDestination.Reader -> {
                                    val requestId = destination.requestId
                                    destinationEntryKeepsTopFocus = false
                                    backStack.openReader(requestId)
                                    currentRoute = ReaderRoute(requestId)
                                    readerActions.load(requestId)
                                    searchActions.consumeDestination(requestId)
                                }

                                null -> Unit
                            }
                        }
                        if (searchContent?.resolvingResultId != null) {
                            KaloscopePlaybackLoadingLayout(
                                stage = PlaybackPreparationStage.Resource,
                                testTag = "search-playback-loading",
                            )
                        } else {
                            RootDestinationFrame {
                                SearchScreen(
                                    session = session,
                                    state = searchState,
                                    requestInitialFocus = !destinationEntryKeepsTopFocus,
                                    indexerEntryFocusRequester = searchContentEntryFocus,
                                    topNavigationFocusRequester = searchFocus,
                                    onRefreshIndexers = searchActions.refreshIndexers,
                                    onSelectIndexer = searchActions.selectIndexer,
                                    onQueryChange = searchActions.updateQuery,
                                    onSearch = searchActions.search,
                                    onRetry = searchActions.retry,
                                    onLoadMore = searchActions.loadMore,
                                    onResultFocused = { resultId ->
                                        restoringSearchFocusAfterPlayer = false
                                        searchActions.rememberFocusedResult(resultId)
                                    },
                                    onGridViewportChanged =
                                        searchActions.rememberGridViewport,
                                    onOpenResult = searchActions.openResult,
                                    onOpenFilters = searchActions.openFilters,
                                    onDismissFilters = searchActions.dismissFilters,
                                    onApplyFilters = searchActions.applyFilters,
                                    onClearFilters = searchActions.clearFilters,
                                    onManageServers = settingsActions.manageServers,
                                )
                            }
                        }
                    }
                    entry<LibraryRoute> {
                        RootDestinationFrame(
                            backdrop = {
                                libraryBackdrop?.let { backdrop ->
                                    RootFullscreenBackdrop(
                                        session = session,
                                        path = backdrop.path,
                                        testTag = "library-fullscreen-backdrop",
                                    )
                                }
                            },
                        ) {
                            LibraryScreen(
                                session = session,
                                state = libraryState,
                                restoreMediaId = restoreMediaId,
                                requestInitialFocus = !destinationEntryKeepsTopFocus,
                                libraryEntryFocusRequester = libraryContentEntryFocus,
                                topNavigationFocusRequester = libraryFocus,
                                onSelectLibrary = libraryActions.select,
                                onQueryChange = libraryActions.updateQuery,
                                onSearch = libraryActions.search,
                                onRetry = libraryActions.retry,
                                onLoadMore = libraryActions.loadMore,
                                onMediaFocused = libraryActions.rememberFocusedMedia,
                                onGridViewportChanged = libraryActions.rememberGridViewport,
                                onBackdropChanged = { libraryBackdrop = it },
                                onOpenMedia = ::navigateToMediaDetail,
                            )
                        }
                    }
                    entry<SettingsRoute> {
                        RootDestinationFrame {
                            SettingsScreen(
                                session = session,
                                state = settingsState,
                                requestInitialFocus = !destinationEntryKeepsTopFocus,
                                selectedSectionFocusRequester =
                                    selectedSettingsSectionFocus,
                                topNavigationFocusRequester = settingsFocus,
                                onRetry = settingsActions.retry,
                                onSelectSection = settingsActions.selectSection,
                                onPlaybackMode = settingsActions.setPlaybackMode,
                                onTranscodeQuality = settingsActions.setTranscodeQuality,
                                onAutoplayNext = settingsActions.setAutoplayNext,
                                onAccentColor = settingsActions.setAccentColor,
                                onDanmakuSettings = settingsActions.setDanmaku,
                                onSubtitleSettings = settingsActions.setSubtitles,
                                onStartPage = settingsActions.setStartPage,
                                onReaderChapterOrder =
                                    settingsActions.setReaderChapterOrder,
                                onImageReaderSettings =
                                    settingsActions.setImageReaderSettings,
                                onTextReaderSettings =
                                    settingsActions.setTextReaderSettings,
                                onTestConnection = settingsActions.testConnection,
                                onManageServers = settingsActions.manageServers,
                                onLogout = settingsActions.logout,
                            )
                        }
                    }
                    entry<MediaDetailRoute> {
                        MediaDetailScreen(
                            session = session,
                            state = detailState,
                            resumePositionsByMediaId = resumePositionsByMediaId,
                            onBack = ::goBack,
                            onRetry = detailActions.retry,
                            onChildFocused = detailActions.rememberFocusedChild,
                            onChildViewportChanged = detailActions.rememberChildViewport,
                            onPlayParent = { detail, resume ->
                                destinationEntryKeepsTopFocus = false
                                detailActions.playParent(detail, resume)
                                    ?.let(::navigateToPlayer)
                            },
                            onPlayChild = { child, resume ->
                                destinationEntryKeepsTopFocus = false
                                detailActions.playChild(child, resume)
                                    ?.let(::navigateToPlayer)
                            },
                        )
                    }
                    entry<PlayerRoute> {
                        val factory = checkNotNull(playbackControllerFactory) {
                            "PlaybackControllerFactory is required for PlayerRoute"
                        }
                        PlayerScreen(
                            session = session,
                            state = playerState,
                            controllerFactory = factory,
                            onProgress = playerActions.recordProgress,
                            onSelectDefinition = playerActions.selectDefinition,
                            onPrevious = { playerActions.switchItem(-1) },
                            onNext = { playerActions.switchItem(1) },
                            onSelectEpisode = playerActions.selectEpisode,
                            onRetryExtra = playerActions.retryExtra,
                            onBack = ::goBack,
                            onSubtitlePreferencesChanged =
                                settingsActions.setPlayerSubtitlePreferences,
                            onDanmakuPreferencesChanged =
                                settingsActions.setPlayerDanmakuPreferences,
                        )
                    }
                    entry<ReaderRoute> {
                        ReaderScreen(
                            session = session,
                            state = readerState,
                            onBack = ::goBack,
                            onSelectChapter = readerActions.selectChapter,
                            onLoadMoreImages = readerActions.loadMoreImages,
                            onImageSettings = readerActions.setImageSettings,
                            onTextSettings = readerActions.setTextSettings,
                            onChapterOrder = readerActions.setChapterOrder,
                            onDismissChapterError = readerActions.dismissChapterError,
                            onDismissPageError = readerActions.dismissPageError,
                            onImagePreferencesChanged =
                                settingsActions.setImageReaderSettings,
                            onTextPreferencesChanged =
                                settingsActions.setTextReaderSettings,
                        )
                    }
                },
            )
            if (!currentRoute.isFullscreenMedia() && !searchPlaybackPreparing) {
                AnimatedVisibility(
                    visible = currentRoute !is MediaDetailRoute,
                    enter = fadeIn(tween(KaloscopeMotion.ContentMillis)),
                    exit = fadeOut(tween(KaloscopeMotion.ContentMillis)),
                ) {
                    MainTopBar(
                        currentRoute = currentRoute,
                        onHome = {
                            activateTopDestination(HomeRoute, keepTopBarFocus = false)
                        },
                        onSearch = {
                            activateTopDestination(SearchRoute, keepTopBarFocus = false)
                        },
                        onLibrary = {
                            activateTopDestination(LibraryRoute, keepTopBarFocus = false)
                        },
                        onSettings = {
                            activateTopDestination(SettingsRoute, keepTopBarFocus = false)
                        },
                        onDestinationFocused = { route ->
                            if (!restoringSearchFocusAfterPlayer || route == SearchRoute) {
                                activateTopDestination(route, keepTopBarFocus = true)
                            }
                        },
                        homeFocus = homeFocus,
                        searchFocus = searchFocus,
                        libraryFocus = libraryFocus,
                        settingsFocus = settingsFocus,
                        searchMenuFocus = searchContentEntryFocus,
                        libraryMenuFocus = libraryContentEntryFocus,
                        settingsMenuFocus = selectedSettingsSectionFocus,
                    )
                }
            }
        }
        if (exitConfirmationOpen) {
            KaloscopeConfirmDialog(
                title = stringResource(R.string.exit_confirmation_title),
                message = stringResource(R.string.exit_confirmation_message),
                cancelLabel = stringResource(R.string.cancel),
                confirmLabel = stringResource(R.string.exit_app),
                confirmTone = KaloscopeControlTone.Danger,
                onDismiss = { exitConfirmationOpen = false },
                onConfirm = {
                    exitConfirmationOpen = false
                    onExit()
                },
            )
        }
    }
}

private fun Any?.isFullscreenMedia(): Boolean =
    this is PlayerRoute || this is ReaderRoute

private fun mainContentTransform(
    initialKey: Any?,
    targetKey: Any?,
): ContentTransform =
    if (initialKey.isFullscreenMedia() || targetKey.isFullscreenMedia()) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        fadeIn(tween(KaloscopeMotion.ContentMillis)) togetherWith
            fadeOut(tween(KaloscopeMotion.ContentMillis))
    }

@Composable
private fun RootDestinationFrame(
    backdrop: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        backdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = BrowseLayoutTokens.ScreenHorizontalPadding,
                    top = BrowseLayoutTokens.ScreenContentTopPadding,
                    end = BrowseLayoutTokens.ScreenHorizontalPadding,
                    bottom = BrowseLayoutTokens.ScreenContentBottomPadding,
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun RootFullscreenBackdrop(
    session: Session,
    path: String,
    testTag: String,
) {
    RootFullscreenBackdropFrame(testTag = testTag) { imageModifier ->
        ServerBackdrop(
            session = session,
            backdropPath = path,
            policy = ServerImagePolicy.Store,
            modifier = imageModifier,
        )
    }
}

@Composable
internal fun RootFullscreenBackdropFrame(
    testTag: String,
    imageLayer: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag),
    ) {
        imageLayer(
            Modifier
                .fillMaxSize()
                .rootBackdropEdgeFade(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x24070B14))
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xD9070B14),
                        0.24f to Color(0x65070B14),
                        0.52f to Color.Transparent,
                    ),
                )
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xF2070B14),
                        0.36f to Color(0xC2070B14),
                        0.7f to Color(0x42070B14),
                        1f to Color(0x30070B14),
                    ),
                )
                .background(
                    Brush.verticalGradient(
                        0.48f to Color.Transparent,
                        0.78f to Color(0x70070B14),
                        1f to Color(0xEB070B14),
                    ),
                ),
        )
    }
}

internal fun Modifier.rootBackdropEdgeFade(): Modifier =
    graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val horizontalMask = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.12f to Color.White,
            0.82f to Color.White,
            1f to Color.Transparent,
        )
        val verticalMask = Brush.verticalGradient(
            0f to Color.Transparent,
            0.18f to Color.White,
            0.76f to Color.White,
            1f to Color.Transparent,
        )
        onDrawWithContent {
            drawContent()
            drawRect(horizontalMask, blendMode = BlendMode.DstIn)
            drawRect(verticalMask, blendMode = BlendMode.DstIn)
        }
    }
