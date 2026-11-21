package io.github.lorber13.jellyfinmusic.core.data.server

/** The outcome of [ServerRepository.findServer]. */
sealed interface ServerLookupResult {
    data class Found(val server: JellyfinServer) : ServerLookupResult

    data class Failed(val reason: ServerLookupFailure) : ServerLookupResult
}

/** Why no Jellyfin server could be used at the address the user entered. */
enum class ServerLookupFailure {
    /** The text can't be read as a server address. */
    INVALID_ADDRESS,

    /** Nothing answered: wrong address, server down, no network, or local network access denied. */
    UNREACHABLE,

    /** A server answered over HTTPS, but its certificate couldn't be trusted. */
    SECURE_CONNECTION_FAILED,

    /** Something answered, but it isn't a Jellyfin server. */
    NOT_A_JELLYFIN_SERVER
}
