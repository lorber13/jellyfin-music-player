#!/usr/bin/env bash
# Stops the local Jellyfin test server. Its data is kept.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
require_commands docker

compose down
