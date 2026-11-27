package io.github.lorber13.jellyfinmusic.feature.player

import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import io.github.lorber13.jellyfinmusic.core.playback.QueueItem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
        val playing = PlayerState("Boiler Room", "The Copper Kettles", null, true, false, true)

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

    private class FakeMusicPlayer : MusicPlayer {
        val commands = mutableListOf<String>()

        override val state = MutableStateFlow<PlayerState?>(null)
        override val positions = MutableSharedFlow<PlaybackPosition>()
        override val errorMessages: Flow<String> = emptyFlow()

        override fun play(queue: List<QueueItem>, startIndex: Int) = Unit

        override fun playOrPause() {
            commands += "playOrPause"
        }

        override fun skipToNext() {
            commands += "skipToNext"
        }
    }
}
