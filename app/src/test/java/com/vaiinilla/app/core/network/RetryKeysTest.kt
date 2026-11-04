package com.vaiinilla.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RetryKeysTest {
    @Test
    fun `un reintento tras un fallo reusa la misma llave`() {
        val keys = RetryKeys()
        val first = keys.withKey("open-cash", "500.00") { key -> Result.failure<String>(IllegalStateException(key)) }
        val second = keys.withKey("open-cash", "500.00") { key -> Result.success(key) }
        assertEquals(first.exceptionOrNull()?.message, second.getOrNull())
    }

    @Test
    fun `tras el exito la siguiente operacion igual recibe llave nueva`() {
        val keys = RetryKeys()
        val first = keys.withKey("open-cash", "500.00") { key -> Result.success(key) }.getOrThrow()
        val next = keys.withKey("open-cash", "500.00") { key -> Result.success(key) }.getOrThrow()
        assertNotEquals(first, next)
    }

    @Test
    fun `datos distintos no comparten llave`() {
        val keys = RetryKeys()
        assertNotEquals(keys.keyFor("open-cash", "500.00"), keys.keyFor("open-cash", "600.00"))
    }
}
