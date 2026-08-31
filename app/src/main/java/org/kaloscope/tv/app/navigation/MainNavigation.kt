package org.kaloscope.tv.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.kaloscope.tv.core.model.StartPage

@Serializable
data object HomeRoute : NavKey

@Serializable
data object SearchRoute : NavKey

@Serializable
data object LibraryRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class MediaDetailRoute(
    val mediaId: Long,
) : NavKey

@Serializable
data class PlayerRoute(
    val requestId: String,
) : NavKey

@Serializable
data class ReaderRoute(
    val requestId: String,
) : NavKey

fun MutableList<NavKey>.selectRoot(route: NavKey) {
    clear()
    add(route)
}

fun MutableList<NavKey>.openSettings() {
    if (lastOrNull() != SettingsRoute) {
        add(SettingsRoute)
    }
}

fun MutableList<NavKey>.openMediaDetail(mediaId: Long) {
    val route = MediaDetailRoute(mediaId)
    if (lastOrNull() != route) {
        add(route)
    }
}

fun MutableList<NavKey>.openPlayer(requestId: String) {
    val route = PlayerRoute(requestId)
    if (lastOrNull() != route) {
        add(route)
    }
}

fun MutableList<NavKey>.openReader(requestId: String) {
    val route = ReaderRoute(requestId)
    if (lastOrNull() != route) {
        add(route)
    }
}

fun MutableList<NavKey>.handleMainBack(): Boolean =
    if (size > 1) {
        removeLastOrNull()
        true
    } else {
        false
    }

fun StartPage.toRootRoute(): NavKey =
    when (this) {
        StartPage.Home -> HomeRoute
        StartPage.Search -> SearchRoute
        StartPage.Library -> LibraryRoute
    }
