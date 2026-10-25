package com.vaiinilla.app.ui.components

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** PEDIDO-15: un pedido apartado para otro día dice cuándo es. */
class ScheduledDeliveryLabelTest {
    private val es = Locale("es", "MX")

    @Test
    fun `an order for later shows its day and time slot`() {
        val label = scheduledDeliveryLabel(summary("2026-10-27", "12:00-13:00"), es)

        assertEquals(true, label?.startsWith("Para el "))
        assertEquals(true, label?.contains("27"))
        assertEquals(true, label?.endsWith("· 12:00-13:00"))
    }

    @Test
    fun `an order for now shows nothing`() {
        assertNull(scheduledDeliveryLabel(summary(null, null), es))
    }

    @Test
    fun `a day without a slot still shows the day`() {
        assertEquals(false, scheduledDeliveryLabel(summary("2026-10-27", null), es)?.contains("·"))
    }

    private fun summary(
        date: String?,
        slot: String?,
    ) = OrderSummary(
        id = "1",
        folio = 1,
        operationalDate = "2026-10-25",
        state = OrderState.PAID,
        paymentMethod = PaymentMethod.BALANCE,
        destination = OrderDestination.TAKE_AWAY,
        space = null,
        subtotal = "1.00",
        combinedSavings = "0.00",
        cashbackAwarded = "0.00",
        total = "1.00",
        version = 1,
        createdAt = "2026-10-25T00:00:00Z",
        updatedAt = "2026-10-25T00:00:00Z",
        deliveryDate = date,
        deliverySlot = slot,
    )
}
