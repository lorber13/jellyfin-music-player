package io.github.lorber13.jellyfinmusic.core.data.session

import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuickConnectFailureTest {
    @Test
    fun start_unauthorized_isDisabled() {
        assertEquals(QuickConnectFailure.DISABLED, quickConnectFailureOf(InvalidStatusException(401)))
    }

    @Test
    fun start_otherStatus_isAServerError() {
        assertEquals(QuickConnectFailure.SERVER_ERROR, quickConnectFailureOf(InvalidStatusException(500)))
    }

    @Test
    fun start_unreadableResponse_isAServerError() {
        assertEquals(QuickConnectFailure.SERVER_ERROR, quickConnectFailureOf(InvalidContentException()))
    }

    @Test
    fun start_connectionError_isUnreachable() {
        assertEquals(QuickConnectFailure.UNREACHABLE, quickConnectFailureOf(ApiClientException("Connection refused")))
    }

    @Test
    fun check_notFound_isExpired() {
        assertEquals(QuickConnectStatus.EXPIRED, quickConnectStatusOf(InvalidStatusException(404)))
    }

    @Test
    fun check_unauthorized_isExpired() {
        assertEquals(QuickConnectStatus.EXPIRED, quickConnectStatusOf(InvalidStatusException(401)))
    }

    @Test
    fun check_serverError_isACheckFailure() {
        assertEquals(QuickConnectStatus.CHECK_FAILED, quickConnectStatusOf(InvalidStatusException(500)))
    }

    @Test
    fun check_timeout_isACheckFailure() {
        assertEquals(QuickConnectStatus.CHECK_FAILED, quickConnectStatusOf(TimeoutException()))
    }

    @Test
    fun request_hidesTheSecret() {
        assertFalse("s3cret" in QuickConnectRequest(code = "123456", secret = "s3cret").toString())
    }
}
