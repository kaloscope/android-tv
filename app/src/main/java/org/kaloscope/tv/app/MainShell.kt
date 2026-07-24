package org.kaloscope.tv.app

import androidx.activity.compose.BackHandler
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
import org.kaloscope.tv.app.navigation.SearchRoute
import org.kaloscope.tv.app.navigation.SettingsRoute
import org.kaloscope.tv.app.navigation.handleMainBack
import org.kaloscope.tv.app.navigation.openSettings
import org.kaloscope.tv.app.navigation.selectRoot
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.feature.home.HomeUiState

private val ShellDivider = Color(0xFF252D40)
private val Card = Color(0xFF182132)

@Composable
internal fun MainShell(
    session: Session,
    homeState: HomeUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val homeFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
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

    BackHandler(enabled = currentRoute != HomeRoute) {
        backStack.handleMainBack()
        currentRoute = backStack.lastOrNull() ?: HomeRoute
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        MainTopBar(
            currentRoute = currentRoute,
            onHome = { selectRoot(HomeRoute) },
            onSearch = { selectRoot(SearchRoute) },
            onLibrary = { selectRoot(LibraryRoute) },
            onSettings = {
                backStack.openSettings()
                currentRoute = SettingsRoute
            },
            homeFocus = homeFocus,
            settingsFocus = settingsFocus,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp, vertical = 24.dp),
        ) {
            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        HomeScreen(
                            state = homeState,
                            onRefresh = onRefresh,
                        )
                    }
                    entry<SearchRoute> {
                        UnavailableDestination(stringResource(R.string.search))
                    }
                    entry<LibraryRoute> {
                        UnavailableDestination(stringResource(R.string.library))
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            session = session,
                            onLogout = onLogout,
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
                // Bridge the gap left by disabled P0 destinations.
                modifier = Modifier
                    .focusRequester(homeFocus)
                    .focusProperties { right = settingsFocus },
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
                enabled = false,
                onClick = onLibrary,
            )
        }
        Spacer(Modifier.weight(1f))
        SettingsButton(
            selected = currentRoute == SettingsRoute,
            onClick = onSettings,
            modifier = Modifier
                .focusRequester(settingsFocus)
                .focusProperties { left = homeFocus },
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

            HomeUiState.Empty -> StatusPanel(
                title = stringResource(R.string.no_history),
                description = stringResource(R.string.no_history_description),
            )

            is HomeUiState.Error -> ErrorPanel(
                error = state.error,
                onRetry = onRefresh,
            )

            is HomeUiState.Content -> HistoryContent(state.items)
        }
    }
}

@Composable
private fun HistoryContent(items: List<WatchHistoryItem>) {
    val featured = items.first()
    FeaturedHistoryCard(featured)
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
                CompactHistoryCard(item)
            }
        }
    }
}

@Composable
private fun FeaturedHistoryCard(item: WatchHistoryItem) {
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
        }
    }
}

@Composable
private fun CompactHistoryCard(item: WatchHistoryItem) {
    Column(
        modifier = Modifier
            .width(250.dp)
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
