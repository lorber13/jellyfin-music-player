package io.github.lorber13.jellyfinmusic.core.data.library

import kotlinx.coroutines.flow.Flow

/** How the user last arranged the library screens, kept across restarts. */
interface LibraryPreferencesRepository {
    /** The last sort picked for the albums list, or [AlbumSort.DEFAULT] if there is none. */
    val albumSort: Flow<AlbumSort>

    /** Remembers [sort] for the next time the albums list opens. */
    suspend fun setAlbumSort(sort: AlbumSort)
}
