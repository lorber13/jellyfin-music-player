package io.github.lorber13.jellyfinmusic.core.data.session

import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import kotlinx.coroutines.flow.StateFlow

/** Signs the user in and holds the signed-in session. */
interface SessionRepository {
    /** The current session, or null while nobody is signed in. */
    val session: StateFlow<UserSession?>

    /** Signs in to [server]. On success, [session] holds the new session. */
    suspend fun signIn(server: JellyfinServer, username: String, password: String): SignInResult

    /** Asks [server] for a Quick Connect code that the user approves in another Jellyfin client. */
    suspend fun startQuickConnect(server: JellyfinServer): QuickConnectStartResult

    /** Asks [server] whether [request] was approved yet. */
    suspend fun checkQuickConnect(server: JellyfinServer, request: QuickConnectRequest): QuickConnectStatus

    /** Signs in to [server] with an approved [request]. On success, [session] holds the new session. */
    suspend fun signInWithQuickConnect(server: JellyfinServer, request: QuickConnectRequest): SignInResult
}
