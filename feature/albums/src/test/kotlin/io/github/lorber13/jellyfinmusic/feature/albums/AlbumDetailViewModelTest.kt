package io.github.lorber13.jellyfinmusic.feature.albums

import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetail
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetailResult
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumRepository
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumsResult
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.data.library.Track
import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.QueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val album = AlbumDetail(
        id = "2",
        name = "Steam & Rust",
        artistName = "The Copper Kettles",
        year = 2021,
        coverImageUrl = null,
        tracks = emptyList()
    )
    private val albumRepository = FakeAlbumRepository()
    private val musicPlayer = FakeMusicPlayer()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTheAlbumOfTheRoute() = runTest(dispatcher) {
        albumRepository.result = AlbumDetailResult.Success(album)

        val viewModel =
            AlbumDetailViewModel(albumId = "2", albumRepository = albumRepository, musicPlayer = musicPlayer)
        assertEquals(AlbumDetailUiState.Loading, viewModel.uiState)
        runCurrent()

        assertEquals(listOf("2"), albumRepository.requestedAlbumIds)
        assertEquals(AlbumDetailUiState.Loaded(album), viewModel.uiState)
    }

    @Test
    fun failedLoad_showsTheReason() = runTest(dispatcher) {
        albumRepository.result = AlbumDetailResult.Failed(LibraryFailure.UNREACHABLE)

        val viewModel =
            AlbumDetailViewModel(albumId = "2", albumRepository = albumRepository, musicPlayer = musicPlayer)
        runCurrent()

        assertEquals(AlbumDetailUiState.Failed(LibraryFailure.UNREACHABLE), viewModel.uiState)
    }

    @Test
    fun retry_loadsAgain() = runTest(dispatcher) {
        albumRepository.result = AlbumDetailResult.Failed(LibraryFailure.SERVER_ERROR)
        val viewModel =
            AlbumDetailViewModel(albumId = "2", albumRepository = albumRepository, musicPlayer = musicPlayer)
        runCurrent()

        albumRepository.result = AlbumDetailResult.Success(album)
        viewModel.retry()
        assertEquals(AlbumDetailUiState.Loading, viewModel.uiState)
        runCurrent()

        assertEquals(listOf("2", "2"), albumRepository.requestedAlbumIds)
        assertEquals(AlbumDetailUiState.Loaded(album), viewModel.uiState)
    }

    @Test
    fun play_queuesTheWholeAlbumFromTheChosenTrack() = runTest(dispatcher) {
        val tracks = listOf(
            Track("a", "Boiler Room", listOf("The Copper Kettles"), 1, 1, null),
            Track("b", "Whistle Stop", emptyList(), 2, 1, null)
        )
        albumRepository.result =
            AlbumDetailResult.Success(album.copy(coverImageUrl = "http://server/cover", tracks = tracks))
        val viewModel =
            AlbumDetailViewModel(albumId = "2", albumRepository = albumRepository, musicPlayer = musicPlayer)
        runCurrent()

        viewModel.play(startIndex = 1)

        val expectedQueue = listOf(
            QueueItem("a", "Boiler Room", listOf("The Copper Kettles"), "Steam & Rust", "http://server/cover"),
            // A track without artists shows the album's.
            QueueItem("b", "Whistle Stop", listOf("The Copper Kettles"), "Steam & Rust", "http://server/cover")
        )
        assertEquals(listOf(expectedQueue to 1), musicPlayer.playRequests)
    }

    @Test
    fun play_beforeTheAlbumLoaded_doesNothing() = runTest(dispatcher) {
        albumRepository.result = AlbumDetailResult.Success(album)
        val viewModel =
            AlbumDetailViewModel(albumId = "2", albumRepository = albumRepository, musicPlayer = musicPlayer)

        viewModel.play(startIndex = 0)

        assertTrue(musicPlayer.playRequests.isEmpty())
    }

    private class FakeMusicPlayer : MusicPlayer {
        val playRequests = mutableListOf<Pair<List<QueueItem>, Int>>()

        override fun play(queue: List<QueueItem>, startIndex: Int) {
            playRequests += queue to startIndex
        }
    }

    private class FakeAlbumRepository : AlbumRepository {
        var result: AlbumDetailResult = AlbumDetailResult.Failed(LibraryFailure.SERVER_ERROR)
        val requestedAlbumIds = mutableListOf<String>()

        override suspend fun getAlbums(sort: AlbumSort): AlbumsResult = error("Not used by the album screen")

        override suspend fun getAlbumDetail(albumId: String): AlbumDetailResult {
            requestedAlbumIds += albumId
            return result
        }
    }
}
