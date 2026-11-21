package io.github.lorber13.jellyfinmusic.core.data.session

import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import kotlinx.coroutines.flow.StateFlow

/** Signs the user in and holds the signed-in session. */
interface SessionRepository {
    /** The current session, or null while nobody is signed in. */
    val session: StateFlow<UserSession?>

    /** Signs in to [server]. On success, [session] holds the new session. */
    suspend fun signIn(server: JellyfinServer, username: String, password: String): SignInResult
}
