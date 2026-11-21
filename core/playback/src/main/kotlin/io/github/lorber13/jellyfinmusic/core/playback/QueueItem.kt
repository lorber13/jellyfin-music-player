package io.github.lorber13.jellyfinmusic.core.playback

/**
 * A track to play, with what the media notification and other controllers, such as Bluetooth
 * devices, show about it.
 *
 * @param artistNames the track's artists; empty when the server has none.
 * @param albumName the album it is on, or null when the server has none.
 * @param artworkUrl the cover to show, or null when there is no artwork.
 */
data class QueueItem(
    val trackId: String,
    val title: String,
    val artistNames: List<String>,
    val albumName: String?,
    val artworkUrl: String?
)
