package io.github.lorber13.jellyfinmusic.core.data.stream

import io.github.lorber13.jellyfinmusic.core.data.di.IoDispatcher
import io.github.lorber13.jellyfinmusic.core.data.session.JellyfinSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.post
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

private const val AUTHORIZATION_HEADER = "Authorization"

/**
 * Finds out how to stream the signed-in user's tracks. The server decides per track whether the
 * phone gets the original file or a conversion: each request describes the formats the phone
 * plays. The access token goes in a header rather than in the address, because addresses end up
 * in logs and error messages.
 */
internal class JellyfinStreamRepository @Inject constructor(
    private val sessionRepository: JellyfinSessionRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : StreamRepository {
    // Unlike the library, no check that someone is signed in: media controllers outside the app,
    // such as Bluetooth devices, can ask for playback at any time.
    override suspend fun streamsOf(
        trackIds: List<String>,
        playableFormats: List<DirectPlayFormat>
    ): List<TrackStream?> {
        val client = sessionRepository.signedInClient ?: return trackIds.map { null }
        val request = playbackInfoRequestOf(playableFormats)
        // The SDK reads responses on the calling thread. All requests run at the same time, and
        // one that fails leaves the others running, so one bad track doesn't stop a whole album.
        return withContext(ioDispatcher) {
            trackIds.map { trackId -> async { streamOf(client, trackId, request) } }.awaitAll()
        }
    }

    override fun requestHeaders(): Map<String, String> {
        val client = sessionRepository.signedInClient ?: return emptyMap()
        return mapOf(AUTHORIZATION_HEADER to authorizationHeaderOf(client))
    }

    private suspend fun streamOf(client: ApiClient, trackId: String, request: PlaybackInfoDto): TrackStream? {
        val itemId = trackId.toUUIDOrNull() ?: return null
        val response = try {
            // The same request as the SDK's getPostedPlaybackInfo, read with the app's own model.
            client.post<PlaybackInfo>(
                pathTemplate = "/Items/{itemId}/PlaybackInfo",
                pathParameters = mapOf("itemId" to itemId),
                requestBody = request
            ).content
        } catch (_: ApiClientException) {
            return null
        }
        return trackStreamOf(client, itemId, response)
    }
}

/** The same header the SDK sends with its own requests, so the server sees one device and session. */
internal fun authorizationHeaderOf(client: ApiClient): String = AuthorizationHeaderBuilder.buildHeader(
    clientName = client.clientInfo.name,
    clientVersion = client.clientInfo.version,
    deviceId = client.deviceInfo.id,
    deviceName = client.deviceInfo.name,
    accessToken = client.accessToken
)
