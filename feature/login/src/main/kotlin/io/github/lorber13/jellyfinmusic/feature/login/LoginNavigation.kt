package io.github.lorber13.jellyfinmusic.feature.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The first screen of the sign-in flow, where the user enters their server's address. */
@Serializable
data object ServerAddressRoute : NavKey

/** Registers the screens of the sign-in flow with the app's navigation. */
fun EntryProviderScope<NavKey>.loginEntries(onServerConnected: () -> Unit) {
    entry<ServerAddressRoute> {
        ServerAddressScreen(onContinue = onServerConnected)
    }
}
