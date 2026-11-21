package io.github.lorber13.jellyfinmusic.core.playback

import android.content.ComponentName
import android.content.Context
import android.icu.text.ListFormatter
import android.net.Uri
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicPlayer"

/**
 * Sends playback commands to [PlaybackService] through a media controller. The controller
 * connects on the first [play] and is released when the app's activity stops (it observes that
 * activity's lifecycle), so the service can stop once nothing plays. Public, with an internal
 * constructor, so the app's activity can observe with it.
 */
@Singleton
class MediaControllerMusicPlayer @Inject internal constructor(@param:ApplicationContext private val context: Context) :
    MusicPlayer,
    DefaultLifecycleObserver {
    // Used only on the main thread, the thread the controller runs on.
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun play(queue: List<QueueItem>, startIndex: Int) {
        require(startIndex in queue.indices) { "Start index $startIndex is outside a queue of ${queue.size}" }
        val mediaItems = queue.map(::mediaItemOf)
        withController { controller ->
            controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            controller.prepare()
            controller.play()
        }
    }

    // Music that is playing goes on: the service has moved itself to the foreground by then, and
    // commands sent just before still arrive.
    override fun onStop(owner: LifecycleOwner) {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
    }

    private fun withController(action: (MediaController) -> Unit) {
        val future = controllerFuture ?: connect().also { controllerFuture = it }
        // The future completes on the main thread, where the controller was built.
        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(controller: MediaController) = action(controller)

                override fun onFailure(error: Throwable) {
                    // Cancelled when the activity stopped before the connection was made; the
                    // user has left the app, so nothing should start playing.
                    if (error !is CancellationException) Log.w(TAG, "Couldn't connect to the playback service", error)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun connect(): ListenableFuture<MediaController> {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        return MediaController.Builder(context, sessionToken).buildAsync()
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
