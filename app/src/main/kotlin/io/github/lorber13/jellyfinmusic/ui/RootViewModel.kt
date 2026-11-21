package io.github.lorber13.jellyfinmusic.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.session.SessionRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SessionState
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Tells [JellyfinMusicRoot] whether someone is signed in, so it opens on the right screen and
 * leaves the shell when nobody is. MainActivity keeps the splash screen up while it is loading.
 */
@HiltViewModel
class RootViewModel @Inject constructor(sessionRepository: SessionRepository) : ViewModel() {
    val session: StateFlow<SessionState> = sessionRepository.session
}
