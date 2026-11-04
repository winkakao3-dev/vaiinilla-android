package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayAtEndSeatTest {
    private fun needsSeat() =
        OrderRepositoryException(
            PAYMENT_AT_END_REQUIRES_SEAT,
            "Para pagar al final tienes que estar sentado en esta mesa.",
            ApiClientException(PAYMENT_AT_END_REQUIRES_SEAT, "…", 403),
        )

    @Test
    fun `sin silla en la mesa se sienta y reintenta el pedido`() {
        var creates = 0
        val joined = mutableListOf<String>()
        val result =
            createSeatingAtTableIfNeeded(
                create = { if (creates++ == 0) Result.failure(needsSeat()) else Result.success("pedido") },
                aliases = listOf("Naev"),
                join = { alias ->
                    joined += alias
                    Result.success(Unit)
                },
            )
        assertEquals("pedido", result.getOrNull())
        assertEquals(2, creates)
        assertEquals(listOf("Naev"), joined)
    }

    @Test
    fun `si el nombre ya esta en la mesa prueba con el nombre completo`() {
        var creates = 0
        val joined = mutableListOf<String>()
        createSeatingAtTableIfNeeded(
            create = { if (creates++ == 0) Result.failure(needsSeat()) else Result.success("pedido") },
            aliases = listOf("Naev", "Naev Pérez"),
            join = { alias ->
                joined += alias
                if (alias == "Naev") {
                    Result.failure(ApiClientException(PARTICIPANT_NAME_TAKEN, "Ya hay una persona…", 409))
                } else {
                    Result.success(Unit)
                }
            },
        )
        assertEquals(listOf("Naev", "Naev Pérez"), joined)
        assertEquals(2, creates)
    }

    @Test
    fun `otro error no intenta sentarlo`() {
        var joins = 0
        val result =
            createSeatingAtTableIfNeeded<String>(
                create = { Result.failure(OrderRepositoryException("PRODUCT_UNAVAILABLE", "x")) },
                aliases = listOf("Naev"),
                join = {
                    joins++
                    Result.success(Unit)
                },
            )
        assertTrue(result.isFailure)
        assertEquals(0, joins)
    }

    @Test
    fun `sin QR no hay a que mesa sentarlo y queda el error del servidor`() {
        var creates = 0
        val result =
            createSeatingAtTableIfNeeded<String>(
                create = {
                    creates++
                    Result.failure(needsSeat())
                },
                aliases = listOf("Naev"),
                join = null,
            )
        assertTrue(result.isFailure)
        assertEquals(1, creates)
    }

    @Test
    fun `si no logra sentarlo devuelve por que`() {
        val result =
            createSeatingAtTableIfNeeded<String>(
                create = { Result.failure(needsSeat()) },
                aliases = listOf("Naev"),
                join = { Result.failure(ApiClientException("SESSION_CLOSED", "La mesa se acaba de cerrar", 409)) },
            )
        assertEquals("La mesa se acaba de cerrar", result.exceptionOrNull()?.message)
    }

    @Test
    fun `alias primero el primer nombre y luego el completo`() {
        assertEquals(listOf("Naev", "Naev Pérez"), payAtEndSeatAliases("  Naev   Pérez "))
        assertEquals(listOf("Cliente"), payAtEndSeatAliases(""))
    }
}
