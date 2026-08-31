package org.kaloscope.tv.app.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.StartPage

class MainNavigationTest {
    @Test
    fun `root navigation replaces the current root`() {
        val backStack = mutableListOf<NavKey>(HomeRoute)

        backStack.selectRoot(SearchRoute)

        assertEquals(listOf(SearchRoute), backStack)
    }

    @Test
    fun `settings returns to the route that opened it`() {
        val backStack = mutableListOf<NavKey>(LibraryRoute)

        backStack.openSettings()
        val handled = backStack.handleMainBack()

        assertTrue(handled)
        assertEquals(listOf(LibraryRoute), backStack)
    }

    @Test
    fun `media detail returns to its library`() {
        val backStack = mutableListOf<NavKey>(LibraryRoute)

        backStack.openMediaDetail(201)
        val handled = backStack.handleMainBack()

        assertTrue(handled)
        assertEquals(listOf(LibraryRoute), backStack)
    }

    @Test
    fun `player route contains only request id and returns to detail`() {
        val backStack = mutableListOf<NavKey>(LibraryRoute, MediaDetailRoute(201))

        backStack.openPlayer("request-1")
        val handled = backStack.handleMainBack()

        assertTrue(handled)
        assertEquals(listOf(LibraryRoute, MediaDetailRoute(201)), backStack)
    }

    @Test
    fun `reader route contains only request id and returns to search`() {
        val backStack = mutableListOf<NavKey>(SearchRoute)

        backStack.openReader("reader-1")
        val route = backStack.last() as ReaderRoute
        val handled = backStack.handleMainBack()

        assertEquals("reader-1", route.requestId)
        assertEquals(listOf("requestId"), ReaderRoute::class.java.declaredFields
            .filterNot {
                it.isSynthetic ||
                    it.name.startsWith("$") ||
                    java.lang.reflect.Modifier.isStatic(it.modifiers)
            }
            .map { it.name })
        assertTrue(handled)
        assertEquals(listOf(SearchRoute), backStack)
    }

    @Test
    fun `home delegates back to the system`() {
        val backStack = mutableListOf<NavKey>(HomeRoute)

        assertFalse(backStack.handleMainBack())
        assertEquals(listOf(HomeRoute), backStack)
    }

    @Test
    fun `non-home root delegates back without changing route`() {
        val backStack = mutableListOf<NavKey>(LibraryRoute)

        assertFalse(backStack.handleMainBack())
        assertEquals(listOf(LibraryRoute), backStack)
    }

    @Test
    fun `saved start page selects the matching root route`() {
        assertEquals(HomeRoute, StartPage.Home.toRootRoute())
        assertEquals(SearchRoute, StartPage.Search.toRootRoute())
        assertEquals(LibraryRoute, StartPage.Library.toRootRoute())
    }
}
