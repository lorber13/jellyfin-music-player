package io.github.lorber13.jellyfinmusic.core.data.library

import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFailureTest {
    @Test
    fun errorStatus_isAServerError() {
        assertEquals(LibraryFailure.SERVER_ERROR, libraryFailureOf(InvalidStatusException(500)))
    }

    @Test
    fun unauthorized_isAServerError() {
        assertEquals(LibraryFailure.SERVER_ERROR, libraryFailureOf(InvalidStatusException(401)))
    }

    @Test
    fun unreadableResponse_isAServerError() {
        assertEquals(LibraryFailure.SERVER_ERROR, libraryFailureOf(InvalidContentException()))
    }

    @Test
    fun timeout_isUnreachable() {
        assertEquals(LibraryFailure.UNREACHABLE, libraryFailureOf(TimeoutException()))
    }

    @Test
    fun untrustedCertificate_isUnreachable() {
        assertEquals(LibraryFailure.UNREACHABLE, libraryFailureOf(SecureConnectionException()))
    }

    @Test
    fun connectionError_isUnreachable() {
        assertEquals(LibraryFailure.UNREACHABLE, libraryFailureOf(ApiClientException("Connection refused")))
    }
}
