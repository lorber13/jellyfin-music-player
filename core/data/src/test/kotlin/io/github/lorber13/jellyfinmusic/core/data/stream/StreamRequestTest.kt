package io.github.lorber13.jellyfinmusic.core.data.stream

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRequestTest {
    private val client = testApiClient()

    @Test
    fun originalFileUrl_asksForTheUnconvertedFileWithoutTheToken() {
        val trackId = "5d1a7f2c-3b6e-4c8a-9f0d-2e4b6a8c0d1f"

        val url = originalFileUrlOf(client, UUID.fromString(trackId))

        // The SDK also adds its own default parameters, which don't matter here.
        assertTrue(url, url.startsWith("http://music.local:8096/Audio/$trackId/stream?"))
        assertTrue(url, url.contains("static=true"))
        assertFalse(url, url.contains(TEST_ACCESS_TOKEN))
    }

    @Test
    fun authorizationHeader_carriesTheTokenAndTheClient() {
        val header = authorizationHeaderOf(client)

        assertTrue(header, header.startsWith("MediaBrowser "))
        assertTrue(header, header.contains("Token=\"secret-token\""))
        assertTrue(header, header.contains("DeviceId=\"device-1\""))
    }
}
