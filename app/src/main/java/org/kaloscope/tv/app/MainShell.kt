package org.kaloscope.tv.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.kaloscope.tv.app.navigation.HomeRoute
import org.kaloscope.tv.app.navigation.LibraryRoute
import org.kaloscope.tv.app.navigation.MediaDetailRoute
import org.kaloscope.tv.app.navigation.PlayerRoute
import org.kaloscope.tv.app.navigation.SearchRoute
import org.kaloscope.tv.app.navigation.SettingsRoute
import org.kaloscope.tv.app.navigation.handleMainBack
import org.kaloscope.tv.app.navigation.openMediaDetail
import org.kaloscope.tv.app.navigation.openPlayer
import org.kaloscope.tv.app.navigation.openSettings
import org.kaloscope.tv.app.navigation.selectRoot
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.ServerBackdrop
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.feature.detail.MediaDetailScreen
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeBackdropPresentation
import org.kaloscope.tv.feature.home.HomeScreen
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerScreen
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.search.SearchScreen
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
    playerState: PlayerUiState = PlayerUiState.Loading,
    playbackControllerFactory: PlaybackControllerFactory? = null,
    playerActions: PlayerActions,
) {
    // Saved start-page changes take effect only when a new authenticated shell is created.
    val launchRoute = remember(session.server.id, session.user.id) { initialRoute }
    val backStack = rememberNavBackStack(launchRoute)
    val homeFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val libraryFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    var restoreMediaId by remember { mutableStateOf<Long?>(null) }
    var currentRoute by remember {
        mutableStateOf<NavKey>(backStack.lastOrNull() ?: HomeRoute)
    }
    var homeBackdrop by remember(session.server.id) {
        mutableStateOf<HomeBackdropPresentation?>(null)
    }

    // TV launchers do not guarantee an initial Compose focus owner.
    LaunchedEffect(launchRoute) {
        when (launchRoute) {
            SearchRoute -> {
                searchActions.open()
                searchFocus.requestFocus()
            }

            LibraryRoute -> {
                libraryActions.open()
                libraryFocus.requestFocus()
            }

            else -> homeFocus.requestFocus()
        }
    }

    fun selectRoot(route: NavKey) {
        backStack.selectRoot(route)
        currentRoute = route
    }

    fun goBack() {
        val leavingRoute = currentRoute
        val returnRoute = backStack.getOrNull(backStack.lastIndex - 1) ?: HomeRoute
        if (leavingRoute is MediaDetailRoute &&
            returnRoute in setOf(HomeRoute, LibraryRoute)
        ) {
            // Publish the restore target before the previous entry becomes active again.
            restoreMediaId = leavingRoute.mediaId
        }
        if (leavingRoute is PlayerRoute) {
            playerActions.close(leavingRoute.requestId)
        }
        backStack.handleMainBack()
        currentRoute = backStack.lastOrNull() ?: HomeRoute
    }

    BackHandler(enabled = currentRoute != HomeRoute && currentRoute !is PlayerRoute) {
        goBack()
    }

    KaloscopeBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentRoute == HomeRoute) {
                homeBackdrop?.let { backdrop ->
                    HomeFullscreenBackdrop(
                        session = session,
                        backdrop = backdrop,
                    )
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                if (currentRoute !is MediaDetailRoute && currentRoute !is PlayerRoute) {
                    MainTopBar(
                        currentRoute = currentRoute,
                        onHome = {
                            restoreMediaId = null
                            selectRoot(HomeRoute)
                        },
                        onSearch = {
                            selectRoot(SearchRoute)
                            searchActions.open()
                        },
                        onLibrary = {
                            restoreMediaId = null
                            selectRoot(LibraryRoute)
                            libraryActions.open()
                        },
                        onSettings = {
                            backStack.openSettings()
                            currentRoute = SettingsRoute
                        },
                        homeFocus = homeFocus,
                        searchFocus = searchFocus,
                        libraryFocus = libraryFocus,
                        settingsFocus = settingsFocus,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (currentRoute is MediaDetailRoute || currentRoute is PlayerRoute) {
                                Modifier
                            } else {
                                Modifier.padding(horizontal = 44.dp, vertical = 24.dp)
                            },
                        ),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = ::goBack,
                        transitionSpec = {
                            if (initialState.key is PlayerRoute || targetState.key is PlayerRoute) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(tween(KaloscopeMotion.ContentMillis)) togetherWith
                                    fadeOut(tween(KaloscopeMotion.ContentMillis))
                            }
                        },
                        popTransitionSpec = {
                            if (initialState.key is PlayerRoute || targetState.key is PlayerRoute) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(tween(KaloscopeMotion.ContentMillis)) togetherWith
                                    fadeOut(tween(KaloscopeMotion.ContentMillis))
                            }
                        },
                        entryProvider = entryProvider {
                            entry<HomeRoute> {
                                HomeScreen(
                                    session = session,
                                    state = homeState,
                                    onRefresh = homeActions.refresh,
                                    restoreMediaId = restoreMediaId,
                                    onOpenLibrary = {
                                        restoreMediaId = null
                                        selectRoot(LibraryRoute)
                                        libraryActions.open()
                                    },
                                    onOpenMedia = { mediaId ->
                                        restoreMediaId = null
                                        backStack.openMediaDetail(mediaId)
                                        currentRoute = MediaDetailRoute(mediaId)
                                        detailActions.open(mediaId)
                                    },
                                    onPlayHistory = { item ->
                                        restoreMediaId = item.mediaId
                                        homeActions.play(item)?.let { requestId ->
                                            backStack.openPlayer(requestId)
                                            currentRoute = PlayerRoute(requestId)
                                            playerActions.load(requestId)
                                        }
                                    },
                                    onBackdropChanged = { homeBackdrop = it },
                                )
                            }
                            entry<SearchRoute> {
                                val pendingRequestId = (
                                    searchState as? SearchUiState.Content
                                )?.pendingPlaybackRequestId
                                LaunchedEffect(pendingRequestId) {
                                    pendingRequestId?.let { requestId ->
                                        backStack.openPlayer(requestId)
                                        currentRoute = PlayerRoute(requestId)
                                        playerActions.load(requestId)
                                        searchActions.consumePlaybackRequest(requestId)
                                    }
                                }
                                SearchScreen(
                                    session = session,
                                    state = searchState,
                                    onRefreshIndexers = searchActions.refreshIndexers,
                                    onSelectIndexer = searchActions.selectIndexer,
                                    onQueryChange = searchActions.updateQuery,
                                    onSearch = searchActions.search,
                                    onRetry = searchActions.retry,
                                    onLoadMore = searchActions.loadMore,
                                    onResultFocused = searchActions.rememberFocusedResult,
                                    onGridViewportChanged = searchActions.rememberGridViewport,
                                    onPlay = searchActions.play,
                                    onOpenFilters = searchActions.openFilters,
                                    onDismissFilters = searchActions.dismissFilters,
                                    onApplyFilters = searchActions.applyFilters,
                                    onClearFilters = searchActions.clearFilters,
                                )
                            }
                            entry<LibraryRoute> {
                                LibraryScreen(
                                    session = session,
                                    state = libraryState,
                                    restoreMediaId = restoreMediaId,
                                    onSelectLibrary = libraryActions.select,
                                    onQueryChange = libraryActions.updateQuery,
                                    onSearch = libraryActions.search,
                                    onRetry = libraryActions.retry,
                                    onLoadMore = libraryActions.loadMore,
                                    onMediaFocused = libraryActions.rememberFocusedMedia,
                                    onGridViewportChanged = libraryActions.rememberGridViewport,
                                    onOpenMedia = { mediaId ->
                                        restoreMediaId = null
                                        backStack.openMediaDetail(mediaId)
                                        currentRoute = MediaDetailRoute(mediaId)
                                        detailActions.open(mediaId)
                                    },
                                )
                            }
                            entry<SettingsRoute> {
                                SettingsScreen(
                                    session = session,
                                    state = settingsState,
                                    onRetry = settingsActions.retry,
                                    onSelectSection = settingsActions.selectSection,
                                    onPlaybackMode = settingsActions.setPlaybackMode,
                                    onTranscodeResolution = settingsActions.setTranscodeResolution,
                                    onAutoplayNext = settingsActions.setAutoplayNext,
                                    onDanmakuSettings = settingsActions.setDanmaku,
                                    onSubtitleSettings = settingsActions.setSubtitles,
                                    onStartPage = settingsActions.setStartPage,
                                    onTestConnection = settingsActions.testConnection,
                                    onManageServers = settingsActions.manageServers,
                                    onLogout = settingsActions.logout,
                                )
                            }
                            entry<MediaDetailRoute> {
                                val displayedId = (detailState as? MediaDetailUiState.Content)?.let {
                                    (it.selectedChild ?: it.parent).id
                                }
                                val resumePosition = (homeState as? HomeUiState.Content)
                                    ?.items
                                    ?.firstOrNull { it.mediaId == displayedId }
                                    ?.positionSeconds
                                MediaDetailScreen(
                                    session = session,
                                    state = detailState,
                                    resumePositionSeconds = resumePosition,
                                    onBack = ::goBack,
                                    onRetry = detailActions.retry,
                                    onSelectChild = detailActions.selectChild,
                                    onPlay = { detail, resume ->
                                        detailActions.play(detail, resume)?.let { requestId ->
                                            backStack.openPlayer(requestId)
                                            currentRoute = PlayerRoute(requestId)
                                            playerActions.load(requestId)
                                        }
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
                                    onRetryExtra = playerActions.retryExtra,
                                    onBack = ::goBack,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFullscreenBackdrop(
    session: Session,
    backdrop: HomeBackdropPresentation,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home-fullscreen-backdrop"),
    ) {
        ServerBackdrop(
            session = session,
            backdropPath = backdrop.path,
            title = backdrop.title,
            modifier = Modifier.fillMaxSize(),
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
