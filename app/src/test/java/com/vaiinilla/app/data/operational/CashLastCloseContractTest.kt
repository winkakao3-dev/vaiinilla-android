package com.vaiinilla.app.data.operational

import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.repository.CashLastClose
import com.vaiinilla.app.ui.admin.mergeLastClose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Con la caja cerrada el último cierre sale de `GET sesiones-caja/ultimo-cierre`. */
class CashLastCloseContractTest {
    private val json = OrderContractJson()

    @Test
    fun `lee el ultimo cierre de la sesion cerrada`() {
        val last =
            json.parseLastClosedSession(
                """
                {"data":{"id":"s1","fecha_operativa":"2026-11-04","monto_inicial":"500.00",
                "fondo_sugerido":null,"monto_final":"1250.00","abierta_en":"2026-11-04T15:00:00.000Z",
                "cerrada_en":"2026-11-05T03:10:00.000Z","cierre_automatico":false,
                "efectivo_esperado":"1260.00","diferencia":"-10.00","ventas_efectivo":"700.00"},
                "meta":{},"error":null}
                """.trimIndent(),
            )!!
        assertEquals("1250.00", last.counted)
        assertEquals("2026-11-05T03:10:00.000Z", last.closedAt)
        assertEquals("500.00", last.opening)
        assertEquals("1260.00", last.expected)
        assertEquals("700.00", last.cashSales)
        assertFalse(last.closedByMe)
    }

    @Test
    fun `sin cierres previos no hay ultimo cierre`() {
        assertNull(json.parseLastClosedSession("""{"data":null,"meta":{},"error":null}"""))
    }

    @Test
    fun `el cierre del servidor conserva el cerro tu si es el mismo`() {
        val local = CashLastClose(counted = "1250", closedAt = "2026-11-05T03:10:01Z", closedByMe = true)
        val server = CashLastClose(counted = "1250.00", closedAt = "2026-11-05T03:10:00.000Z", opening = "500.00")
        val merged = mergeLastClose(server, local)!!
        assertTrue(merged.closedByMe)
        assertEquals("500.00", merged.opening)

        assertFalse(mergeLastClose(server.copy(counted = "900.00"), local)!!.closedByMe)
        assertEquals(local, mergeLastClose(null, local))
    }
}
