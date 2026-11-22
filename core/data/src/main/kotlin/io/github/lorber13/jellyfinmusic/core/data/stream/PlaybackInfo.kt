package io.github.lorber13.jellyfinmusic.core.data.stream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.PlaybackErrorCode

/**
 * The parts of the server's answer to a PlaybackInfo request that the player needs. Read with a
 * model of its own because the SDK's model requires fields that Jellyfin 10.9 doesn't send, such
 * as `MediaStream.IsOriginal`, so it can't read 10.9's answers.
 */
@Serializable
internal data class PlaybackInfo(
    @SerialName("MediaSources") val mediaSources: List<PlaybackSource> = emptyList(),
    @SerialName("ErrorCode") val errorCode: PlaybackErrorCode? = null
)

/** One way to get a track: its file, and whether the phone can play it or the server would convert it. */
@Serializable
internal data class PlaybackSource(
    @SerialName("SupportsDirectPlay") val supportsDirectPlay: Boolean = false,
    @SerialName("SupportsTranscoding") val supportsTranscoding: Boolean = false,
    @SerialName("TranscodingUrl") val transcodingUrl: String? = null,
    @SerialName("TranscodingSubProtocol") val transcodingSubProtocol: MediaStreamProtocol? = null
)
