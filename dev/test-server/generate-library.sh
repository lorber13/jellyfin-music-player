#!/usr/bin/env bash
# Generates the sample music library in data/music. Tracks are short sine
# tones with full tags; all artists and albums are fictional. The albums are
# chosen to cover cases the app has to handle: several formats including
# hi-res FLAC, a multi-disc album, a compilation, a gapless album, Unicode
# names, embedded or missing artwork and one long track. See README.md.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
require_commands ffmpeg magick

if [[ -f "$MUSIC_DIR/.complete" ]]; then
    echo "Sample library already generated in $MUSIC_DIR"
    exit 0
fi

# Build in a separate folder and move it into place at the end, so an
# interrupted run never leaves a half-made library behind.
WORK_DIR="$DATA_DIR/music.partial"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

DEFAULT_TRACK_SECONDS=30
# A pentatonic scale, so consecutive tracks are easy to tell apart by ear.
TONE_FREQUENCIES=(220 247 277 330 370 440 494 554 659 740)
generated_track_count=0

# Set by start_album and start_disc, read by add_track.
album_dir=""
album_title=""
album_artist=""
album_year=""
album_genre=""
album_format=""
album_artwork=""
album_cover=""
album_is_compilation=false
album_is_gapless=false
album_seconds_so_far=0
disc_number=1
is_multi_disc=false
track_number=0

# start_album ARTIST TITLE YEAR GENRE FORMAT ARTWORK COLOR_FROM COLOR_TO
#   FORMAT:  flac | flac-hires | mp3 | opus | m4a | ogg
#   ARTWORK: folder (folder.jpg next to the tracks) | embedded (inside each
#            file, mp3 and m4a only) | none
start_album() {
    album_artist="$1"
    album_title="$2"
    album_year="$3"
    album_genre="$4"
    album_format="$5"
    album_artwork="$6"
    album_dir="$WORK_DIR/$album_artist/$album_title ($album_year)"
    album_is_compilation=false
    album_is_gapless=false
    album_seconds_so_far=0
    disc_number=1
    is_multi_disc=false
    track_number=0
    mkdir -p "$album_dir"

    case "$album_artwork" in
        folder) album_cover="$album_dir/folder.jpg" ;;
        embedded) album_cover="$WORK_DIR/.cover.jpg" ;;
        none) album_cover="" ;;
    esac
    if [[ -n "$album_cover" ]]; then
        make_cover "$album_cover" "$7" "$8"
    fi
    echo "  $album_artist — $album_title"
}

start_disc() {
    disc_number="$1"
    is_multi_disc=true
    track_number=0
}

make_cover() {
    local output_file="$1" color_from="$2" color_to="$3"
    # No text on the cover: fonts differ between machines, and the app shows
    # the album title anyway.
    magick -size 600x600 "gradient:$color_from-$color_to" \
        -fill "rgba(255,255,255,0.2)" -draw "circle 300,300 300,140" \
        -fill "rgba(0,0,0,0.15)" -draw "circle 300,300 300,240" \
        -quality 90 "$output_file"
}

# add_track TITLE
# Optional variables for one call: track_artist (defaults to the album
# artist) and track_seconds (defaults to DEFAULT_TRACK_SECONDS).
add_track() {
    local title="$1"
    local artist="${track_artist:-$album_artist}"
    local seconds="${track_seconds:-$DEFAULT_TRACK_SECONDS}"
    track_number=$((track_number + 1))
    generated_track_count=$((generated_track_count + 1))

    local file_name
    file_name="$(printf '%02d - %s' "$track_number" "$title")"
    if [[ "$is_multi_disc" == true ]]; then
        file_name="$disc_number-$file_name"
    fi
    local output_file
    output_file="$album_dir/$file_name.$(file_extension)"

    local sample_rate=44100
    case "$album_format" in
        flac-hires) sample_rate=96000 ;;
        opus) sample_rate=48000 ;;
    esac

    local audio_source
    if [[ "$album_is_gapless" == true ]]; then
        audio_source="$(gapless_sweep_source "$seconds" "$sample_rate")"
    else
        audio_source="$(tone_source "$seconds" "$sample_rate")"
    fi

    local -a inputs=(-f lavfi -i "$audio_source")
    local -a stream_options=(-map 0:a)
    if [[ "$album_artwork" == embedded ]]; then
        inputs+=(-i "$album_cover")
        stream_options+=(-map 1:v -c:v copy -disposition:v:0 attached_pic)
    fi

    local -a tags=(
        -metadata "title=$title"
        -metadata "artist=$artist"
        -metadata "album_artist=$album_artist"
        -metadata "album=$album_title"
        -metadata "track=$track_number"
        -metadata "disc=$disc_number"
        -metadata "date=$album_year"
        -metadata "genre=$album_genre"
    )
    if [[ "$album_is_compilation" == true ]]; then
        tags+=(-metadata "compilation=1")
    fi

    local -a encoder_options
    read -r -a encoder_options <<<"$(encoder_options_for_format)"

    ffmpeg -hide_banner -loglevel error -nostdin -y \
        "${inputs[@]}" "${stream_options[@]}" \
        -ac 2 "${encoder_options[@]}" "${tags[@]}" \
        "$output_file"

    album_seconds_so_far=$((album_seconds_so_far + seconds))
}

# A steady tone with short fades, so tracks don't start or end with a click.
tone_source() {
    local seconds="$1" sample_rate="$2"
    local frequency="${TONE_FREQUENCIES[$((generated_track_count % ${#TONE_FREQUENCIES[@]}))]}"
    local fade_out_start=$((seconds - 1))
    echo "sine=frequency=$frequency:sample_rate=$sample_rate:duration=$seconds,volume=0.3,afade=t=in:d=0.5,afade=t=out:st=$fade_out_start:d=1"
}

# One rising sweep that runs across the whole album. Each track computes its
# part from the album time it starts at, so the waveform continues sample by
# sample into the next track: any gap or click during playback is audible.
gapless_sweep_source() {
    local seconds="$1" sample_rate="$2"
    local album_time="(t+$album_seconds_so_far)"
    # Frequency rises from 110 Hz by 4 Hz per second; the phase is its integral.
    echo "aevalsrc=exprs='0.3*sin(2*PI*(110*$album_time+2*$album_time*$album_time))':sample_rate=$sample_rate:duration=$seconds"
}

file_extension() {
    case "$album_format" in
        flac | flac-hires) echo flac ;;
        *) echo "$album_format" ;;
    esac
}

encoder_options_for_format() {
    case "$album_format" in
        flac) echo "-c:a flac -sample_fmt s16" ;;
        flac-hires) echo "-c:a flac -sample_fmt s32 -bits_per_raw_sample 24" ;;
        # ID3v2.3 is what most taggers and players read best.
        mp3) echo "-c:a libmp3lame -b:a 320k -id3v2_version 3" ;;
        opus) echo "-c:a libopus -b:a 128k" ;;
        m4a) echo "-c:a aac -b:a 256k" ;;
        ogg) echo "-c:a libvorbis -q:a 6" ;;
    esac
}

echo "Generating the sample library in $MUSIC_DIR"

start_album "Aurora Fields" "Morning Light" 2015 Ambient flac folder "#f59f00" "#e8590c"
add_track "First Light"
add_track "Dew on the Grass"
add_track "Slow Clouds"
add_track "Open Windows"
add_track "Warm Stones"
add_track "Paper Boats"
add_track "Noon Shadows"
add_track "Leaving Early"

start_album "Aurora Fields" "Evening Tide" 2018 Ambient flac-hires folder "#1c7ed6" "#0b1a4a"
add_track "Low Water"
add_track "Harbour Lights"
add_track "Salt Air"
add_track "Undertow"
add_track "Lantern Walk"
add_track "Night Swim"

start_album "Aurora Fields" "Gapless Horizons" 2021 Ambient flac folder "#12b886" "#087f5b"
album_is_gapless=true
add_track "Horizon I"
add_track "Horizon II"
add_track "Horizon III"
add_track "Horizon IV"
add_track "Horizon V"

start_album "The Copper Kettles" "Steam & Rust" 2009 Rock mp3 embedded "#c92a2a" "#5c0a0a"
add_track "Boiler Room"
add_track "Pressure Gauge"
add_track "Rust Belt Blues"
add_track "Whistle Stop"
add_track "Iron Lung"
add_track "Copper Wire"
add_track "Back Burner"
add_track "Smoke Signals"
add_track "Overheated"
add_track "Cooling Down"

start_album "The Copper Kettles" "Pressure Valve" 2012 Rock mp3 embedded "#e67700" "#5f3dc4"
add_track "Release"
add_track "Hiss"
add_track "Red Line"
add_track "Safety First"
add_track "Blow Off"
add_track "Gasket"
add_track "Steady State"
add_track "Ticking"
add_track "All Clear"

start_album "Café Ñandú" "Canciones del Río" 2016 Jazz m4a embedded "#2b8a3e" "#e9ecef"
add_track "Agua Dulce"
add_track "La Orilla"
add_track "Corriente"
add_track "Niño del Puerto"
add_track "Señales"
add_track "Remanso"
add_track "Lluvia de Junio"
add_track "Desembocadura"

start_album "Café Ñandú" "Été à Montréal" 2019 Jazz opus folder "#ae3ec9" "#ffd43b"
add_track "Rue Saint-Denis"
add_track "Café Crème"
add_track "Pluie d'été"
add_track "Ça va aller"
add_track "Les Îles"
add_track "Minuit"
add_track "Déjà vu"

start_album "東京 Echo Unit" "ネオン Signals" 2020 Electronic opus folder "#f06595" "#1864ab"
add_track "渋谷 Crossing"
add_track "Last Train"
add_track "ネオン"
add_track "Vending Machine Glow"
add_track "雨 (Rain)"
add_track "Karaoke Box"
add_track "Late Shift"
add_track "夜明け"

start_album "東京 Echo Unit" "Grid Lines (Deluxe Edition)" 2022 Electronic flac folder "#343a40" "#4dabf7"
start_disc 1
add_track "Grid One"
add_track "Grid Two"
add_track "Grid Three"
add_track "Grid Four"
add_track "Grid Five"
add_track "Grid Six"
start_disc 2
add_track "Grid One (Remix)"
add_track "Grid Two (Remix)"
add_track "Grid Three (Live)"
add_track "Demo 1"
add_track "Demo 2"
add_track "Outtake"

start_album "東京 Echo Unit" "Long Form" 2023 Electronic ogg none "" ""
add_track "Prelude"
track_seconds=600 add_track "The Long Way Round"
add_track "Coda"

start_album "Various Artists" "Summer Sampler Vol. 1" 2024 Pop mp3 folder "#fab005" "#fd7e14"
album_is_compilation=true
track_artist="Aurora Fields" add_track "Sunburn"
track_artist="The Copper Kettles" add_track "Heatwave"
track_artist="Café Ñandú" add_track "Verano"
track_artist="東京 Echo Unit" add_track "夏 (Summer)"
track_artist="Mira Kovač" add_track "Lemonade Stand"
track_artist="Jørgen Ølstrøm" add_track "Midnattssol"
track_artist="Mira Kovač feat. Jørgen Ølstrøm" add_track "Two Seasons"
track_artist="The Lighthouse Collective" add_track "Last Day of August"

rm -f "$WORK_DIR/.cover.jpg"
echo "$generated_track_count" >"$WORK_DIR/.track-count"
touch "$WORK_DIR/.complete"
rm -rf "$MUSIC_DIR"
mv "$WORK_DIR" "$MUSIC_DIR"
echo "Generated $generated_track_count tracks"
