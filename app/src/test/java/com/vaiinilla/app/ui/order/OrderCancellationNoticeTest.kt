package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** H04: el cliente lee quién rechazó su pedido y por qué, en la lista y en la pantalla de recogida. */
class OrderCancellationNoticeTest {
    @Test
    fun `Cocina rechaza con motivo y el cliente lo lee`() {
        val notice = orderCancellationNotice(order(OrderState.CANCELED, role = "cocina", reason = "  Se acabó el pan "))
        assertEquals("Cocina canceló tu pedido", notice?.title)
        assertEquals("Se acabó el pan", notice?.reason)
        assertEquals("El importe regresó a tu saldo Vaiinilla.", notice?.note)
    }

    @Test
    fun `sin quién ni motivo dice solo que se canceló`() {
        val notice = orderCancellationNotice(order(OrderState.CANCELED, role = "cliente", reason = " "))
        assertEquals("Pedido cancelado", notice?.title)
        assertNull(notice?.reason)
    }

    @Test
    fun `un pedido a la cuenta avisa que no se cobra`() {
        val notice =
            orderCancellationNotice(order(OrderState.CANCELED, role = "cajero", reason = "Error", payAtEnd = true))
        assertEquals("Caja canceló tu pedido", notice?.title)
        assertEquals("No se te cobrará: este pedido iba a tu cuenta.", notice?.note)
    }

    @Test
    fun `un pedido vivo no tiene aviso`() {
        assertNull(orderCancellationNotice(order(OrderState.PREPARING, role = null, reason = null)))
    }

    @Test
    fun `la pantalla de recogida dice el motivo del rechazo`() {
        assertEquals(
            "Cocina canceló tu pedido: Se acabó el pan",
            pickupHint(order(OrderState.CANCELED, role = "cocina", reason = "Se acabó el pan")),
        )
        assertEquals("Este pedido se canceló.", pickupHint(order(OrderState.CANCELED, role = "cocina", reason = null)))
    }

    private fun order(
        state: OrderState,
        role: String?,
        reason: String?,
        payAtEnd: Boolean = false,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "1",
                folio = 42,
                operationalDate = "2026-10-11",
                state = state,
                paymentMethod = PaymentMethod.BALANCE,
                destination = OrderDestination.TAKE_AWAY,
                space = null,
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-10-11T00:00:00Z",
                updatedAt = "2026-10-11T00:00:00Z",
                payAtEnd = payAtEnd,
                cancelReason = reason,
                canceledByRole = role,
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
        pickupToken = "VN-42",
    )
}
