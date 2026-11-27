package io.github.lorber13.jellyfinmusic.feature.player

import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import kotlin.time.Duration
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
class MiniPlayerViewModelTest {
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
        val viewModel = MiniPlayerViewModel(musicPlayer)
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
    fun buttons_sendTheirCommandsToThePlayer() {
        val viewModel = MiniPlayerViewModel(musicPlayer)

        viewModel.playOrPause()
        viewModel.skipToNext()

        assertEquals(listOf("playOrPause", "skipToNext"), musicPlayer.commands)
    }

    @Test
    fun progress_followsThePosition() = runTest {
        val viewModel = MiniPlayerViewModel(musicPlayer)
        // stateIn only reads positions while someone collects the progress.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.progress.collect {} }

        musicPlayer.positions.emit(PlaybackPosition(position = 1.minutes, duration = 4.minutes))

        assertEquals(0.25f, viewModel.progress.value)
    }

    @Test
    fun progressOf_anUnknownOrEmptyLength_isNull() {
        assertNull(progressOf(PlaybackPosition(position = 10.seconds, duration = null)))
        assertNull(progressOf(PlaybackPosition(position = Duration.ZERO, duration = Duration.ZERO)))
    }

    // The player can report a position a moment past the end of the track.
    @Test
    fun progressOf_aPositionPastTheEnd_isFull() {
        assertEquals(1f, progressOf(PlaybackPosition(position = 4.minutes + 1.seconds, duration = 4.minutes)))
    }
}
