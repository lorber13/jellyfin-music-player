package io.github.lorber13.jellyfinmusic.feature.player

import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {
    private val musicPlayer = FakeMusicPlayer()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun state_isThePlayersState() {
        val viewModel = NowPlayingViewModel(musicPlayer)
        val playing = PlayerState(
            title = "Boiler Room",
            artist = "The Copper Kettles",
            album = "Steam & Rust",
            artworkUrl = null,
            isPlaying = true,
            isLoading = false,
            isSeekable = true,
            hasPrevious = true,
            hasNext = true
        )

        musicPlayer.state.value = playing

        assertEquals(playing, viewModel.state.value)
    }

    @Test
    fun position_isUnknownUntilThePlayerSays_thenFollowsIt() = runTest {
        val viewModel = NowPlayingViewModel(musicPlayer)
        // stateIn only reads positions while someone collects them.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.position.collect {} }
        assertNull(viewModel.position.value)

        val position = PlaybackPosition(position = 1.minutes, duration = 4.minutes)
        musicPlayer.positions.emit(position)

        assertEquals(position, viewModel.position.value)
    }

    @Test
    fun controls_sendTheirCommandsToThePlayer() {
        val viewModel = NowPlayingViewModel(musicPlayer)

        viewModel.skipToPrevious()
        viewModel.playOrPause()
        viewModel.skipToNext()
        viewModel.seekTo(83.seconds)

        assertEquals(listOf("skipToPrevious", "playOrPause", "skipToNext", "seekTo"), musicPlayer.commands)
        assertEquals(listOf(83.seconds), musicPlayer.seekPositions)
    }
}
