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

/**
 * Registers the screens of the sign-in flow with the app's navigation. Signing in itself needs
 * no callback: the app shows the shell as soon as the data layer holds a session.
 *
 * @param onServerConnected the user found their server and wants to sign in to it.
 * @param onChangeServer the user wants to go back from signing in and pick another server.
 */
fun EntryProviderScope<NavKey>.loginEntries(onServerConnected: (JellyfinServer) -> Unit, onChangeServer: () -> Unit) {
    entry<ServerAddressRoute> {
        ServerAddressScreen(onContinue = onServerConnected)
    }
    entry<SignInRoute> { route ->
        SignInScreen(route = route, onChangeServer = onChangeServer)
    }
}
