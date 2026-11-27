package io.github.lorber13.jellyfinmusic.core.playback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Plays music in the background playback service. */
interface MusicPlayer {
    /**
     * What is playing, or null while nothing is queued. It follows the player while the app is
     * visible and keeps its last value while it isn't.
     */
    val state: StateFlow<PlayerState?>

    /**
     * The current track's position, read about once a second while collected. Nothing comes while
     * the app isn't visible.
     */
    val positions: Flow<PlaybackPosition>

    /**
     * Messages for the user when some or all of the music asked for can't be played, already
     * worded by the playback service. They only come while the app is visible, and one that comes
     * while nothing collects is lost.
     */
    val errorMessages: Flow<String>

    /**
     * Replaces what is playing with [queue] and starts playing it from the item at [startIndex],
     * which must be one of [queue]'s indices. Call it on the main thread.
     */
    fun play(queue: List<QueueItem>, startIndex: Int)

    /** Pauses what is playing, or plays what is paused. Call it on the main thread. */
    fun playOrPause()

    /** Skips to the next track. Call it on the main thread. */
    fun skipToNext()
}
