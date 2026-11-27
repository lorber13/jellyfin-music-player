package io.github.lorber13.jellyfinmusic.ui

import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import io.github.lorber13.jellyfinmusic.core.playback.QueueItem
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
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
    fun playbackError_isShownUntilReportedShown() {
        val viewModel = AppShellViewModel(musicPlayer)

        musicPlayer.errorMessages.tryEmit("Skipped 1 song")
        assertEquals("Skipped 1 song", viewModel.playbackErrorMessage)

        viewModel.onPlaybackErrorShown("Skipped 1 song")
        assertNull(viewModel.playbackErrorMessage)
    }

    @Test
    fun newerPlaybackError_staysWhenTheOlderIsReportedShown() {
        val viewModel = AppShellViewModel(musicPlayer)

        musicPlayer.errorMessages.tryEmit("Skipped 1 song")
        musicPlayer.errorMessages.tryEmit("Couldn't load this music")
        viewModel.onPlaybackErrorShown("Skipped 1 song")

        assertEquals("Couldn't load this music", viewModel.playbackErrorMessage)
    }

    private class FakeMusicPlayer : MusicPlayer {
        override val state: StateFlow<PlayerState?> = MutableStateFlow(null)
        override val positions: Flow<PlaybackPosition> = emptyFlow()
        override val errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)

        override fun play(queue: List<QueueItem>, startIndex: Int) = Unit

        override fun playOrPause() = Unit

        override fun skipToPrevious() = Unit

        override fun skipToNext() = Unit

        override fun seekTo(position: Duration) = Unit
    }
}
