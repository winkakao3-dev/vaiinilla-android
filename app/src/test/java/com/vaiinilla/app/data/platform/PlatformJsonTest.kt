package com.vaiinilla.app.data.platform

import com.vaiinilla.app.ui.platform.PulsePeriod
import com.vaiinilla.app.ui.platform.pulseRange
import com.vaiinilla.app.ui.platform.slugFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlatformJsonTest {
    @Test
    fun `la lista conserva estado del administrador y cursor`() {
        val page =
            parsePlatformStores(
                """{"data":[{"id":"1","nombre":"Boliche","slug":"boliche","administrador":"sin_asignar","stripe":null,"extra":1}],"meta":{"cursor":"c"},"error":null}""",
            )
        assertEquals("sin_asignar", page.stores.single().administrador)
        assertEquals("c", page.cursor)
    }

    @Test
    fun `las metricas aceptan dinero en texto`() {
        val metrics =
            parsePlatformMetrics(
                """{"data":{"resumen":{"ventas_totales":"84320.50","pedidos":12,"ticket_promedio":"70.00","comisiones":"10.00"},"operacion":{"establecimientos_activos":3}}}""",
            )
        assertEquals("84320.50", metrics.resumen.sales)
        assertEquals(3, metrics.operacion.active)
    }

    @Test
    fun `los campos vacios viajan como null`() {
        assertEquals("""{"direccion":null,"activo":true}""", encodeStoreFields(mapOf("direccion" to null, "activo" to true)))
    }

    @Test
    fun `asignacion distingue cuenta existente`() {
        assertTrue(parsePlatformAssignment("""{"data":{"email":"a@b.mx","asignado":true}}""").asignado)
    }

    @Test
    fun `el enlace sale del nombre sin acentos`() {
        assertEquals("cafe-nino-2", slugFrom("  Café Niño #2 "))
    }

    @Test
    fun `los periodos terminan hoy`() {
        val today = LocalDate.of(2026, 11, 4)
        assertEquals("2026-10-29" to "2026-11-04", pulseRange(PulsePeriod.WEEK, today))
        assertEquals("2026-11-01" to "2026-11-04", pulseRange(PulsePeriod.THIS_MONTH, today))
    }
}
