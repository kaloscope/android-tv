package org.kaloscope.tv.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
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
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.BackgroundRaised
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeBrand
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.StartPage
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
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerScreen
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.search.SearchScreen
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.settings.SettingsScreen
import org.kaloscope.tv.feature.settings.SettingsSection
import org.kaloscope.tv.feature.settings.SettingsUiState

private val ShellDivider = Color(0xFF252D40)
private val Card = Color(0xFF182132)

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
    onSubtitleEnabledSetting: (Boolean) -> Unit = {},
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
                    EnterTransition.None togetherWith ExitTransition.None
                },
                popTransitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
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
                            onSubtitleEnabled = onSubtitleEnabledSetting,
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

@Composable
private fun HomeScreen(
    session: Session,
    state: HomeUiState,
    onRefresh: () -> Unit,
    restoreMediaId: Long?,
    onOpenLibrary: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home),
                    color = OnBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.recent_watch_description),
                    color = Muted,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF202738),
                    focusedContainerColor = Primary,
                ),
            ) {
                Text(stringResource(R.string.refresh))
            }
        }
        Spacer(Modifier.height(28.dp))
        when (state) {
            HomeUiState.Loading -> StatusPanel(
                title = stringResource(R.string.loading_history),
                description = stringResource(R.string.loading_history_description),
            )

            HomeUiState.Empty -> HomeEmpty(
                onOpenLibrary = onOpenLibrary,
            )

            is HomeUiState.Error -> ErrorPanel(
                error = state.error,
                onRetry = onRefresh,
            )

            is HomeUiState.Content -> HistoryContent(
                session = session,
                items = state.items,
                restoreMediaId = restoreMediaId,
                onOpenMedia = onOpenMedia,
                onPlayHistory = onPlayHistory,
            )
        }
    }
}

@Composable
private fun HistoryContent(
    session: Session,
    items: List<WatchHistoryItem>,
    restoreMediaId: Long?,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    val featured = items.first()
    FeaturedHistoryCard(
        session = session,
        item = featured,
        restoreFocus = featured.mediaId == restoreMediaId,
        onOpenMedia = onOpenMedia,
        onPlayHistory = onPlayHistory,
    )
    if (items.size > 1) {
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.more_history),
            color = OnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(
                items = items.drop(1),
                key = WatchHistoryItem::historyId,
            ) { item ->
                CompactHistoryCard(
                    session = session,
                    item = item,
                    restoreFocus = item.mediaId == restoreMediaId,
                    onPlayHistory = onPlayHistory,
                )
            }
        }
    }
}

@Composable
private fun FeaturedHistoryCard(
    session: Session,
    item: WatchHistoryItem,
    restoreFocus: Boolean,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    val detailFocus = remember(item.mediaId) { FocusRequester() }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) {
            withFrameNanos { }
            detailFocus.requestFocus()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .testTag("home-hero"),
    ) {
        ServerImage(
            session = session,
            rawValue = item.backdropPath ?: item.posterPath,
            fallbackText = item.title,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFA070B14),
                        0.53f to Color(0xCC070B14),
                        1f to Color(0x26070B14),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.66f)
                .padding(horizontal = 34.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.continue_watching),
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.title,
                color = OnBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            HistoryMetadata(item)
            Spacer(Modifier.height(24.dp))
            ProgressBar(item.percentage)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.watched_percent, item.percentage),
                color = Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onPlayHistory(item) },
                    modifier = Modifier.focusRequester(detailFocus),
                    colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                ) {
                    Text(stringResource(R.string.resume_playback))
                }
                Button(
                    onClick = { onOpenMedia(item.mediaId) },
                    colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                ) {
                    Text(stringResource(R.string.view_detail))
                }
            }
        }
    }
}

@Composable
private fun CompactHistoryCard(
    session: Session,
    item: WatchHistoryItem,
    restoreFocus: Boolean,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    val cardFocus = remember(item.mediaId) { FocusRequester() }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) {
            withFrameNanos { }
            cardFocus.requestFocus()
        }
    }
    KaloscopeFocusSurface(
        onClick = { onPlayHistory(item) },
        shape = RoundedCornerShape(16.dp),
        containerColor = Panel.copy(alpha = 0.72f),
        focusedContainerColor = PanelElevated,
        focusScale = 1.04f,
        modifier = Modifier
            .width(258.dp)
            .focusRequester(cardFocus),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            ServerImage(
                session = session,
                rawValue = item.backdropPath ?: item.posterPath,
                fallbackText = item.title,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.title,
                color = OnBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            ProgressBar(item.percentage)
        }
    }
}

@Composable
private fun HomeEmpty(
    onOpenLibrary: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Panel, RoundedCornerShape(20.dp))
            .padding(34.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.no_history),
            color = OnBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.no_history_description),
            color = Muted,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onOpenLibrary,
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.open_library))
        }
    }
}

@Composable
private fun HistoryMetadata(item: WatchHistoryItem) {
    val metadata = listOfNotNull(
        item.year?.toString(),
        item.season?.let { season ->
            item.episode?.let { episode ->
                stringResource(R.string.season_episode, season, episode)
            }
        },
        item.rating?.let { stringResource(R.string.rating, it) },
    ).joinToString("  ·  ")
    if (metadata.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = metadata,
            color = Muted,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun ProgressBar(percentage: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(ShellDivider, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percentage.coerceIn(0, 100) / 100f)
                .fillMaxHeight()
                .background(Primary, RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun StatusPanel(
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Panel, RoundedCornerShape(20.dp))
            .padding(34.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = title,
                color = OnBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                color = Muted,
                fontSize = 17.sp,
            )
        }
    }
}

@Composable
private fun ErrorPanel(
    error: AppError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(20.dp))
            .padding(34.dp),
    ) {
        Text(
            text = stringResource(R.string.history_load_failed),
            color = OnBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = historyErrorText(error),
            color = Danger,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun historyErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }
