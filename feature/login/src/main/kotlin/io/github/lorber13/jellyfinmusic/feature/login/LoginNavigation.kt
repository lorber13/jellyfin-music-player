package io.github.lorber13.jellyfinmusic.feature.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import kotlinx.serialization.Serializable

/** The first screen of the sign-in flow, where the user enters their server's address. */
@Serializable
data object ServerAddressRoute : NavKey

/** The screen where the user signs in to the server found on [ServerAddressRoute]. */
@Serializable
data class SignInRoute(val serverAddress: String, val serverName: String, val serverVersion: String) : NavKey {
    constructor(server: JellyfinServer) : this(server.address, server.name, server.version)

    internal val server: JellyfinServer
        get() = JellyfinServer(address = serverAddress, name = serverName, version = serverVersion)
}

/** The screen where the user signs in by approving a code in another Jellyfin client. */
@Serializable
data class QuickConnectRoute(val serverAddress: String, val serverName: String, val serverVersion: String) : NavKey {
    constructor(server: JellyfinServer) : this(server.address, server.name, server.version)

    internal val server: JellyfinServer
        get() = JellyfinServer(address = serverAddress, name = serverName, version = serverVersion)
}

/**
 * Registers the screens of the sign-in flow with the app's navigation.
 *
 * @param onServerConnected the user found their server and wants to sign in to it.
 * @param onSignedIn the user is signed in; the data layer holds the session.
 * @param onChangeServer the user wants to go back from signing in and pick another server.
 * @param onUseQuickConnect the user wants to sign in to the server with Quick Connect.
 * @param onUsePassword the user wants to go back from Quick Connect to the password form.
 */
fun EntryProviderScope<NavKey>.loginEntries(
    onServerConnected: (JellyfinServer) -> Unit,
    onSignedIn: () -> Unit,
    onChangeServer: () -> Unit,
    onUseQuickConnect: (JellyfinServer) -> Unit,
    onUsePassword: () -> Unit
) {
    entry<ServerAddressRoute> {
        ServerAddressScreen(onContinue = onServerConnected)
    }
    entry<SignInRoute> { route ->
        SignInScreen(
            route = route,
            onSignedIn = onSignedIn,
            onChangeServer = onChangeServer,
            onUseQuickConnect = { onUseQuickConnect(route.server) }
        )
    }
    entry<QuickConnectRoute> { route ->
        QuickConnectScreen(route = route, onSignedIn = onSignedIn, onUsePassword = onUsePassword)
    }
}
