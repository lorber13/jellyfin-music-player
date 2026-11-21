package io.github.lorber13.jellyfinmusic.core.data.library

/** The outcome of [AlbumRepository.getAlbums]. */
sealed interface AlbumsResult {
    data class Success(val albums: List<Album>) : AlbumsResult

    data class Failed(val reason: LibraryFailure) : AlbumsResult
}

/** Why the library couldn't be loaded. */
enum class LibraryFailure {
    /** The server didn't answer: no network, server down, or local network access denied. */
    UNREACHABLE,

    /** The server answered, but with an error or a response the app couldn't read. */
    SERVER_ERROR
}
