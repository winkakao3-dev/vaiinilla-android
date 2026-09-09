package com.vaiinilla.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class GetRequestRetryPolicyTest {
    @Test
    fun `treats timeouts and connect failures as transient`() {
        assertTrue(GetRequestRetryPolicy.isTransient(SocketTimeoutException("timeout")))
        assertTrue(GetRequestRetryPolicy.isTransient(ConnectException("refused")))
    }

    @Test
    fun `treats 502-504 API errors as transient`() {
        assertTrue(GetRequestRetryPolicy.isTransient(apiError(502)))
        assertTrue(GetRequestRetryPolicy.isTransient(apiError(503)))
        assertTrue(GetRequestRetryPolicy.isTransient(apiError(504)))
    }

    @Test
    fun `does not retry client errors, unknown host, or other exceptions`() {
        assertFalse(GetRequestRetryPolicy.isTransient(apiError(400)))
        assertFalse(GetRequestRetryPolicy.isTransient(apiError(401)))
        assertFalse(GetRequestRetryPolicy.isTransient(apiError(500)))
        assertFalse(GetRequestRetryPolicy.isTransient(UnknownHostException("no dns")))
        assertFalse(GetRequestRetryPolicy.isTransient(IllegalStateException("unrelated")))
    }

    @Test
    fun `uses the backoff schedule when no Retry-After header is present`() {
        val error = SocketTimeoutException("timeout")
        assertEquals(500L, GetRequestRetryPolicy.delayMillisFor(error, attemptIndex = 0))
        assertEquals(1500L, GetRequestRetryPolicy.delayMillisFor(error, attemptIndex = 1))
        // Beyond the schedule length, keeps using the last configured delay.
        assertEquals(1500L, GetRequestRetryPolicy.delayMillisFor(error, attemptIndex = 5))
    }

    @Test
    fun `prefers the server's Retry-After header over the default backoff`() {
        val error = apiError(status = 503, retryAfterSeconds = 4)
        assertEquals(4_000L, GetRequestRetryPolicy.delayMillisFor(error, attemptIndex = 0))
    }

    private fun apiError(
        status: Int,
        retryAfterSeconds: Long? = null,
    ) = ApiClientException(
        code = "HTTP_$status",
        message = "boom",
        httpStatus = status,
        retryAfterSeconds = retryAfterSeconds,
    )
}
