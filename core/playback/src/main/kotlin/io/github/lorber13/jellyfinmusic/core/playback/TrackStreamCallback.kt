package io.github.lorber13.jellyfinmusic.core.playback

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamRepository

/** Turns the track ids that controllers ask for into stream addresses the player can load. */
internal class TrackStreamCallback(private val streams: StreamRepository) : MediaSession.Callback {
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        val playableItems = playableMediaItemsOf(mediaItems, streams)
            ?: return Futures.immediateFailedFuture(
                UnsupportedOperationException("Only tracks of the signed-in user's server can be played")
            )
        return Futures.immediateFuture(playableItems)
    }
}

/**
 * [mediaItems] with the stream address of their track, or null if any of them isn't a track of
 * the signed-in user's server. One bad item rejects them all, because leaving it out would shift
 * the position playback starts at. The address always comes from the track id, never from an
 * address a controller sent: every request carries the user's credentials, which must only go
 * to their server.
 */
internal fun playableMediaItemsOf(mediaItems: List<MediaItem>, streams: StreamRepository): List<MediaItem>? =
    mediaItems.map { item ->
        val streamUrl = streams.streamUrlOf(item.mediaId) ?: return null
        item.buildUpon().setUri(streamUrl).build()
    }
