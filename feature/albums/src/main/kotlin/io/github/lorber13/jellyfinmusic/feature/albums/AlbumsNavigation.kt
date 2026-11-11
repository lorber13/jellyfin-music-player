package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object AlbumsRoute : NavKey

@Serializable
data class AlbumDetailRoute(val albumId: String) : NavKey

/** Registers the screens of the albums feature with the app's navigation. */
fun EntryProviderScope<NavKey>.albumsEntries(onAlbumClick: (albumId: String) -> Unit, onNavigateBack: () -> Unit) {
    entry<AlbumsRoute> {
        AlbumsScreen(onAlbumClick = onAlbumClick)
    }
    entry<AlbumDetailRoute> { route ->
        AlbumDetailScreen(albumId = route.albumId, onNavigateBack = onNavigateBack)
    }
}
