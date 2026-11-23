package io.github.lorber13.jellyfinmusic.core.playback

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayableQueueTest {
    @Test
    fun everyTrackPlayable_keepsTheQueueAndItsStart() {
        val queue = playableQueueOf(listOf("1", "2", "3"), startIndex = 1, startPositionMs = 5_000)

        assertEquals(PlayableQueue(listOf("1", "2", "3"), startIndex = 1, startPositionMs = 5_000), queue)
    }

    @Test
    fun trackBeforeTheStartLeftOut_startsAtTheSameTrack() {
        val queue = playableQueueOf(listOf("1", null, "3", "4"), startIndex = 2, startPositionMs = 5_000)

        assertEquals(PlayableQueue(listOf("1", "3", "4"), startIndex = 1, startPositionMs = 5_000), queue)
    }

    @Test
    fun startTrackLeftOut_startsAtTheBeginningOfTheNextPlayableOne() {
        val queue = playableQueueOf(listOf("1", null, null, "4", "5"), startIndex = 1, startPositionMs = 5_000)

        assertEquals(PlayableQueue(listOf("1", "4", "5"), startIndex = 1, startPositionMs = C.TIME_UNSET), queue)
    }

    @Test
    fun nothingPlayableFromTheStartOn_isEmpty() {
        val queue = playableQueueOf(listOf("1", "2", null, null), startIndex = 2, startPositionMs = C.TIME_UNSET)

        assertEquals(PlayableQueue(emptyList<String>(), C.INDEX_UNSET, C.TIME_UNSET), queue)
    }

    @Test
    fun startLeftToThePlayer_leavesOutTracksAndKeepsTheStartUnset() {
        val queue = playableQueueOf(listOf(null, "2", "3"), startIndex = C.INDEX_UNSET, startPositionMs = C.TIME_UNSET)

        assertEquals(PlayableQueue(listOf("2", "3"), C.INDEX_UNSET, C.TIME_UNSET), queue)
    }
}
