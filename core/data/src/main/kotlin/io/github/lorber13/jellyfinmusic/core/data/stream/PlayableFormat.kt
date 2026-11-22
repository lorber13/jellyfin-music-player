package io.github.lorber13.jellyfinmusic.core.data.stream

/** A file format the phone plays as it is: files in [container] whose audio is in one of [codecs]. */
data class DirectPlayFormat(val container: AudioContainer, val codecs: Set<AudioCodec>)

/** The kinds of file music comes in. */
enum class AudioContainer {
    MP3,
    FLAC,

    /** MPEG-4 audio, such as `.m4a` files. */
    MP4,

    /** Ogg, including `.opus` files. */
    OGG,

    /** Matroska, including WebM. */
    MATROSKA,
    WAV,

    /** Raw AAC in `.aac` files. */
    ADTS
}

/** The ways the audio inside a file can be encoded. */
enum class AudioCodec {
    AAC,
    MP3,
    FLAC,
    OPUS,
    VORBIS,

    /** Apple Lossless. */
    ALAC,

    /** Dolby Digital. */
    AC3,

    /** Dolby Digital Plus. */
    EAC3,

    /** Uncompressed audio, as in WAV files. */
    PCM
}
