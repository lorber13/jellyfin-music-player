package io.github.lorber13.jellyfinmusic.core.data.server

/** The outcome of [ServerRepository.findServer]. */
sealed interface ServerLookupResult {
    data class Found(val server: JellyfinServer) : ServerLookupResult

    data class Failed(val reason: ServerLookupFailure) : ServerLookupResult
}

/** Why no Jellyfin server could be used at the address the user entered. */
sealed interface ServerLookupFailure {
    /** The text can't be read as a server address. */
    data object InvalidAddress : ServerLookupFailure

    /** Nothing answered: wrong address, server down, no network, or local network access denied. */
    data object Unreachable : ServerLookupFailure

    /** A server answered over HTTPS, but its certificate couldn't be trusted. */
    data object SecureConnectionFailed : ServerLookupFailure

    /** Something answered, but it isn't a Jellyfin server. */
    data object NotAJellyfinServer : ServerLookupFailure

    /** A Jellyfin server answered, but its version is older than the app supports. */
    data class UnsupportedVersion(
        /** The version the server reports, e.g. `10.8.13`. */
        val serverVersion: String,
        /** The oldest version the app supports, e.g. `10.9`. */
        val minimumVersion: String
    ) : ServerLookupFailure
}
