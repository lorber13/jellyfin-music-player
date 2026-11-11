package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

// Stand-in album IDs so list-to-detail navigation can be tried before LIB-03 loads real albums.
private val sampleAlbumIds = listOf("1", "2", "3")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumsScreen(onAlbumClick: (albumId: String) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_albums_title)) }) }
    ) { contentPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            items(sampleAlbumIds) { albumId ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.feature_albums_sample_album, albumId)) },
                    modifier = Modifier.clickable { onAlbumClick(albumId) }
                )
            }
        }
    }
}
