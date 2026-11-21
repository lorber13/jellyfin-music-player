package io.github.lorber13.jellyfinmusic.core.data.library

import kotlin.time.Duration

/**
 * A song on an album.
 *
 * @param artistNames the track's own artists, which on compilations differ from the album artist.
 * @param trackNumber the position on its disc, or null when the file isn't tagged with one.
 * @param discNumber the disc it is on, or null when the file isn't tagged with one.
 * @param duration how long it plays, or null when the server doesn't know.
 */
data class Track(
    val id: String,
    val name: String,
    val artistNames: List<String>,
    val trackNumber: Int?,
    val discNumber: Int?,
    val duration: Duration?
)
