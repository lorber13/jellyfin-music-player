#!/usr/bin/env bash
# Stops the server and deletes its data for JELLYFIN_VERSION, so the next
# ./up.sh starts from a fresh install. With --all, also deletes every
# version's data and the generated music.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
require_commands docker

compose down
if [[ "${1:-}" == "--all" ]]; then
    rm -rf "$DATA_DIR"
    echo "Deleted all server data and the generated music"
else
    rm -rf "${DATA_DIR:?}/$JELLYFIN_VERSION"
    echo "Deleted the Jellyfin $JELLYFIN_VERSION server data"
fi
