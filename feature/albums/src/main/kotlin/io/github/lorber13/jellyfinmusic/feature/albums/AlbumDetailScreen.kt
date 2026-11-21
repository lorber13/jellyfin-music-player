package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.lorber13.jellyfinmusic.core.designsystem.component.PlaceholderScreen

/** Stands in for the album screen until LIB-07 shows the album's tracks. */
@Composable
internal fun AlbumDetailScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_albums_album_title),
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}
