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
import io.github.lorber13.jellyfinmusic.core.data.session.SessionState
import io.github.lorber13.jellyfinmusic.feature.login.QuickConnectRoute
import io.github.lorber13.jellyfinmusic.feature.login.ServerAddressRoute
import io.github.lorber13.jellyfinmusic.feature.login.SignInRoute
import io.github.lorber13.jellyfinmusic.feature.login.loginEntries
import io.github.lorber13.jellyfinmusic.feature.player.NowPlayingRoute
import io.github.lorber13.jellyfinmusic.feature.player.playerEntries
import io.github.lorber13.jellyfinmusic.navigation.AppShellRoute

/**
 * The app's top-level navigation: the sign-in screens, then the app shell, with Now Playing over
 * it while open. They share one back stack, so moving from one to the other animates like any
 * other screen change. The sign-in screens and Now Playing fill the whole window, without the
 * navigation bar.
 */
@Composable
fun JellyfinMusicRoot(viewModel: RootViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    // MainActivity keeps the splash screen up while the stored session is read, so nothing is
    // drawn until the app knows whether to open on sign-in or on the tabs.
    if (session != SessionState.Loading) {
        RootNavDisplay(isSignedIn = session is SessionState.SignedIn)
    }
}

@Composable
private fun RootNavDisplay(isSignedIn: Boolean) {
    // Only used when there is no saved back stack: on a cold start, or when the user reopens the
    // app after leaving it with Back.
    val backStack = rememberNavBackStack(if (isSignedIn) AppShellRoute else ServerAddressRoute)

    // The back stack survives process death even when the stored session couldn't be read back
    // (for example after the Keystore key was lost): the app would come back on the shell with
    // nobody signed in.
    LaunchedEffect(isSignedIn) {
        if (!isSignedIn && AppShellRoute in backStack) {
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
                if (isSignedIn) {
                    JellyfinMusicApp(
                        onOpenNowPlaying = {
                            // A second tap while the screen opens would add it twice, and
                            // the same route can only be on the back stack once.
                            if (NowPlayingRoute !in backStack) backStack.add(NowPlayingRoute)
                        }
                    )
                }
            }
            // Removed by its key, not as the last screen: Now Playing also closes itself when
            // nothing is queued, which can happen while it is already closing.
            playerEntries(onCloseNowPlaying = { backStack.remove(NowPlayingRoute) })
        }
    )
}

/** Makes [route] the only screen on the back stack. Adds it first, so the stack is never empty. */
private fun NavBackStack<NavKey>.replaceAllWith(route: NavKey) {
    add(route)
    removeAll { it != route }
}
