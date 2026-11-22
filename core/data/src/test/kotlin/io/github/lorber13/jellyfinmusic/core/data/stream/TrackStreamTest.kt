package io.github.lorber13.jellyfinmusic.core.data.stream

import java.util.UUID
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.PlaybackErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackStreamTest {
    private val trackId = UUID.fromString("5d1a7f2c-3b6e-4c8a-9f0d-2e4b6a8c0d1f")

    // The shape of the address Jellyfin returns, with the token in it.
    private val conversionPath = "/audio/$trackId/master.m3u8?MediaSourceId=5d1a7f2c&AudioCodec=aac" +
        "&AudioBitrate=256000&SegmentContainer=mp4&PlaySessionId=session-1&ApiKey=$TEST_ACCESS_TOKEN&RequireAvc=false"

    @Test
    fun directPlay_streamsTheOriginalFile() {
        val stream = trackStreamOf(testApiClient(), trackId, responseWith(source(supportsDirectPlay = true)))

        assertEquals(StreamKind.ORIGINAL_FILE, stream?.kind)
        assertTrue(stream?.url, stream?.url.orEmpty().startsWith("http://music.local:8096/Audio/$trackId/stream?"))
    }

    @Test
    fun conversion_streamsTheServersHlsPlaylistWithoutTheToken() {
        val stream = trackStreamOf(testApiClient(), trackId, responseWith(conversionSource()))

        assertEquals(
            TrackStream(
                url = "http://music.local:8096/audio/$trackId/master.m3u8?MediaSourceId=5d1a7f2c&AudioCodec=aac" +
                    "&AudioBitrate=256000&SegmentContainer=mp4&PlaySessionId=session-1&RequireAvc=false",
                kind = StreamKind.HLS_CONVERSION
            ),
            stream
        )
    }

    @Test
    fun conversion_onAServerWithAPathPrefix_keepsThePrefix() {
        val client = testApiClient(baseUrl = "https://music.example/jellyfin/")

        val stream = trackStreamOf(client, trackId, responseWith(conversionSource()))

        assertTrue(
            stream?.url,
            stream?.url.orEmpty().startsWith("https://music.example/jellyfin/audio/$trackId/master.m3u8?")
        )
    }

    @Test
    fun trackThatCantBeConverted_hasNoStream() {
        // What the server answers when the user may not have music converted.
        val response = responseWith(source(supportsDirectPlay = false, supportsTranscoding = false))

        assertNull(trackStreamOf(testApiClient(), trackId, response))
    }

    @Test
    fun progressiveConversion_hasNoStream() {
        val source = conversionSource().copy(transcodingSubProtocol = MediaStreamProtocol.HTTP)

        assertNull(trackStreamOf(testApiClient(), trackId, responseWith(source)))
    }

    @Test
    fun serverError_hasNoStream() {
        val response = responseWith(source(supportsDirectPlay = true)).copy(errorCode = PlaybackErrorCode.NOT_ALLOWED)

        assertNull(trackStreamOf(testApiClient(), trackId, response))
    }

    @Test
    fun trackWithoutSources_hasNoStream() {
        assertNull(trackStreamOf(testApiClient(), trackId, PlaybackInfo(mediaSources = emptyList())))
    }

    @Test
    fun conversionUrl_removesTheTokenInEitherSpelling() {
        assertEquals(
            "http://h/a.m3u8?b=1&AudioCodec=aac&c=2",
            conversionUrlOf("http://h", "/a.m3u8?api_key=x&b=1&AudioCodec=aac&ApiKey=y&c=2")
        )
    }

    @Test
    fun conversionUrl_withoutACodec_asksForAac() {
        // Jellyfin 10.9 leaves the codec out, and then copies the original audio.
        val path = "/audio/$trackId/master.m3u8?MediaSourceId=5d1a7f2c&AudioBitrate=256000&api_key=$TEST_ACCESS_TOKEN"

        assertEquals(
            "http://music.local:8096/audio/$trackId/master.m3u8?MediaSourceId=5d1a7f2c&AudioBitrate=256000&AudioCodec=aac",
            conversionUrlOf("http://music.local:8096", path)
        )
    }

    @Test
    fun conversionUrl_keepsTheServersCodec() {
        assertEquals("http://h/a.m3u8?audioCodec=mp3", conversionUrlOf("http://h/", "/a.m3u8?audioCodec=mp3"))
    }

    private fun responseWith(source: PlaybackSource) = PlaybackInfo(mediaSources = listOf(source))

    private fun conversionSource() = source(supportsDirectPlay = false, supportsTranscoding = true)
        .copy(transcodingUrl = conversionPath, transcodingSubProtocol = MediaStreamProtocol.HLS)

    private fun source(supportsDirectPlay: Boolean, supportsTranscoding: Boolean = true) = PlaybackSource(
        supportsDirectPlay = supportsDirectPlay,
        supportsTranscoding = supportsTranscoding,
        transcodingSubProtocol = MediaStreamProtocol.HTTP
    )
}
