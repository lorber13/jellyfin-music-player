package io.github.lorber13.jellyfinmusic.core.data.session

/** The user signed in to a Jellyfin server. */
data class UserSession(val serverAddress: String, val serverName: String, val userId: String, val userName: String)
