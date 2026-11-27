package io.github.lorber13.jellyfinmusic.core.playback

import android.content.ComponentName
import android.content.Context
import android.icu.text.ListFormatter
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionError
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

private const val TAG = "MusicPlayer"

// Often enough for a progress line to move smoothly; a seek bar's time label changes once a second.
private val POSITION_INTERVAL = 1.seconds

/**
 * Sends playback commands to [PlaybackService] through a media controller and follows what it
 * plays. The controller connects when the app's activity starts and is released when it stops
 * (it observes that activity's lifecycle), so the service can stop once nothing plays. Public,
 * with an internal constructor, so the app's activity can observe with it.
 */
@Singleton
class MediaControllerMusicPlayer @Inject internal constructor(@param:ApplicationContext private val context: Context) :
    MusicPlayer,
    DefaultLifecycleObserver {
    // Both used only on the main thread, the thread the controller runs on. The controller is
    // null until the connection is made and once it is released.
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerStates = MutableStateFlow<PlayerState?>(null)
    override val state: StateFlow<PlayerState?> = playerStates.asStateFlow()

    // Every player event, also those that leave the state as it was, such as a seek or a track
    // starting again. Never suspends the player's callback.
    private val playerEvents =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // The controller offers no position updates, so it is asked on every tick. It is also asked
    // after every player event, so a seek or a new track doesn't show the old position until the
    // next tick.
    override val positions: Flow<PlaybackPosition> = merge(playerEvents, ticks())
        .mapNotNull { controller?.let(::playbackPositionOf) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Main.immediate)

    // Never suspends the controller's callback: a newer message replaces one not yet collected.
    private val errorMessageEvents =
        MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val errorMessages: Flow<String> = errorMessageEvents.asSharedFlow()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            playerStates.value = playerStateOf(player)
            playerEvents.tryEmit(Unit)
        }
    }

    private val controllerListener = object : MediaController.Listener {
        // Opts in to Media3's unstable API for onError only: it is how the session reports music
        // it couldn't play without stopping playback.
        @OptIn(UnstableApi::class)
        override fun onError(controller: MediaController, sessionError: SessionError) {
            errorMessageEvents.tryEmit(sessionError.message)
        }

        // Also called for a controller released in onStop, which is no longer the current one.
        // Otherwise the service went away with its queue, and the next start or play connects again.
        override fun onDisconnected(controller: MediaController) {
            if (controller !== this@MediaControllerMusicPlayer.controller) return
            this@MediaControllerMusicPlayer.controller = null
            controllerFuture = null
            playerStates.value = null
        }
    }

    // Connects as soon as the app is visible, so it shows music that was already playing.
    override fun onStart(owner: LifecycleOwner) {
        connectIfNeeded()
    }

    // Music that is playing goes on: the service has moved itself to the foreground by then, and
    // commands sent just before still arrive. The state keeps its last value, so the app shows the
    // same track until the next start connects again, instead of nothing for a moment.
    override fun onStop(owner: LifecycleOwner) {
        val future = controllerFuture ?: return
        controller = null
        controllerFuture = null
        MediaController.releaseFuture(future)
    }

    override fun play(queue: List<QueueItem>, startIndex: Int) {
        require(startIndex in queue.indices) { "Start index $startIndex is outside a queue of ${queue.size}" }
        val mediaItems = queue.map(::mediaItemOf)
        withController { controller ->
            controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            controller.prepare()
            controller.play()
        }
    }

    // Opts in to Media3's unstable API for handlePlayPauseButtonAction only: it is what the media
    // notification's button does, which also replays ended music and retries after an error.
    @OptIn(UnstableApi::class)
    override fun playOrPause() {
        withController { controller -> Util.handlePlayPauseButtonAction(controller) }
    }

    override fun skipToPrevious() {
        withController { controller -> controller.seekToPrevious() }
    }

    override fun skipToNext() {
        withController { controller -> controller.seekToNext() }
    }

    override fun seekTo(position: Duration) {
        withController { controller -> controller.seekTo(position.inWholeMilliseconds) }
    }

    private fun withController(action: (MediaController) -> Unit) {
        // The future completes on the main thread, where the controller was built.
        Futures.addCallback(
            connectIfNeeded(),
            object : FutureCallback<MediaController> {
                override fun onSuccess(controller: MediaController) = action(controller)

                // Already logged by connect. Also when the activity stopped before the connection
                // was made: the user has left the app, so nothing should start playing.
                override fun onFailure(error: Throwable) = Unit
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun connectIfNeeded(): ListenableFuture<MediaController> =
        controllerFuture ?: connect().also { controllerFuture = it }

    private fun connect(): ListenableFuture<MediaController> {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).setListener(controllerListener).buildAsync()
        // Added before any command's callback, so the state follows the player from the start.
        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(connected: MediaController) {
                    controller = connected
                    connected.addListener(playerListener)
                    playerStates.value = playerStateOf(connected)
                    playerEvents.tryEmit(Unit)
                }

                // Cancelled when the activity stopped before the connection was made.
                override fun onFailure(error: Throwable) {
                    if (error !is CancellationException) Log.w(TAG, "Couldn't connect to the playback service", error)
                }
            },
            MoreExecutors.directExecutor()
        )
        return future
    }
}

/**
 * The media item the service is asked to play. It has no address: [PlaybackService] turns the
 * track id into one, so the server's address and credentials stay in the service, and other
 * controllers, such as Android Auto later, can ask for tracks the same way.
 */
internal fun mediaItemOf(item: QueueItem): MediaItem {
    val artist = item.artistNames.takeIf { it.isNotEmpty() }?.let { ListFormatter.getInstance().format(it) }
    val metadata = MediaMetadata.Builder()
        .setTitle(item.title)
        .setArtist(artist)
        .setAlbumTitle(item.albumName)
        .setArtworkUri(item.artworkUrl?.let(Uri::parse))
        .build()
    return MediaItem.Builder()
        .setMediaId(item.trackId)
        .setMediaMetadata(metadata)
        .build()
}

/**
 * What [player] is on, or null when nothing is queued. The metadata is the one the queue was
 * sent with, not the tags in the file. Opts in to Media3's unstable API for shouldShowPlayButton
 * only, so the play/pause button shows what the media notification's shows, also when the music
 * has ended, failed or is held back by the system.
 */
@OptIn(UnstableApi::class)
internal fun playerStateOf(player: Player): PlayerState? {
    val metadata = player.currentMediaItem?.mediaMetadata ?: return null
    return PlayerState(
        title = metadata.title?.toString().orEmpty(),
        artist = metadata.artist?.toString(),
        album = metadata.albumTitle?.toString(),
        artworkUrl = metadata.artworkUri?.toString(),
        isPlaying = !Util.shouldShowPlayButton(player),
        isLoading = player.playWhenReady && player.playbackState == Player.STATE_BUFFERING,
        // Only available once the track has loaded and its stream can be moved in.
        isSeekable = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
        hasPrevious = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS),
        // Also true on the last track when the queue repeats.
        hasNext = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)
    )
}

private fun ticks(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(POSITION_INTERVAL)
    }
}

private fun playbackPositionOf(player: Player): PlaybackPosition = PlaybackPosition(
    position = player.currentPosition.milliseconds,
    duration = player.duration.takeIf { it != C.TIME_UNSET }?.milliseconds
)
