package io.github.lorber13.jellyfinmusic.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import io.github.lorber13.jellyfinmusic.R
import io.github.lorber13.jellyfinmusic.feature.albums.AlbumsRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object SearchRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

/** The tabs of the navigation bar (or rail), in display order. */
enum class TopLevelDestination(
    /** The first screen of the tab, at the bottom of its back stack. */
    val startRoute: NavKey,
    @param:StringRes val labelResource: Int,
    @param:DrawableRes val iconResource: Int
) {
    HOME(HomeRoute, R.string.destination_home, R.drawable.ic_home),

    // The library only has albums until the other lists (LIB-02, LIB-04, LIB-05) are built.
    LIBRARY(AlbumsRoute, R.string.destination_library, R.drawable.ic_library_music),
    SEARCH(SearchRoute, R.string.destination_search, R.drawable.ic_search),
    SETTINGS(SettingsRoute, R.string.destination_settings, R.drawable.ic_settings)
}
