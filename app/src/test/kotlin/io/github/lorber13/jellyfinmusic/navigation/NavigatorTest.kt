package io.github.lorber13.jellyfinmusic.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.lorber13.jellyfinmusic.feature.albums.AlbumDetailRoute
import io.github.lorber13.jellyfinmusic.feature.albums.AlbumsRoute
import org.junit.Assert.assertEquals
import org.junit.Test

// Albums is the only tab until LIB-02 adds Artists. Tests that switch between tabs come back then.
class NavigatorTest {
    private val state = NavigationState(
        startDestination = TopLevelDestination.ALBUMS,
        currentDestination = mutableStateOf(TopLevelDestination.ALBUMS),
        backStacks = TopLevelDestination.entries.associateWith { destination ->
            NavBackStack<NavKey>(destination.startRoute)
        }
    )
    private val navigator = Navigator(state)

    @Test
    fun navigate_addsRouteToCurrentTab() {
        navigator.navigate(AlbumDetailRoute(albumId = "1"))

        assertEquals(listOf(AlbumsRoute, AlbumDetailRoute("1")), state.currentBackStack.toList())
    }

    @Test
    fun reselectingCurrentTab_returnsToItsFirstScreen() {
        navigator.navigate(AlbumDetailRoute(albumId = "1"))
        navigator.navigate(AlbumDetailRoute(albumId = "2"))

        navigator.selectTopLevelDestination(TopLevelDestination.ALBUMS)

        assertEquals(listOf<NavKey>(AlbumsRoute), state.currentBackStack.toList())
    }

    @Test
    fun goBack_closesTheCurrentScreen() {
        navigator.navigate(AlbumDetailRoute(albumId = "1"))

        navigator.goBack()

        assertEquals(TopLevelDestination.ALBUMS, state.currentDestination)
        assertEquals(listOf<NavKey>(AlbumsRoute), state.currentBackStack.toList())
    }

    @Test
    fun destinationsInUse_isTheStartTab() {
        assertEquals(listOf(TopLevelDestination.ALBUMS), state.destinationsInUse)
    }
}
