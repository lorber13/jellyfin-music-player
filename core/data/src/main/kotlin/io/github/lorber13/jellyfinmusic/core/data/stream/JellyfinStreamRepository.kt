package io.github.lorber13.jellyfinmusic.core.data.stream

import io.github.lorber13.jellyfinmusic.core.data.session.JellyfinSessionRepository
import javax.inject.Inject
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

private const val AUTHORIZATION_HEADER = "Authorization"

/**
 * Builds stream addresses for the signed-in user's server. The access token goes in a header
 * rather than in the address, because addresses end up in logs and error messages.
 */
internal class JellyfinStreamRepository @Inject constructor(private val sessionRepository: JellyfinSessionRepository) :
    StreamRepository {
    // Unlike the library, no check that someone is signed in: media controllers outside the app,
    // such as Bluetooth devices, can ask for playback at any time.
    override fun streamUrlOf(trackId: String): String? {
        val client = sessionRepository.signedInClient ?: return null
        return streamUrlOf(client, trackId)
    }

    override fun requestHeaders(): Map<String, String> {
        val client = sessionRepository.signedInClient ?: return emptyMap()
        return mapOf(AUTHORIZATION_HEADER to authorizationHeaderOf(client))
    }
}

/** The original file, unconverted, so seeking works and quality isn't lost. Server conversion is PLY-13. */
internal fun streamUrlOf(client: ApiClient, trackId: String): String? {
    val itemId = trackId.toUUIDOrNull() ?: return null
    return client.audioApi.getAudioStreamUrl(itemId = itemId, static = true)
}

/** The same header the SDK sends with its own requests, so the server sees one device and session. */
internal fun authorizationHeaderOf(client: ApiClient): String = AuthorizationHeaderBuilder.buildHeader(
    clientName = client.clientInfo.name,
    clientVersion = client.clientInfo.version,
    deviceId = client.deviceInfo.id,
    deviceName = client.deviceInfo.name,
    accessToken = client.accessToken
)
