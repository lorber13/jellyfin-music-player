package io.github.lorber13.jellyfinmusic.core.playback

import android.media.MediaCodecList
import android.util.Log
import androidx.media3.common.MimeTypes
import io.github.lorber13.jellyfinmusic.core.data.stream.AudioCodec
import io.github.lorber13.jellyfinmusic.core.data.stream.AudioContainer
import io.github.lorber13.jellyfinmusic.core.data.stream.DirectPlayFormat

private const val TAG = "PlayableFormats"

/**
 * The containers Media3 reads from a progressive stream, with the codecs each can hold (Media3's
 * supported formats page). Whether a codec plays depends on the phone's decoders.
 */
private val codecsByContainer: Map<AudioContainer, Set<AudioCodec>> = mapOf(
    AudioContainer.MP3 to setOf(AudioCodec.MP3),
    AudioContainer.FLAC to setOf(AudioCodec.FLAC),
    AudioContainer.MP4 to setOf(
        AudioCodec.AAC,
        AudioCodec.ALAC,
        AudioCodec.FLAC,
        AudioCodec.MP3,
        AudioCodec.OPUS,
        AudioCodec.AC3,
        AudioCodec.EAC3
    ),
    AudioContainer.OGG to setOf(AudioCodec.VORBIS, AudioCodec.OPUS, AudioCodec.FLAC),
    AudioContainer.MATROSKA to setOf(
        AudioCodec.AAC,
        AudioCodec.MP3,
        AudioCodec.FLAC,
        AudioCodec.OPUS,
        AudioCodec.VORBIS,
        AudioCodec.AC3,
        AudioCodec.EAC3,
        AudioCodec.PCM
    ),
    AudioContainer.WAV to setOf(AudioCodec.PCM),
    AudioContainer.ADTS to setOf(AudioCodec.AAC)
)

private val codecsByDecoderMimeType: Map<String, AudioCodec> = mapOf(
    MimeTypes.AUDIO_AAC to AudioCodec.AAC,
    MimeTypes.AUDIO_MPEG to AudioCodec.MP3,
    MimeTypes.AUDIO_FLAC to AudioCodec.FLAC,
    MimeTypes.AUDIO_OPUS to AudioCodec.OPUS,
    MimeTypes.AUDIO_VORBIS to AudioCodec.VORBIS,
    MimeTypes.AUDIO_ALAC to AudioCodec.ALAC,
    MimeTypes.AUDIO_AC3 to AudioCodec.AC3,
    MimeTypes.AUDIO_E_AC3 to AudioCodec.EAC3
)

/**
 * The formats the phone plays as they are, given the codecs it has decoders for: each container
 * Media3 reads, with the codecs in it that can be decoded. Containers with none are left out.
 */
internal fun directPlayFormatsOf(decodableCodecs: Set<AudioCodec>): List<DirectPlayFormat> {
    // Media3 plays uncompressed audio itself, without a decoder.
    val playableCodecs = decodableCodecs + AudioCodec.PCM
    return codecsByContainer.mapNotNull { (container, codecs) ->
        val playableCodecsInContainer = codecs intersect playableCodecs
        if (playableCodecsInContainer.isEmpty()) null else DirectPlayFormat(container, playableCodecsInContainer)
    }
}

/**
 * The decoders Android's compatibility rules (CDD section 5.1.2, from Android 10 on) require on
 * every phone with audio output. Optional ones, such as ALAC and AC-3, differ between phones.
 */
private val requiredAudioCodecs: Set<AudioCodec> =
    setOf(AudioCodec.AAC, AudioCodec.MP3, AudioCodec.FLAC, AudioCodec.OPUS, AudioCodec.VORBIS)

/**
 * The audio codecs this phone has a decoder for. Asked rather than assumed, because decoders
 * differ between phones: the Pixel 10, for example, has none for ALAC but has one for AC-3.
 *
 * When the phone fails to list them, as some do (ExoPlayer guards the same query), the ones every
 * phone must have, so that only tracks in optional formats are converted. If the player can't
 * find its decoders either, playback can still fail later, in the player.
 */
internal fun decodableAudioCodecs(): Set<AudioCodec> = try {
    MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        .filterNot { it.isEncoder }
        .flatMap { it.supportedTypes.asList() }
        .mapNotNullTo(mutableSetOf()) { mimeType -> codecsByDecoderMimeType[mimeType.lowercase()] }
} catch (error: RuntimeException) {
    Log.w(TAG, "Couldn't list the phone's audio decoders; assuming only the required ones", error)
    requiredAudioCodecs
}
