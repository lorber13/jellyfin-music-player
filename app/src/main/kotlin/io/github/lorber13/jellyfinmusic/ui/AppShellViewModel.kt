package io.github.lorber13.jellyfinmusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Holds what the shell shows over every screen: for now, the message about music that couldn't
 * be played, which can arrive after the user has left the screen that started it.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(musicPlayer: MusicPlayer) : ViewModel() {
    /** The message to show, until [onPlaybackErrorShown] says it has been. */
    var playbackErrorMessage: String? by mutableStateOf(null)
        private set

    init {
        // Kept as state, as the architecture guide recommends for events, so a message that comes
        // while the shell is being recreated still gets shown.
        viewModelScope.launch {
            musicPlayer.errorMessages.collect { message -> playbackErrorMessage = message }
        }
    }

    fun onPlaybackErrorShown(message: String) {
        // A newer message that came in while this one was showing stays to be shown.
        if (playbackErrorMessage == message) playbackErrorMessage = null
    }
}
