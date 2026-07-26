package org.kaloscope.tv.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.tv.material3.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import org.kaloscope.tv.R
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
import org.kaloscope.tv.core.designsystem.BackgroundRaised
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeBrand
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.feature.detail.MediaDetailScreen
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeScreen
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerExtra
import org.kaloscope.tv.feature.player.PlayerScreen
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.search.SearchScreen
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.settings.SettingsScreen
import org.kaloscope.tv.feature.settings.SettingsSection
import org.kaloscope.tv.feature.settings.SettingsUiState

@Composable
internal fun MainShell(
    session: Session,
    homeState: HomeUiState,
    searchState: SearchUiState = SearchUiState.Loading,
    libraryState: LibraryUiState,
    detailState: MediaDetailUiState,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onRefreshIndexers: () -> Unit = {},
    onSelectIndexer: (Long) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchNetwork: () -> Unit = {},
    onRetrySearch: () -> Unit = {},
    onLoadMoreSearch: () -> Unit = {},
    onSearchResultFocused: (String) -> Unit = {},
    onSearchGridViewportChanged: (GridViewportSnapshot) -> Unit = {},
    onPlaySearchResult: (String) -> Unit = {},
    onOpenSearchFilters: () -> Unit = {},
    onDismissSearchFilters: () -> Unit = {},
    onApplySearchFilters: (Map<String, SearchFilterValue>) -> Unit = {},
    onClearSearchFilters: () -> Unit = {},
    onConsumeSearchPlayback: (String) -> Unit = {},
    onOpenLibrary: () -> Unit,
    onSelectLibrary: (Long) -> Unit,
    onLibraryQueryChange: (String) -> Unit,
    onSearchLibrary: () -> Unit,
    onRetryLibrary: () -> Unit,
    onLoadMoreMedia: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onLibraryGridViewportChanged: (GridViewportSnapshot) -> Unit = {},
    onOpenMedia: (Long) -> Unit,
    onRetryDetail: () -> Unit,
    onSelectMediaChild: (Long) -> Unit,
    onLogout: () -> Unit,
    settingsState: SettingsUiState = SettingsUiState.Content(TvSettings()),
    initialRoute: NavKey = HomeRoute,
    onRetrySettings: () -> Unit = {},
    onSelectSettingsSection: (SettingsSection) -> Unit = {},
    onPlaybackModeSetting: (PlaybackMode) -> Unit = {},
    onTranscodeResolutionSetting: (TranscodeResolution) -> Unit = {},
    onAutoplayNextSetting: (Boolean) -> Unit = {},
    onDanmakuSettings: (DanmakuSettings) -> Unit = {},
    onSubtitleSettings: (SubtitleSettings) -> Unit = {},
    onStartPageSetting: (StartPage) -> Unit = {},
    onTestConnection: () -> Unit = {},
    onManageServers: () -> Unit = {},
    playerState: PlayerUiState = PlayerUiState.Loading,
    playbackControllerFactory: PlaybackControllerFactory? = null,
    onPlayHistory: (WatchHistoryItem) -> String? = { null },
    onPlayDetail: (MediaDetail, Long?) -> String? = { _, _ -> null },
    onLoadPlayer: (String) -> Unit = {},
    onPlayerProgress: (PlaybackRequest, Long, Long, ProgressReason) -> Unit =
        { _, _, _, _ -> },
    onSelectPlayerDefinition: (Int, Long) -> Unit = { _, _ -> },
    onSwitchPlayerItem: (Int) -> Unit = {},
    onRetryPlayerExtra: (PlayerExtra) -> Unit = {},
    onClosePlayer: (String) -> Unit = {},
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

    // TV launchers do not guarantee an initial Compose focus owner.
    LaunchedEffect(launchRoute) {
        when (launchRoute) {
            SearchRoute -> {
                onOpenSearch()
                searchFocus.requestFocus()
            }

            LibraryRoute -> {
                onOpenLibrary()
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
            onClosePlayer(leavingRoute.requestId)
        }
        backStack.handleMainBack()
        currentRoute = backStack.lastOrNull() ?: HomeRoute
    }

    BackHandler(enabled = currentRoute != HomeRoute && currentRoute !is PlayerRoute) {
        goBack()
    }

    KaloscopeBackground {
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
                    onOpenSearch()
                },
                onLibrary = {
                    restoreMediaId = null
                    selectRoot(LibraryRoute)
                    onOpenLibrary()
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
                            onRefresh = onRefresh,
                            restoreMediaId = restoreMediaId,
                            onOpenLibrary = {
                                restoreMediaId = null
                                selectRoot(LibraryRoute)
                                onOpenLibrary()
                            },
                            onOpenMedia = { mediaId ->
                                restoreMediaId = null
                                backStack.openMediaDetail(mediaId)
                                currentRoute = MediaDetailRoute(mediaId)
                                onOpenMedia(mediaId)
                            },
                            onPlayHistory = { item ->
                                restoreMediaId = item.mediaId
                                onPlayHistory(item)?.let { requestId ->
                                    backStack.openPlayer(requestId)
                                    currentRoute = PlayerRoute(requestId)
                                    onLoadPlayer(requestId)
                                }
                            },
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
                                onLoadPlayer(requestId)
                                onConsumeSearchPlayback(requestId)
                            }
                        }
                        SearchScreen(
                            session = session,
                            state = searchState,
                            onRefreshIndexers = onRefreshIndexers,
                            onSelectIndexer = onSelectIndexer,
                            onQueryChange = onSearchQueryChange,
                            onSearch = onSearchNetwork,
                            onRetry = onRetrySearch,
                            onLoadMore = onLoadMoreSearch,
                            onResultFocused = onSearchResultFocused,
                            onGridViewportChanged = onSearchGridViewportChanged,
                            onPlay = onPlaySearchResult,
                            onOpenFilters = onOpenSearchFilters,
                            onDismissFilters = onDismissSearchFilters,
                            onApplyFilters = onApplySearchFilters,
                            onClearFilters = onClearSearchFilters,
                        )
                    }
                    entry<LibraryRoute> {
                        LibraryScreen(
                            session = session,
                            state = libraryState,
                            restoreMediaId = restoreMediaId,
                            onSelectLibrary = onSelectLibrary,
                            onQueryChange = onLibraryQueryChange,
                            onSearch = onSearchLibrary,
                            onRetry = onRetryLibrary,
                            onLoadMore = onLoadMoreMedia,
                            onMediaFocused = onMediaFocused,
                            onGridViewportChanged = onLibraryGridViewportChanged,
                            onOpenMedia = { mediaId ->
                                restoreMediaId = null
                                backStack.openMediaDetail(mediaId)
                                currentRoute = MediaDetailRoute(mediaId)
                                onOpenMedia(mediaId)
                            },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            session = session,
                            state = settingsState,
                            onRetry = onRetrySettings,
                            onSelectSection = onSelectSettingsSection,
                            onPlaybackMode = onPlaybackModeSetting,
                            onTranscodeResolution = onTranscodeResolutionSetting,
                            onAutoplayNext = onAutoplayNextSetting,
                            onDanmakuSettings = onDanmakuSettings,
                            onSubtitleSettings = onSubtitleSettings,
                            onStartPage = onStartPageSetting,
                            onTestConnection = onTestConnection,
                            onManageServers = onManageServers,
                            onLogout = onLogout,
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
                            onRetry = onRetryDetail,
                            onSelectChild = onSelectMediaChild,
                            onPlay = { detail, resume ->
                                onPlayDetail(detail, resume)?.let { requestId ->
                                    backStack.openPlayer(requestId)
                                    currentRoute = PlayerRoute(requestId)
                                    onLoadPlayer(requestId)
                                }
                            },
                        )
                    }
                    entry<PlayerRoute> { route ->
                        val factory = checkNotNull(playbackControllerFactory) {
                            "PlaybackControllerFactory is required for PlayerRoute"
                        }
                        PlayerScreen(
                            session = session,
                            state = playerState,
                            controllerFactory = factory,
                            onProgress = onPlayerProgress,
                            onSelectDefinition = onSelectPlayerDefinition,
                            onPrevious = { onSwitchPlayerItem(-1) },
                            onNext = { onSwitchPlayerItem(1) },
                            onRetryExtra = onRetryPlayerExtra,
                            onBack = ::goBack,
                        )
                    }
                },
            )
        }
    }
    }
}

@Composable
private fun MainTopBar(
    currentRoute: NavKey?,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
    homeFocus: FocusRequester,
    searchFocus: FocusRequester,
    libraryFocus: FocusRequester,
    settingsFocus: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(BackgroundRaised.copy(alpha = 0.88f))
            .padding(horizontal = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KaloscopeBrand(
            name = stringResource(R.string.app_name),
            caption = stringResource(R.string.tv_experience),
            compact = true,
            modifier = Modifier.width(230.dp),
        )
        Row(
            modifier = Modifier
                .background(Panel.copy(alpha = 0.82f), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MainNavButton(
                text = stringResource(R.string.home),
                selected = currentRoute == HomeRoute,
                enabled = true,
                onClick = onHome,
                modifier = Modifier
                    .focusRequester(homeFocus)
                    .focusProperties { right = searchFocus },
            )
            MainNavButton(
                text = stringResource(R.string.search),
                selected = currentRoute == SearchRoute,
                enabled = true,
                onClick = onSearch,
                modifier = Modifier
                    .focusRequester(searchFocus)
                    .focusProperties {
                        left = homeFocus
                        right = libraryFocus
                    },
            )
            MainNavButton(
                text = stringResource(R.string.library),
                selected = currentRoute == LibraryRoute,
                enabled = true,
                onClick = onLibrary,
                modifier = Modifier
                    .focusRequester(libraryFocus)
                    .focusProperties {
                        left = searchFocus
                        right = settingsFocus
                    },
            )
        }
        Spacer(Modifier.weight(1f))
        SettingsButton(
            selected = currentRoute == SettingsRoute,
            onClick = onSettings,
            modifier = Modifier
                .focusRequester(settingsFocus)
                .focusProperties { left = libraryFocus },
        )
        Spacer(Modifier.width(18.dp))
        Clock()
    }
}

@Composable
private fun MainNavButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeFocusSurface(
        onClick = onClick,
        selected = selected,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(11.dp),
        focusedContainerColor = PanelElevated,
        focusScale = 1.02f,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) OnBackground else Color(0xFF656D80),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SettingsButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.settings)
    KaloscopeFocusSurface(
        onClick = onClick,
        selected = selected,
        shape = RoundedCornerShape(14.dp),
        focusedContainerColor = PanelElevated,
        focusScale = 1.02f,
        modifier = modifier
            .size(46.dp)
            .semantics {
                contentDescription = label
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⚙",
                color = OnBackground,
                fontSize = 22.sp,
            )
        }
    }
}

@Composable
private fun Clock() {
    var currentTime by remember { mutableStateOf(formatCurrentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatCurrentTime()
            delay(30_000)
        }
    }
    Text(
        text = currentTime,
        color = OnBackground,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
    )
}

private fun formatCurrentTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
