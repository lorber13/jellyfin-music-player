package io.github.lorber13.jellyfinmusic.feature.albums

import android.icu.text.ListFormatter
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumDetail
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.data.library.Track
import io.github.lorber13.jellyfinmusic.core.designsystem.component.AlbumCover
import io.github.lorber13.jellyfinmusic.core.designsystem.component.NavigateBackButton
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val HEADER_ITEM_KEY = "header"
private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L

@Composable
internal fun AlbumDetailScreen(albumId: String, onNavigateBack: () -> Unit) {
    val viewModel = hiltViewModel<AlbumDetailViewModel, AlbumDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(albumId) }
    )
    AlbumDetailContent(
        uiState = viewModel.uiState,
        onPlay = viewModel::play,
        onRetry = viewModel::retry,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumDetailContent(
    uiState: AlbumDetailUiState,
    onPlay: (startIndex: Int) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    // The header shows the album's name; the title bar takes it over once the header has scrolled away.
    val isHeaderScrolledAway by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is AlbumDetailUiState.Loaded) {
                        AnimatedVisibility(visible = isHeaderScrolledAway, enter = fadeIn(), exit = fadeOut()) {
                            Text(text = uiState.album.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = { NavigateBackButton(onClick = onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(contentPadding)
        when (uiState) {
            AlbumDetailUiState.Loading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is AlbumDetailUiState.Failed -> AlbumLoadFailure(
                reason = uiState.reason,
                onRetry = onRetry,
                modifier = contentModifier
            )

            is AlbumDetailUiState.Loaded -> AlbumTracks(
                album = uiState.album,
                listState = listState,
                onPlay = onPlay,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun AlbumTracks(
    album: AlbumDetail,
    listState: LazyListState,
    onPlay: (startIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    // Tracks without a disc number are grouped together; disc headings only help when there are several discs.
    // Each track keeps its index in the album's track list, which is its place in the queue.
    val tracksByDisc = album.tracks.withIndex().groupBy { (_, track) -> track.discNumber }
    val showsDiscHeadings = tracksByDisc.size > 1

    LazyColumn(modifier = modifier, state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = HEADER_ITEM_KEY) {
            AlbumHeader(album = album, locale = locale, onPlay = { onPlay(0) })
        }
        tracksByDisc.forEach { (discNumber, indexedTracks) ->
            if (showsDiscHeadings && discNumber != null) {
                item(key = "disc-$discNumber") {
                    DiscHeading(discNumber)
                }
            }
            items(indexedTracks, key = { (_, track) -> track.id }) { (index, track) ->
                TrackRow(
                    track = track,
                    albumArtistName = album.artistName,
                    locale = locale,
                    onClick = { onPlay(index) }
                )
            }
        }
    }
}

@Composable
private fun AlbumHeader(album: AlbumDetail, locale: Locale, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlbumCover(imageUrl = album.coverImageUrl, modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth())
        Text(
            text = album.name,
            modifier = Modifier
                .padding(top = 16.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        album.artistName?.let { artistName ->
            Text(
                text = artistName,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = summaryOf(album, locale),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PlayButton(onClick = onPlay, enabled = album.tracks.isNotEmpty(), modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        // The button's text already describes it to screen readers.
        Icon(
            painter = painterResource(R.drawable.feature_albums_ic_play),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.feature_albums_detail_play))
    }
}

@Composable
private fun summaryOf(album: AlbumDetail, locale: Locale): String {
    val songCount = album.tracks.size
    val songs = pluralStringResource(R.plurals.feature_albums_detail_song_count, songCount, songCount)
    val duration = formatAlbumDuration(album.totalDuration, locale)
    val year = album.year
    return if (year != null) {
        stringResource(R.string.feature_albums_detail_summary, year, songs, duration)
    } else {
        stringResource(R.string.feature_albums_detail_summary_without_year, songs, duration)
    }
}

@Composable
private fun DiscHeading(discNumber: Int) {
    Text(
        text = stringResource(R.string.feature_albums_detail_disc, discNumber),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TrackRow(track: Track, albumArtistName: String?, locale: Locale, onClick: () -> Unit) {
    // Compilations and guest appearances name the track's own artists; otherwise they repeat the header.
    val showsArtists = track.artistNames.isNotEmpty() && track.artistNames != listOfNotNull(albumArtistName)
    ListItem(
        modifier = Modifier.clickable(
            onClickLabel = stringResource(R.string.feature_albums_detail_play_track),
            onClick = onClick
        ),
        headlineContent = { Text(text = track.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (showsArtists) {
            {
                Text(
                    text = ListFormatter.getInstance(locale).format(track.artistNames),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            null
        },
        leadingContent = {
            // A minimum width keeps the titles lined up whether the number has one digit or two.
            Text(
                text = track.trackNumber?.toString().orEmpty(),
                modifier = Modifier.widthIn(min = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        trailingContent = track.duration?.let { duration ->
            { Text(text = formatTrackDuration(duration, locale)) }
        }
    )
}

@Composable
private fun AlbumLoadFailure(reason: LibraryFailure, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (reason) {
        LibraryFailure.UNREACHABLE -> R.string.feature_albums_error_unreachable
        LibraryFailure.SERVER_ERROR -> R.string.feature_albums_detail_error_server
    }
    CenteredMessage(modifier = modifier) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.feature_albums_retry))
        }
    }
}

/**
 * A track's length as a clock reading in the user's language, such as "3:45" or "1:02:03".
 * Unlike `DateUtils.formatElapsedTime`, minutes have no leading zero, as in other music apps.
 */
internal fun formatTrackDuration(duration: Duration, locale: Locale): String {
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val measures = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        add(Measure(minutes, MeasureUnit.MINUTE))
        add(Measure(seconds, MeasureUnit.SECOND))
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NUMERIC).formatMeasures(*measures.toTypedArray())
}

/**
 * The album's length in the user's language, such as "48 min" or "1 hr, 5 min", rounded to the
 * nearest minute. A short album still reads "1 min" rather than "0 min".
 */
internal fun formatAlbumDuration(duration: Duration, locale: Locale): String {
    val roundedMinutes = (duration + 30.seconds).inWholeMinutes
    val totalMinutes = if (duration > Duration.ZERO) roundedMinutes.coerceAtLeast(1) else 0
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    val measures = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        if (minutes > 0 || hours == 0L) add(Measure(minutes, MeasureUnit.MINUTE))
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT).formatMeasures(*measures.toTypedArray())
}

@Preview
@Composable
private fun AlbumDetailContentPreview() {
    val album = AlbumDetail(
        id = "1",
        name = "Steam & Rust",
        artistName = "The Copper Kettles",
        year = 2021,
        coverImageUrl = null,
        tracks = listOf(
            Track("t1", "Boiler Room", listOf("The Copper Kettles"), 1, 1, 225.seconds),
            Track("t2", "Whistle Stop", listOf("The Copper Kettles", "Aurora Fields"), 2, 1, 242.seconds),
            Track("t3", "Cold Iron", listOf("The Copper Kettles"), 1, 2, 178.seconds)
        )
    )
    JellyfinMusicTheme {
        AlbumDetailContent(uiState = AlbumDetailUiState.Loaded(album), onPlay = {}, onRetry = {}, onNavigateBack = {})
    }
}
