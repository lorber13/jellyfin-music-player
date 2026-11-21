package io.github.lorber13.jellyfinmusic.core.data.library

import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumDetailMappingTest {
    private val trackId = UUID.fromString("0b9a2f1e-58c4-4d1a-9b7e-3c2d1e0f4a5b")
    private val albumId = UUID.fromString("fe3a74bd-7a78-fa0d-925b-a11ddf0db87b")

    @Test
    fun mapsTheTrack() {
        val item = track(
            name = "Dawn",
            artists = listOf("Aurora Fields", "Café Ñandú"),
            indexNumber = 3,
            parentIndexNumber = 2,
            // 3 minutes 45.5 seconds, in ticks of 100 nanoseconds.
            runTimeTicks = 2_255_000_000
        )

        assertEquals(
            Track(
                id = trackId.toString(),
                name = "Dawn",
                artistNames = listOf("Aurora Fields", "Café Ñandú"),
                trackNumber = 3,
                discNumber = 2,
                duration = 3.minutes + 45.seconds + 500.milliseconds
            ),
            trackOf(item)
        )
    }

    @Test
    fun untaggedTrack_hasNoNumbersOrDuration() {
        val track = trackOf(track(artists = null, indexNumber = null, parentIndexNumber = null, runTimeTicks = null))

        assertEquals(emptyList<String>(), track.artistNames)
        assertNull(track.trackNumber)
        assertNull(track.discNumber)
        assertNull(track.duration)
    }

    @Test
    fun blankArtistNames_areLeftOut() {
        assertEquals(listOf("Aurora Fields"), trackOf(track(artists = listOf(" ", "Aurora Fields"))).artistNames)
    }

    @Test
    fun mapsTheAlbumWithItsYearAndTracks() {
        val tracks = listOf(trackOf(track(name = "Dawn")))
        val item = BaseItemDto(
            id = albumId,
            type = BaseItemKind.MUSIC_ALBUM,
            name = "Morning Light",
            albumArtist = "Aurora Fields",
            productionYear = 2021
        )

        assertEquals(
            AlbumDetail(
                id = albumId.toString(),
                name = "Morning Light",
                artistName = "Aurora Fields",
                year = 2021,
                coverImageUrl = "cover",
                tracks = tracks
            ),
            albumDetailOf(item, coverImageUrl = "cover", tracks = tracks)
        )
    }

    @Test
    fun totalDuration_addsUpTheTracksAndSkipsUnknownOnes() {
        val album = AlbumDetail(
            id = "1",
            name = "Morning Light",
            artistName = null,
            year = null,
            coverImageUrl = null,
            tracks = listOf(
                trackOf(track(runTimeTicks = 1_800_000_000)),
                trackOf(track(runTimeTicks = null)),
                trackOf(track(runTimeTicks = 600_000_000))
            )
        )

        assertEquals(4.minutes, album.totalDuration)
    }

    private fun track(
        name: String? = "Track",
        artists: List<String>? = listOf("Aurora Fields"),
        indexNumber: Int? = 1,
        parentIndexNumber: Int? = 1,
        runTimeTicks: Long? = 1_800_000_000
    ) = BaseItemDto(
        id = trackId,
        type = BaseItemKind.AUDIO,
        name = name,
        artists = artists,
        indexNumber = indexNumber,
        parentIndexNumber = parentIndexNumber,
        runTimeTicks = runTimeTicks
    )
}
