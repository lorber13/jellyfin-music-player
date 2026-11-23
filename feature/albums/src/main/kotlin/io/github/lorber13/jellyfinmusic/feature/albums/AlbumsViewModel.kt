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
    /**
     * The order the sort menu shows: that of the albums on screen, or with none on screen, the one
     * loading. A sort picked while albums are on screen only replaces theirs once its albums have
     * loaded, so the menu never shows an order the grid isn't in.
     */
    val sort: AlbumSort

    /** Loading with nothing to show yet. */
    data class Loading(override val sort: AlbumSort) : AlbumsUiState

    data class Loaded(val albums: List<Album>, override val sort: AlbumSort) : AlbumsUiState

    /** Loading failed with nothing to show. [sort] is the one that failed, so Retry uses it again. */
    data class Failed(val reason: LibraryFailure, override val sort: AlbumSort) : AlbumsUiState
}

@HiltViewModel
internal class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val libraryPreferences: LibraryPreferencesRepository
) : ViewModel() {
    var uiState: AlbumsUiState by mutableStateOf(AlbumsUiState.Loading(AlbumSort.DEFAULT))
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
            loadAlbums(libraryPreferences.albumSort.first())
        }
    }

    /** Sorts by [field] in its natural direction, or reverses the direction if [field] is already selected. */
    fun selectSort(field: AlbumSortField) {
        val currentSort = uiState.sort
        val newSort = if (field == currentSort.field) {
            currentSort.copy(direction = currentSort.direction.reversed())
        } else {
            AlbumSort(field, field.naturalDirection)
        }
        load(newSort)
    }

    /** Loads the albums again, replacing a load that is still running. */
    fun reload() {
        load(uiState.sort)
    }

    fun onRefreshFailureShown() {
        refreshFailure = null
    }

    private fun load(sort: AlbumSort) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadAlbums(sort) }
    }

    // No `finally` resets isRefreshing: a load is only cancelled by the next one, which sets it itself.
    private suspend fun loadAlbums(sort: AlbumSort) {
        // Albums already on screen stay there, in their order, while the new ones load.
        val hasAlbums = uiState is AlbumsUiState.Loaded
        if (hasAlbums) isRefreshing = true else uiState = AlbumsUiState.Loading(sort)

        coroutineScope {
            val indicatorTimer = if (hasAlbums) launch { delay(minimumRefreshIndicatorTime) } else null
            // The result shows as soon as it arrives; only the indicator waits for the timer.
            when (val result = albumRepository.getAlbums(sort)) {
                is AlbumsResult.Success -> {
                    uiState = AlbumsUiState.Loaded(result.albums, sort)
                    // Remembered only once the albums show in it, so the next launch opens in the order
                    // the user last saw (DataStore skips the write if it hasn't changed). In its own
                    // coroutine, so the next reload doesn't cancel it.
                    viewModelScope.launch { libraryPreferences.setAlbumSort(sort) }
                }

                is AlbumsResult.Failed ->
                    if (hasAlbums) {
                        refreshFailure = result.reason
                    } else {
                        uiState =
                            AlbumsUiState.Failed(result.reason, sort)
                    }
            }
            indicatorTimer?.join()
        }
        isRefreshing = false
    }
}
