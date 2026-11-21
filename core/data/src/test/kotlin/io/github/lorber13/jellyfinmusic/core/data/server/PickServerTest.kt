package io.github.lorber13.jellyfinmusic.core.data.server

import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.discovery.RecommendedServerIssue
import org.jellyfin.sdk.model.ServerVersion
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class PickServerTest {
    @Test
    fun noCandidates_isAnInvalidAddress() {
        assertEquals(ServerLookupResult.Failed(ServerLookupFailure.InvalidAddress), pickServer(emptyList()))
    }

    @Test
    fun prefersTheBestScore_thenTheFirstCandidate() {
        val results = listOf(
            answered("https://music.example.com", RecommendedServerInfoScore.GOOD, name = "Slow"),
            answered("https://music.example.com:8096", RecommendedServerInfoScore.GREAT, name = "First great"),
            answered("http://music.example.com", RecommendedServerInfoScore.GREAT, name = "Second great")
        )

        val expected = JellyfinServer("https://music.example.com:8096", "First great", "12.4.0")
        assertEquals(ServerLookupResult.Found(expected), pickServer(results))
    }

    @Test
    fun serverOlderThanTheMinimumVersion_isAnUnsupportedVersion() {
        val tooOld = answered("http://old.example.com:8096", RecommendedServerInfoScore.OK, version = "10.8.13")
            .copy(issues = listOf(RecommendedServerIssue.UnsupportedServerVersion(ServerVersion(10, 8, 13))))

        val result = pickServer(listOf(unreachable("https://old.example.com"), tooOld))

        assertEquals(
            ServerLookupResult.Failed(
                ServerLookupFailure.UnsupportedVersion(serverVersion = "10.8.13", minimumVersion = "10.9")
            ),
            result
        )
    }

    @Test
    fun serverWithoutAName_isNamedAfterItsAddress() {
        val result =
            pickServer(listOf(answered("http://192.168.1.10:8096", RecommendedServerInfoScore.GREAT, name = "")))

        assertEquals("http://192.168.1.10:8096", (result as ServerLookupResult.Found).server.name)
    }

    @Test
    fun onlyUnreachableCandidates_isUnreachable() {
        val results = listOf(unreachable("https://nowhere.example.com"), unreachable("http://nowhere.example.com"))

        assertEquals(ServerLookupResult.Failed(ServerLookupFailure.Unreachable), pickServer(results))
    }

    @Test
    fun certificateProblem_winsOverOtherFailures() {
        val results = listOf(
            certificateProblem("https://music.example.com"),
            notJellyfin("http://music.example.com"),
            unreachable("http://music.example.com:8096")
        )

        assertEquals(ServerLookupResult.Failed(ServerLookupFailure.SecureConnectionFailed), pickServer(results))
    }

    @Test
    fun blockedOrRefusedConnection_isUnreachable() {
        // A blocked local network connection or a reset fails with a plain IO error.
        val results = listOf(failed("http://192.168.1.10:8096", ApiClientException("Unknown IO error occurred!")))

        assertEquals(ServerLookupResult.Failed(ServerLookupFailure.Unreachable), pickServer(results))
    }

    @Test
    fun answerFromAnotherProduct_isNotAJellyfinServer() {
        val otherProduct = answered("http://router.example.com", RecommendedServerInfoScore.BAD)
            .copy(systemInfo = Result.success(PublicSystemInfo(productName = "Emby Server", version = "4.8.0")))

        assertEquals(
            ServerLookupResult.Failed(ServerLookupFailure.NotAJellyfinServer),
            pickServer(listOf(otherProduct))
        )
    }

    @Test
    fun somethingElseAnswering_isNotAJellyfinServer() {
        val results = listOf(unreachable("https://router.example.com"), notJellyfin("http://router.example.com"))

        assertEquals(ServerLookupResult.Failed(ServerLookupFailure.NotAJellyfinServer), pickServer(results))
    }

    private fun answered(
        address: String,
        score: RecommendedServerInfoScore,
        name: String = "Music",
        version: String = "12.4.0"
    ) = RecommendedServerInfo(
        address = address,
        responseTime = 100,
        score = score,
        issues = emptyList(),
        systemInfo = Result.success(
            PublicSystemInfo(serverName = name, version = version, productName = "Jellyfin Server")
        )
    )

    // Like the SDK, every failed request also gets a MissingVersion issue.
    private fun unreachable(address: String) = failed(address, TimeoutException())

    private fun notJellyfin(address: String) = failed(address, InvalidStatusException(404))

    private fun certificateProblem(address: String) = failed(address, SecureConnectionException())

    private fun failed(address: String, error: Exception) = RecommendedServerInfo(
        address = address,
        responseTime = 100,
        score = RecommendedServerInfoScore.BAD,
        issues = listOf(RecommendedServerIssue.MissingSystemInfo(error), RecommendedServerIssue.MissingVersion),
        systemInfo = Result.failure(error)
    )
}
