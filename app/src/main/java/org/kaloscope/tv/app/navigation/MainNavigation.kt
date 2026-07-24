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

fun MutableList<NavKey>.selectRoot(route: NavKey) {
    clear()
    add(route)
}

fun MutableList<NavKey>.openSettings() {
    if (lastOrNull() != SettingsRoute) {
        add(SettingsRoute)
    }
}

fun MutableList<NavKey>.handleMainBack(): Boolean =
    when {
        lastOrNull() == SettingsRoute && size > 1 -> {
            removeLastOrNull()
            true
        }

        lastOrNull() != HomeRoute -> {
            selectRoot(HomeRoute)
            true
        }

        else -> false
    }
