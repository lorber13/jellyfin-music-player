package io.github.lorber13.jellyfinmusic

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import io.github.lorber13.jellyfinmusic.feature.albums.R as AlbumsR
import io.github.lorber13.jellyfinmusic.ui.JellyfinMusicApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The albums come from FakeLibraryModule.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppShellNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    // The app opens on the sign-in flow, so the shell is shown on its own.
    @Before
    fun showAppShell() {
        composeRule.setContent {
            JellyfinMusicTheme {
                JellyfinMusicApp()
            }
        }
    }

    @Test
    fun tappingEachTab_showsItsScreen() {
        composeRule.onNode(screenTitle(R.string.destination_home)).assertExists()

        composeRule.onNode(navigationItem(R.string.destination_library)).performClick()
        composeRule.onNode(screenTitle(AlbumsR.string.feature_albums_title)).assertExists()

        composeRule.onNode(navigationItem(R.string.destination_search)).performClick()
        composeRule.onNode(screenTitle(R.string.destination_search)).assertExists()

        composeRule.onNode(navigationItem(R.string.destination_settings)).performClick()
        composeRule.onNode(screenTitle(R.string.destination_settings)).assertExists()
    }

    @Test
    fun openingAnAlbumAndGoingBack_returnsToTheAlbumsList() {
        composeRule.onNode(navigationItem(R.string.destination_library)).performClick()

        composeRule.onNode(hasText("Steam & Rust") and hasClickAction()).performClick()
        composeRule.onNode(screenTitle(AlbumsR.string.feature_albums_album_title)).assertExists()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNode(screenTitle(AlbumsR.string.feature_albums_title)).assertExists()
        composeRule.onNode(hasText("Steam & Rust")).assertExists()
    }

    // A tab label and a screen title can show the same text; only the tab is clickable.
    private fun navigationItem(@StringRes label: Int): SemanticsMatcher =
        hasText(composeRule.activity.getString(label)) and hasClickAction()

    private fun screenTitle(@StringRes title: Int): SemanticsMatcher =
        hasText(composeRule.activity.getString(title)) and hasClickAction().not()
}
