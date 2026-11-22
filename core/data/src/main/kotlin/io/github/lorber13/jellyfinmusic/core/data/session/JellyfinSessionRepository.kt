package io.github.lorber13.jellyfinmusic.core.data.session

import io.github.lorber13.jellyfinmusic.core.data.client.JellyfinClients
import io.github.lorber13.jellyfinmusic.core.data.di.ApplicationScope
import io.github.lorber13.jellyfinmusic.core.data.di.IoDispatcher
import io.github.lorber13.jellyfinmusic.core.data.server.JellyfinServer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.AuthenticationResult
import org.jellyfin.sdk.model.api.QuickConnectDto

/**
 * Signs in and keeps the session in [sessionStore], so the user stays signed in across restarts.
 * A restored session is trusted without asking the server, so the app also starts offline; the
 * server rejecting it later is ACC-06. The SDK reads responses on the calling thread, so every
 * call runs on the IO dispatcher.
 */
@Singleton
internal class JellyfinSessionRepository @Inject constructor(
    private val clients: JellyfinClients,
    private val sessionStore: SessionStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope
) : SessionRepository {
    private val currentSession = MutableStateFlow<SessionState>(SessionState.Loading)
    override val session: StateFlow<SessionState> = currentSession.asStateFlow()

    /**
     * The client with the signed-in user's access token, for the data layer's other repositories.
     * Volatile because the player reads it on its loading threads.
     */
    @Volatile
    var signedInClient: ApiClient? = null
        private set

    init {
        applicationScope.launch { restoreSession() }
    }

    private suspend fun restoreSession() {
        val storedSession = sessionStore.read()
        if (storedSession == null) {
            currentSession.value = SessionState.SignedOut
            return
        }
        signedInClient =
            clients.createClient(baseUrl = storedSession.serverAddress, accessToken = storedSession.accessToken)
        currentSession.value = SessionState.SignedIn(storedSession.userSession)
    }

    override suspend fun signIn(server: JellyfinServer, username: String, password: String): SignInResult {
        val client = clients.createClient(baseUrl = server.address)
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
                clients.createClient(baseUrl = server.address).authenticationApi.initiateQuickConnect().content
            }
        } catch (error: ApiClientException) {
            return QuickConnectStartResult.Failed(quickConnectFailureOf(error))
        }
        return QuickConnectStartResult.Started(QuickConnectRequest(code = result.code, secret = result.secret))
    }

    override suspend fun checkQuickConnect(server: JellyfinServer, request: QuickConnectRequest): QuickConnectStatus {
        val result = try {
            withContext(ioDispatcher) {
                clients.createClient(baseUrl = server.address).authenticationApi.getQuickConnectState(request.secret)
                    .content
            }
        } catch (error: ApiClientException) {
            return quickConnectStatusOf(error)
        }
        return if (result.authenticated) QuickConnectStatus.APPROVED else QuickConnectStatus.WAITING
    }

    override suspend fun signInWithQuickConnect(server: JellyfinServer, request: QuickConnectRequest): SignInResult {
        val client = clients.createClient(baseUrl = server.address)
        val result = try {
            withContext(ioDispatcher) {
                client.authenticationApi.authenticateWithQuickConnect(QuickConnectDto(secret = request.secret)).content
            }
        } catch (error: ApiClientException) {
            return SignInResult.Failed(signInFailureOf(error))
        }
        return startSession(client, server, result, fallbackUserName = null)
    }

    /** Makes the user [result] signed in and stores the session, whichever way they signed in. */
    private suspend fun startSession(
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

        val storedSession = StoredSession(
            serverAddress = server.address,
            serverName = server.name,
            userId = user.id.toString(),
            userName = userName,
            accessToken = accessToken
        )
        // Stored before the session is published, so the app can't be closed signed in but not remembered.
        sessionStore.save(storedSession)
        client.update(accessToken = accessToken)
        signedInClient = client
        currentSession.value = SessionState.SignedIn(storedSession.userSession)
        return SignInResult.Success(storedSession.userSession)
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
