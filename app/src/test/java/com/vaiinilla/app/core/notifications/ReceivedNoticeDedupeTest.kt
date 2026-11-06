package com.vaiinilla.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** En vivo, W04: el avance que arma la app y el push del servidor son un solo aviso. */
class ReceivedNoticeDedupeTest {
    private fun notice(
        id: String,
        at: Long,
        orderId: String = "o-1",
        state: String = "listo",
        remote: Boolean,
        arrival: ReceivedNotice.Arrival = ReceivedNotice.Arrival.SHOWN,
    ) = ReceivedNotice(
        id = id,
        receivedAtMillis = at,
        title = "Pedido #1",
        body = "",
        payload = mapOf("type" to "order_state", "orderId" to orderId, "state" to state),
        isRemote = remote,
        arrivals = listOf(arrival),
    )

    @Test
    fun `el aviso de la app y el push del mismo avance quedan en uno`() {
        val app = notice("app:o-1:listo", at = 1_000, remote = false)
        val push = notice("0:fcm", at = 4_000, remote = true, arrival = ReceivedNotice.Arrival.BACKGROUND)
        val kept = ReceivedNoticeLog.dedupe(listOf(push, app)).single()
        assertTrue(kept.isRemote)
        assertEquals("0:fcm", kept.id)
        assertEquals(1_000, kept.receivedAtMillis)
        assertEquals(
            setOf(ReceivedNotice.Arrival.SHOWN, ReceivedNotice.Arrival.BACKGROUND),
            kept.arrivals.toSet(),
        )
    }

    @Test
    fun `otro estado, otro pedido o mucho despues no se juntan`() {
        val base = notice("a", at = 0, remote = true)
        assertFalse(ReceivedNoticeLog.sameEvent(base, notice("b", at = 0, state = "entregado", remote = true)))
        assertFalse(ReceivedNoticeLog.sameEvent(base, notice("c", at = 0, orderId = "o-6", remote = true)))
        assertFalse(ReceivedNoticeLog.sameEvent(base, notice("d", at = 60 * 60 * 1000L, remote = true)))
        assertTrue(ReceivedNoticeLog.sameEvent(base, notice("e", at = 30_000, remote = true)))
        val others =
            listOf(
                base,
                notice("f", at = 0, orderId = "o-6", remote = true),
                notice("g", at = 0, state = "entregado", remote = true),
            )
        assertEquals(3, ReceivedNoticeLog.dedupe(others).size)
    }
}
