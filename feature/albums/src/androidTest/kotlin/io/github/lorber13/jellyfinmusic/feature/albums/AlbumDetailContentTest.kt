package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetail
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.data.library.Track
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumDetailContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val album = AlbumDetail(
        id = "2",
        name = "Steam & Rust",
        artistName = "The Copper Kettles",
        year = 2021,
        coverImageUrl = null,
        tracks = listOf(
            track(id = "a", name = "Boiler Room", trackNumber = 1, discNumber = 1, duration = 3.minutes + 45.seconds),
            track(id = "b", name = "Whistle Stop", trackNumber = 2, discNumber = 1, duration = 4.minutes),
            track(id = "c", name = "Cold Iron", trackNumber = 1, discNumber = 2, duration = 2.minutes + 15.seconds)
        )
    )

    @Test
    fun showsTheAlbumAndItsTracks() {
        showContent(album)

        composeRule.onNode(hasText("Steam & Rust")).assertExists()
        composeRule.onNode(hasText("The Copper Kettles")).assertExists()
        composeRule.onNode(hasText("2021 · 3 songs · ${tenMinutes()}")).assertExists()
        composeRule.onNode(hasText("Boiler Room")).assertExists()
        composeRule.onNode(hasText("3:45")).assertExists()
        composeRule.onNode(hasText("Cold Iron")).assertExists()
    }

    @Test
    fun albumWithoutYear_leavesItOutOfTheSummary() {
        showContent(album.copy(year = null))

        composeRule.onNode(hasText("3 songs · ${tenMinutes()}")).assertExists()
    }

    @Test
    fun severalDiscs_showDiscHeadings() {
        showContent(album)

        composeRule.onNode(hasText("Disc 1")).assertExists()
        composeRule.onNode(hasText("Disc 2")).assertExists()
    }

    @Test
    fun oneDisc_hasNoDiscHeading() {
        showContent(album.copy(tracks = album.tracks.filter { it.discNumber == 1 }))

        composeRule.onNode(hasText("Disc 1")).assertDoesNotExist()
    }

    @Test
    fun trackByOtherArtists_namesThem() {
        val guestTrack = track(id = "d", name = "Duet", artistNames = listOf("The Copper Kettles", "Aurora Fields"))
        showContent(album.copy(tracks = listOf(guestTrack)))

        composeRule.onNode(hasText("The Copper Kettles and Aurora Fields")).assertExists()
    }

    @Test
    fun trackByTheAlbumArtist_doesNotRepeatTheirName() {
        showContent(album.copy(tracks = listOf(track(id = "a", name = "Boiler Room"))))

        // Only the header shows the name.
        composeRule.onAllNodes(hasText("The Copper Kettles")).assertCountEquals(1)
    }

    @Test
    fun playButton_playsFromTheFirstTrack() {
        val startIndices = mutableListOf<Int>()
        showContent(uiState = AlbumDetailUiState.Loaded(album), onPlay = { startIndices += it })

        composeRule.onNode(hasText(string(R.string.feature_albums_detail_play)) and hasClickAction()).performClick()

        assertEquals(listOf(0), startIndices)
    }

    @Test
    fun tappingATrack_playsFromThatTrack() {
        val startIndices = mutableListOf<Int>()
        showContent(uiState = AlbumDetailUiState.Loaded(album), onPlay = { startIndices += it })

        // Cold Iron is the third track; the disc headings above it don't count.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Cold Iron"))
        composeRule.onNode(hasText("Cold Iron")).performClick()

        assertEquals(listOf(2), startIndices)
    }

    @Test
    fun albumWithoutTracks_cannotBePlayed() {
        showContent(album.copy(tracks = emptyList()))

        composeRule.onNode(
            hasText(string(R.string.feature_albums_detail_play)) and hasClickAction()
        ).assertIsNotEnabled()
    }

    @Test
    fun failedLoad_showsTheErrorAndTriesAgain() {
        var retries = 0
        showContent(uiState = AlbumDetailUiState.Failed(LibraryFailure.SERVER_ERROR), onRetry = { retries++ })

        composeRule.onNode(hasText(string(R.string.feature_albums_detail_error_server))).assertExists()
        composeRule.onNode(hasText(string(R.string.feature_albums_retry)) and hasClickAction()).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun albumDuration_isRoundedToMinutesWithHoursFromOneHour() {
        assertEquals("48 min", formatAlbumDuration(48.minutes + 10.seconds, Locale.US))
        assertEquals("1 hr, 5 min", formatAlbumDuration(65.minutes, Locale.US))
        assertEquals("2 hr", formatAlbumDuration(120.minutes, Locale.US))
        assertEquals("1 min", formatAlbumDuration(20.seconds, Locale.US))
        assertEquals("0 min", formatAlbumDuration(Duration.ZERO, Locale.US))
    }

    @Test
    fun trackDuration_isAClockReadingWithoutALeadingZero() {
        assertEquals("3:45", formatTrackDuration(3.minutes + 45.seconds, Locale.US))
        assertEquals("0:07", formatTrackDuration(7.seconds, Locale.US))
        assertEquals("1:02:03", formatTrackDuration(62.minutes + 3.seconds, Locale.US))
    }

    private fun track(
        id: String,
        name: String,
        artistNames: List<String> = listOf("The Copper Kettles"),
        trackNumber: Int = 1,
        discNumber: Int = 1,
        duration: Duration = 3.minutes
    ) = Track(id, name, artistNames, trackNumber, discNumber, duration)

    private fun showContent(album: AlbumDetail) = showContent(uiState = AlbumDetailUiState.Loaded(album))

    private fun showContent(uiState: AlbumDetailUiState, onPlay: (Int) -> Unit = {}, onRetry: () -> Unit = {}) {
        composeRule.setContent {
            JellyfinMusicTheme {
                AlbumDetailContent(uiState = uiState, onPlay = onPlay, onRetry = onRetry, onNavigateBack = {})
            }
        }
    }

    private fun string(resource: Int): String = composeRule.activity.getString(resource)

    // The unit's short name depends on the device's language and region ("10 min" in the US, "10 mins" in Italy).
    private fun tenMinutes(): String =
        formatAlbumDuration(10.minutes, composeRule.activity.resources.configuration.locales[0])
}
