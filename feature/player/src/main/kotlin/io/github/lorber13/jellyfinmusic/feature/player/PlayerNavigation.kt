package io.github.lorber13.jellyfinmusic.feature.player

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

/** The Now Playing screen, which covers the whole window, tabs and mini player included. */
@Serializable
data object NowPlayingRoute : NavKey

/**
 * Registers the player's screens with the app's navigation. Now Playing slides up from the
 * bottom over the screen it opens from, which stays in place under it, and slides back down
 * when it closes, also while the predictive back gesture is in progress.
 *
 * @param onCloseNowPlaying takes Now Playing off the back stack. It is also called when nothing
 * is queued any more, possibly while the screen is already closing.
 */
fun EntryProviderScope<NavKey>.playerEntries(onCloseNowPlaying: () -> Unit) {
    entry<NowPlayingRoute>(
        metadata = metadata {
            put(NavDisplay.TransitionKey) {
                slideInVertically(initialOffsetY = { height -> height }) togetherWith
                    ExitTransition.KeepUntilTransitionsFinished
            }
            put(NavDisplay.PopTransitionKey) {
                EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { height -> height })
            }
            put(NavDisplay.PredictivePopTransitionKey) { _ ->
                EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { height -> height })
            }
        }
    ) {
        NowPlayingScreen(onClose = onCloseNowPlaying)
    }
}
