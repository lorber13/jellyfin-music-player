package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.library.Album
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSortField
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumsResult
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryPreferencesRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * How long the refresh indicator stays up at least. A fast server answers in a few milliseconds,
 * and an indicator that vanishes as the finger lifts looks like the refresh didn't happen.
 */
private val minimumRefreshIndicatorTime = 500.milliseconds

internal sealed interface AlbumsUiState {
    /** Loading with nothing to show yet. */
    data object Loading : AlbumsUiState

    /** [albums] are in the order of [sort], which lags behind a newly picked sort until it has loaded. */
    data class Loaded(val albums: List<Album>, val sort: AlbumSort) : AlbumsUiState

    /** Loading failed with nothing to show. */
    data class Failed(val reason: LibraryFailure) : AlbumsUiState
}

@HiltViewModel
internal class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val libraryPreferences: LibraryPreferencesRepository
) : ViewModel() {
    var uiState: AlbumsUiState by mutableStateOf(AlbumsUiState.Loading)
        private set

    /** The sort the user picked. */
    var sort: AlbumSort by mutableStateOf(AlbumSort.DEFAULT)
        private set

    /** Whether the albums on screen are being reloaded, after a pull to refresh or a new sort. */
    var isRefreshing by mutableStateOf(false)
        private set

    /** Why reloading failed while albums were shown, until the screen has told the user. */
    var refreshFailure: LibraryFailure? by mutableStateOf(null)
        private set

    private var loadJob: Job? = null

    init {
        loadJob = viewModelScope.launch {
            sort = libraryPreferences.albumSort.first()
            loadAlbums()
        }
    }

    /** Sorts by [field] in its natural direction, or reverses the direction if [field] is already selected. */
    fun selectSort(field: AlbumSortField) {
        val newSort = if (field == sort.field) {
            sort.copy(direction = sort.direction.reversed())
        } else {
            AlbumSort(field, field.naturalDirection)
        }
        sort = newSort
        reload()
        // Saved alongside the reload rather than before it, so the new order shows even if saving fails.
        viewModelScope.launch { libraryPreferences.setAlbumSort(newSort) }
    }

    /** Loads the albums again, replacing a load that is still running. */
    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadAlbums() }
    }

    fun onRefreshFailureShown() {
        refreshFailure = null
    }

    // No `finally` resets isRefreshing: a load is only cancelled by the next one, which sets it itself.
    private suspend fun loadAlbums() {
        val sortToLoad = sort
        // Albums already on screen stay there while the new ones load.
        val hasAlbums = uiState is AlbumsUiState.Loaded
        if (hasAlbums) isRefreshing = true else uiState = AlbumsUiState.Loading

        coroutineScope {
            val indicatorTimer = if (hasAlbums) launch { delay(minimumRefreshIndicatorTime) } else null
            // The result shows as soon as it arrives; only the indicator waits for the timer.
            when (val result = albumRepository.getAlbums(sortToLoad)) {
                is AlbumsResult.Success -> uiState = AlbumsUiState.Loaded(result.albums, sortToLoad)

                is AlbumsResult.Failed ->
                    if (hasAlbums) refreshFailure = result.reason else uiState = AlbumsUiState.Failed(result.reason)
            }
            indicatorTimer?.join()
        }
        isRefreshing = false
    }
}
