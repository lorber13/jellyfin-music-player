package io.github.lorber13.jellyfinmusic.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

/** The app shell: the navigation bar (or rail on wider windows) around the current screen. */
@Composable
fun JellyfinMusicApp() {
    val navigationState = rememberNavigationState(startDestination = TopLevelDestination.HOME)
    val navigator = remember(navigationState) { Navigator(navigationState) }

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
        NavDisplay(
            entries = navigationState.toDecoratedEntries(entryProvider),
            onBack = navigator::goBack
        )
    }
}
