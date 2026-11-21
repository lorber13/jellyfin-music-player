package io.github.lorber13.jellyfinmusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.lorber13.jellyfinmusic.feature.login.QuickConnectRoute
import io.github.lorber13.jellyfinmusic.feature.login.ServerAddressRoute
import io.github.lorber13.jellyfinmusic.feature.login.SignInRoute
import io.github.lorber13.jellyfinmusic.feature.login.loginEntries
import io.github.lorber13.jellyfinmusic.navigation.AppShellRoute

/**
 * The app's top-level navigation: the sign-in screens, then the app shell. They share one back
 * stack, so moving from one to the other animates like any other screen change. The sign-in
 * screens fill the whole window, without the navigation bar.
 */
@Composable
fun JellyfinMusicRoot(viewModel: RootViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    // Only used when there is no saved back stack, e.g. when the user reopens the app after
    // leaving it with Back while the session is still in memory.
    val backStack = rememberNavBackStack(if (session != null) AppShellRoute else ServerAddressRoute)

    // The back stack survives process death, but until ACC-04 the session doesn't: the app could
    // come back on the shell with nobody signed in.
    LaunchedEffect(session) {
        if (session == null && AppShellRoute in backStack) {
            backStack.replaceAllWith(ServerAddressRoute)
        }
    }

    NavDisplay(
        backStack = backStack,
        // Each screen gets its own ViewModel, cleared when the screen leaves the back stack.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider<NavKey> {
            loginEntries(
                onServerConnected = { server -> backStack.add(SignInRoute(server)) },
                // Back from the shell leaves the app instead of returning to the sign-in screens.
                onSignedIn = { backStack.replaceAllWith(AppShellRoute) },
                onChangeServer = { backStack.removeLastOrNull() },
                onUseQuickConnect = { server -> backStack.add(QuickConnectRoute(server)) },
                onUsePassword = { backStack.removeLastOrNull() }
            )
            entry<AppShellRoute> {
                // The shell never runs without a signed-in user. Without a session the entry stays
                // empty for the moment it takes the effect above to go back to the sign-in screens.
                if (session != null) JellyfinMusicApp()
            }
        }
    )
}

/** Makes [route] the only screen on the back stack. Adds it first, so the stack is never empty. */
private fun NavBackStack<NavKey>.replaceAllWith(route: NavKey) {
    add(route)
    removeAll { it != route }
}
