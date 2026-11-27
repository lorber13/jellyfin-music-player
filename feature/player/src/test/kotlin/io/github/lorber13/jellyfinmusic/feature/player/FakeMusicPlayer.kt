package io.github.lorber13.jellyfinmusic.feature.player

import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import io.github.lorber13.jellyfinmusic.core.playback.QueueItem
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Plays nothing, and records the commands the player's screens send it. */
internal class FakeMusicPlayer : MusicPlayer {
    /** The names of the commands received, in order. */
    val commands = mutableListOf<String>()

    /** The positions [seekTo] was asked to move to, in order. */
    val seekPositions = mutableListOf<Duration>()

    override val state = MutableStateFlow<PlayerState?>(null)
    override val positions = MutableSharedFlow<PlaybackPosition>()
    override val errorMessages: Flow<String> = emptyFlow()

    override fun play(queue: List<QueueItem>, startIndex: Int) = Unit

    override fun playOrPause() {
        commands += "playOrPause"
    }

    override fun skipToPrevious() {
        commands += "skipToPrevious"
    }

    override fun skipToNext() {
        commands += "skipToNext"
    }

    override fun seekTo(position: Duration) {
        commands += "seekTo"
        seekPositions += position
    }
}
