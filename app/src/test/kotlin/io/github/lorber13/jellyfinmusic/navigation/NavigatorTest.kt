package io.github.lorber13.jellyfinmusic.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.lorber13.jellyfinmusic.feature.albums.AlbumDetailRoute
import io.github.lorber13.jellyfinmusic.feature.albums.AlbumsRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {
    private val state = NavigationState(
        startDestination = TopLevelDestination.HOME,
        currentDestination = mutableStateOf(TopLevelDestination.HOME),
        backStacks = TopLevelDestination.entries.associateWith { destination ->
            NavBackStack<NavKey>(destination.startRoute)
        }
    )
    private val navigator = Navigator(state)

    @Test
    fun navigate_addsRouteToCurrentTabOnly() {
        navigator.selectTopLevelDestination(TopLevelDestination.LIBRARY)

        navigator.navigate(AlbumDetailRoute(albumId = "1"))

        assertEquals(listOf(AlbumsRoute, AlbumDetailRoute("1")), state.currentBackStack.toList())
        assertEquals(listOf<NavKey>(HomeRoute), state.backStacks.getValue(TopLevelDestination.HOME).toList())
    }

    @Test
    fun switchingTabs_keepsEachTabsBackStack() {
        navigator.selectTopLevelDestination(TopLevelDestination.LIBRARY)
        navigator.navigate(AlbumDetailRoute(albumId = "1"))

        navigator.selectTopLevelDestination(TopLevelDestination.SEARCH)
        navigator.selectTopLevelDestination(TopLevelDestination.LIBRARY)

        assertEquals(listOf(AlbumsRoute, AlbumDetailRoute("1")), state.currentBackStack.toList())
    }

    @Test
    fun reselectingCurrentTab_returnsToItsFirstScreen() {
        navigator.selectTopLevelDestination(TopLevelDestination.LIBRARY)
        navigator.navigate(AlbumDetailRoute(albumId = "1"))

        navigator.selectTopLevelDestination(TopLevelDestination.LIBRARY)

        assertEquals(listOf<NavKey>(AlbumsRoute), state.currentBackStack.toList())
    }

    @Test
    fun goBack_closesTheCurrentScreen() {
        navigator.selectTopLevelDestination(TopLevelDestination.LIBRARY)
        navigator.navigate(AlbumDetailRoute(albumId = "1"))

        navigator.goBack()

        assertEquals(TopLevelDestination.LIBRARY, state.currentDestination)
        assertEquals(listOf<NavKey>(AlbumsRoute), state.currentBackStack.toList())
    }

    @Test
    fun goBack_fromFirstScreenOfAnotherTab_returnsToStartTab() {
        navigator.selectTopLevelDestination(TopLevelDestination.SETTINGS)

        navigator.goBack()

        assertEquals(TopLevelDestination.HOME, state.currentDestination)
    }

    @Test
    fun destinationsInUse_alwaysStartsWithStartTab() {
        assertEquals(listOf(TopLevelDestination.HOME), state.destinationsInUse)

        navigator.selectTopLevelDestination(TopLevelDestination.SEARCH)

        assertEquals(listOf(TopLevelDestination.HOME, TopLevelDestination.SEARCH), state.destinationsInUse)
    }
}
