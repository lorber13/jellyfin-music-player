package io.github.lorber13.jellyfinmusic.core.data.session

import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

class SignInFailureTest {
    @Test
    fun unauthorized_isWrongCredentials() {
        assertEquals(SignInFailure.WRONG_CREDENTIALS, signInFailureOf(InvalidStatusException(401)))
    }

    @Test
    fun otherStatus_isAServerError() {
        assertEquals(SignInFailure.SERVER_ERROR, signInFailureOf(InvalidStatusException(500)))
    }

    @Test
    fun unreadableResponse_isAServerError() {
        assertEquals(SignInFailure.SERVER_ERROR, signInFailureOf(InvalidContentException()))
    }

    @Test
    fun timeout_isUnreachable() {
        assertEquals(SignInFailure.UNREACHABLE, signInFailureOf(TimeoutException()))
    }

    @Test
    fun untrustedCertificate_isUnreachable() {
        assertEquals(SignInFailure.UNREACHABLE, signInFailureOf(SecureConnectionException()))
    }

    @Test
    fun connectionError_isUnreachable() {
        assertEquals(SignInFailure.UNREACHABLE, signInFailureOf(ApiClientException("Connection refused")))
    }
}
