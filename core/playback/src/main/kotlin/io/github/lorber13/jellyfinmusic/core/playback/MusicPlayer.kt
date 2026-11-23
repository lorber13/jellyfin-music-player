package io.github.lorber13.jellyfinmusic.core.playback

import kotlinx.coroutines.flow.Flow

/** Plays music in the background playback service. */
interface MusicPlayer {
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
}
