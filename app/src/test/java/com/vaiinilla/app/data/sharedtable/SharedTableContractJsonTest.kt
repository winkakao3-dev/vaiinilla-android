package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.screens.tableOrderStateLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SharedTableContractJsonTest {
    private val json = SharedTableContractJson()

    private val body =
        """
        {"data":{"espacio":{"id":3,"nombre":"Mesa 3","tipo":"mesa"},"mi_alias":"Ana","cuenta_abierta":true,
         "participantes":[{"alias":"Luis","soy_yo":false,"unido_en":null},{"alias":"Ana","soy_yo":true,"unido_en":null}],
         "grupos":[
           {"alias":"Luis","soy_yo":false,"total":"20.20","pagado":"0.00","pendiente":"20.20",
            "pedidos":[{"id":null,"folio":7,"estado":"cobrado","items_resumen":"1× Torta","total":"20.20","pendiente_cobro":true,"pagara":"Ana","lo_pago_yo":true,"creado_en":"a"}]},
           {"alias":"Ana","soy_yo":true,"total":"10.10","pagado":"0.00","pendiente":"10.10",
            "pedidos":[{"id":"p1","folio":8,"estado":"preparando","items_resumen":"1× Taco","total":"10.10","pendiente_cobro":true,"pagara":null,"lo_pago_yo":false,"creado_en":"b"}]}],
         "totales":{"total":"30.30","pagado":"0.00","pendiente":"30.30"},
         "mi_parte":{"total":"30.30","pagado":"0.00","pendiente":"30.30"}},"error":null}
        """.trimIndent()

    @Test
    fun `lee la mesa con montos exactos y tus pedidos primero`() {
        val table = json.parseTable(body)!!
        assertEquals("Mesa 3", table.spaceName)
        assertEquals(listOf("Ana", "Luis"), table.orderedGroups.map { it.alias })
        assertEquals(BigDecimal("30.30"), table.myShare.pending)
        val luis =
            table.groups
                .first { it.alias == "Luis" }
                .orders
                .single()
        assertNull(luis.id)
        assertEquals("Ana", luis.payer)
        assertTrue(luis.iPayIt)
    }

    @Test
    fun `sin mesa la API responde data null`() {
        assertNull(json.parseTable("""{"data":null,"meta":{},"error":null}"""))
    }

    @Test
    fun `a la cuenta sin pagar no dice Cobrado`() {
        val order =
            json
                .parseTable(body)!!
                .groups
                .first { it.alias == "Luis" }
                .orders
                .single()
        assertEquals(OrderState.PAID, order.status)
        assertEquals("En la cuenta", tableOrderStateLabel(order))
        assertEquals(
            "Entregado · pagado",
            tableOrderStateLabel(order.copy(status = OrderState.DELIVERED, pendingPayment = false)),
        )
    }

    @Test
    fun `codifica unirse y esto lo pago yo`() {
        assertEquals("""{"token":"qr","alias":"Ana"}""", json.encodeJoin("qr", "Ana"))
        assertEquals("""{"folio":7,"pago_yo":true}""", json.encodeClaim(7, true))
    }
}
