package io.github.lorber13.jellyfinmusic

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeFollowsSystemTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nightModeOn_usesDarkColors() {
        val surface = themeSurfaceColor(Configuration.UI_MODE_NIGHT_YES)
        assertTrue("surface $surface should be dark", surface.luminance() < 0.5f)
    }

    @Test
    fun nightModeOff_usesLightColors() {
        val surface = themeSurfaceColor(Configuration.UI_MODE_NIGHT_NO)
        assertTrue("surface $surface should be light", surface.luminance() > 0.5f)
    }

    /**
     * Returns the surface color the app theme picks when the system night mode is [nightMode].
     * Checks brightness instead of exact colors because, with dynamic color on (as shipped),
     * the colors come from the device wallpaper.
     */
    private fun themeSurfaceColor(nightMode: Int): Color {
        var surface = Color.Unspecified
        composeRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                JellyfinMusicTheme {
                    surface = MaterialTheme.colorScheme.surface
                }
            }
        }
        composeRule.waitForIdle()
        return surface
    }
}
