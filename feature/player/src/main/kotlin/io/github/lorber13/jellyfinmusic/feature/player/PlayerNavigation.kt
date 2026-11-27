package io.github.lorber13.jellyfinmusic.feature.player

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The Now Playing screen, which covers the whole window, tabs and mini player included. */
@Serializable
data object NowPlayingRoute : NavKey

/**
 * Registers the player's screens with the app's navigation.
 *
 * @param onCloseNowPlaying takes Now Playing off the back stack. It is also called when nothing
 * is queued any more, possibly while the screen is already closing.
 */
fun EntryProviderScope<NavKey>.playerEntries(onCloseNowPlaying: () -> Unit) {
    entry<NowPlayingRoute> {
        NowPlayingScreen(onClose = onCloseNowPlaying)
    }
}
