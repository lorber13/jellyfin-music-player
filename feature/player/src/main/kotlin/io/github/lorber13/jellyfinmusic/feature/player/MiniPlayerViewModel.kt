package io.github.lorber13.jellyfinmusic.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the mini player shows, and its buttons. */
@HiltViewModel
internal class MiniPlayerViewModel @Inject constructor(private val musicPlayer: MusicPlayer) : ViewModel() {
    /** What is playing, or null while nothing is queued and the mini player is hidden. */
    val state: StateFlow<PlayerState?> = musicPlayer.state

    /** How far the current track has played, from 0 to 1, or null while its length isn't known. */
    val progress: StateFlow<Float?> = musicPlayer.positions
        .map(::progressOf)
        // Keeps reading through a configuration change, and stops soon after the app is left.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds), initialValue = null)

    fun playOrPause() = musicPlayer.playOrPause()

    fun skipToNext() = musicPlayer.skipToNext()
}

/** [position] as a fraction of the track's length, or null while the length isn't known. */
internal fun progressOf(position: PlaybackPosition): Float? {
    val duration = position.duration?.takeIf { it.isPositive() } ?: return null
    return (position.position / duration).toFloat().coerceIn(0f, 1f)
}
