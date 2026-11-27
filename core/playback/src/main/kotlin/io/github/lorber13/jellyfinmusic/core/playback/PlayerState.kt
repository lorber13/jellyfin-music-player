package io.github.lorber13.jellyfinmusic.core.playback

import kotlin.time.Duration

/**
 * The track the player is on and what it is doing, as the app's player controls show it.
 *
 * @param artist the track's artists, already joined for display, or null when it has none.
 * @param album the album the track is on, or null when it has none.
 * @param artworkUrl the cover to show, or null when there is no artwork.
 * @param isPlaying true while the music plays or is about to, so the controls offer Pause.
 * @param isLoading true while the player waits for the track to load before it can play.
 * @param isSeekable true when the track can be moved to another position; false while it loads.
 * @param hasPrevious true when Previous can go back: to the start of the track, or to the one before.
 * @param hasNext true when there is a track to skip to.
 */
data class PlayerState(
    val title: String,
    val artist: String?,
    val album: String?,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val isSeekable: Boolean,
    val hasPrevious: Boolean,
    val hasNext: Boolean
)

/**
 * How far the current track has played.
 *
 * @param duration the track's length, or null while it isn't known.
 */
data class PlaybackPosition(val position: Duration, val duration: Duration?)
