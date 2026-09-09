package com.vaiinilla.app.core.network

import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Retry/backoff decision logic for transient network failures. Deliberately scoped to
 * idempotent GET requests: retrying POST/PUT/DELETE automatically is unsafe without an
 * idempotency key (checkout/payment mutations must never be retried silently).
 */
internal object GetRequestRetryPolicy {
    /** Total attempts including the first try (1 initial + 2 retries). */
    const val MAX_ATTEMPTS = 3

    private val BACKOFF_MILLIS = longArrayOf(500L, 1500L)
    private val TRANSIENT_HTTP_STATUSES = setOf(502, 503, 504)

    fun isTransient(error: Throwable): Boolean =
        when (error) {
            is SocketTimeoutException, is ConnectException -> true
            is ApiClientException -> error.httpStatus in TRANSIENT_HTTP_STATUSES
            else -> false
        }

    /** [attemptIndex] is 0 for the delay before the first retry, 1 before the second, etc. */
    fun delayMillisFor(
        error: Throwable,
        attemptIndex: Int,
    ): Long {
        val retryAfterMillis = (error as? ApiClientException)?.retryAfterSeconds?.let { it * 1000L }
        return retryAfterMillis ?: BACKOFF_MILLIS.getOrElse(attemptIndex) { BACKOFF_MILLIS.last() }
    }
}
