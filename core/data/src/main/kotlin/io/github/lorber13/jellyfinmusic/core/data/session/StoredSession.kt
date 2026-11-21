package io.github.lorber13.jellyfinmusic.core.data.session

import kotlinx.serialization.Serializable

/** The session as written to the device, so the user stays signed in across restarts. */
@Serializable
internal data class StoredSession(
    val serverAddress: String,
    val serverName: String,
    val userId: String,
    val userName: String,
    val accessToken: String
) {
    val userSession: UserSession
        get() = UserSession(
            serverAddress = serverAddress,
            serverName = serverName,
            userId = userId,
            userName = userName
        )

    // Keeps the access token out of logs and crash output.
    override fun toString(): String =
        "StoredSession(serverAddress=$serverAddress, serverName=$serverName, userId=$userId, userName=$userName)"
}
