package io.github.lorber13.jellyfinmusic.core.data.library

/** The albums of the signed-in user's library. */
interface AlbumRepository {
    /** Every album the user can see on the server, in the order of [sort]. */
    suspend fun getAlbums(sort: AlbumSort): AlbumsResult
}
