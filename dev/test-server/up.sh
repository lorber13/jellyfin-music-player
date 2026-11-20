#!/usr/bin/env bash
# Starts the local Jellyfin test server and sets it up on the first run:
# test user, Music library, full scan and a sample playlist. Safe to run
# again; every step checks what already exists. See README.md.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
require_commands docker ffmpeg magick jq curl

CLIENT_HEADER='MediaBrowser Client="dev-test-server", Device="dev-test-server", DeviceId="dev-test-server", Version="1.0"'
PLAYLIST_NAME="Sample Playlist"
access_token=""
user_id=""

main() {
    "$SCRIPT_DIR/generate-library.sh"
    start_server
    wait_for_server
    if [[ "$(public_server_info | jq -r .StartupWizardCompleted)" != true ]]; then
        run_startup_wizard
    fi
    log_in
    add_music_library_if_missing
    wait_for_library_scan
    create_sample_playlist_if_missing
    forward_port_to_device
    print_summary
}

start_server() {
    # Create the folders before Docker does, or they end up owned by root.
    mkdir -p "$DATA_DIR/$JELLYFIN_VERSION/config" "$DATA_DIR/$JELLYFIN_VERSION/cache"
    echo "Starting Jellyfin $JELLYFIN_VERSION"
    compose up --detach --quiet-pull
}

public_server_info() {
    curl --silent --fail "$SERVER_URL/System/Info/Public"
}

wait_for_server() {
    local attempt
    for attempt in $(seq 1 90); do
        # Recent servers answer with a "starting up" page before they're ready,
        # so wait for real JSON, not just any response.
        if public_server_info 2>/dev/null | jq -e .Version >/dev/null 2>&1; then
            return
        fi
        if ((attempt % 10 == 0)); then
            echo "  still waiting for the server…"
        fi
        sleep 1
    done
    echo "error: the server did not start; see 'docker compose logs' in $SCRIPT_DIR" >&2
    exit 1
}

# api METHOD PATH [JSON_BODY]
api() {
    local method="$1" path="$2" body="${3:-}"
    local authorization="$CLIENT_HEADER"
    if [[ -n "$access_token" ]]; then
        authorization="$CLIENT_HEADER, Token=\"$access_token\""
    fi
    local -a body_options=()
    if [[ -n "$body" ]]; then
        body_options=(--header "Content-Type: application/json" --data "$body")
    fi
    curl --silent --show-error --fail --request "$method" \
        --header "Authorization: $authorization" \
        "${body_options[@]}" "$SERVER_URL$path"
}

run_startup_wizard() {
    echo "Running the first-run setup"
    api POST /Startup/Configuration \
        '{"UICulture": "en-US", "MetadataCountryCode": "US", "PreferredMetadataLanguage": "en"}'
    # The server creates the initial user when it is first read.
    api GET /Startup/User >/dev/null
    api POST /Startup/User \
        "$(jq -n --arg name "$TEST_USER" --arg password "$TEST_PASSWORD" '{Name: $name, Password: $password}')"
    api POST /Startup/RemoteAccess '{"EnableRemoteAccess": true, "EnableAutomaticPortMapping": false}'
    api POST /Startup/Complete
}

log_in() {
    local response
    response="$(api POST /Users/AuthenticateByName \
        "$(jq -n --arg name "$TEST_USER" --arg password "$TEST_PASSWORD" '{Username: $name, Pw: $password}')")"
    access_token="$(jq -r .AccessToken <<<"$response")"
    user_id="$(jq -r .User.Id <<<"$response")"
}

add_music_library_if_missing() {
    if api GET /Library/VirtualFolders | jq -e 'any(.[]; .Name == "Music")' >/dev/null; then
        return
    fi
    echo "Adding the Music library"
    # No internet metadata or images (MusicBrainz, TheAudioDB): the artists are
    # fictional and must not be matched to real ones. "Image Extractor" reads
    # artwork embedded in the files, so it stays on.
    local library_options='{
        "LibraryOptions": {
            "TypeOptions": [
                {"Type": "MusicArtist", "MetadataFetchers": [], "ImageFetchers": []},
                {"Type": "MusicAlbum", "MetadataFetchers": [], "ImageFetchers": []},
                {"Type": "Audio", "MetadataFetchers": [], "ImageFetchers": ["Image Extractor"]}
            ]
        }
    }'
    api POST "/Library/VirtualFolders?name=Music&collectionType=music&paths=%2Fmedia%2Fmusic" \
        "$library_options"
    # Start the scan separately: the one that refreshLibrary=true starts can
    # run before the new library is registered, and then finds nothing.
    api POST /Library/Refresh
}

audio_item_count() {
    api GET "/Items?userId=$user_id&includeItemTypes=Audio&recursive=true&limit=0" | jq .TotalRecordCount
}

wait_for_library_scan() {
    local expected_count
    expected_count="$(cat "$MUSIC_DIR/.track-count")"
    local attempt count
    for attempt in $(seq 1 120); do
        count="$(audio_item_count)"
        if ((count >= expected_count)); then
            return
        fi
        if ((attempt % 5 == 1)); then
            echo "  scanning the library: $count of $expected_count tracks"
        fi
        sleep 1
    done
    echo "error: the library scan did not finish ($count of $expected_count tracks)" >&2
    exit 1
}

create_sample_playlist_if_missing() {
    if api GET "/Items?userId=$user_id&includeItemTypes=Playlist&recursive=true" |
        jq -e --arg name "$PLAYLIST_NAME" 'any(.Items[]; .Name == $name)' >/dev/null; then
        return
    fi
    echo "Creating \"$PLAYLIST_NAME\""
    # Every 8th track in album order, so the playlist mixes albums and formats.
    local track_ids
    track_ids="$(api GET "/Items?userId=$user_id&includeItemTypes=Audio&recursive=true&sortBy=Album,ParentIndexNumber,IndexNumber" |
        jq '[.Items | to_entries[] | select(.key % 8 == 0) | .value.Id] | .[:10]')"
    api POST /Playlists "$(jq -n --arg name "$PLAYLIST_NAME" --arg user_id "$user_id" --argjson ids "$track_ids" \
        '{Name: $name, Ids: $ids, UserId: $user_id, MediaType: "Audio"}')" >/dev/null
}

# Lets the phone reach the loopback-only server at localhost:8096 over USB.
forward_port_to_device() {
    if ! command -v adb >/dev/null; then
        return
    fi
    local device_count
    device_count="$(adb devices | grep -c $'\tdevice$' || true)"
    if [[ -n "${ANDROID_SERIAL:-}" || "$device_count" -eq 1 ]]; then
        adb reverse "tcp:$SERVER_PORT" "tcp:$SERVER_PORT" >/dev/null
        echo "Forwarded the device's localhost:$SERVER_PORT to this machine (adb reverse)"
    elif ((device_count > 1)); then
        echo "Several adb devices are connected. To reach the server from one of them, run:"
        echo "  adb -s <serial> reverse tcp:$SERVER_PORT tcp:$SERVER_PORT"
    fi
}

print_summary() {
    local version
    version="$(public_server_info | jq -r .Version)"
    cat <<EOF

Jellyfin $version is ready.
  This machine and phone (adb reverse):  http://localhost:$SERVER_PORT
  Android emulator:                      http://10.0.2.2:$SERVER_PORT
  User / password:                       $TEST_USER / $TEST_PASSWORD
Stop it with ./down.sh, start over with ./reset.sh.
EOF
}

main
