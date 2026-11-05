package com.vaiinilla.app.data.operational

import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import com.vaiinilla.app.domain.repository.CashDrawerRules
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashDrawerJsonTest {
    private val report =
        """
        {"data":{
          "resumen":{"fondo":"500.00","cobros_efectivo":"320.00","propinas":"20.00","recargas":"100.00",
            "entradas":"50.00","salidas":"80.00","devoluciones_efectivo":"30.00","efectivo_esperado":"880.00",
            "cobros_tarjeta":"0.00"},
          "movimientos":[
            {"id":"a","tipo":"apertura","sale":false,"monto":"500.00","motivo":null,"registrado_por":null,
             "pedido":null,"devolucion_id":null,"creado_en":"2026-11-05T15:00:00.000Z"},
            {"id":"b","tipo":"ajuste_salida","sale":true,"monto":"80.00","motivo":"Compra de hielo",
             "registrado_por":{"nombre":"Ana"},"pedido":null,"devolucion_id":null,
             "creado_en":"2026-11-05T17:00:00.000Z"},
            {"id":"c","tipo":"ajuste_salida","sale":true,"monto":"30.00","motivo":null,"registrado_por":null,
             "pedido":{"id":"p","folio":42,"espacio":null},"devolucion_id":null,"creado_en":"2026-11-05T16:00:00.000Z"}
          ]}}
        """.trimIndent()

    @Test
    fun `lee el resumen y los renglones del cajon`() {
        val parsed = CashDrawerJson.parseReport(report)
        assertEquals("500.00", parsed.summary.opening)
        assertEquals("320.00", parsed.summary.cashSales)
        assertEquals("80.00", parsed.summary.cashOut)
        assertEquals("30.00", parsed.summary.refunds)
        assertEquals("880.00", parsed.summary.expected)
        assertEquals(3, parsed.movements.size)
        val salida = parsed.movements[1]
        assertTrue(salida.outgoing)
        assertFalse(salida.isRefund)
        assertEquals("Ana", salida.registeredBy)
        assertEquals("Compra de hielo", salida.reason)
        val devolucion = parsed.movements[2]
        assertTrue(devolucion.isRefund)
        assertEquals(42, devolucion.orderFolio)
        assertNull(parsed.movements[0].reason)
    }

    @Test
    fun `arma el cuerpo del movimiento como lo pide el servidor`() {
        val raw = CashDrawerJson.encodeMovement(CashDrawerMovementType.OUT, "80.00", "  Hielo ")
        val body = Json.parseToJsonElement(raw).jsonObject
        assertEquals("salida", body.getValue("tipo").jsonPrimitive.content)
        assertEquals("80.00", body.getValue("monto").jsonPrimitive.content)
        assertEquals("Hielo", body.getValue("motivo").jsonPrimitive.content)
    }

    @Test
    fun `lee el esperado tras anotar`() {
        val raw = """{"data":{"movimiento":{},"efectivo_esperado":"820.00"}}"""
        assertEquals("820.00", CashDrawerJson.parseExpectedAfterMovement(raw))
    }

    @Test
    fun `el motivo va de 3 a 160 letras`() {
        assertFalse(CashDrawerRules.validReason(" ab "))
        assertTrue(CashDrawerRules.validReason("abc"))
        assertFalse(CashDrawerRules.validReason("x".repeat(161)))
    }
}
