package io.github.lorber13.jellyfinmusic.core.data.stream

import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.TranscodeSeekInfo
import org.jellyfin.sdk.model.api.TranscodingProfile

/**
 * The bitrate of the server's conversions, in bits per second: transparent for nearly everyone.
 * The server keeps a lossy file's own bitrate when it is lower. Without it, the server converts
 * at 128 kbps.
 */
internal const val CONVERSION_BITRATE = 256_000

/** The codec of the server's conversions: AAC plays on every Android phone. */
internal const val CONVERSION_CODEC = "aac"

/** What a PlaybackInfo request sends: what the phone plays as it is, and what to convert the rest to. */
internal fun playbackInfoRequestOf(playableFormats: List<DirectPlayFormat>): PlaybackInfoDto = PlaybackInfoDto(
    deviceProfile = deviceProfileOf(playableFormats),
    enableDirectPlay = true,
    enableTranscoding = true
)

internal fun deviceProfileOf(playableFormats: List<DirectPlayFormat>): DeviceProfile = DeviceProfile(
    musicStreamingTranscodingBitrate = CONVERSION_BITRATE,
    directPlayProfiles = playableFormats.map(::directPlayProfileOf),
    transcodingProfiles = listOf(conversionProfile),
    containerProfiles = emptyList(),
    codecProfiles = emptyList(),
    subtitleProfiles = emptyList()
)

private fun directPlayProfileOf(format: DirectPlayFormat): DirectPlayProfile = DirectPlayProfile(
    container = format.container.jellyfinNames,
    audioCodec = format.codecs.sorted().joinToString(",") { it.jellyfinNames },
    type = DlnaProfileType.AUDIO
)

/**
 * Conversions are HLS with fMP4 segments, because Jellyfin's progressive conversions can't be
 * seeked (they are sent without a length or byte ranges).
 */
private val conversionProfile = TranscodingProfile(
    container = "mp4",
    type = DlnaProfileType.AUDIO,
    audioCodec = CONVERSION_CODEC,
    // Required by the SDK; empty, because the conversions are audio only.
    videoCodec = "",
    protocol = MediaStreamProtocol.HLS,
    context = EncodingContext.STREAMING,
    estimateContentLength = false,
    enableMpegtsM2TsMode = false,
    transcodeSeekInfo = TranscodeSeekInfo.AUTO,
    copyTimestamps = false,
    enableSubtitlesInManifest = false,
    minSegments = 0,
    segmentLength = 0,
    conditions = emptyList(),
    // Constant bitrate, so conversions have the bitrate above rather than a quality level.
    enableAudioVbrEncoding = false
)

/** The names the server gives these files: their extensions, with `.opus` files reported as "ogg". */
private val AudioContainer.jellyfinNames: String
    get() = when (this) {
        AudioContainer.MP3 -> "mp3"
        AudioContainer.FLAC -> "flac"
        AudioContainer.MP4 -> "m4a,m4b,mp4,mov"
        AudioContainer.OGG -> "ogg,oga,opus"
        AudioContainer.MATROSKA -> "mka,mkv,webm,weba"
        AudioContainer.WAV -> "wav"
        AudioContainer.ADTS -> "aac"
    }

/**
 * The server's codec names, which are FFmpeg's. PCM is little-endian, as in WAV files, in the
 * sample formats Media3 reads.
 */
private val AudioCodec.jellyfinNames: String
    get() = when (this) {
        AudioCodec.AAC -> "aac"
        AudioCodec.MP3 -> "mp3"
        AudioCodec.FLAC -> "flac"
        AudioCodec.OPUS -> "opus"
        AudioCodec.VORBIS -> "vorbis"
        AudioCodec.ALAC -> "alac"
        AudioCodec.AC3 -> "ac3"
        AudioCodec.EAC3 -> "eac3"
        AudioCodec.PCM -> "pcm_u8,pcm_s16le,pcm_s24le,pcm_s32le,pcm_f32le"
    }
