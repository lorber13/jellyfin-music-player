package io.github.lorber13.jellyfinmusic.core.data.server

import org.jellyfin.sdk.model.ServerVersion

/**
 * The product supports Jellyfin 10.9 and newer. The SDK itself targets newer servers and would
 * otherwise rank 10.9 to 11.x as unsupported.
 */
internal val minimumSupportedServerVersion = ServerVersion(major = 10, minor = 9, patch = 0)
