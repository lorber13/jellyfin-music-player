package io.github.lorber13.jellyfinmusic.core.playback

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerStateTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var player: ExoPlayer

    // A player is used on the thread it is built on, here the main thread. It is never prepared,
    // so it loads nothing from the addresses its items get.
    @Before
    fun setUp() {
        instrumentation.runOnMainSync { player = ExoPlayer.Builder(instrumentation.targetContext).build() }
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync { player.release() }
    }

    @Test
    fun withNothingQueued_thereIsNoState() {
        assertNull(stateOnMainThread())
    }

    @Test
    fun state_showsWhatTheQueueWasSentWith() {
        onMainThread { player.setMediaItems(listOf(playableItemOf(boilerRoom))) }

        val state = checkNotNull(stateOnMainThread())

        assertEquals("Boiler Room", state.title)
        assertEquals("The Copper Kettles", state.artist)
        assertEquals("http://music.local:8096/Items/album-1/Images/Primary", state.artworkUrl)
    }

    @Test
    fun trackWithoutArtistsOrArtwork_hasNone() {
        val untitled = QueueItem("track-9", "Untitled", artistNames = emptyList(), albumName = null, artworkUrl = null)
        onMainThread { player.setMediaItems(listOf(playableItemOf(untitled))) }

        val state = checkNotNull(stateOnMainThread())

        assertNull(state.artist)
        assertNull(state.artworkUrl)
    }

    @Test
    fun pausedPlayer_isNotPlayingOrLoading() {
        onMainThread { player.setMediaItems(listOf(playableItemOf(boilerRoom))) }

        val state = checkNotNull(stateOnMainThread())

        assertFalse(state.isPlaying)
        assertFalse(state.isLoading)
    }

    @Test
    fun firstOfTwoTracks_hasANextTrack_andTheLastHasNone() {
        onMainThread { player.setMediaItems(listOf(playableItemOf(boilerRoom), playableItemOf(whistleStop))) }
        assertTrue(checkNotNull(stateOnMainThread()).hasNext)

        onMainThread { player.seekToNextMediaItem() }
        val lastTrack = checkNotNull(stateOnMainThread())

        assertEquals("Whistle Stop", lastTrack.title)
        assertFalse(lastTrack.hasNext)
    }

    private fun onMainThread(action: () -> Unit) = instrumentation.runOnMainSync(action)

    // The player only takes items with an address, which the service adds when they are queued.
    private fun playableItemOf(item: QueueItem): MediaItem =
        mediaItemOf(item).buildUpon().setUri("http://music.local:8096/Audio/${item.trackId}/stream").build()

    private fun stateOnMainThread(): PlayerState? {
        var state: PlayerState? = null
        onMainThread { state = playerStateOf(player) }
        return state
    }

    private companion object {
        // One artist each, so the results don't depend on the device's language.
        val boilerRoom = QueueItem(
            trackId = "track-1",
            title = "Boiler Room",
            artistNames = listOf("The Copper Kettles"),
            albumName = "Steam & Rust",
            artworkUrl = "http://music.local:8096/Items/album-1/Images/Primary"
        )
        val whistleStop = QueueItem(
            trackId = "track-2",
            title = "Whistle Stop",
            artistNames = listOf("The Copper Kettles"),
            albumName = "Steam & Rust",
            artworkUrl = null
        )
    }
}
