package io.github.lorber13.jellyfinmusic.core.data.stream

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRequestTest {
    private val trackId = "5d1a7f2c-3b6e-4c8a-9f0d-2e4b6a8c0d1f"
    private val accessToken = "secret-token"

    // createJellyfin needs an Android Context, so the client is built by the SDK's HTTP factory directly.
    private val client: ApiClient = OkHttpFactory().let { factory ->
        factory.create(
            baseUrl = "http://music.local:8096",
            accessToken = accessToken,
            clientInfo = ClientInfo(name = "Jellyfin Music Player", version = "0.1.0"),
            deviceInfo = DeviceInfo(id = "device-1", name = "Pixel 10"),
            httpClientOptions = HttpClientOptions(),
            socketConnectionFactory = factory
        )
    }

    @Test
    fun streamUrl_asksForTheOriginalFileWithoutTheToken() {
        val url = streamUrlOf(client, trackId).orEmpty()

        // The SDK also adds its own default parameters, which don't matter here.
        assertTrue(url, url.startsWith("http://music.local:8096/Audio/$trackId/stream?"))
        assertTrue(url, url.contains("static=true"))
        assertFalse(url, url.contains(accessToken))
    }

    @Test
    fun streamUrl_ofSomethingThatIsNotAnId_isNull() {
        assertNull(streamUrlOf(client, "not-an-id"))
    }

    @Test
    fun authorizationHeader_carriesTheTokenAndTheClient() {
        val header = authorizationHeaderOf(client)

        assertTrue(header, header.startsWith("MediaBrowser "))
        assertTrue(header, header.contains("Token=\"secret-token\""))
        assertTrue(header, header.contains("DeviceId=\"device-1\""))
    }
}
