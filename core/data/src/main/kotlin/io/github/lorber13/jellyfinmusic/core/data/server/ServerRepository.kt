package io.github.lorber13.jellyfinmusic.core.data.server

/** Finds the Jellyfin server behind an address the user typed. */
interface ServerRepository {
    /**
     * Checks [address] and fills in what the user left out: `music.example.com` is tried with
     * HTTPS and HTTP and with Jellyfin's default ports.
     */
    suspend fun findServer(address: String): ServerLookupResult

    /**
     * The users [serverAddress] shows on its sign-in screen. Empty when the server hides them
     * all, which many do, or when it can't be reached.
     */
    suspend fun getPublicUsers(serverAddress: String): List<PublicUser>

    /** Whether [serverAddress] allows signing in with Quick Connect. False when it can't be reached. */
    suspend fun isQuickConnectEnabled(serverAddress: String): Boolean
}
