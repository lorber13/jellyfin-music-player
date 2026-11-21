package io.github.lorber13.jellyfinmusic.core.data.library

import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.NameGuidPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumMappingTest {
    private val id = UUID.fromString("fe3a74bd-7a78-fa0d-925b-a11ddf0db87b")

    @Test
    fun mapsTheAlbum() {
        val item = album(name = "Morning Light", albumArtist = "Aurora Fields")

        assertEquals(
            Album(id = id.toString(), name = "Morning Light", artistName = "Aurora Fields", coverImageUrl = "cover"),
            albumOf(item, coverImageUrl = "cover")
        )
    }

    @Test
    fun missingAlbumArtist_usesTheFirstOfTheAlbumArtists() {
        val item = album(albumArtist = " ", albumArtists = listOf(artist("Café Ñandú"), artist("Other")))

        assertEquals("Café Ñandú", albumOf(item, coverImageUrl = null).artistName)
    }

    @Test
    fun noArtistAtAll_hasNoArtistName() {
        val item = album(albumArtist = null, albumArtists = emptyList())

        assertNull(albumOf(item, coverImageUrl = null).artistName)
    }

    @Test
    fun missingName_isEmpty() {
        assertEquals("", albumOf(album(name = null), coverImageUrl = null).name)
    }

    private fun album(name: String? = "Album", albumArtist: String? = null, albumArtists: List<NameGuidPair>? = null) =
        BaseItemDto(
            id = id,
            type = BaseItemKind.MUSIC_ALBUM,
            name = name,
            albumArtist = albumArtist,
            albumArtists = albumArtists
        )

    private fun artist(name: String) = NameGuidPair(id = UUID.randomUUID(), name = name)
}
