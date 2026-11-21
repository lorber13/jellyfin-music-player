package io.github.lorber13.jellyfinmusic.core.data.stream

/** Where the player streams the signed-in user's music from. */
interface StreamRepository {
    /**
     * The address of the track's original file, or null when nobody is signed in or [trackId] is
     * not a track id. The address holds no credentials; send [requestHeaders] with it.
     */
    fun streamUrlOf(trackId: String): String?

    /**
     * The headers that authorize a stream request, or none when nobody is signed in. Safe to call
     * from any thread. Read them for each request, because signing in again changes them.
     */
    fun requestHeaders(): Map<String, String>
}
