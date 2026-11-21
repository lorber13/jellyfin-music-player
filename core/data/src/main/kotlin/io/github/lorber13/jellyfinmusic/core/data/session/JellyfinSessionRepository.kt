package io.github.lorber13.jellyfinmusic.core.data.session

import io.github.lorber13.jellyfinmusic.core.data.di.IoDispatcher
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.AuthenticationResult
import org.jellyfin.sdk.model.api.QuickConnectDto

/**
 * Keeps the session in memory only: staying signed in across restarts is ACC-04. The SDK reads
 * responses on the calling thread, so every call runs on the IO dispatcher.
 */
@Singleton
internal class JellyfinSessionRepository @Inject constructor(
    private val jellyfin: Jellyfin,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SessionRepository {
    private val currentSession = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = currentSession.asStateFlow()

    /** The client with the signed-in user's access token, for the data layer's other repositories. */
    var signedInClient: ApiClient? = null
        private set

    override suspend fun signIn(server: JellyfinServer, username: String, password: String): SignInResult {
        val client = jellyfin.createApi(baseUrl = server.address)
        val result = try {
            withContext(ioDispatcher) {
                client.authenticationApi.authenticateUserByName(
                    AuthenticateUserByName(username = username, pw = password)
                ).content
            }
        } catch (error: ApiClientException) {
            return SignInResult.Failed(signInFailureOf(error))
        }
        return startSession(client, server, result, fallbackUserName = username)
    }

    override suspend fun startQuickConnect(server: JellyfinServer): QuickConnectStartResult {
        val result = try {
            withContext(ioDispatcher) {
                jellyfin.createApi(baseUrl = server.address).authenticationApi.initiateQuickConnect().content
            }
        } catch (error: ApiClientException) {
            return QuickConnectStartResult.Failed(quickConnectFailureOf(error))
        }
        return QuickConnectStartResult.Started(QuickConnectRequest(code = result.code, secret = result.secret))
    }

    override suspend fun checkQuickConnect(server: JellyfinServer, request: QuickConnectRequest): QuickConnectStatus {
        val result = try {
            withContext(ioDispatcher) {
                jellyfin.createApi(baseUrl = server.address).authenticationApi.getQuickConnectState(request.secret)
                    .content
            }
        } catch (error: ApiClientException) {
            return quickConnectStatusOf(error)
        }
        return if (result.authenticated) QuickConnectStatus.APPROVED else QuickConnectStatus.WAITING
    }

    override suspend fun signInWithQuickConnect(server: JellyfinServer, request: QuickConnectRequest): SignInResult {
        val client = jellyfin.createApi(baseUrl = server.address)
        val result = try {
            withContext(ioDispatcher) {
                client.authenticationApi.authenticateWithQuickConnect(QuickConnectDto(secret = request.secret)).content
            }
        } catch (error: ApiClientException) {
            return SignInResult.Failed(signInFailureOf(error))
        }
        return startSession(client, server, result, fallbackUserName = null)
    }

    /** Makes the user [result] signed in the current session, whichever way they signed in. */
    private fun startSession(
        client: ApiClient,
        server: JellyfinServer,
        result: AuthenticationResult,
        fallbackUserName: String?
    ): SignInResult {
        val accessToken = result.accessToken
        val user = result.user
        val userName = user?.name ?: fallbackUserName
        if (accessToken == null || user == null || userName == null) {
            return SignInResult.Failed(SignInFailure.SERVER_ERROR)
        }

        client.update(accessToken = accessToken)
        signedInClient = client
        val newSession = UserSession(
            serverAddress = server.address,
            serverName = server.name,
            userId = user.id.toString(),
            userName = userName
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

internal fun quickConnectFailureOf(error: ApiClientException): QuickConnectFailure = when (error) {
    // Jellyfin answers 401 when Quick Connect is turned off.
    is InvalidStatusException ->
        if (error.status == 401) QuickConnectFailure.DISABLED else QuickConnectFailure.SERVER_ERROR

    is InvalidContentException -> QuickConnectFailure.SERVER_ERROR

    else -> QuickConnectFailure.UNREACHABLE
}

internal fun quickConnectStatusOf(error: ApiClientException): QuickConnectStatus {
    // 404: the server dropped the request, usually after its 10-minute timeout. 401: Quick
    // Connect was turned off meanwhile, so the request can't be approved any more either.
    val isGone = error is InvalidStatusException && (error.status == 404 || error.status == 401)
    return if (isGone) QuickConnectStatus.EXPIRED else QuickConnectStatus.CHECK_FAILED
}
