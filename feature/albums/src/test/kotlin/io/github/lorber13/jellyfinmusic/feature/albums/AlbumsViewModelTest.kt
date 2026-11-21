package io.github.lorber13.jellyfinmusic.feature.albums

import io.github.lorber13.jellyfinmusic.core.data.library.Album
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetailResult
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSortField
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumsResult
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryPreferencesRepository
import io.github.lorber13.jellyfinmusic.core.data.library.SortDirection
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val albums = listOf(
        Album(id = "1", name = "Morning Light", artistName = "Aurora Fields", coverImageUrl = null),
        Album(id = "2", name = "Steam & Rust", artistName = "The Copper Kettles", coverImageUrl = null)
    )
    private val yearNewestFirst = AlbumSort(AlbumSortField.YEAR, SortDirection.DESCENDING)
    private val albumRepository = FakeAlbumRepository()
    private val preferences = FakeLibraryPreferencesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTheAlbumsInTheStoredSort() = runTest(dispatcher) {
        preferences.storedSort.value = yearNewestFirst
        albumRepository.result = AlbumsResult.Success(albums)

        val viewModel = AlbumsViewModel(albumRepository, preferences)
        assertEquals(AlbumsUiState.Loading, viewModel.uiState)
        runCurrent()

        assertEquals(yearNewestFirst, viewModel.sort)
        assertEquals(listOf(yearNewestFirst), albumRepository.requestedSorts)
        assertEquals(AlbumsUiState.Loaded(albums, yearNewestFirst), viewModel.uiState)
    }

    @Test
    fun selectingAnotherField_sortsInItsNaturalDirectionAndRemembersIt() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.selectSort(AlbumSortField.YEAR)
        runCurrent()

        assertEquals(yearNewestFirst, viewModel.sort)
        assertEquals(yearNewestFirst, albumRepository.requestedSorts.last())
        assertEquals(AlbumsUiState.Loaded(albums, yearNewestFirst), viewModel.uiState)
        assertEquals(yearNewestFirst, preferences.storedSort.value)
    }

    @Test
    fun selectingTheSelectedField_reversesTheDirection() = runTest(dispatcher) {
        val viewModel = loadedViewModel()

        viewModel.selectSort(AlbumSortField.NAME)
        runCurrent()

        val nameZToA = AlbumSort(AlbumSortField.NAME, SortDirection.DESCENDING)
        assertEquals(nameZToA, viewModel.sort)
        assertEquals(nameZToA, preferences.storedSort.value)
    }

    @Test
    fun newSort_keepsTheAlbumsOnScreenWhileLoading() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        val pending = CompletableDeferred<AlbumsResult>()
        albumRepository.pendingResult = pending

        viewModel.selectSort(AlbumSortField.YEAR)
        runCurrent()

        assertEquals(true, viewModel.isRefreshing)
        assertEquals(AlbumsUiState.Loaded(albums, AlbumSort.DEFAULT), viewModel.uiState)

        pending.complete(AlbumsResult.Success(albums.reversed()))
        advanceUntilIdle()

        assertEquals(false, viewModel.isRefreshing)
        assertEquals(AlbumsUiState.Loaded(albums.reversed(), yearNewestFirst), viewModel.uiState)
    }

    @Test
    fun fastRefresh_showsTheAlbumsAtOnceAndKeepsTheIndicatorForHalfASecond() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        albumRepository.result = AlbumsResult.Success(albums.reversed())

        viewModel.reload()
        runCurrent()

        assertEquals(AlbumsUiState.Loaded(albums.reversed(), AlbumSort.DEFAULT), viewModel.uiState)
        assertEquals(true, viewModel.isRefreshing)
        advanceTimeBy(499.milliseconds)
        assertEquals(true, viewModel.isRefreshing)
        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(false, viewModel.isRefreshing)
    }

    @Test
    fun firstLoad_doesNotWaitForTheIndicator() = runTest(dispatcher) {
        albumRepository.result = AlbumsResult.Success(albums)

        val viewModel = AlbumsViewModel(albumRepository, preferences)
        runCurrent()

        assertEquals(AlbumsUiState.Loaded(albums, AlbumSort.DEFAULT), viewModel.uiState)
        assertEquals(false, viewModel.isRefreshing)
    }

    @Test
    fun failure_showsTheReason() = runTest(dispatcher) {
        albumRepository.result = AlbumsResult.Failed(LibraryFailure.UNREACHABLE)

        val viewModel = AlbumsViewModel(albumRepository, preferences)
        runCurrent()

        assertEquals(AlbumsUiState.Failed(LibraryFailure.UNREACHABLE), viewModel.uiState)
    }

    @Test
    fun reloadAfterFailure_loadsAgain() = runTest(dispatcher) {
        albumRepository.result = AlbumsResult.Failed(LibraryFailure.SERVER_ERROR)
        val viewModel = AlbumsViewModel(albumRepository, preferences)
        runCurrent()
        val pending = CompletableDeferred<AlbumsResult>()
        albumRepository.pendingResult = pending

        viewModel.reload()
        runCurrent()
        assertEquals(AlbumsUiState.Loading, viewModel.uiState)
        pending.complete(AlbumsResult.Success(albums))
        runCurrent()

        assertEquals(AlbumsUiState.Loaded(albums, AlbumSort.DEFAULT), viewModel.uiState)
    }

    @Test
    fun failedRefresh_keepsTheAlbumsAndReportsTheFailure() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        albumRepository.result = AlbumsResult.Failed(LibraryFailure.UNREACHABLE)

        viewModel.reload()
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Loaded(albums, AlbumSort.DEFAULT), viewModel.uiState)
        assertEquals(false, viewModel.isRefreshing)
        assertEquals(LibraryFailure.UNREACHABLE, viewModel.refreshFailure)

        viewModel.onRefreshFailureShown()
        assertNull(viewModel.refreshFailure)
    }

    @Test
    fun reloadWhileLoading_replacesTheRunningLoad() = runTest(dispatcher) {
        val viewModel = loadedViewModel()
        val stale = CompletableDeferred<AlbumsResult>()
        albumRepository.pendingResult = stale
        viewModel.selectSort(AlbumSortField.YEAR)
        runCurrent()

        albumRepository.pendingResult = null
        viewModel.selectSort(AlbumSortField.DATE_ADDED)
        runCurrent()
        stale.complete(AlbumsResult.Success(emptyList()))
        advanceUntilIdle()

        val dateAddedNewestFirst = AlbumSort(AlbumSortField.DATE_ADDED, SortDirection.DESCENDING)
        assertEquals(AlbumsUiState.Loaded(albums, dateAddedNewestFirst), viewModel.uiState)
        assertEquals(false, viewModel.isRefreshing)
    }

    private fun TestScope.loadedViewModel(): AlbumsViewModel {
        albumRepository.result = AlbumsResult.Success(albums)
        val viewModel = AlbumsViewModel(albumRepository, preferences)
        runCurrent()
        return viewModel
    }

    private class FakeAlbumRepository : AlbumRepository {
        var result: AlbumsResult = AlbumsResult.Success(emptyList())

        /** When set, loads wait for it instead of returning [result]. */
        var pendingResult: CompletableDeferred<AlbumsResult>? = null
        val requestedSorts = mutableListOf<AlbumSort>()

        override suspend fun getAlbums(sort: AlbumSort): AlbumsResult {
            requestedSorts += sort
            return pendingResult?.await() ?: result
        }

        override suspend fun getAlbumDetail(albumId: String): AlbumDetailResult = error("Not used by the albums list")
    }

    private class FakeLibraryPreferencesRepository : LibraryPreferencesRepository {
        val storedSort = MutableStateFlow(AlbumSort.DEFAULT)
        override val albumSort: Flow<AlbumSort> = storedSort

        override suspend fun setAlbumSort(sort: AlbumSort) {
            storedSort.value = sort
        }
    }
}
