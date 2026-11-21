package io.github.lorber13.jellyfinmusic.core.playback

/** Plays music in the background playback service. */
interface MusicPlayer {
    /**
     * Replaces what is playing with [queue] and starts playing it from the item at [startIndex],
     * which must be one of [queue]'s indices. Call it on the main thread.
     */
    fun play(queue: List<QueueItem>, startIndex: Int)
}
