package io.github.lorber13.jellyfinmusic.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.session.SessionRepository
import io.github.lorber13.jellyfinmusic.core.data.session.UserSession
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Tells [JellyfinMusicRoot] whether someone is signed in, so it can leave the shell when nobody is. */
@HiltViewModel
class RootViewModel @Inject constructor(sessionRepository: SessionRepository) : ViewModel() {
    val session: StateFlow<UserSession?> = sessionRepository.session
}
