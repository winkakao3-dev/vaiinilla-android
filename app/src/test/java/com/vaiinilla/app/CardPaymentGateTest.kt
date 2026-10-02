package com.vaiinilla.app

import com.vaiinilla.app.data.contract.ContractResponseParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La tarjeta se ofrece solo si el dueño la activó en su panel (`acepta_tarjeta`). */
class CardPaymentGateTest {
    private val parser = ContractResponseParser()

    private fun status(acceptsCard: Boolean?): String {
        val field = acceptsCard?.let { ""","acepta_tarjeta":$it""" }.orEmpty()
        return """
            {"data":{"recibiendo_pedidos":true,"sesion_caja_abierta":true,"caja_en_linea":true,
            "cocina_en_linea":true,"tiempo_estimado_min":5,"consultado_en":"2026-09-30T23:50:00.000Z"$field},
            "meta":{},"error":null}
            """.trimIndent()
    }

    @Test
    fun `the card is offered only when the owner turned it on`() {
        assertTrue(parser.parseOperationalStatus(status(true)).acceptsCard)
        assertFalse(parser.parseOperationalStatus(status(false)).acceptsCard)
    }

    @Test
    fun `an older server that does not send it hides the card`() {
        assertFalse(parser.parseOperationalStatus(status(null)).acceptsCard)
    }
}
