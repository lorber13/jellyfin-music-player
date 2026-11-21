package io.github.lorber13.jellyfinmusic.core.data.library

/**
 * An album in the user's library.
 *
 * @param artistName the album artist, or null when the server has none.
 * @param coverImageUrl the cover sized for lists and grids, or null when the album has no artwork.
 */
data class Album(val id: String, val name: String, val artistName: String?, val coverImageUrl: String?)
