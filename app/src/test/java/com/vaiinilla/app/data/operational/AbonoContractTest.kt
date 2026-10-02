package com.vaiinilla.app.data.operational

import com.vaiinilla.app.domain.repository.AbonoMode
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.ui.operational.abonoIdempotencyKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbonoContractTest {
    private val json = WaiterContractJson()

    private fun body(raw: String): JsonObject = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun `an amount in cash sends the amount, the cash received and the expected remaining`() {
        val sent =
            body(json.encodeAbono(AccountPaymentMethod.CASH, AbonoMode.AMOUNT, "100.00", null, "120.00", "200.00"))
        assertEquals("efectivo", sent["metodo_pago"]!!.jsonPrimitive.content)
        assertEquals("monto", sent["modo"]!!.jsonPrimitive.content)
        assertEquals("100.00", sent["monto"]!!.jsonPrimitive.content)
        assertEquals("120.00", sent["monto_recibido"]!!.jsonPrimitive.content)
        assertEquals("200.00", sent["restante_esperado"]!!.jsonPrimitive.content)
        assertFalse("partes" in sent)
        assertFalse("propina" in sent)
    }

    @Test
    fun `parts on the terminal send no amount and no cash`() {
        val sent =
            body(json.encodeAbono(AccountPaymentMethod.TERMINAL, AbonoMode.PARTS, null, 3, "999.00", "100.00", "10.00"))
        assertEquals("terminal", sent["metodo_pago"]!!.jsonPrimitive.content)
        assertEquals(3, sent["partes"]!!.jsonPrimitive.content.toInt())
        assertFalse("monto" in sent)
        assertFalse("monto_recibido" in sent)
        assertEquals("10.00", sent["propina"]!!.jsonPrimitive.content)
    }

    @Test
    fun `parses the abono and whether the account was settled`() {
        val raw =
            """
            {"data":{"abono":{"id":"a1","metodo_pago":"efectivo","monto":"33.34","monto_recibido":"50.00","cambio":"16.66"},
             "restante":"0.00","liquidada":true,"pedidos_cobrados":2},"meta":{},"error":null}
            """.trimIndent()
        val result = json.parseAbono(raw)
        assertEquals("33.34", result.amount)
        assertEquals("16.66", result.change)
        assertTrue(result.settled)
        assertEquals(2, result.ordersCollected)
    }

    @Test
    fun `the account reads what was paid in and what remains, and who pays each order`() {
        val raw =
            """
            {"data":{"espacio":{"id":7,"nombre":"Mesa 1","tipo":"mesa"},"estado":"ocupada","saldada":false,
             "cuenta":{"pedidos":[{"id":"p1","folio":3,"estado":"cobrado","total":120,"pago_diferido":true,
               "pendiente_cobro":true,"items_resumen":"Tacos","pagara":"Ana"}],
             "total":120,"pendiente":120,"pagado":0,"saldada":false,"abonado":50,"restante":70}},
             "meta":{},"error":null}
            """.trimIndent()
        val account = json.parseSessionDetail(raw).account!!
        assertEquals("50.00", account.paidIn)
        assertEquals("70.00", account.remaining)
        assertTrue(account.splitInProgress)
        assertEquals("Ana", account.orders.single().payer)
    }

    @Test
    fun `retrying the same abono reuses its key, a new one does not`() {
        val first = abonoIdempotencyKey(7, AccountPaymentMethod.CASH, AbonoMode.PARTS, null, 3, "100.00", null)
        val retry = abonoIdempotencyKey(7, AccountPaymentMethod.CASH, AbonoMode.PARTS, null, 3, "100.00", null)
        val next = abonoIdempotencyKey(7, AccountPaymentMethod.CASH, AbonoMode.PARTS, null, 2, "66.67", null)
        assertEquals(first, retry)
        assertNotEquals(first, next)
    }
}
