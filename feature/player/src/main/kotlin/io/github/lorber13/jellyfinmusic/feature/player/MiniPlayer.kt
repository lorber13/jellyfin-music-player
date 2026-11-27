package io.github.lorber13.jellyfinmusic.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lorber13.jellyfinmusic.core.designsystem.component.AlbumCover
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState

/**
 * The bar under every screen that shows what is playing, with play/pause and next buttons. It
 * slides in when music is queued and out when the queue is emptied. Place it at the bottom of the
 * window or above the navigation bar: it keeps its content clear of the system bars there.
 */
@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val viewModel: MiniPlayerViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        // Only showing and hiding the bar animate; a new track or a pause just updates it. While
        // the bar slides out it keeps showing the last track.
        contentKey = { shownState -> shownState != null },
        transitionSpec = { slideInVertically { height -> height } togetherWith slideOutVertically { height -> height } }
    ) { shownState ->
        if (shownState != null) {
            val progress by viewModel.progress.collectAsStateWithLifecycle()
            MiniPlayerContent(
                state = shownState,
                // Read while drawing, so the line moves without recomposing the bar every second.
                progress = { progress },
                onPlayOrPause = viewModel::playOrPause,
                onSkipToNext = viewModel::skipToNext
            )
        }
    }
}

@Composable
internal fun MiniPlayerContent(
    state: PlayerState,
    progress: () -> Float?,
    onPlayOrPause: () -> Unit,
    onSkipToNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The background reaches the window's edges; the content stays clear of the system bars.
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
        ) {
            TrackProgressLine(progress = progress, isLoading = state.isLoading)
            Row(
                // No fixed height, so it grows with bigger fonts.
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumCover(imageUrl = state.artworkUrl, modifier = Modifier.size(48.dp))
                TrackText(state = state, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                // Media controls keep their left-to-right order in right-to-left languages.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    PlaybackButtons(state = state, onPlayOrPause = onPlayOrPause, onSkipToNext = onSkipToNext)
                }
            }
        }
    }
}

/**
 * A thin line across the top of the bar that fills as the track plays, and moves on its own while
 * the track loads. It doesn't flip in right-to-left languages, like other media controls. Screen
 * readers skip it: the time of a song isn't worth hearing on every pass over the bar.
 */
@Composable
private fun TrackProgressLine(progress: () -> Float?, isLoading: Boolean) {
    val lineModifier = Modifier
        .fillMaxWidth()
        .clearAndSetSemantics {}
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (isLoading) {
            LinearProgressIndicator(modifier = lineModifier, strokeCap = StrokeCap.Butt, gapSize = 0.dp)
        } else {
            LinearProgressIndicator(
                progress = { progress() ?: 0f },
                modifier = lineModifier,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun TrackText(state: PlayerState, modifier: Modifier = Modifier) {
    // Read as one item by screen readers.
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        state.artist?.let { artist ->
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackButtons(state: PlayerState, onPlayOrPause: () -> Unit, onSkipToNext: () -> Unit) {
    Row {
        IconButton(onClick = onPlayOrPause) {
            if (state.isPlaying) {
                Icon(
                    painter = painterResource(R.drawable.feature_player_ic_pause),
                    contentDescription = stringResource(R.string.feature_player_pause)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.feature_player_ic_play),
                    contentDescription = stringResource(R.string.feature_player_play)
                )
            }
        }
        IconButton(onClick = onSkipToNext, enabled = state.hasNext) {
            Icon(
                painter = painterResource(R.drawable.feature_player_ic_skip_next),
                contentDescription = stringResource(R.string.feature_player_next_song)
            )
        }
    }
}

@Preview
@Composable
private fun MiniPlayerContentPreview() {
    val state = PlayerState(
        title = "Whistle Stop",
        artist = "The Copper Kettles, Aurora Fields",
        artworkUrl = null,
        isPlaying = true,
        isLoading = false,
        hasNext = true
    )
    JellyfinMusicTheme {
        MiniPlayerContent(state = state, progress = { 0.4f }, onPlayOrPause = {}, onSkipToNext = {})
    }
}
