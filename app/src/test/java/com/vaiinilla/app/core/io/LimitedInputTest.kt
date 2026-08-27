package com.vaiinilla.app.core.io

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class LimitedInputTest {
    @Test
    fun `bounded read returns bytes at or below limit`() {
        val payload = ByteArray(32) { it.toByte() }
        val result = ByteArrayInputStream(payload).readBytesLimited(32)
        assertArrayEquals(payload, result)
    }

    @Test
    fun `bounded read rejects stream that exceeds limit`() {
        val payload = ByteArray(33) { it.toByte() }
        assertNull(ByteArrayInputStream(payload).readBytesLimited(32))
    }
}
