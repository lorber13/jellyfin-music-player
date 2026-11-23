package io.github.lorber13.jellyfinmusic.core.data.stream

/** Where the player streams the signed-in user's music from. */
interface StreamRepository {
    /**
     * How to stream each of [trackIds], in the same order: the original file when its format is
     * one of [playableFormats], otherwise a conversion by the server. An entry is null when that
     * track can't be streamed: nobody is signed in, the id is not a track id, the server can't be
     * reached, or it can neither send nor convert the track. The addresses hold no credentials;
     * send [requestHeaders] with them.
     */
    suspend fun streamsOf(trackIds: List<String>, playableFormats: List<DirectPlayFormat>): List<TrackStream?>

    /**
     * The headers that authorize a stream request, or none when nobody is signed in. Safe to call
     * from any thread. Read them for each request, because signing in again changes them.
     */
    fun requestHeaders(): Map<String, String>
}
