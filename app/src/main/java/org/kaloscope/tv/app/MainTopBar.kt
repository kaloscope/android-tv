package org.kaloscope.tv.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
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
import org.kaloscope.tv.core.designsystem.KaloscopeBrand
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel

@Composable
internal fun MainTopBar(
    currentRoute: NavKey?,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
    onDestinationFocused: (NavKey) -> Unit,
    homeFocus: FocusRequester,
    searchFocus: FocusRequester,
    libraryFocus: FocusRequester,
    settingsFocus: FocusRequester,
    settingsMenuFocus: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
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
                onClick = onHome,
                onFocused = { onDestinationFocused(HomeRoute) },
                modifier = Modifier
                    .focusRequester(homeFocus)
                    .focusProperties { right = searchFocus },
            )
            MainNavButton(
                text = stringResource(R.string.search),
                selected = currentRoute == SearchRoute,
                onClick = onSearch,
                onFocused = { onDestinationFocused(SearchRoute) },
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
                onClick = onLibrary,
                onFocused = { onDestinationFocused(LibraryRoute) },
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
            onFocused = { onDestinationFocused(SettingsRoute) },
            modifier = Modifier
                .focusRequester(settingsFocus)
                .focusProperties {
                    left = libraryFocus
                    down = settingsMenuFocus
                },
        )
        Spacer(Modifier.width(18.dp))
        Clock()
    }
}

@Composable
private fun MainNavButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeButton(
        onClick = onClick,
        selected = selected,
        variant = KaloscopeControlVariant.Ghost,
        size = KaloscopeControlSize.Compact,
        modifier = modifier
            .height(42.dp)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onFocused()
                }
            },
        shape = RoundedCornerShape(11.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SettingsButton(
    selected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.settings)
    KaloscopeIconButton(
        onClick = onClick,
        selected = selected,
        variant = KaloscopeControlVariant.Ghost,
        size = KaloscopeControlSize.Compact,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .size(46.dp)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onFocused()
                }
            }
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
