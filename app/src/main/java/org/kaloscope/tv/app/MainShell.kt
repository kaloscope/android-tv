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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
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
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.feature.detail.MediaDetailScreen
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerScreen
import org.kaloscope.tv.feature.player.PlayerUiState

private val ShellDivider = Color(0xFF252D40)
private val Card = Color(0xFF182132)

@Composable
internal fun MainShell(
    session: Session,
    homeState: HomeUiState,
    libraryState: LibraryUiState,
    detailState: MediaDetailUiState,
    onRefresh: () -> Unit,
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
    playerState: PlayerUiState = PlayerUiState.Loading,
    playbackControllerFactory: PlaybackControllerFactory? = null,
    onPlayHistory: (WatchHistoryItem) -> String? = { null },
    onPlayDetail: (MediaDetail, Long?) -> String? = { _, _ -> null },
    onLoadPlayer: (String) -> Unit = {},
    onPlayerProgress: (Long, Long, ProgressReason) -> Unit = { _, _, _ -> },
    onClosePlayer: (String) -> Unit = {},
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val homeFocus = remember { FocusRequester() }
    val libraryFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    var restoreMediaId by remember { mutableStateOf<Long?>(null) }
    var currentRoute by remember {
        mutableStateOf<NavKey>(backStack.lastOrNull() ?: HomeRoute)
    }

    // TV launchers do not guarantee an initial Compose focus owner.
    LaunchedEffect(homeFocus) {
        homeFocus.requestFocus()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        if (currentRoute !is MediaDetailRoute && currentRoute !is PlayerRoute) {
            MainTopBar(
                currentRoute = currentRoute,
                onHome = {
                    restoreMediaId = null
                    selectRoot(HomeRoute)
                },
                onSearch = { selectRoot(SearchRoute) },
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
                        UnavailableDestination(stringResource(R.string.search))
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
                            onBack = ::goBack,
                        )
                    }
                },
            )
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
    libraryFocus: FocusRequester,
    settingsFocus: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(Color(0xFF0B101C))
            .padding(horizontal = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(230.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                color = OnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.tv_experience),
                color = Primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MainNavButton(
                text = stringResource(R.string.home),
                selected = currentRoute == HomeRoute,
                enabled = true,
                onClick = onHome,
                // Skip the disabled search destination in the top-level focus chain.
                modifier = Modifier
                    .focusRequester(homeFocus)
                    .focusProperties { right = libraryFocus },
            )
            MainNavButton(
                text = stringResource(R.string.search),
                selected = currentRoute == SearchRoute,
                enabled = false,
                onClick = onSearch,
            )
            MainNavButton(
                text = stringResource(R.string.library),
                selected = currentRoute == LibraryRoute,
                enabled = true,
                onClick = onLibrary,
                modifier = Modifier
                    .focusRequester(libraryFocus)
                    .focusProperties {
                        left = homeFocus
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
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
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
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .size(52.dp)
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
    items: List<WatchHistoryItem>,
    restoreMediaId: Long?,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    val featured = items.first()
    FeaturedHistoryCard(
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Card, RoundedCornerShape(20.dp))
            .padding(30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HistoryArtwork(
            item = item,
            modifier = Modifier
                .width(290.dp)
                .fillMaxHeight(),
        )
        Spacer(Modifier.width(30.dp))
        Column(modifier = Modifier.weight(1f)) {
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
    Surface(
        onClick = { onPlayHistory(item) },
        modifier = Modifier
            .width(250.dp)
            .focusRequester(cardFocus),
    ) {
        Column(
            modifier = Modifier
                .background(Card, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            HistoryArtwork(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
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
private fun HistoryArtwork(
    item: WatchHistoryItem,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.background(
            color = Color(0xFF25334D),
            shape = RoundedCornerShape(14.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.title.take(1).ifBlank { "K" },
            color = Color(0xFFBAC6E8),
            fontSize = 58.sp,
            fontWeight = FontWeight.Light,
        )
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

@Composable
private fun SettingsScreen(
    session: Session,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.settings),
            color = OnBackground,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.settings_description),
            color = Muted,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel, RoundedCornerShape(20.dp))
                .padding(30.dp),
        ) {
            Text(
                text = stringResource(R.string.server_and_account),
                color = OnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(22.dp))
            SettingValue(
                label = stringResource(R.string.current_server),
                value = session.server.name,
                detail = session.server.origin,
            )
            Spacer(Modifier.height(18.dp))
            SettingValue(
                label = stringResource(R.string.current_account),
                value = session.user.username,
                detail = session.user.role,
            )
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF2A3043),
                    focusedContainerColor = Danger,
                ),
            ) {
                Text(stringResource(R.string.logout))
            }
        }
    }
}

@Composable
private fun SettingValue(
    label: String,
    value: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = OnBackground,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = detail,
            color = Muted,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun UnavailableDestination(title: String) {
    StatusPanel(
        title = title,
        description = stringResource(R.string.not_available_yet),
    )
}
