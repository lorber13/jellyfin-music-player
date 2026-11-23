package io.github.lorber13.jellyfinmusic.ui

import io.github.lorber13.jellyfinmusic.core.playback.MusicPlayer
import io.github.lorber13.jellyfinmusic.core.playback.QueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
        override val errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)

        override fun play(queue: List<QueueItem>, startIndex: Int) = Unit
    }
}
