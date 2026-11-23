package io.github.lorber13.jellyfinmusic.core.playback

import androidx.media3.common.C

/**
 * The tracks that will play, and where they start.
 *
 * @param startIndex the index in [items] to start at, or [C.INDEX_UNSET] to let the player choose.
 * @param startPositionMs where in that track to start, or [C.TIME_UNSET] for its beginning.
 */
internal data class PlayableQueue<T>(val items: List<T>, val startIndex: Int, val startPositionMs: Long)

/**
 * [queue] without the tracks that can't be played, which are null. It starts at the track that
 * was asked for ([startIndex]) or, when that one can't be played, at the beginning of the next
 * one that can. Empty when neither that track nor any after it can be played: the tracks before
 * it weren't asked for. [startIndex] is [C.INDEX_UNSET] when the player chooses where to start.
 */
internal fun <T : Any> playableQueueOf(queue: List<T?>, startIndex: Int, startPositionMs: Long): PlayableQueue<T> {
    val playableItems = queue.filterNotNull()
    if (startIndex == C.INDEX_UNSET) return PlayableQueue(playableItems, C.INDEX_UNSET, startPositionMs)

    val firstPlayableIndex = (startIndex until queue.size).firstOrNull { index -> queue[index] != null }
        ?: return PlayableQueue(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
    // Every track left out before it moves the start one place forward.
    val startIndexAmongPlayable = queue.subList(0, firstPlayableIndex).count { item -> item != null }
    val startPosition = if (firstPlayableIndex == startIndex) startPositionMs else C.TIME_UNSET
    return PlayableQueue(playableItems, startIndexAmongPlayable, startPosition)
}
