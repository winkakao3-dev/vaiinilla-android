package com.vaiinilla.app.ui.components

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.screens.quickCashAmounts
import com.vaiinilla.app.ui.screens.remainingText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class SpaceMotionTest {
    @Test
    fun `the clock keeps minutes and seconds under an hour and adds hours above`() {
        assertEquals("00:00", clockText(0))
        assertEquals("00:00", clockText(-5_000))
        assertEquals("00:45", clockText(45_000))
        assertEquals("19:59", clockText((19 * 60 + 59) * 1_000L))
        assertEquals("1:05:10", clockText((3_600 + 5 * 60 + 10) * 1_000L))
    }

    @Test
    fun `remaining text is short and rounds minutes up`() {
        assertEquals("0:00", remainingText(0))
        assertEquals("0:45", remainingText(45_000))
        assertEquals("1 min", remainingText(1_000 * 60L))
        assertEquals("43 min", remainingText((42 * 60 + 10) * 1_000L))
        assertEquals("1 h 05 min", remainingText((3_600 + 5 * 60) * 1_000L))
    }

    @Test
    fun `turn progress is how much of the turn already passed`() {
        val now = Instant.now()
        val fraction = turnFraction(now.minusSeconds(1_800).toString(), now.plusSeconds(1_800).toString())
        assertNotNull(fraction)
        assertEquals(0.5f, fraction!!, 0.02f)
        assertEquals(1f, turnFraction(now.minusSeconds(7_200).toString(), now.minusSeconds(3_600).toString())!!, 0f)
        assertEquals(0f, turnFraction(now.plusSeconds(600).toString(), now.plusSeconds(4_200).toString())!!, 0f)
    }

    @Test
    fun `turn progress is unknown without a start, an end or a valid turn`() {
        val now = Instant.now().toString()
        assertNull(turnFraction(null, now))
        assertNull(turnFraction(now, null))
        assertNull(turnFraction("no es una fecha", now))
        assertNull(turnFraction(now, now))
    }

    @Test
    fun `millis until is negative for a past moment and null for garbage`() {
        assertTrue(millisUntil(Instant.now().minusSeconds(60).toString())!! < 0)
        assertTrue(millisUntil(Instant.now().plusSeconds(60).toString())!! > 0)
        assertNull(millisUntil(null))
        assertNull(millisUntil("x"))
    }

    @Test
    fun `quick cash offers the next round bills above the total`() {
        assertEquals(listOf(250, 300, 400), quickCashAmounts(BigDecimal("220.00")).map { it.toInt() })
        assertEquals(listOf(50, 60, 100), quickCashAmounts(BigDecimal("42.50")).map { it.toInt() })
        assertEquals(listOf(200, 500, 1000), quickCashAmounts(BigDecimal("180.00")).map { it.toInt() })
    }

    @Test
    fun `quick cash never offers the exact total or less`() {
        assertTrue(quickCashAmounts(BigDecimal("100.00")).all { it > BigDecimal("100.00") })
        assertTrue(
            quickCashAmounts(BigDecimal("0.00")).isEmpty() ||
                quickCashAmounts(BigDecimal("0.00")).all { it.signum() > 0 },
        )
    }

    private fun detail(
        state: OrderState = OrderState.PAID,
        payAtEnd: Boolean = false,
        pending: Boolean = false,
        destination: OrderDestination = OrderDestination.IN_SPACE,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "o-1",
                folio = 1,
                operationalDate = "2026-09-29",
                state = state,
                paymentMethod = PaymentMethod.CASH,
                destination = destination,
                space = null,
                subtotal = "10.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "10.00",
                version = 1,
                createdAt = "2026-09-29T20:00:00Z",
                updatedAt = "2026-09-29T20:00:00Z",
                payAtEnd = payAtEnd,
                paymentPending = pending,
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
    )

    @Test
    fun `an order to the account says it is paid at the end, then that the account was paid`() {
        assertEquals("Efectivo", orderPaymentLabel(detail().summary))
        assertEquals("Pagar al final", orderPaymentLabel(detail(payAtEnd = true, pending = true).summary))
        assertEquals("Cuenta pagada", orderPaymentLabel(detail(payAtEnd = true, pending = false).summary))
    }

    @Test
    fun `an order to the account is received, not charged`() {
        assertEquals("Cobrado", orderStateLabel(detail().summary))
        assertEquals("Recibido", orderStateLabel(detail(payAtEnd = true, pending = true).summary))
        assertEquals("Preparando", orderStateLabel(detail(state = OrderState.PREPARING, payAtEnd = true).summary))
    }

    @Test
    fun `the customer shows a QR for take away and, in a space, only when the establishment asks`() {
        assertTrue(detail(destination = OrderDestination.TAKE_AWAY).needsPickupQr(deliveryRequiresQr = false))
        assertTrue(detail().needsPickupQr(deliveryRequiresQr = true))
        assertFalse(detail().needsPickupQr(deliveryRequiresQr = false))
    }
}
