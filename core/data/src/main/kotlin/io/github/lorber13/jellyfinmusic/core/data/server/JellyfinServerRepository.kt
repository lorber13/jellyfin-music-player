package io.github.lorber13.jellyfinmusic.core.data.server

import io.github.lorber13.jellyfinmusic.core.data.di.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore

// The SDK reads responses on the calling thread, so every call runs on the IO dispatcher.
internal class JellyfinServerRepository @Inject constructor(
    private val jellyfin: Jellyfin,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ServerRepository {
    override suspend fun findServer(address: String): ServerLookupResult {
        val candidates = jellyfin.discovery.getAddressCandidates(address.trim())
        if (candidates.isEmpty()) return ServerLookupResult.Failed(ServerLookupFailure.INVALID_ADDRESS)
        val results = withContext(ioDispatcher) { jellyfin.discovery.getRecommendedServers(candidates).toList() }
        return pickServer(results)
    }

    override suspend fun getPublicUsers(serverAddress: String): List<PublicUser> {
        val users = try {
            withContext(ioDispatcher) { jellyfin.createApi(baseUrl = serverAddress).userApi.getPublicUsers().content }
        } catch (_: ApiClientException) {
            // The picker is a shortcut; without it the user types their name.
            return emptyList()
        }
        return users.mapNotNull { user ->
            user.name?.takeIf { it.isNotBlank() }?.let { name -> PublicUser(id = user.id.toString(), name = name) }
        }
    }

    override suspend fun isQuickConnectEnabled(serverAddress: String): Boolean = try {
        withContext(ioDispatcher) {
            jellyfin.createApi(baseUrl = serverAddress).authenticationApi.getQuickConnectEnabled().content
        }
    } catch (_: ApiClientException) {
        // Quick Connect is an alternative; without it the user signs in with their password.
        false
    }
}

/**
 * The scores we connect to, best first. OK is the score the SDK gives servers older than the
 * minimum version; they are still accepted here so that ACC-07 can tell the user the version is
 * unsupported, instead of the app saying it found no server.
 */
private val acceptedScores = listOf(
    RecommendedServerInfoScore.GREAT,
    RecommendedServerInfoScore.GOOD,
    RecommendedServerInfoScore.OK
)

/**
 * Picks the server to use from the SDK's results for every address candidate. The results come
 * in candidate order, which the SDK sorts most secure first, so within the same score the first
 * result wins.
 */
internal fun pickServer(results: List<RecommendedServerInfo>): ServerLookupResult {
    if (results.isEmpty()) return ServerLookupResult.Failed(ServerLookupFailure.INVALID_ADDRESS)

    for (score in acceptedScores) {
        val result = results.firstOrNull { it.score == score } ?: continue
        val systemInfo = result.systemInfo.getOrNull() ?: continue
        val server = JellyfinServer(
            address = result.address,
            // A server name is optional in Jellyfin; the address still tells the user which server it is.
            name = systemInfo.serverName?.takeIf { it.isNotBlank() } ?: result.address,
            version = systemInfo.version.orEmpty()
        )
        return ServerLookupResult.Found(server)
    }
    return ServerLookupResult.Failed(mostHelpfulFailure(results.map(::failureOf)))
}

/**
 * Several candidates can fail in different ways. A certificate problem is the most useful to
 * report, because the server is clearly there. Next is "not a Jellyfin server", because
 * something answered. Otherwise nothing was reachable.
 */
private fun mostHelpfulFailure(failures: List<ServerLookupFailure>): ServerLookupFailure = listOf(
    ServerLookupFailure.SECURE_CONNECTION_FAILED,
    ServerLookupFailure.NOT_A_JELLYFIN_SERVER
).firstOrNull { it in failures } ?: ServerLookupFailure.UNREACHABLE

/**
 * Why one candidate failed. This looks at the response rather than the SDK's issues: the SDK
 * also reports a missing version for candidates that never answered, which would make an
 * unreachable server look like something else.
 */
private fun failureOf(result: RecommendedServerInfo): ServerLookupFailure {
    // The request worked, but the product name or version showed it isn't a usable Jellyfin server.
    val error = result.systemInfo.exceptionOrNull() ?: return ServerLookupFailure.NOT_A_JELLYFIN_SERVER
    return when (error) {
        is SecureConnectionException -> ServerLookupFailure.SECURE_CONNECTION_FAILED

        is InvalidStatusException, is InvalidContentException -> ServerLookupFailure.NOT_A_JELLYFIN_SERVER

        // Timeouts, refused connections and blocked local network access.
        else -> ServerLookupFailure.UNREACHABLE
    }
}
