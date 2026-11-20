# Settings shared by up.sh, down.sh and reset.sh. Source it, don't run it.
# shellcheck shell=bash
# The variables are used by the scripts that source this file.
# shellcheck disable=SC2034

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$SCRIPT_DIR/data"
MUSIC_DIR="$DATA_DIR/music"

# Override to test other servers, e.g. 10.9.11 (oldest supported) or 10.8.13
# (unsupported, ACC-07).
export JELLYFIN_VERSION="${JELLYFIN_VERSION:-12.4}"
export HOST_UID HOST_GID
HOST_UID="$(id -u)"
HOST_GID="$(id -g)"

SERVER_URL="http://127.0.0.1:8096"
SERVER_PORT=8096
TEST_USER="test"
TEST_PASSWORD="test"

compose() {
    docker compose --project-directory "$SCRIPT_DIR" -f "$SCRIPT_DIR/compose.yaml" "$@"
}

require_commands() {
    local command_name
    for command_name in "$@"; do
        if ! command -v "$command_name" >/dev/null; then
            echo "error: '$command_name' is required but not installed" >&2
            exit 1
        fi
    done
}
