package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashierDayHelpersTest {
    @Test
    fun `unpaid age reads like the web panel`() {
        assertEquals("hace menos de 1 h", unpaidAgeLabel(0))
        assertEquals("hace 1 h", unpaidAgeLabel(1))
        assertEquals("hace 5 h", unpaidAgeLabel(5))
    }

    @Test
    fun `unpaid summary counts accounts and people who left without paying`() {
        assertEquals("\$330 en 2 cuentas", unpaidSummary("330.00", 2, 0))
        assertEquals("\$90 en 1 cuenta · 1 se fue sin pagar", unpaidSummary("90.00", 1, 1))
        assertEquals("\$330 en 3 cuentas · 2 se fueron sin pagar", unpaidSummary("330.00", 3, 2))
    }

    @Test
    fun `booking status uses the Caja labels from the web`() {
        assertEquals("Por pagar en efectivo", bookingStatusLabel(ReservationState.PENDING_PAYMENT))
        assertEquals("Pagada", bookingStatusLabel(ReservationState.CONFIRMED))
        assertEquals("Pagada", bookingStatusLabel(ReservationState.IN_PROGRESS))
        assertEquals("Pagada", bookingStatusLabel(ReservationState.FINISHED))
        assertEquals("Expirada", bookingStatusLabel(ReservationState.EXPIRED))
        assertEquals("Con conflicto", bookingStatusLabel(ReservationState.CONFLICT))
        assertEquals("Cancelada", bookingStatusLabel(ReservationState.CANCELLED))
        assertFalse(isBookingVisible(ReservationState.CANCELLED))
        assertTrue(isBookingVisible(ReservationState.PENDING_PAYMENT))
    }

    @Test
    fun `received cash is normalized to two decimals and rejects non amounts`() {
        assertEquals("350.00", normalizeReceived("350"))
        assertEquals("350.50", normalizeReceived("350,5"))
        assertEquals("12.34", normalizeReceived(" 12.34 "))
        assertNull(normalizeReceived(""))
        assertNull(normalizeReceived("abc"))
        assertNull(normalizeReceived("1.234"))
    }

    @Test
    fun `change is only offered when the cash covers the amount`() {
        assertEquals("50.00", cashChange("200", "150.00")?.toPlainString())
        assertEquals("0.00", cashChange("150", "150.00")?.toPlainString())
        assertNull(cashChange("100", "150.00"))
        assertNull(cashChange("", "150.00"))
    }

    @Test
    fun `a 403 gets the role message and other failures keep their own message`() {
        val forbidden =
            ReservationException(
                "FORBIDDEN_ROLE",
                "Rol sin permiso.",
                ApiClientException("FORBIDDEN_ROLE", "Rol sin permiso.", 403),
            )
        assertEquals(BOOKINGS_LIST_FORBIDDEN, bookingFailure(forbidden, BOOKINGS_LIST_FORBIDDEN))
        val other = IllegalStateException("Mensaje propio.")
        assertEquals("Mensaje propio.", bookingFailure(other, BOOKINGS_LIST_FORBIDDEN))
    }
}
