package io.github.lorber13.jellyfinmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import io.github.lorber13.jellyfinmusic.core.data.session.SessionState
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import io.github.lorber13.jellyfinmusic.ui.JellyfinMusicRoot
import io.github.lorber13.jellyfinmusic.ui.RootViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Until the stored session is read, the app doesn't know whether to open on sign-in or on the tabs.
        splashScreen.setKeepOnScreenCondition { rootViewModel.session.value == SessionState.Loading }
        enableEdgeToEdge()
        setContent {
            JellyfinMusicTheme {
                JellyfinMusicRoot(rootViewModel)
            }
        }
    }
}
