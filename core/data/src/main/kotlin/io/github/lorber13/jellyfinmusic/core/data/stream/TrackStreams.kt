package io.github.lorber13.jellyfinmusic.core.data.stream

import java.util.UUID
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.model.api.MediaStreamProtocol

/** The query parameters the server puts the access token in, in older and newer versions. */
private val accessTokenParameters = setOf("apikey", "api_key")

/**
 * How to stream the track [itemId] after the server's [response] to a PlaybackInfo request: its
 * original file if the phone plays it, else the server's conversion. Null when the server can do
 * neither, for example because the user may not have music converted.
 */
internal fun trackStreamOf(client: ApiClient, itemId: UUID, response: PlaybackInfo): TrackStream? {
    if (response.errorCode != null) return null
    // Music files have a single source.
    val source = response.mediaSources.firstOrNull() ?: return null
    if (source.supportsDirectPlay) return TrackStream(originalFileUrlOf(client, itemId), StreamKind.ORIGINAL_FILE)

    val conversionPath = source.transcodingUrl
    val baseUrl = client.baseUrl
    val isHlsConversion = source.supportsTranscoding && source.transcodingSubProtocol == MediaStreamProtocol.HLS
    if (!isHlsConversion || conversionPath == null || baseUrl == null) return null
    return TrackStream(conversionUrlOf(baseUrl, conversionPath), StreamKind.HLS_CONVERSION)
}

/** The original file, unconverted, so seeking works and quality isn't lost. */
internal fun originalFileUrlOf(client: ApiClient, itemId: UUID): String =
    client.audioApi.getAudioStreamUrl(itemId = itemId, static = true)

/**
 * The full address of the conversion at [conversionPath], which the server gives from its root;
 * [baseUrl] includes a path prefix if the server has one. Two of the server's parameters change:
 * - The access token goes, because addresses end up in logs and error messages. The player sends
 *   it in a header instead.
 * - The codec is added if it is missing. Jellyfin 10.9 leaves it out and then copies the
 *   original audio, which is exactly what the phone can't play.
 */
internal fun conversionUrlOf(baseUrl: String, conversionPath: String): String {
    val address = baseUrl.trimEnd('/') + conversionPath.substringBefore('?')
    val serverParameters = conversionPath.substringAfter('?', missingDelimiterValue = "").split('&')
        .filter { parameter -> parameter.isNotEmpty() && nameOf(parameter) !in accessTokenParameters }
    val hasCodec = serverParameters.any { parameter -> nameOf(parameter) == "audiocodec" }
    val parameters = if (hasCodec) serverParameters else serverParameters + "AudioCodec=$CONVERSION_CODEC"
    return address + "?" + parameters.joinToString("&")
}

/** A query parameter's name, in lowercase: the server reads names regardless of case. */
private fun nameOf(parameter: String): String = parameter.substringBefore('=').lowercase()
