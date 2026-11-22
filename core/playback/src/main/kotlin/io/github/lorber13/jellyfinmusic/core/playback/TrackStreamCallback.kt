package io.github.lorber13.jellyfinmusic.core.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import io.github.lorber13.jellyfinmusic.core.data.stream.DirectPlayFormat
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamKind
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamRepository
import io.github.lorber13.jellyfinmusic.core.data.stream.TrackStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

/**
 * Turns the track ids that controllers ask for into stream addresses the player can load: the
 * original file when the phone plays its format ([playableFormats]), else the server's
 * conversion. Finding out takes a request to the server per track, which runs in [scope].
 */
internal class TrackStreamCallback(
    private val streams: StreamRepository,
    private val playableFormats: List<DirectPlayFormat>,
    private val scope: CoroutineScope
) : MediaSession.Callback {
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> = scope.future {
        // One track without a stream rejects them all, because leaving it out would shift the
        // position playback starts at.
        val trackStreams = streams.streamsOf(mediaItems.map { it.mediaId }, playableFormats)
            ?: throw UnsupportedOperationException("Only tracks of the signed-in user's server can be played")
        playableMediaItemsOf(mediaItems, trackStreams)
    }
}

/**
 * [mediaItems] with the stream of their track; [streams] holds one per item, in the same order.
 * The address always comes from the track id, never from an address a controller sent: every
 * request carries the user's credentials, which must only go to their server. The MIME type is
 * set either way, so a type a controller sent never applies either.
 */
internal fun playableMediaItemsOf(mediaItems: List<MediaItem>, streams: List<TrackStream>): List<MediaItem> =
    mediaItems.zip(streams) { item, stream ->
        val mimeType = if (stream.kind == StreamKind.HLS_CONVERSION) MimeTypes.APPLICATION_M3U8 else null
        item.buildUpon().setUri(stream.url).setMimeType(mimeType).build()
    }
