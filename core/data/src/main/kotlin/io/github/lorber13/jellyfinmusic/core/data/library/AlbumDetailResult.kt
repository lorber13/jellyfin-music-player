package io.github.lorber13.jellyfinmusic.core.data.library

/** The outcome of [AlbumRepository.getAlbumDetail]. */
sealed interface AlbumDetailResult {
    data class Success(val album: AlbumDetail) : AlbumDetailResult

    data class Failed(val reason: LibraryFailure) : AlbumDetailResult
}
