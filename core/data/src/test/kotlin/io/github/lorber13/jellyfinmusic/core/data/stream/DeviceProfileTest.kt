package io.github.lorber13.jellyfinmusic.core.data.stream

import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProfileTest {
    @Test
    fun directPlayProfiles_useTheServersNamesForEachContainerAndItsCodecs() {
        val profile = deviceProfileOf(
            listOf(
                DirectPlayFormat(AudioContainer.MP4, setOf(AudioCodec.ALAC, AudioCodec.AAC)),
                DirectPlayFormat(AudioContainer.OGG, setOf(AudioCodec.OPUS, AudioCodec.VORBIS))
            )
        )

        assertEquals(
            listOf("m4a,m4b,mp4,mov" to "aac,alac", "ogg,oga,opus" to "opus,vorbis"),
            profile.directPlayProfiles.map { it.container to it.audioCodec }
        )
        assertTrue(profile.directPlayProfiles.all { it.type == DlnaProfileType.AUDIO })
    }

    @Test
    fun directPlayProfile_forPcm_listsTheSampleFormatsOfWavFiles() {
        val profile = deviceProfileOf(listOf(DirectPlayFormat(AudioContainer.WAV, setOf(AudioCodec.PCM))))

        assertEquals("pcm_u8,pcm_s16le,pcm_s24le,pcm_s32le,pcm_f32le", profile.directPlayProfiles.single().audioCodec)
    }

    @Test
    fun conversions_areAacInHlsAt256Kbps() {
        val profile = deviceProfileOf(emptyList())

        val conversion = profile.transcodingProfiles.single()
        assertEquals(DlnaProfileType.AUDIO, conversion.type)
        assertEquals(MediaStreamProtocol.HLS, conversion.protocol)
        assertEquals("mp4", conversion.container)
        assertEquals("aac", conversion.audioCodec)
        assertEquals(EncodingContext.STREAMING, conversion.context)
        assertEquals(256_000, profile.musicStreamingTranscodingBitrate)
    }

    @Test
    fun playbackInfoRequest_allowsTheOriginalFileAndConversions() {
        val request = playbackInfoRequestOf(listOf(DirectPlayFormat(AudioContainer.MP3, setOf(AudioCodec.MP3))))

        assertEquals(true, request.enableDirectPlay)
        assertEquals(true, request.enableTranscoding)
        assertEquals("mp3", request.deviceProfile?.directPlayProfiles?.single()?.container)
    }
}
