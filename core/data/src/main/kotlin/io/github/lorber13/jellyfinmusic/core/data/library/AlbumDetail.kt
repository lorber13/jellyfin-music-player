package io.github.lorber13.jellyfinmusic.core.data.library

import kotlin.time.Duration

/**
 * An album with its tracks.
 *
 * @param artistName the album artist, or null when the server has none.
 * @param year the release year, or null when the server has none.
 * @param coverImageUrl the cover sized for a large header, or null when the album has no artwork.
 * @param tracks in disc and track order.
 */
data class AlbumDetail(
    val id: String,
    val name: String,
    val artistName: String?,
    val year: Int?,
    val coverImageUrl: String?,
    val tracks: List<Track>
) {
    /** How long the whole album plays. Tracks of unknown length count as zero. */
    val totalDuration: Duration
        get() = tracks.fold(Duration.ZERO) { total, track -> total + (track.duration ?: Duration.ZERO) }
}
