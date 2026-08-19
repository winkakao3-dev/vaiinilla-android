package com.vaiinilla.app

import com.vaiinilla.app.data.order.OrderUserDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderUserDtoNullabilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `accepts null matricula from backend`() {
        val dto = json.decodeFromString<OrderUserDto>("""{"nombre":"David Ramirez","matricula":null}""")

        assertEquals("David Ramirez", dto.name)
        assertNull(dto.enrollment)
    }
}
