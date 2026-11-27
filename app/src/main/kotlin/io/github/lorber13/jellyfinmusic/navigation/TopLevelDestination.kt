package io.github.lorber13.jellyfinmusic.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import io.github.lorber13.jellyfinmusic.R
import io.github.lorber13.jellyfinmusic.feature.albums.AlbumsRoute

/**
 * The tabs of the navigation bar (or rail), in display order. The first one is where the app opens.
 *
 * The tabs are the library lists: Albums, then Artists (LIB-02), Playlists (LIB-04) and Tracks
 * (LIB-13), each added when its list is built.
 */
enum class TopLevelDestination(
    /** The first screen of the tab, at the bottom of its back stack. */
    val startRoute: NavKey,
    @param:StringRes val labelResource: Int,
    /** Filled icon, shown while the tab is selected. */
    @param:DrawableRes val selectedIconResource: Int,
    /** Outlined icon, shown while another tab is selected. */
    @param:DrawableRes val unselectedIconResource: Int
) {
    ALBUMS(
        AlbumsRoute,
        R.string.destination_albums,
        R.drawable.ic_library_music_filled,
        R.drawable.ic_library_music_outlined
    )
}
