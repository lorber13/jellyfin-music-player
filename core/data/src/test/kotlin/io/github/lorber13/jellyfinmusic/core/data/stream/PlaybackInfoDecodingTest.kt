package io.github.lorber13.jellyfinmusic.core.data.stream

import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.PlaybackErrorCode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackInfoDecodingTest {
    @Test
    fun answerFromJellyfin10_9_canBeRead() {
        // A Jellyfin 10.9.11 answer for an ALAC track, shortened. Its media stream has no
        // IsOriginal field, which the SDK's own model requires.
        val body = """
            {"MediaSources":[{"Protocol":"File","Id":"dc6b0479126e5868728b0c67f5b33fd7",
            "Container":"mov,mp4,m4a,3gp,3g2,mj2","SupportsDirectPlay":false,"SupportsDirectStream":false,
            "SupportsTranscoding":true,"MediaStreams":[{"Codec":"alac","Type":"Audio","Index":0,"IsExternal":false}],
            "TranscodingUrl":"/audio/dc6b0479-126e-5868-728b-0c67f5b33fd7/master.m3u8?MediaSourceId=dc6b0479126e5868728b0c67f5b33fd7&AudioBitrate=256000&api_key=secret-token",
            "TranscodingSubProtocol":"hls","TranscodingContainer":"mp4"}],"PlaySessionId":"session-1"}
        """.trimIndent()

        // The same decoder the SDK uses for the request.
        val playbackInfo = ApiSerializer.decodeResponseBody<PlaybackInfo>(body)

        val source = playbackInfo.mediaSources.single()
        assertEquals(false, source.supportsDirectPlay)
        assertEquals(true, source.supportsTranscoding)
        assertEquals(MediaStreamProtocol.HLS, source.transcodingSubProtocol)
        assertEquals(
            "/audio/dc6b0479-126e-5868-728b-0c67f5b33fd7/master.m3u8?MediaSourceId=dc6b0479126e5868728b0c67f5b33fd7&AudioBitrate=256000&api_key=secret-token",
            source.transcodingUrl
        )
    }

    @Test
    fun answerWithAnError_canBeRead() {
        val playbackInfo = ApiSerializer.decodeResponseBody<PlaybackInfo>(
            """{"MediaSources":[],"ErrorCode":"NotAllowed"}"""
        )

        assertEquals(PlaybackErrorCode.NOT_ALLOWED, playbackInfo.errorCode)
    }
}
