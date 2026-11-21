package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.library.Album
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSortField
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.designsystem.component.AlbumCover
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme

@Composable
internal fun AlbumsScreen(onAlbumClick: (albumId: String) -> Unit, viewModel: AlbumsViewModel = hiltViewModel()) {
    AlbumsContent(
        uiState = viewModel.uiState,
        sort = viewModel.sort,
        isRefreshing = viewModel.isRefreshing,
        refreshFailure = viewModel.refreshFailure,
        onSelectSort = viewModel::selectSort,
        onReload = viewModel::reload,
        onRefreshFailureShown = viewModel::onRefreshFailureShown,
        onAlbumClick = onAlbumClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumsContent(
    uiState: AlbumsUiState,
    sort: AlbumSort,
    isRefreshing: Boolean,
    refreshFailure: LibraryFailure?,
    onSelectSort: (AlbumSortField) -> Unit,
    onReload: () -> Unit,
    onRefreshFailureShown: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // The title bar stays in place and fills with a contrasting color once the grid scrolls under it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    RefreshFailureMessage(refreshFailure, snackbarHostState, onRefreshFailureShown)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_albums_title)) },
                actions = { AlbumSortMenu(sort = sort, onSelectSort = onSelectSort) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(contentPadding)
        when (uiState) {
            AlbumsUiState.Loading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is AlbumsUiState.Failed -> LoadFailure(
                reason = uiState.reason,
                onRetry = onReload,
                modifier = contentModifier
            )

            is AlbumsUiState.Loaded -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onReload,
                modifier = contentModifier
            ) {
                if (uiState.albums.isEmpty()) {
                    EmptyLibrary()
                } else {
                    AlbumGrid(albums = uiState.albums, sort = uiState.sort, onAlbumClick = onAlbumClick)
                }
            }
        }
    }
}

/** Tells the user that reloading failed, while the albums they already had stay on screen. */
@Composable
private fun RefreshFailureMessage(
    refreshFailure: LibraryFailure?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit
) {
    val message = refreshFailure?.let { stringResource(messageOf(it)) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onShown()
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, sort: AlbumSort, onAlbumClick: (albumId: String) -> Unit) {
    // A new grid for every order: a new sort starts at the top, while coming back from an album
    // keeps the position. Keyed by the names, which unlike enum hash codes stay the same when the
    // saved scroll position is restored in a new process.
    key("${sort.field.name}-${sort.direction.name}") {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { album -> album.id }) { album ->
                AlbumGridItem(album = album, onClick = { onAlbumClick(album.id) })
            }
        }
    }
}

@Composable
private fun AlbumGridItem(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Rounds the press ripple at the top to match the cover. The bottom corners stay square:
    // rounded ones would cut into the first letter of the artist's name.
    val rippleShape = MaterialTheme.shapes.medium.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)
    Column(modifier = modifier.clip(rippleShape).clickable(onClick = onClick)) {
        AlbumCover(imageUrl = album.coverImageUrl, modifier = Modifier.fillMaxWidth())
        Text(
            text = album.name,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Shown even when empty, so every item in a row has the same height.
        Text(
            text = album.artistName.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A message in the middle of the screen. It scrolls, so long text fits and pull to refresh still works. */
@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
private fun EmptyLibrary() {
    CenteredMessage {
        Text(
            text = stringResource(R.string.feature_albums_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadFailure(reason: LibraryFailure, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage(modifier = modifier) {
        Text(
            text = stringResource(messageOf(reason)),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.feature_albums_retry))
        }
    }
}

@StringRes
private fun messageOf(failure: LibraryFailure): Int = when (failure) {
    LibraryFailure.UNREACHABLE -> R.string.feature_albums_error_unreachable
    LibraryFailure.SERVER_ERROR -> R.string.feature_albums_error_server
}

@Preview
@Composable
private fun AlbumsContentPreview() {
    val albums = listOf(
        Album(id = "1", name = "Morning Light", artistName = "Aurora Fields", coverImageUrl = null),
        Album(id = "2", name = "Grid Lines (Deluxe Edition)", artistName = "東京 Echo Unit", coverImageUrl = null),
        Album(id = "3", name = "Untitled", artistName = null, coverImageUrl = null)
    )
    JellyfinMusicTheme {
        AlbumsContent(
            uiState = AlbumsUiState.Loaded(albums, AlbumSort.DEFAULT),
            sort = AlbumSort.DEFAULT,
            isRefreshing = false,
            refreshFailure = null,
            onSelectSort = {},
            onReload = {},
            onRefreshFailureShown = {},
            onAlbumClick = {}
        )
    }
}
