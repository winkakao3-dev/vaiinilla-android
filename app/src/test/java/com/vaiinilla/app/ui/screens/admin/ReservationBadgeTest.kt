package com.vaiinilla.app.ui.screens.admin

import com.vaiinilla.app.domain.model.ReservationState
import org.junit.Assert.assertEquals
import org.junit.Test

/** R27: un apartado vencido o en conflicto no sale como «Pagada». */
class ReservationBadgeTest {
    @Test
    fun `each reservation state gets its own badge`() {
        assertEquals("Por cobrar" to AdminBadgeTone.WARN, reservationBadge(ReservationState.PENDING_PAYMENT))
        assertEquals("Pagada" to AdminBadgeTone.OK, reservationBadge(ReservationState.CONFIRMED))
        assertEquals("Pagada" to AdminBadgeTone.OK, reservationBadge(ReservationState.IN_PROGRESS))
        assertEquals("Pagada" to AdminBadgeTone.OK, reservationBadge(ReservationState.FINISHED))
        assertEquals("Vencida" to AdminBadgeTone.NEUTRAL, reservationBadge(ReservationState.EXPIRED))
        assertEquals("Revisar" to AdminBadgeTone.DANGER, reservationBadge(ReservationState.CONFLICT))
        assertEquals("Cancelada" to AdminBadgeTone.NEUTRAL, reservationBadge(ReservationState.CANCELLED))
    }
}
