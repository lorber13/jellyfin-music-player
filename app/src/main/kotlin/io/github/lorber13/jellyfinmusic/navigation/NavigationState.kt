package io.github.lorber13.jellyfinmusic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * Where the user is in the app: the selected tab and a separate back stack for every tab,
 * so each tab remembers its own history. Follows the "multiple back stacks" Navigation 3 recipe.
 */
class NavigationState(
    val startDestination: TopLevelDestination,
    currentDestination: MutableState<TopLevelDestination>,
    val backStacks: Map<TopLevelDestination, NavBackStack<NavKey>>
) {
    var currentDestination: TopLevelDestination by currentDestination

    val currentBackStack: NavBackStack<NavKey>
        get() = backStacks.getValue(currentDestination)

    /**
     * The tabs whose screens are currently on the combined back stack. The start tab is always
     * underneath, so going back from any other tab lands on it before leaving the app.
     */
    val destinationsInUse: List<TopLevelDestination>
        get() = if (currentDestination == startDestination) {
            listOf(startDestination)
        } else {
            listOf(startDestination, currentDestination)
        }
}

/** Creates a [NavigationState] that survives configuration changes and process death. */
@Composable
fun rememberNavigationState(startDestination: TopLevelDestination): NavigationState {
    val currentDestination = rememberSaveable { mutableStateOf(startDestination) }
    val backStacks = TopLevelDestination.entries.associateWith { destination ->
        rememberNavBackStack(destination.startRoute)
    }
    return remember(startDestination) {
        NavigationState(startDestination, currentDestination, backStacks)
    }
}

/** Turns the back stacks of the tabs in use into the single list of entries NavDisplay shows. */
@Composable
fun NavigationState.toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
    val entriesByDestination = backStacks.mapValues { (_, backStack) ->
        // Each tab gets its own state holder so its screens keep their UI state while hidden.
        val decorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>())
        rememberDecoratedNavEntries(backStack, decorators, entryProvider)
    }
    return destinationsInUse.flatMap { destination -> entriesByDestination.getValue(destination) }
}
