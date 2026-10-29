package com.vaiinilla.app.domain

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.OperationalStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffPresenceTest {
    private fun status(extra: String): OperationalStatus =
        ContractResponseParser().parseOperationalStatus(
            """
            {"data":{"recibiendo_pedidos":false,"sesion_caja_abierta":true,
            "caja_en_linea":false,"cocina_en_linea":false,"tiempo_estimado_min":5,"consultado_en":"2026-10-29T20:00:00.000Z"
            $extra},"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
            """.trimIndent(),
        )

    @Test
    fun `con el telefono bloqueado el admin sigue viendo a Caja y Cocina en linea dos minutos`() {
        val bloqueado = status(""","caja_conectada":true,"cocina_conectada":true""")

        assertFalse(bloqueado.cashierOnline)
        assertTrue(bloqueado.cashierPresent)
        assertTrue(bloqueado.kitchenPresent)
        assertFalse(status(""","caja_conectada":false,"cocina_conectada":false""").kitchenPresent)
    }

    @Test
    fun `sin los campos nuevos el resumen usa los 15 s de antes`() {
        val anterior = status("")

        assertNull(anterior.cashierConnected)
        assertFalse(anterior.cashierPresent)
        assertFalse(anterior.kitchenPresent)
    }
}
