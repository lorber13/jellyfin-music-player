package io.github.lorber13.jellyfinmusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.lorber13.jellyfinmusic.R
import io.github.lorber13.jellyfinmusic.core.designsystem.component.PlaceholderScreen
import io.github.lorber13.jellyfinmusic.feature.albums.AlbumDetailRoute
import io.github.lorber13.jellyfinmusic.feature.albums.albumsEntries
import io.github.lorber13.jellyfinmusic.navigation.HomeRoute
import io.github.lorber13.jellyfinmusic.navigation.Navigator
import io.github.lorber13.jellyfinmusic.navigation.SearchRoute
import io.github.lorber13.jellyfinmusic.navigation.SettingsRoute
import io.github.lorber13.jellyfinmusic.navigation.TopLevelDestination
import io.github.lorber13.jellyfinmusic.navigation.rememberNavigationState
import io.github.lorber13.jellyfinmusic.navigation.toDecoratedEntries

/**
 * The app shell: the navigation bar (or rail on wider windows) around the current screen, and
 * messages about playback over every screen.
 */
@Composable
fun JellyfinMusicApp(viewModel: AppShellViewModel = hiltViewModel()) {
    val navigationState = rememberNavigationState(startDestination = TopLevelDestination.HOME)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val snackbarHostState = remember { SnackbarHostState() }
    PlaybackErrorMessage(viewModel.playbackErrorMessage, snackbarHostState, viewModel::onPlaybackErrorShown)

    val entryProvider = entryProvider<NavKey> {
        entry<HomeRoute> { PlaceholderScreen(title = stringResource(R.string.destination_home)) }
        entry<SearchRoute> { PlaceholderScreen(title = stringResource(R.string.destination_search)) }
        entry<SettingsRoute> { PlaceholderScreen(title = stringResource(R.string.destination_settings)) }
        albumsEntries(
            onAlbumClick = { albumId -> navigator.navigate(AlbumDetailRoute(albumId)) },
            onNavigateBack = navigator::goBack
        )
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination == navigationState.currentDestination,
                    onClick = { navigator.selectTopLevelDestination(destination) },
                    // The label next to the icon already describes it to screen readers.
                    icon = {
                        val isSelected = destination == navigationState.currentDestination
                        val iconResource =
                            if (isSelected) destination.selectedIconResource else destination.unselectedIconResource
                        Icon(painterResource(iconResource), contentDescription = null)
                    },
                    label = { Text(stringResource(destination.labelResource)) }
                )
            }
        }
    ) {
        Box {
            NavDisplay(
                entries = navigationState.toDecoratedEntries(entryProvider),
                onBack = navigator::goBack
            )
            // The navigation suite has no place for messages, so they go over the screen, above
            // the navigation bar and the system bars.
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        }
    }
}

/** Shows [message], then reports it shown with [onShown]. */
@Composable
private fun PlaybackErrorMessage(message: String?, snackbarHostState: SnackbarHostState, onShown: (String) -> Unit) {
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onShown(message)
        }
    }
}
