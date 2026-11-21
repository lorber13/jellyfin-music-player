package io.github.lorber13.jellyfinmusic.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    inversePrimary = inversePrimaryLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    surfaceTint = surfaceTintLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    surfaceBright = surfaceBrightLight,
    surfaceDim = surfaceDimLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
    primaryFixed = primaryFixedLight,
    primaryFixedDim = primaryFixedDimLight,
    onPrimaryFixed = onPrimaryFixedLight,
    onPrimaryFixedVariant = onPrimaryFixedVariantLight,
    secondaryFixed = secondaryFixedLight,
    secondaryFixedDim = secondaryFixedDimLight,
    onSecondaryFixed = onSecondaryFixedLight,
    onSecondaryFixedVariant = onSecondaryFixedVariantLight,
    tertiaryFixed = tertiaryFixedLight,
    tertiaryFixedDim = tertiaryFixedDimLight,
    onTertiaryFixed = onTertiaryFixedLight,
    onTertiaryFixedVariant = onTertiaryFixedVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    inversePrimary = inversePrimaryDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    surfaceTint = surfaceTintDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    surfaceBright = surfaceBrightDark,
    surfaceDim = surfaceDimDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
    primaryFixed = primaryFixedDark,
    primaryFixedDim = primaryFixedDimDark,
    onPrimaryFixed = onPrimaryFixedDark,
    onPrimaryFixedVariant = onPrimaryFixedVariantDark,
    secondaryFixed = secondaryFixedDark,
    secondaryFixedDim = secondaryFixedDimDark,
    onSecondaryFixed = onSecondaryFixedDark,
    onSecondaryFixedVariant = onSecondaryFixedVariantDark,
    tertiaryFixed = tertiaryFixedDark,
    tertiaryFixedDim = tertiaryFixedDimDark,
    onTertiaryFixed = onTertiaryFixedDark,
    onTertiaryFixedVariant = onTertiaryFixedVariantDark
)

/**
 * The app theme. On Android 12+ the colors follow the wallpaper (Material You); on older
 * versions they come from the fixed app palette in Color.kt.
 *
 * @param dynamicColor false forces the fixed app palette, e.g. in previews.
 */
@Composable
fun JellyfinMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = colorScheme(darkTheme, dynamicColor), content = content)
}

@Composable
private fun colorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    val dynamicColorAvailable = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        dynamicColorAvailable && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColorAvailable -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}

@Preview(name = "Fixed palette, light")
@Composable
private fun LightPalettePreview() {
    JellyfinMusicTheme(darkTheme = false, dynamicColor = false) { PaletteSwatches() }
}

@Preview(name = "Fixed palette, dark")
@Composable
private fun DarkPalettePreview() {
    JellyfinMusicTheme(darkTheme = true, dynamicColor = false) { PaletteSwatches() }
}

// Only used by the previews above: the labels are color role names for developers.
@Suppress("HardcodedComposeText")
@Composable
private fun PaletteSwatches() {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.background(colors.surface).padding(8.dp)) {
        Swatch("primary", colors.primary, colors.onPrimary)
        Swatch("primaryContainer", colors.primaryContainer, colors.onPrimaryContainer)
        Swatch("secondaryContainer", colors.secondaryContainer, colors.onSecondaryContainer)
        Swatch("tertiaryContainer", colors.tertiaryContainer, colors.onTertiaryContainer)
        Swatch("surface", colors.surface, colors.onSurface)
        Swatch("surfaceContainer", colors.surfaceContainer, colors.onSurface)
        Swatch("error", colors.error, colors.onError)
    }
}

@Composable
private fun Swatch(role: String, color: Color, onColor: Color) {
    Text(
        text = role,
        color = onColor,
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(12.dp)
    )
}
