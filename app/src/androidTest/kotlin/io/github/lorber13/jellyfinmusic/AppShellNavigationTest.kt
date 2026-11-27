package io.github.lorber13.jellyfinmusic

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import io.github.lorber13.jellyfinmusic.feature.albums.R as AlbumsR
import io.github.lorber13.jellyfinmusic.feature.player.R as PlayerR
import io.github.lorber13.jellyfinmusic.ui.JellyfinMusicApp
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The albums come from FakeLibraryModule, and what is playing from FakePlaybackModule.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppShellNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var musicPlayer: FakeMusicPlayer

    // The app opens on the sign-in flow, so the shell is shown on its own.
    @Before
    fun showAppShell() {
        hiltRule.inject()
        composeRule.setContent {
            JellyfinMusicTheme {
                JellyfinMusicApp()
            }
        }
    }

    // Albums is the only tab until LIB-02 adds Artists, so there is no navigation bar yet.
    @Test
    fun opensOnTheAlbumsList_withoutANavigationBar() {
        composeRule.onNode(screenTitle(AlbumsR.string.feature_albums_title)).assertExists()
        composeRule.onNode(navigationItem(R.string.destination_albums)).assertDoesNotExist()
    }

    @Test
    fun openingAnAlbumAndGoingBack_returnsToTheAlbumsList() {
        composeRule.onNode(hasText("Steam & Rust") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Steam & Rust Overture")).assertExists()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNode(screenTitle(AlbumsR.string.feature_albums_title)).assertExists()
        composeRule.onNode(hasText("Steam & Rust")).assertExists()
    }

    // Each album screen has its own ViewModel, so the second album doesn't show the first one's tracks.
    @Test
    fun openingAnotherAlbum_showsItsOwnTracks() {
        composeRule.onNode(hasText("Steam & Rust") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Steam & Rust Overture")).assertExists()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNode(hasText("Morning Light") and hasClickAction()).performClick()

        composeRule.onNode(hasText("Morning Light Overture")).assertExists()
    }

    @Test
    fun withNothingQueued_thereIsNoMiniPlayer() {
        composeRule.onNode(miniPlayerButton(PlayerR.string.feature_player_next_song)).assertDoesNotExist()
    }

    @Test
    fun miniPlayer_showsOverTheAlbumsListAndTheAlbumScreen() {
        musicPlayer.state.value = PlayerState(
            title = "Boiler Room",
            artist = "The Copper Kettles",
            artworkUrl = null,
            isPlaying = true,
            isLoading = false,
            hasNext = true
        )
        composeRule.onNode(hasText("Boiler Room")).assertExists()
        composeRule.onNode(miniPlayerButton(PlayerR.string.feature_player_pause)).assertExists()

        composeRule.onNode(hasText("Steam & Rust") and hasClickAction()).performClick()

        composeRule.onNode(hasText("Steam & Rust Overture")).assertExists()
        composeRule.onNode(hasText("Boiler Room")).assertExists()
    }

    // A tab label and a screen title can show the same text; only the tab is clickable.
    private fun navigationItem(@StringRes label: Int): SemanticsMatcher =
        hasText(composeRule.activity.getString(label)) and hasClickAction()

    private fun screenTitle(@StringRes title: Int): SemanticsMatcher =
        hasText(composeRule.activity.getString(title)) and hasClickAction().not()

    private fun miniPlayerButton(@StringRes label: Int): SemanticsMatcher =
        hasContentDescription(composeRule.activity.getString(label))
}
