package io.github.lorber13.jellyfinmusic.core.data.server

/** Finds the Jellyfin server behind an address the user typed. */
interface ServerRepository {
    /**
     * Checks [address] and fills in what the user left out: `music.example.com` is tried with
     * HTTPS and HTTP and with Jellyfin's default ports.
     */
    suspend fun findServer(address: String): ServerLookupResult
}
