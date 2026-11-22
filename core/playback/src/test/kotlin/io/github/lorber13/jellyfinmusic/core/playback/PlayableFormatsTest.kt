package io.github.lorber13.jellyfinmusic.core.playback

import io.github.lorber13.jellyfinmusic.core.data.stream.AudioCodec
import io.github.lorber13.jellyfinmusic.core.data.stream.AudioContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayableFormatsTest {
    // The decoders of the Pixel 10, which has none for ALAC.
    private val pixelCodecs = setOf(
        AudioCodec.AAC,
        AudioCodec.MP3,
        AudioCodec.FLAC,
        AudioCodec.OPUS,
        AudioCodec.VORBIS,
        AudioCodec.AC3,
        AudioCodec.EAC3
    )

    @Test
    fun withoutAnAlacDecoder_noFormatHasAlac() {
        val formats = directPlayFormatsOf(pixelCodecs)

        assertFalse(formats.any { AudioCodec.ALAC in it.codecs })
        assertTrue(AudioCodec.AAC in formats.single { it.container == AudioContainer.MP4 }.codecs)
    }

    @Test
    fun withAnAlacDecoder_mp4FilesWithAlacPlay() {
        val formats = directPlayFormatsOf(pixelCodecs + AudioCodec.ALAC)

        assertTrue(AudioCodec.ALAC in formats.single { it.container == AudioContainer.MP4 }.codecs)
    }

    @Test
    fun wavFiles_playWithoutADecoder() {
        val formats = directPlayFormatsOf(emptySet())

        assertEquals(setOf(AudioCodec.PCM), formats.single { it.container == AudioContainer.WAV }.codecs)
    }

    @Test
    fun containersWithoutADecodableCodec_areLeftOut() {
        val containers = directPlayFormatsOf(setOf(AudioCodec.MP3)).map { it.container }

        assertEquals(
            listOf(AudioContainer.MP3, AudioContainer.MP4, AudioContainer.MATROSKA, AudioContainer.WAV),
            containers
        )
    }

    @Test
    fun codecsAreOnlyListedInContainersThatCanHoldThem() {
        val formats = directPlayFormatsOf(pixelCodecs)

        assertEquals(setOf(AudioCodec.MP3), formats.single { it.container == AudioContainer.MP3 }.codecs)
        assertEquals(
            setOf(AudioCodec.VORBIS, AudioCodec.OPUS, AudioCodec.FLAC),
            formats.single { it.container == AudioContainer.OGG }.codecs
        )
    }
}
