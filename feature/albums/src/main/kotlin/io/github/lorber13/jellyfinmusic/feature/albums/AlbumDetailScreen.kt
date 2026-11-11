package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.lorber13.jellyfinmusic.core.designsystem.component.PlaceholderScreen

@Composable
internal fun AlbumDetailScreen(albumId: String, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_albums_sample_album, albumId),
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}
