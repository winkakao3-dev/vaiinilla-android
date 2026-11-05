package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.ui.screens.TableClaim
import com.vaiinilla.app.ui.screens.myShareHint
import com.vaiinilla.app.ui.screens.othersAtTableLine
import com.vaiinilla.app.ui.screens.tableOrderClaim
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
    fun `la renta pagada de la pista llega aparte de la cuenta (U05)`() {
        assertNull(json.parseTable(body)!!.rent)
        val withRent =
            body.replace(
                "\"mi_parte\":{\"total\":\"30.30\",\"pagado\":\"0.00\",\"pendiente\":\"30.30\"}",
                "\"mi_parte\":{\"total\":\"30.30\",\"pagado\":\"0.00\",\"pendiente\":\"30.30\"}," +
                    "\"renta\":{\"pagada\":\"300.00\",\"minutos\":120,\"jugadores\":4}",
            )
        val rent = json.parseTable(withRent)!!.rent!!
        assertEquals(BigDecimal("300.00"), rent.paid)
        assertEquals(120, rent.minutes)
        assertEquals(4, rent.players)
        // No cambia lo que falta pagar.
        assertEquals(BigDecimal("30.30"), json.parseTable(withRent)!!.myShare.pending)
    }

    @Test
    fun `los abonos de la mesa bajan lo que falta (R21)`() {
        val withAbonos =
            body.replace(
                "\"totales\":{\"total\":\"30.30\",\"pagado\":\"0.00\",\"pendiente\":\"30.30\"}",
                "\"totales\":{\"total\":\"30.30\",\"pagado\":\"0.00\",\"pendiente\":\"30.30\"," +
                    "\"abonado\":\"25.00\",\"restante\":\"5.30\"}",
            )
        val table = json.parseTable(withAbonos)!!
        assertEquals(BigDecimal("25.00"), table.totals.paidIn)
        assertEquals(BigDecimal("5.30"), table.totals.remaining)
        // Nadie debe más de lo que falta en toda la mesa.
        assertEquals(BigDecimal("5.30"), table.myShare.pending)
        assertEquals(BigDecimal("5.30"), table.groups.first { it.isMe }.amounts.pending)
        // Sin abonos todo sigue igual.
        assertEquals(BigDecimal("30.30"), json.parseTable(body)!!.myShare.pending)
        assertEquals(BigDecimal("30.30"), json.parseTable(body)!!.totals.remaining)
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

    /** Pedido 8 de Ana: por cobrar, sin reclamo. */
    private val unclaimedOrder =
        json
            .parseTable(body)!!
            .groups
            .first { it.alias == "Ana" }
            .orders
            .single()

    @Test
    fun `un pedido ya cobrado no tiene botón de reclamo`() {
        assertNull(tableOrderClaim(unclaimedOrder.copy(pendingPayment = false)))
    }

    @Test
    fun `sin reclamo el botón dice esto lo pago yo y lo pide`() {
        assertEquals(TableClaim.Toggle("Esto lo pago yo", payIt = true), tableOrderClaim(unclaimedOrder))
    }

    @Test
    fun `si lo pago yo el botón se marca y lo quita`() {
        val mine = unclaimedOrder.copy(payer = "Ana", iPayIt = true)
        assertEquals(TableClaim.Toggle("Lo pago yo ✓", payIt = false), tableOrderClaim(mine))
    }

    @Test
    fun `si otro lo reclamó muestra quién paga en vez del botón`() {
        val theirs = unclaimedOrder.copy(payer = "Luis", iPayIt = false)
        assertEquals(TableClaim.OtherPays("Luis"), tableOrderClaim(theirs))
    }

    @Test
    fun `tu parte sale tal cual la manda la API`() {
        val share = json.parseTable(body)!!.myShare
        assertEquals(BigDecimal("30.30"), share.total)
        assertEquals(BigDecimal("0.00"), share.paid)
        assertEquals(BigDecimal("30.30"), share.pending)
    }

    @Test
    fun `con parte pendiente se paga en caja con tu nombre`() {
        val pending = SharedTableAmounts(BigDecimal("30.30"), BigDecimal("0.00"), BigDecimal("30.30"))
        assertEquals("Págala en caja o con tu mesero. Di tu nombre: Ana.", myShareHint(pending, "Ana"))
    }

    @Test
    fun `con parte saldada no pide pagar`() {
        val paid = SharedTableAmounts(BigDecimal("30.30"), BigDecimal("30.30"), BigDecimal("0.00"))
        assertEquals("Tu parte ya está pagada.", myShareHint(paid, "Ana"))
    }

    @Test
    fun `sin pedidos no hay texto de parte`() {
        val none = SharedTableAmounts(BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("0.00"))
        assertNull(myShareHint(none, "Ana"))
    }

    @Test
    fun `el espacio del QR da los nombres de los demás, sin el propio`() {
        val raw =
            """
            {"data":{"espacio":{"id":4,"nombre":"Mesa 4","tipo":"mesa"},"sesion_id":"s1",
             "participantes":[{"id":"a","alias":"Luis","soy_yo":false},{"id":"b","alias":"Kao","soy_yo":true},
              {"id":"c","alias":"Mariana","soy_yo":false}],"yo":{"id":"b","alias":"Kao"}},"error":null}
            """.trimIndent()
        assertEquals(listOf("Luis", "Mariana"), json.parseOthersAtSpace(raw))
    }

    @Test
    fun `la invitación nombra a dos y resume a los demás`() {
        assertNull(othersAtTableLine(emptyList(), "la mesa"))
        assertEquals("Luis ya está en la mesa.", othersAtTableLine(listOf("Luis"), "la mesa"))
        assertEquals("Luis y Mariana ya están en la mesa.", othersAtTableLine(listOf("Luis", "Mariana"), "la mesa"))
        assertEquals(
            "Luis, Mariana y alguien más ya están en la mesa.",
            othersAtTableLine(listOf("Luis", "Mariana", "Andrés"), "la mesa"),
        )
        assertEquals("Luis, Mariana y 2 más ya están en la barra.", othersAtTableLine(listOf("Luis", "Mariana", "A", "B"), "la barra"))
    }
}
