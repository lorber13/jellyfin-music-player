package io.github.lorber13.jellyfinmusic.core.data.server

/** A Jellyfin server that answered at [address]. */
data class JellyfinServer(
    /** The full base URL that worked, e.g. `https://music.example.com` or `http://192.168.1.10:8096`. */
    val address: String,
    val name: String,
    /** The version the server reports, e.g. `12.4.0`. */
    val version: String
)
