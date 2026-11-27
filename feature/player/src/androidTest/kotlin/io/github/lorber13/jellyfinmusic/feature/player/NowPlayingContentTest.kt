package io.github.lorber13.jellyfinmusic.feature.player

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.designsystem.format.formatTrackDuration
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowPlayingContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val playing = PlayerState(
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
    private val elapsed = 1.minutes + 23.seconds
    private val length = 4.minutes + 56.seconds
    private val clicks = mutableListOf<String>()
    private val seekPositions = mutableListOf<Duration>()

    @Test
    fun showsTheTrackItsArtistAndItsAlbum() {
        showContent(playing)

        composeRule.onNode(hasText("Boiler Room")).assertExists()
        composeRule.onNode(hasText("The Copper Kettles")).assertExists()
        composeRule.onNode(hasText("Steam & Rust")).assertExists()
    }

    // The times are formatted in the device's language, like the album screen's track lengths.
    @Test
    fun showsTheElapsedTimeAndTheTracksLength() {
        showContent(playing)

        composeRule.onNode(hasText(formatTrackDuration(elapsed, deviceLocale()))).assertExists()
        composeRule.onNode(hasText(formatTrackDuration(length, deviceLocale()))).assertExists()
    }

    @Test
    fun seekBar_tellsScreenReadersThePositionInWords() {
        showContent(playing)
        val expected = composeRule.activity.getString(
            R.string.feature_player_position_of_duration,
            formatSpokenDuration(elapsed, deviceLocale()),
            formatSpokenDuration(length, deviceLocale())
        )

        composeRule.onNode(seekBar())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected))
    }

    @Test
    fun settingTheSeekBar_movesTheTrackThere() {
        showContent(playing)

        composeRule.onNode(seekBar()).assertIsEnabled()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.5f) }

        assertEquals(listOf(length / 2), seekPositions)
    }

    @Test
    fun whileTheTrackLoads_theSeekBarIsDisabled() {
        showContent(playing.copy(isSeekable = false))

        composeRule.onNode(seekBar()).assertIsNotEnabled()
    }

    @Test
    fun whileTheLengthIsUnknown_theSeekBarIsDisabled() {
        showContent(playing, PlaybackPosition(position = Duration.ZERO, duration = null))

        composeRule.onNode(seekBar()).assertIsNotEnabled()
    }

    @Test
    fun whilePlaying_offersPause() {
        showContent(playing)

        composeRule.onNode(button(R.string.feature_player_pause)).performClick()

        composeRule.onNode(button(R.string.feature_player_play)).assertDoesNotExist()
        assertEquals(listOf("playOrPause"), clicks)
    }

    @Test
    fun whilePaused_offersPlay() {
        showContent(playing.copy(isPlaying = false))

        composeRule.onNode(button(R.string.feature_player_play)).performClick()

        composeRule.onNode(button(R.string.feature_player_pause)).assertDoesNotExist()
        assertEquals(listOf("playOrPause"), clicks)
    }

    @Test
    fun previousAndNext_skipBackAndForward() {
        showContent(playing)

        composeRule.onNode(button(R.string.feature_player_previous_song)).assertIsEnabled().performClick()
        composeRule.onNode(button(R.string.feature_player_next_song)).assertIsEnabled().performClick()

        assertEquals(listOf("skipToPrevious", "skipToNext"), clicks)
    }

    @Test
    fun onTheLastTrack_nextIsDisabled() {
        showContent(playing.copy(hasNext = false))

        composeRule.onNode(button(R.string.feature_player_next_song)).assertIsNotEnabled()
    }

    @Test
    fun theDownArrow_closesTheScreen() {
        showContent(playing)

        composeRule.onNode(button(R.string.feature_player_close_now_playing)).performClick()

        assertEquals(listOf("close"), clicks)
    }

    @Test
    fun spokenDuration_leavesOutUnitsThatAreZero() {
        assertEquals("1 minute, 23 seconds", formatSpokenDuration(83.seconds, Locale.US))
        assertEquals("4 minutes", formatSpokenDuration(4.minutes, Locale.US))
        assertEquals("1 hour, 2 minutes, 3 seconds", formatSpokenDuration(62.minutes + 3.seconds, Locale.US))
        assertEquals("0 seconds", formatSpokenDuration(Duration.ZERO, Locale.US))
    }

    private fun showContent(state: PlayerState, position: PlaybackPosition = PlaybackPosition(elapsed, length)) {
        composeRule.setContent {
            JellyfinMusicTheme {
                NowPlayingContent(
                    state = state,
                    position = { position },
                    onClose = { clicks += "close" },
                    onPlayOrPause = { clicks += "playOrPause" },
                    onSkipToPrevious = { clicks += "skipToPrevious" },
                    onSkipToNext = { clicks += "skipToNext" },
                    onSeek = { seekPosition -> seekPositions += seekPosition }
                )
            }
        }
    }

    private fun deviceLocale(): Locale = composeRule.activity.resources.configuration.locales[0]

    private fun seekBar() = hasContentDescription(composeRule.activity.getString(R.string.feature_player_song_position))

    private fun button(@StringRes label: Int) = hasContentDescription(composeRule.activity.getString(label))
}
