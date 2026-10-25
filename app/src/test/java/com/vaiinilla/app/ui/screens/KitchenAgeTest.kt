package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class KitchenAgeTest {
    private fun minutes(value: Int) = value * 60_000L

    @Test
    fun `el color cambia a los 10 y a los 20 minutos`() {
        assertEquals(KitchenAge.FRESH, kitchenAge(minutes(9) + 59_000))
        assertEquals(KitchenAge.WAITING, kitchenAge(minutes(10)))
        assertEquals(KitchenAge.WAITING, kitchenAge(minutes(19) + 59_000))
        assertEquals(KitchenAge.LATE, kitchenAge(minutes(20)))
    }

    @Test
    fun `se lee en minutos y pasada una hora dice mas de una hora`() {
        assertEquals("0 min", kitchenAgeLabel(42_000))
        assertEquals("7 min", kitchenAgeLabel(minutes(7) + 30_000))
        assertEquals("59 min", kitchenAgeLabel(minutes(59) + 59_000))
        assertEquals("+1 h", kitchenAgeLabel(minutes(60)))
        assertEquals("+1 h", kitchenAgeLabel(minutes(1127)))
        assertEquals("0 min", kitchenAgeLabel(-5_000))
    }

    @Test
    fun `un apartado cuenta desde el inicio de su franja y uno para ahora desde que se creo`() {
        val zone = ZoneId.of("America/Mexico_City")
        assertEquals("2026-10-27T18:00:00Z", kitchenAgeStart(summary("2026-10-27", "12:00-13:00"), zone))
        assertEquals("2026-10-25T00:00:00Z", kitchenAgeStart(summary(null, null), zone))
        // Si la franja ya había empezado cuando se creó, cuenta desde la creación.
        assertEquals("2026-10-25T00:00:00Z", kitchenAgeStart(summary("2026-10-24", "08:00-09:00"), zone))
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
