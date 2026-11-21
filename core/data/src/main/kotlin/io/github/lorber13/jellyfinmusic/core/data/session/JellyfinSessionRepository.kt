package io.github.lorber13.jellyfinmusic.core.data.session

import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.model.api.AuthenticateUserByName

/** Keeps the session in memory only: staying signed in across restarts is ACC-04. */
@Singleton
internal class JellyfinSessionRepository @Inject constructor(private val jellyfin: Jellyfin) : SessionRepository {
    private val currentSession = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = currentSession.asStateFlow()

    /** The client with the signed-in user's access token, for the data layer's other repositories. */
    var signedInClient: ApiClient? = null
        private set

    override suspend fun signIn(server: JellyfinServer, username: String, password: String): SignInResult {
        val client = jellyfin.createApi(baseUrl = server.address)
        val result = try {
            client.authenticationApi.authenticateUserByName(AuthenticateUserByName(username = username, pw = password))
                .content
        } catch (error: ApiClientException) {
            return SignInResult.Failed(signInFailureOf(error))
        }

        val accessToken = result.accessToken
        val user = result.user
        if (accessToken == null || user == null) return SignInResult.Failed(SignInFailure.SERVER_ERROR)

        client.update(accessToken = accessToken)
        signedInClient = client
        val newSession = UserSession(
            serverAddress = server.address,
            serverName = server.name,
            userId = user.id.toString(),
            userName = user.name ?: username
        )
        currentSession.value = newSession
        return SignInResult.Success(newSession)
    }
}

internal fun signInFailureOf(error: ApiClientException): SignInFailure = when (error) {
    // Jellyfin answers 401 both for a wrong password and for an unknown or disabled user.
    is InvalidStatusException ->
        if (error.status == 401) SignInFailure.WRONG_CREDENTIALS else SignInFailure.SERVER_ERROR

    is InvalidContentException -> SignInFailure.SERVER_ERROR

    // Timeouts, refused connections, certificate problems and blocked local network access.
    else -> SignInFailure.UNREACHABLE
}
