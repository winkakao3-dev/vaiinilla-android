package com.vaiinilla.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdempotencyHeaderAndJsonTest {
    @Serializable
    private data class Sample(
        val a: Int = 1,
        val b: String?,
    )

    @Test
    fun `la cabecera lleva la llave tal cual`() {
        assertEquals(mapOf("Idempotency-Key" to "abc"), idempotencyHeaders("abc"))
    }

    @Test
    fun `cada Json escribe como los que reemplazo`() {
        assertEquals("{\"b\":null}", ApiJson.plain.encodeToString(Sample(b = null)))
        assertEquals("{\"a\":1,\"b\":null}", ApiJson.withDefaults.encodeToString(Sample(b = null)))
        assertEquals("{}", ApiJson.omitNulls.encodeToString(Sample(b = null)))
        assertEquals("{\"a\":1}", ApiJson.omitNullsWithDefaults.encodeToString(Sample(b = null)))
    }

    @Test
    fun `todos ignoran claves desconocidas`() {
        val raw = """{"a":2,"b":null,"nuevo":true}"""
        listOf(ApiJson.plain, ApiJson.withDefaults, ApiJson.omitNulls, ApiJson.coercing, ApiJson.lenientCoercing)
            .forEach { assertEquals(2, it.decodeFromString<Sample>(raw).a) }
    }

    @Test
    fun `el retry-after se topa y no se vuelve negativo`() {
        val long = ApiClientException("X", "x", 503, retryAfterSeconds = 3600)
        assertEquals(GetRequestRetryPolicy.MAX_RETRY_AFTER_MILLIS, GetRequestRetryPolicy.delayMillisFor(long, 0))
        val short = ApiClientException("X", "x", 503, retryAfterSeconds = 2)
        assertEquals(2_000L, GetRequestRetryPolicy.delayMillisFor(short, 0))
        assertTrue(
            GetRequestRetryPolicy.delayMillisFor(ApiClientException("X", "x", 503, retryAfterSeconds = -5), 0) >= 0,
        )
    }
}
