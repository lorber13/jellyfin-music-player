package io.github.lorber13.jellyfinmusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.lorber13.jellyfinmusic.feature.login.ServerAddressRoute
import io.github.lorber13.jellyfinmusic.feature.login.loginEntries

/**
 * Shows the sign-in flow until the user is through it, then the app shell. The sign-in screens
 * fill the whole window, without the navigation bar.
 */
@Composable
fun JellyfinMusicRoot() {
    // Not kept across app restarts yet: staying signed in is ACC-04.
    var isSignedIn by rememberSaveable { mutableStateOf(false) }

    if (isSignedIn) {
        JellyfinMusicApp()
    } else {
        SignInFlow(onSignedIn = { isSignedIn = true })
    }
}

@Composable
private fun SignInFlow(onSignedIn: () -> Unit) {
    val backStack = rememberNavBackStack(ServerAddressRoute)
    NavDisplay(
        backStack = backStack,
        // Each screen gets its own ViewModel, cleared when the screen leaves the back stack.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider<NavKey> {
            loginEntries(onServerConnected = onSignedIn)
        }
    )
}
