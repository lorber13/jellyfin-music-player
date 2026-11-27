package io.github.lorber13.jellyfinmusic.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** What the Now Playing screen shows, and its controls. */
@HiltViewModel
internal class NowPlayingViewModel @Inject constructor(private val musicPlayer: MusicPlayer) : ViewModel() {
    /** What is playing, or null while nothing is queued. */
    val state: StateFlow<PlayerState?> = musicPlayer.state

    /** How far the current track has played, or null until the player has said. */
    val position: StateFlow<PlaybackPosition?> = musicPlayer.positions
        // Keeps reading through a configuration change, and stops soon after the app is left.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds), initialValue = null)

    fun playOrPause() = musicPlayer.playOrPause()

    fun skipToPrevious() = musicPlayer.skipToPrevious()

    fun skipToNext() = musicPlayer.skipToNext()

    fun seekTo(position: Duration) = musicPlayer.seekTo(position)
}
