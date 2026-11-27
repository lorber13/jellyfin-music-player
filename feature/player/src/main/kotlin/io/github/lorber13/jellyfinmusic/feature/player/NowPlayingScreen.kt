package io.github.lorber13.jellyfinmusic.feature.player

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lorber13.jellyfinmusic.core.designsystem.component.AlbumCover
import io.github.lorber13.jellyfinmusic.core.designsystem.format.formatTrackDuration
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackPosition
import io.github.lorber13.jellyfinmusic.core.playback.PlayerState
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L

// Keeps the cover and the seek bar from stretching across a tablet.
private val MaxContentWidth = 560.dp

/**
 * The full-screen player: the cover, the track, a seek bar and the playback buttons. It closes
 * itself with [onClose] when nothing is queued any more, the way the mini player hides.
 */
@Composable
internal fun NowPlayingScreen(onClose: () -> Unit) {
    val viewModel: NowPlayingViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val shownState = state
    if (shownState == null) {
        // The playback service went away with its queue, or the app came back after the system
        // ended it while paused. PLY-11 restores the queue once the player connects, so this
        // will have to wait for that instead of closing at once.
        LaunchedEffect(Unit) { onClose() }
    } else {
        NowPlayingContent(
            state = shownState,
            // Read by the seek bar only, so the rest of the screen doesn't recompose every second.
            position = { position },
            onClose = onClose,
            onPlayOrPause = viewModel::playOrPause,
            onSkipToPrevious = viewModel::skipToPrevious,
            onSkipToNext = viewModel::skipToNext,
            onSeek = viewModel::seekTo
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingContent(
    state: PlayerState,
    position: () -> PlaybackPosition?,
    onClose: () -> Unit,
    onPlayOrPause: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenName = stringResource(R.string.feature_player_now_playing)
    Scaffold(
        // Screen readers announce the screen by this name when it opens.
        modifier = modifier.semantics { paneTitle = screenName },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { CloseButton(onClick = onClose) },
                // Also clear of the camera cutout, which is at the side in landscape.
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            val details = @Composable { detailsModifier: Modifier ->
                TrackDetails(
                    state = state,
                    position = position,
                    onPlayOrPause = onPlayOrPause,
                    onSkipToPrevious = onSkipToPrevious,
                    onSkipToNext = onSkipToNext,
                    onSeek = onSeek,
                    // Scrolls when it doesn't fit, such as in landscape with bigger fonts.
                    modifier = detailsModifier
                        .widthIn(max = MaxContentWidth)
                        .verticalScroll(rememberScrollState())
                )
            }
            if (maxWidth > maxHeight) {
                // Wider than tall, as in landscape: the cover beside the track, each in half of the space.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        // The largest square that fits.
                        AlbumCover(imageUrl = state.artworkUrl, modifier = Modifier.widthIn(max = MaxContentWidth))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        details(Modifier.fillMaxWidth())
                    }
                }
            } else {
                // Taller than wide: the cover above the track. The track is laid out first, so the cover
                // gets the room that is left and shrinks with bigger fonts.
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumCover(imageUrl = state.artworkUrl, modifier = Modifier.widthIn(max = MaxContentWidth))
                    }
                    details(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.feature_player_ic_keyboard_arrow_down),
            contentDescription = stringResource(R.string.feature_player_close_now_playing)
        )
    }
}

@Composable
private fun TrackDetails(
    state: PlayerState,
    position: () -> PlaybackPosition?,
    onPlayOrPause: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TrackText(state = state)
        SeekBar(
            position = position,
            isSeekable = state.isSeekable,
            onSeek = onSeek,
            modifier = Modifier.padding(top = 16.dp)
        )
        PlaybackButtons(
            state = state,
            onPlayOrPause = onPlayOrPause,
            onSkipToPrevious = onSkipToPrevious,
            onSkipToNext = onSkipToNext,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun TrackText(state: PlayerState) {
    // Read as one item by screen readers.
    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        state.artist?.let { artist ->
            Text(
                text = artist,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        state.album?.let { album ->
            Text(
                text = album,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The bar that shows how far the track has played, with the elapsed time and the track's length
 * under it. Dragging it shows the new position, and the track moves there once it is let go.
 * Like other media controls, it doesn't flip in right-to-left languages.
 */
@Composable
private fun SeekBar(
    position: () -> PlaybackPosition?,
    isSeekable: Boolean,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val playbackPosition = position()
    val duration = playbackPosition?.duration?.takeIf { it.isPositive() }
    // Where the user is dragging to, from 0 to 1, until they let go.
    var draggedFraction by remember { mutableStateOf<Float?>(null) }
    val dragged = draggedFraction
    val shownPosition = when {
        dragged != null && duration != null -> duration * dragged.toDouble()
        else -> playbackPosition?.position ?: Duration.ZERO
    }
    val sliderLabel = stringResource(R.string.feature_player_song_position)
    val spokenPosition = if (duration != null) {
        stringResource(
            R.string.feature_player_position_of_duration,
            formatSpokenDuration(shownPosition, locale),
            formatSpokenDuration(duration, locale)
        )
    } else {
        formatSpokenDuration(shownPosition, locale)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier) {
            Slider(
                value = dragged ?: playbackPosition?.let(::progressOf) ?: 0f,
                onValueChange = { fraction -> draggedFraction = fraction },
                // Also called after a screen reader sets the position.
                onValueChangeFinished = {
                    val target = draggedFraction
                    if (target != null && duration != null) onSeek(duration * target.toDouble())
                    draggedFraction = null
                },
                enabled = isSeekable && duration != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = sliderLabel
                        stateDescription = spokenPosition
                    }
            )
            // Screen readers get the same times, spelled out, from the bar.
            Row(modifier = Modifier.fillMaxWidth().semantics { hideFromAccessibility() }) {
                TimeLabel(text = formatTrackDuration(shownPosition, locale), modifier = Modifier.weight(1f))
                if (duration != null) TimeLabel(text = formatTrackDuration(duration, locale))
            }
        }
    }
}

@Composable
private fun TimeLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PlaybackButtons(
    state: PlayerState,
    onPlayOrPause: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Media controls keep their left-to-right order in right-to-left languages.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSkipToPrevious, enabled = state.hasPrevious, modifier = Modifier.size(56.dp)) {
                Icon(
                    painter = painterResource(R.drawable.feature_player_ic_skip_previous),
                    contentDescription = stringResource(R.string.feature_player_previous_song),
                    modifier = Modifier.size(32.dp)
                )
            }
            PlayPauseButton(isPlaying = state.isPlaying, isLoading = state.isLoading, onClick = onPlayOrPause)
            IconButton(onClick = onSkipToNext, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    painter = painterResource(R.drawable.feature_player_ic_skip_next),
                    contentDescription = stringResource(R.string.feature_player_next_song),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/** The big play/pause button, circled by a moving ring while the track loads. */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        if (isLoading) {
            // Screen readers skip it: the button is what they need, and it works while loading.
            CircularProgressIndicator(modifier = Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        FilledIconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
            if (isPlaying) {
                Icon(
                    painter = painterResource(R.drawable.feature_player_ic_pause),
                    contentDescription = stringResource(R.string.feature_player_pause),
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.feature_player_ic_play),
                    contentDescription = stringResource(R.string.feature_player_play),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

/**
 * [duration] in words in the user's language, such as "1 minute, 23 seconds", for screen readers.
 * Units that are zero are left out, except for "0 seconds".
 */
internal fun formatSpokenDuration(duration: Duration, locale: Locale): String {
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val measures = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        if (minutes > 0) add(Measure(minutes, MeasureUnit.MINUTE))
        if (seconds > 0 || isEmpty()) add(Measure(seconds, MeasureUnit.SECOND))
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE).formatMeasures(*measures.toTypedArray())
}

private val previewState = PlayerState(
    title = "Whistle Stop",
    artist = "The Copper Kettles, Aurora Fields",
    album = "Steam & Rust",
    artworkUrl = null,
    isPlaying = true,
    isLoading = false,
    isSeekable = true,
    hasPrevious = true,
    hasNext = true
)

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun NowPlayingContentPortraitPreview() {
    JellyfinMusicTheme {
        NowPlayingContent(
            state = previewState,
            position = { PlaybackPosition(position = 83.seconds, duration = 4.minutes + 56.seconds) },
            onClose = {},
            onPlayOrPause = {},
            onSkipToPrevious = {},
            onSkipToNext = {},
            onSeek = {}
        )
    }
}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun NowPlayingContentLandscapePreview() {
    JellyfinMusicTheme {
        NowPlayingContent(
            state = previewState.copy(isPlaying = false, isLoading = true),
            position = { PlaybackPosition(position = Duration.ZERO, duration = null) },
            onClose = {},
            onPlayOrPause = {},
            onSkipToPrevious = {},
            onSkipToNext = {},
            onSeek = {}
        )
    }
}
