package com.vaiinilla.app.data.cashcut

import com.vaiinilla.app.ui.admin.ReportPeriod
import com.vaiinilla.app.ui.screens.admin.signedMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** El JSON de `/sesiones-caja/{id}/corte`, `/sesiones-caja/cortes` y `/reportes/contable` (backend PR #201). */
class CashCutJsonTest {
    private val breakdown =
        """
        "resumen": {"ventas": "350.00", "pedidos": 4, "ticket_promedio": "87.50", "articulos": 6,
          "importe_articulos": "350.00", "descuentos": "0.00", "envios": "0.00", "rentas": "100.00"},
        "formas_pago": [{"metodo": "efectivo", "pedidos": 2, "monto": "220.00"},
                        {"metodo": "terminal", "pedidos": 2, "monto": "90.00"}],
        "propinas": {"total": "25.00", "efectivo": "0.00", "terminal": "25.00"},
        "cancelados": {"pedidos": 1, "monto": "120.00"},
        "por_categoria": [{"categoria_id": 1, "categoria": "Bebidas", "cantidad": 5, "importe": "230.00"}],
        "por_producto": [{"producto_id": 3, "producto": "Cerveza", "categoria": "Bebidas", "cantidad": 4,
          "importe": "200.00"}],
        "por_personal": [
          {"usuario_id": "u-1", "nombre": "Mario Mesero", "rol": "mesero", "pedidos": 2, "ventas": "220.00",
           "ticket_promedio": "110.00"},
          {"usuario_id": null, "nombre": null, "rol": null, "pedidos": 1, "ventas": "30.00", "ticket_promedio": "30.00"}
        ],
        "por_espacio": [
          {"espacio_id": 7, "nombre": "Pista 1", "tipo": "cancha", "pedidos": 2, "ventas": "150.00",
           "rentas": "100.00", "consumo": "50.00"},
          {"espacio_id": null, "nombre": null, "tipo": null, "pedidos": 1, "ventas": "30.00", "rentas": "0.00",
           "consumo": "30.00"}
        ]
        """.trimIndent()

    private val shift =
        """
        {"id": "s-1", "fecha_operativa": "2026-11-09", "abierta_en": "2026-11-09T16:00:00.000Z",
         "cerrada_en": "2026-11-09T22:00:00.000Z", "cierre_automatico": false,
         "abierta_por": {"usuario_id": "u-2", "nombre": "Carla Caja"},
         "cerrada_por": {"usuario_id": "u-2", "nombre": "Carla Caja"},
         "efectivo": {"fondo": "500.00", "cobros_efectivo": "220.00", "propinas": "0.00", "recargas": "0.00",
           "entradas": "0.00", "salidas": "50.00", "devoluciones_efectivo": "0.00", "efectivo_esperado": "670.00",
           "contado": "665.00", "diferencia": "-5.00", "arqueo": "falta"}}
        """.trimIndent()

    @Test
    fun `corte del turno con faltante, mesero y pista`() {
        val raw = """{"data": {"tipo": "final", "turno": $shift, $breakdown, "generado_en": "x"}, "meta": {}}"""
        val cut = CashCutJson.shiftCut(raw)
        assertTrue(cut.final)
        assertEquals("Carla Caja", cut.shift.openedBy)
        assertFalse(cut.shift.open)
        assertEquals(CashCountResult.SHORT, cut.shift.cash.result)
        assertEquals("670.00", cut.shift.cash.expected)
        assertEquals("Efectivo", cut.breakdown.payments.first().label)
        assertEquals("Clientes (app o QR)", cut.breakdown.staff[1].label)
        assertEquals("Para llevar o mostrador", cut.breakdown.spaces[1].label)
        assertEquals("100.00", cut.breakdown.spaces[0].rentals)
        assertEquals("100.00", cut.breakdown.summary.rentals)
    }

    @Test
    fun `corte del dia con un turno abierto no tiene arqueo`() {
        val raw =
            """
            {"data": {"fecha": "2026-11-09", "tipo": "parcial", "turnos": [$shift],
              "efectivo": {"fondo": "800.00", "efectivo_esperado": "1020.00", "contado": null, "diferencia": null,
                "arqueo": null}, $breakdown}}
            """.trimIndent()
        val day = CashCutJson.dayCut(raw)
        assertFalse(day.final)
        assertEquals(1, day.shifts.size)
        assertNull(day.cash.counted)
        assertNull(day.cash.result)
        assertEquals("1020.00", day.cash.expected)
        assertEquals(0, day.outsideShift.orders)
    }

    @Test
    fun `corte del dia con lo cobrado sin caja y las devoluciones`() {
        val raw =
            """
            {"data": {"fecha": "2026-11-09", "tipo": "parcial", "turnos": [],
              "sin_turno": {"pedidos": 2, "ventas": "150.00"},
              "devoluciones": {"articulos_rechazados": "120.00", "reembolsos_reserva": "0.00",
                "devoluciones_entregadas": "30.00"}}}
            """.trimIndent()
        val day = CashCutJson.dayCut(raw)
        assertEquals(2, day.outsideShift.orders)
        assertEquals("150.00", day.outsideShift.sales)
        assertEquals("120.00", day.breakdown.refunds.rejectedItems)
        assertEquals("30.00", day.breakdown.refunds.handedBack)
    }

    @Test
    fun `reporte contable con su periodo`() {
        val raw =
            """
            {"data": {"negocio": "Boliche", "periodo": {"desde": "2026-11-01", "hasta": "2026-11-09", "dias": 9},
              $breakdown, "por_dia": [{"fecha": "2026-11-09", "pedidos": 4, "ventas": "350.00"}]}}
            """.trimIndent()
        val report = CashCutJson.accounting(raw)
        assertEquals("2026-11-01", report.from)
        assertEquals("Boliche", report.business)
        assertEquals(1, report.breakdown.days.size)
        assertEquals("Mesero", staffRoleLabel(report.breakdown.staff[0].role))
    }

    @Test
    fun `periodos del reporte`() {
        val today = LocalDate.of(2026, 11, 9)
        assertEquals(LocalDate.of(2026, 11, 3) to today, ReportPeriod.WEEK.range(today))
        assertEquals(LocalDate.of(2026, 11, 1) to today, ReportPeriod.THIS_MONTH.range(today))
        assertEquals(LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31), ReportPeriod.LAST_MONTH.range(today))
    }

    @Test
    fun `diferencia con signo`() {
        assertEquals("—", signedMoney(null))
        assertTrue(signedMoney("-5.00").startsWith("−"))
        assertTrue(signedMoney("10.00").startsWith("+"))
    }

    @Test
    fun `hora en que empieza el dia`() {
        assertEquals("05:00", CashCutJson.dayStart("""{"data": {"hora_inicio_dia": "05:00"}}"""))
        assertNull(CashCutJson.dayStart("""{"data": {"tipo": "bar"}}"""))
        assertEquals("""{"hora_inicio_dia":"04:00"}""", CashCutJson.dayStartBody("04:00"))
    }
}
