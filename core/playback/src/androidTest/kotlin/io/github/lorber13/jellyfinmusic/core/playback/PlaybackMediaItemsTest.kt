package io.github.lorber13.jellyfinmusic.core.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamKind
import io.github.lorber13.jellyfinmusic.core.data.stream.TrackStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackMediaItemsTest {
    private val originalFile =
        TrackStream("http://music.local:8096/Audio/track-1/stream?static=true", StreamKind.ORIGINAL_FILE)
    private val conversion =
        TrackStream("http://music.local:8096/audio/track-2/master.m3u8?AudioCodec=aac", StreamKind.HLS_CONVERSION)

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
        val items =
            playableMediaItemsOf(listOf(itemWithId("track-1"), itemWithId("track-2")), listOf(originalFile, conversion))

        assertEquals(listOf(originalFile.url, conversion.url), items.map { it?.localConfiguration?.uri.toString() })
        assertEquals(listOf("track-1", "track-2"), items.map { it?.mediaId })
    }

    @Test
    fun playableItems_ofConversions_areHlsAndOriginalFilesHaveNoType() {
        val items =
            playableMediaItemsOf(listOf(itemWithId("track-1"), itemWithId("track-2")), listOf(originalFile, conversion))

        assertEquals(listOf(null, MimeTypes.APPLICATION_M3U8), items.map { it?.localConfiguration?.mimeType })
    }

    @Test
    fun playableItems_replaceAnAddressAndTypeSentByTheController() {
        val item = itemWithId("track-1").buildUpon()
            .setUri("https://elsewhere.example/song.m3u8")
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        val playable = checkNotNull(playableMediaItemsOf(listOf(item), listOf(originalFile)).single())

        assertEquals(originalFile.url, playable.localConfiguration?.uri.toString())
        assertNull(playable.localConfiguration?.mimeType)
    }

    @Test
    fun playableItems_withoutAStream_areNull() {
        val items = playableMediaItemsOf(listOf(itemWithId("track-1"), itemWithId("track-2")), listOf(null, conversion))

        assertNull(items[0])
        assertEquals("track-2", items[1]?.mediaId)
    }

    private fun itemWithId(mediaId: String): MediaItem = MediaItem.Builder().setMediaId(mediaId).build()
}
