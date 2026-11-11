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
    /** Filled icon, shown while the tab is selected. */
    @param:DrawableRes val selectedIconResource: Int,
    /** Outlined icon, shown while another tab is selected. */
    @param:DrawableRes val unselectedIconResource: Int
) {
    HOME(HomeRoute, R.string.destination_home, R.drawable.ic_home_filled, R.drawable.ic_home_outlined),

    // The library only has albums until the other lists (LIB-02, LIB-04, LIB-05) are built.
    LIBRARY(
        AlbumsRoute,
        R.string.destination_library,
        R.drawable.ic_library_music_filled,
        R.drawable.ic_library_music_outlined
    ),

    // The search icon has no filled version, so both states use the same drawable.
    SEARCH(SearchRoute, R.string.destination_search, R.drawable.ic_search, R.drawable.ic_search),
    SETTINGS(
        SettingsRoute,
        R.string.destination_settings,
        R.drawable.ic_settings_filled,
        R.drawable.ic_settings_outlined
    )
}
