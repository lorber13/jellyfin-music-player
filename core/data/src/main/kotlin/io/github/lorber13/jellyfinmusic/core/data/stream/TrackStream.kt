package io.github.lorber13.jellyfinmusic.core.data.stream

/** Where the player gets a track's audio from, and in which form. */
data class TrackStream(val url: String, val kind: StreamKind)

enum class StreamKind {
    /** The track's file as it is on the server. */
    ORIGINAL_FILE,

    /** An HLS playlist of the track converted by the server, for a format the phone can't play. */
    HLS_CONVERSION
}
