package io.github.lorber13.jellyfinmusic.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The app theme. For now it only switches between the default Material light and dark colors,
 * so the edge-to-edge system bar icons stay readable. Dynamic color and the app's own palette
 * come with UI-01 and UI-02.
 */
@Composable
fun JellyfinMusicTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}
