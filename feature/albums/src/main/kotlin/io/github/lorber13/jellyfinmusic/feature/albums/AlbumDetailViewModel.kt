package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetail
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetailResult
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.data.library.Track
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.QueueItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal sealed interface AlbumDetailUiState {
    data object Loading : AlbumDetailUiState

    data class Loaded(val album: AlbumDetail) : AlbumDetailUiState

    data class Failed(val reason: LibraryFailure) : AlbumDetailUiState
}

/** Loads one album and its tracks, and plays them. The album's id comes from the navigation route. */
@HiltViewModel(assistedFactory = AlbumDetailViewModel.Factory::class)
internal class AlbumDetailViewModel @AssistedInject constructor(
    @Assisted private val albumId: String,
    private val albumRepository: AlbumRepository,
    private val musicPlayer: MusicPlayer
) : ViewModel() {
    var uiState: AlbumDetailUiState by mutableStateOf(AlbumDetailUiState.Loading)
        private set

    private var loadJob: Job? = null

    init {
        retry()
    }

    /** Loads the album again, replacing a load that is still running. */
    fun retry() {
        loadJob?.cancel()
        uiState = AlbumDetailUiState.Loading
        loadJob = viewModelScope.launch {
            uiState = when (val result = albumRepository.getAlbumDetail(albumId)) {
                is AlbumDetailResult.Success -> AlbumDetailUiState.Loaded(result.album)
                is AlbumDetailResult.Failed -> AlbumDetailUiState.Failed(result.reason)
            }
        }
    }

    /** Plays the whole album, starting with the track at [startIndex] of its track list. */
    fun play(startIndex: Int) {
        val album = (uiState as? AlbumDetailUiState.Loaded)?.album ?: return
        if (startIndex !in album.tracks.indices) return
        musicPlayer.play(album.tracks.map { track -> queueItemOf(track, album) }, startIndex)
    }

    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AlbumDetailViewModel
    }
}

/** What the media notification shows for a track of [album]. Tracks without artists show the album's. */
internal fun queueItemOf(track: Track, album: AlbumDetail): QueueItem = QueueItem(
    trackId = track.id,
    title = track.name,
    artistNames = track.artistNames.ifEmpty { listOfNotNull(album.artistName) },
    albumName = album.name,
    artworkUrl = album.coverImageUrl
)
