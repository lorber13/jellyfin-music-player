package io.github.lorber13.jellyfinmusic.core.data.server

import io.github.lorber13.jellyfinmusic.core.data.client.JellyfinClients
import io.github.lorber13.jellyfinmusic.core.data.di.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.discovery.RecommendedServerIssue

// The SDK reads responses on the calling thread, so every call runs on the IO dispatcher.
internal class JellyfinServerRepository @Inject constructor(
    private val clients: JellyfinClients,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ServerRepository {
    override suspend fun findServer(address: String): ServerLookupResult {
        val jellyfin = clients.jellyfin()
        val candidates = jellyfin.discovery.getAddressCandidates(address.trim())
        if (candidates.isEmpty()) return ServerLookupResult.Failed(ServerLookupFailure.InvalidAddress)
        val results = withContext(ioDispatcher) { jellyfin.discovery.getRecommendedServers(candidates).toList() }
        return pickServer(results)
    }

    override suspend fun getPublicUsers(serverAddress: String): List<PublicUser> {
        val users = try {
            withContext(ioDispatcher) { clients.createClient(baseUrl = serverAddress).userApi.getPublicUsers().content }
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
            clients.createClient(baseUrl = serverAddress).authenticationApi.getQuickConnectEnabled().content
        }
    } catch (_: ApiClientException) {
        // Quick Connect is an alternative; without it the user signs in with their password.
        false
    }
}

/**
 * The scores we connect to, best first. The SDK gives OK to servers older than the minimum
 * version; those are reported as unsupported instead.
 */
private val acceptedScores = listOf(RecommendedServerInfoScore.GREAT, RecommendedServerInfoScore.GOOD)

/**
 * Picks the server to use from the SDK's results for every address candidate. The SDK suggests
 * the first GREAT result, otherwise the first GOOD one, but a slow answer only gets GOOD. A slow
 * HTTPS answer would then lose to a fast plain HTTP one, which sends the password unencrypted,
 * so any working HTTPS address wins over HTTP. The results come in candidate order, which the SDK
 * sorts most secure first, so within the same score the first result wins.
 */
internal fun pickServer(results: List<RecommendedServerInfo>): ServerLookupResult {
    if (results.isEmpty()) return ServerLookupResult.Failed(ServerLookupFailure.InvalidAddress)

    val accepted = results.filter { it.score in acceptedScores && it.systemInfo.isSuccess }
    val chosen = bestScoredOf(accepted.filter(::usesHttps)) ?: bestScoredOf(accepted)
    if (chosen != null) {
        val systemInfo = chosen.systemInfo.getOrThrow()
        val server = JellyfinServer(
            address = chosen.address,
            // A server name is optional in Jellyfin; the address still tells the user which server it is.
            name = systemInfo.serverName?.takeIf { it.isNotBlank() } ?: chosen.address,
            version = systemInfo.version.orEmpty()
        )
        return ServerLookupResult.Found(server)
    }
    unsupportedVersionOf(results)?.let { return ServerLookupResult.Failed(it) }
    return ServerLookupResult.Failed(mostHelpfulFailure(results.map(::failureOf)))
}

/** The first result with the best of the [acceptedScores], or null if there is none. */
private fun bestScoredOf(results: List<RecommendedServerInfo>): RecommendedServerInfo? =
    acceptedScores.firstNotNullOfOrNull { score -> results.firstOrNull { it.score == score } }

private fun usesHttps(result: RecommendedServerInfo): Boolean = result.address.startsWith("https://", ignoreCase = true)

/**
 * The failure for a Jellyfin server that answered but is older than the app supports. This is
 * checked before the other failures, which would call a server that answered "not a Jellyfin
 * server". It looks for the SDK's issue rather than the score, which other issues could share.
 */
private fun unsupportedVersionOf(results: List<RecommendedServerInfo>): ServerLookupFailure.UnsupportedVersion? {
    val result = results.firstOrNull { result ->
        result.issues.any { it is RecommendedServerIssue.UnsupportedServerVersion }
    } ?: return null
    return ServerLookupFailure.UnsupportedVersion(
        serverVersion = result.systemInfo.getOrNull()?.version.orEmpty(),
        // "10.9" rather than "10.9.0": the message says "10.9 or newer".
        minimumVersion = "${minimumSupportedServerVersion.major}.${minimumSupportedServerVersion.minor}"
    )
}

/**
 * Several candidates can fail in different ways. A certificate problem is the most useful to
 * report, because the server is clearly there. Next is "not a Jellyfin server", because
 * something answered. Otherwise nothing was reachable.
 */
private fun mostHelpfulFailure(failures: List<ServerLookupFailure>): ServerLookupFailure = listOf(
    ServerLookupFailure.SecureConnectionFailed,
    ServerLookupFailure.NotAJellyfinServer
).firstOrNull { it in failures } ?: ServerLookupFailure.Unreachable

/**
 * Why one candidate failed. This looks at the response rather than the SDK's issues: the SDK
 * also reports a missing version for candidates that never answered, which would make an
 * unreachable server look like something else.
 */
private fun failureOf(result: RecommendedServerInfo): ServerLookupFailure {
    // The request worked, but the product name or version showed it isn't a usable Jellyfin server.
    val error = result.systemInfo.exceptionOrNull() ?: return ServerLookupFailure.NotAJellyfinServer
    return when (error) {
        is SecureConnectionException -> ServerLookupFailure.SecureConnectionFailed

        is InvalidStatusException, is InvalidContentException -> ServerLookupFailure.NotAJellyfinServer

        // Timeouts, refused connections and blocked local network access.
        else -> ServerLookupFailure.Unreachable
    }
}
