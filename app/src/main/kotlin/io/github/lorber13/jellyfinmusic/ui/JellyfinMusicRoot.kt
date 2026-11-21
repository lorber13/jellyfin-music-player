package io.github.lorber13.jellyfinmusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.lorber13.jellyfinmusic.feature.login.ServerAddressRoute
import io.github.lorber13.jellyfinmusic.feature.login.SignInRoute
import io.github.lorber13.jellyfinmusic.feature.login.loginEntries

/**
 * Shows the sign-in flow until a user is signed in, then the app shell. The sign-in screens
 * fill the whole window, without the navigation bar.
 */
@Composable
fun JellyfinMusicRoot(viewModel: RootViewModel = hiltViewModel()) {
    // Kept in memory only, so a restart signs the user out: staying signed in is ACC-04.
    val session by viewModel.session.collectAsStateWithLifecycle()

    if (session != null) {
        JellyfinMusicApp()
    } else {
        SignInFlow()
    }
}

@Composable
private fun SignInFlow() {
    val backStack = rememberNavBackStack(ServerAddressRoute)
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
                onChangeServer = { backStack.removeLastOrNull() }
            )
        }
    )
}
