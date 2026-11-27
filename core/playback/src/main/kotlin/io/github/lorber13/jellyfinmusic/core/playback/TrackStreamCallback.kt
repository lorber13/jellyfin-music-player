package io.github.lorber13.jellyfinmusic.core.playback

import android.content.res.Resources
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.util.concurrent.ListenableFuture
import io.github.lorber13.jellyfinmusic.core.data.stream.DirectPlayFormat
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamKind
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamRepository
import io.github.lorber13.jellyfinmusic.core.data.stream.TrackStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.guava.future

/**
 * Turns the track ids that controllers ask for into stream addresses the player can load: the
 * original file when the phone plays its format ([playableFormats], read when the first track is
 * looked up), else the server's conversion. Finding out takes a request to the server per track,
 * which runs in [scope].
 *
 * Tracks without a stream are left out, and the controller that asked for them gets an error
 * saying so, worded for the user with [resources]. When that controller is a system one, such as
 * Android Auto, Media3 puts the error in the platform session's playback state.
 */
internal class TrackStreamCallback(
    private val streams: StreamRepository,
    private val playableFormats: Deferred<List<DirectPlayFormat>>,
    private val resources: Resources,
    private val scope: CoroutineScope
) : MediaSession.Callback {
    // Overridden, rather than left to call onAddMediaItems, so the start moves with the tracks
    // left out before it. Opts in to Media3's unstable API for this callback and its result only:
    // the session guide names it as the way to change where playback starts.
    @OptIn(UnstableApi::class)
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
        val queue = playableQueueOf(withStreams(mediaItems), startIndex, startPositionMs)
        reportTracksLeftOut(mediaSession, controller, askedFor = mediaItems.size, playing = queue.items.size)
        // Even an empty queue replaces the old one. Failing instead would keep it, and the play
        // command that controllers send next would resume it.
        MediaSession.MediaItemsWithStartPosition(queue.items, queue.startIndex, queue.startPositionMs)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> = scope.future {
        val playableItems = withStreams(mediaItems).filterNotNull()
        reportTracksLeftOut(mediaSession, controller, askedFor = mediaItems.size, playing = playableItems.size)
        playableItems
    }

    /** [mediaItems] with their stream, or null for those that have none. */
    private suspend fun withStreams(mediaItems: List<MediaItem>): List<MediaItem?> {
        val trackStreams = streams.streamsOf(mediaItems.map { it.mediaId }, playableFormats.await())
        return playableMediaItemsOf(mediaItems, trackStreams)
    }

    // Opts in to Media3's unstable API for SessionError and sendError only: they are how a
    // session tells controllers about a problem that doesn't stop playback.
    @OptIn(UnstableApi::class)
    private fun reportTracksLeftOut(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        askedFor: Int,
        playing: Int
    ) {
        val message = when (playing) {
            askedFor -> return

            0 -> resources.getString(R.string.playback_error_nothing_playable)

            else -> (askedFor - playing).let { count ->
                resources.getQuantityString(R.plurals.playback_error_songs_skipped, count, count)
            }
        }
        // A track is left out when its request to the server fails or the server can't send it.
        mediaSession.sendError(controller, SessionError(SessionError.ERROR_IO, message))
    }
}

/**
 * [mediaItems] with the stream of their track, or null where [streams], which holds one entry
 * per item in the same order, has none. The address always comes from the track id, never from
 * an address a controller sent: every request carries the user's credentials, which must only go
 * to their server. The MIME type is set either way, so a type a controller sent never applies
 * either.
 */
internal fun playableMediaItemsOf(mediaItems: List<MediaItem>, streams: List<TrackStream?>): List<MediaItem?> =
    mediaItems.zip(streams) { item, stream ->
        stream?.let {
            val mimeType = if (stream.kind == StreamKind.HLS_CONVERSION) MimeTypes.APPLICATION_M3U8 else null
            item.buildUpon().setUri(stream.url).setMimeType(mimeType).build()
        }
    }
