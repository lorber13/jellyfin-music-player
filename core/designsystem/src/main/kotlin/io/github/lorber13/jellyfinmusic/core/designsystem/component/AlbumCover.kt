package io.github.lorber13.jellyfinmusic.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.lorber13.jellyfinmusic.core.designsystem.R
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme

/**
 * An album's square cover. An album icon shows while the image loads, when it fails, and when
 * the album has no artwork ([imageUrl] is null).
 *
 * The cover is decorative: it always sits next to the album's name, which is what screen readers read.
 */
@Composable
fun AlbumCover(imageUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        // Drawn under the image, so it shows through until the image covers it.
        Icon(
            painter = painterResource(R.drawable.designsystem_ic_album),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.4f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Preview
@Composable
private fun AlbumCoverWithoutArtworkPreview() {
    JellyfinMusicTheme {
        AlbumCover(imageUrl = null, modifier = Modifier.width(160.dp))
    }
}
