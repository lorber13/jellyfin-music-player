package io.github.lorber13.jellyfinmusic.core.playback

import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackMediaItemsTest {
    private val streams = object : StreamRepository {
        override fun streamUrlOf(trackId: String): String? =
            if (trackId.startsWith("track-")) "http://music.local:8096/Audio/$trackId/stream?static=true" else null

        override fun requestHeaders(): Map<String, String> = emptyMap()
    }

    @Test
    fun mediaItem_carriesTheTrackIdAndWhatTheNotificationShows_butNoAddress() {
        val item = mediaItemOf(
            QueueItem(
                trackId = "track-1",
                title = "Boiler Room",
                // One artist, so the result doesn't depend on the device's language.
                artistNames = listOf("The Copper Kettles"),
                albumName = "Steam & Rust",
                artworkUrl = "http://music.local:8096/Items/album-1/Images/Primary"
            )
        )

        assertEquals("track-1", item.mediaId)
        assertNull(item.localConfiguration)
        assertEquals("Boiler Room", item.mediaMetadata.title)
        assertEquals("The Copper Kettles", item.mediaMetadata.artist)
        assertEquals("Steam & Rust", item.mediaMetadata.albumTitle)
        assertEquals("http://music.local:8096/Items/album-1/Images/Primary", item.mediaMetadata.artworkUri.toString())
    }

    @Test
    fun mediaItem_withoutArtists_hasNoArtist() {
        val item = mediaItemOf(
            QueueItem(
                trackId = "track-1",
                title = "Untitled",
                artistNames = emptyList(),
                albumName = null,
                artworkUrl = null
            )
        )

        assertNull(item.mediaMetadata.artist)
        assertNull(item.mediaMetadata.artworkUri)
    }

    @Test
    fun playableItems_getTheStreamAddressOfTheirTrack() {
        val items = playableMediaItemsOf(listOf(itemWithId("track-1"), itemWithId("track-2")), streams)

        assertEquals(
            listOf(
                "http://music.local:8096/Audio/track-1/stream?static=true",
                "http://music.local:8096/Audio/track-2/stream?static=true"
            ),
            items?.map { it.localConfiguration?.uri.toString() }
        )
    }

    @Test
    fun playableItems_replaceAnAddressSentByTheController() {
        val item = itemWithId("track-1").buildUpon().setUri("https://elsewhere.example/song.mp3").build()

        val items = playableMediaItemsOf(listOf(item), streams)

        assertEquals(
            "http://music.local:8096/Audio/track-1/stream?static=true",
            items?.single()?.localConfiguration?.uri.toString()
        )
    }

    @Test
    fun playableItems_withAnUnknownId_areRejectedTogether() {
        assertNull(playableMediaItemsOf(listOf(itemWithId("track-1"), itemWithId("somewhere-else")), streams))
    }

    private fun itemWithId(mediaId: String): MediaItem = MediaItem.Builder().setMediaId(mediaId).build()
}
