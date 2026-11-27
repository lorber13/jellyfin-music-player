package io.github.lorber13.jellyfinmusic.feature.player

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniPlayerContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val playing = PlayerState(
        title = "Boiler Room",
        artist = "The Copper Kettles",
        artworkUrl = null,
        isPlaying = true,
        isLoading = false,
        hasNext = true
    )
    private val clicks = mutableListOf<String>()

    @Test
    fun showsTheTrackAndItsArtist() {
        showContent(playing)

        composeRule.onNode(hasText("Boiler Room")).assertExists()
        composeRule.onNode(hasText("The Copper Kettles")).assertExists()
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
    fun next_skipsToTheNextTrack() {
        showContent(playing)

        composeRule.onNode(button(R.string.feature_player_next_song)).assertIsEnabled().performClick()

        assertEquals(listOf("skipToNext"), clicks)
    }

    @Test
    fun onTheLastTrack_nextIsDisabled() {
        showContent(playing.copy(hasNext = false))

        composeRule.onNode(button(R.string.feature_player_next_song)).assertIsNotEnabled()
    }

    private fun showContent(state: PlayerState) {
        composeRule.setContent {
            JellyfinMusicTheme {
                MiniPlayerContent(
                    state = state,
                    progress = { 0.5f },
                    onPlayOrPause = { clicks += "playOrPause" },
                    onSkipToNext = { clicks += "skipToNext" }
                )
            }
        }
    }

    private fun button(@StringRes label: Int) = hasContentDescription(composeRule.activity.getString(label))
}
