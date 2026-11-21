package io.github.lorber13.jellyfinmusic.core.data.session

/** Whether someone is signed in. */
sealed interface SessionState {
    /** The stored session is still being read from the device, right after the app starts. */
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val session: UserSession) : SessionState
}
