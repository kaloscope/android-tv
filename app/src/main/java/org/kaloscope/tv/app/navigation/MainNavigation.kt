package org.kaloscope.tv.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

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

fun MutableList<NavKey>.handleMainBack(): Boolean =
    when {
        size > 1 -> {
            removeLastOrNull()
            true
        }

        lastOrNull() != HomeRoute -> {
            selectRoot(HomeRoute)
            true
        }

        else -> false
    }
