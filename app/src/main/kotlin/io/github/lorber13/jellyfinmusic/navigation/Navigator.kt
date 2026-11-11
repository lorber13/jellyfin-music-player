package io.github.lorber13.jellyfinmusic.navigation

import androidx.navigation3.runtime.NavKey

/** Changes the [NavigationState] in response to navigation events. */
class Navigator(private val state: NavigationState) {

    /** Switches to a tab. Selecting the tab that is already open returns to its first screen. */
    fun selectTopLevelDestination(destination: TopLevelDestination) {
        if (destination == state.currentDestination) {
            popCurrentBackStackToStart()
        } else {
            state.currentDestination = destination
        }
    }

    /** Opens a screen on top of the current tab. */
    fun navigate(route: NavKey) {
        state.currentBackStack.add(route)
    }

    /** Closes the current screen, or returns to the start tab from the first screen of another tab. */
    fun goBack() {
        val backStack = state.currentBackStack
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            state.currentDestination = state.startDestination
        }
    }

    private fun popCurrentBackStackToStart() {
        val backStack = state.currentBackStack
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}
