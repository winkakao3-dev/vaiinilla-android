package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.ui.screens.TableClaim
import com.vaiinilla.app.ui.screens.myShareHint
import com.vaiinilla.app.ui.screens.tableOrderClaim
import com.vaiinilla.app.ui.screens.tableOrderStateLabel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
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
    fun `lee la sesion del QR con sus participantes y quien soy`() {
        val session =
            json.parseSession(
                """
                {"data":{"espacio":{"id":3,"nombre":"Mesa 3","tipo":"mesa"},"sesion_id":12,
                 "participantes":[{"id":"p-1","alias":"Luis","soy_yo":false},{"id":"p-2","alias":"Ana","soy_yo":true}],
                 "yo":{"id":"p-2","alias":"Ana"}},"error":null}
                """.trimIndent(),
            )
        assertEquals(3, session.spaceId)
        assertEquals("Mesa 3", session.spaceName)
        assertEquals(12L, session.sessionId)
        assertEquals(listOf("Luis", "Ana"), session.participants.map { it.alias })
        assertEquals(false, session.participants.first().isMe)
        assertEquals("p-2", session.me?.id)
        assertTrue(session.me?.isMe == true)
    }

    @Test
    fun `sin yo la sesion no tiene quien soy todavia`() {
        val session =
            json.parseSession(
                """
                {"data":{"espacio":{"id":3,"nombre":"Mesa 3","tipo":"mesa"},"sesion_id":12,
                 "participantes":[],"yo":null},"error":null}
                """.trimIndent(),
            )
        assertNull(session.me)
        assertTrue(session.participants.isEmpty())
    }

    @Test
    fun `continuar como codifica el token y el participante`() {
        val element = Json.parseToJsonElement(json.encodeContinue("qr-tok", "p-2"))
        val sent = element.jsonObject
        assertEquals("qr-tok", (sent.getValue("token") as JsonPrimitive).content)
        assertEquals("p-2", (sent.getValue("participante_id") as JsonPrimitive).content)
    }
}
