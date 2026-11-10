package com.vaiinilla.app.ui.notices

import com.vaiinilla.app.core.notifications.NoticeExplanation
import com.vaiinilla.app.core.notifications.NoticeLookup
import com.vaiinilla.app.core.notifications.NoticeMembership
import com.vaiinilla.app.core.notifications.NoticeOrderLookup
import com.vaiinilla.app.core.notifications.NoticeOrderSummary
import com.vaiinilla.app.core.notifications.NoticePhoneContext
import com.vaiinilla.app.core.notifications.ReceivedNotice
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** AVISOS-02: pendientes, negocio de cada aviso, chips, orden, fechas y búsqueda sin tope de modos. */
class NoticePanelTest {
    private val zone = ZoneId.of("America/Chihuahua")

    private fun found(
        business: String,
        state: String,
    ) = NoticeLookup.Found(
        NoticeOrderSummary(
            establishmentName = business,
            readAs = "Caja en $business",
            folio = 10,
            status = state,
            customerName = "Ana",
            total = "$95",
            paymentMethod = "Efectivo",
            destination = "Para llevar",
            space = null,
            items = emptyList(),
            createdAt = "2026-10-31T12:00:00Z",
            stateWire = state,
        ),
        checkedAtMillis = 0,
    )

    private fun notice(
        id: String,
        at: Long,
        alert: String = "por_cobrar",
        lookup: NoticeLookup? = null,
        establishmentId: String? = null,
        phone: NoticePhoneContext? = null,
    ) = ReceivedNotice(
        id = id,
        receivedAtMillis = at,
        title = "Pedido",
        body = "",
        payload =
            buildMap {
                put("type", "staff_alert")
                put("alert", alert)
                put("orderId", "p-$id")
                establishmentId?.let { put("establecimientoId", it) }
            },
        isRemote = true,
        arrivals = listOf(ReceivedNotice.Arrival.BACKGROUND),
        phone = phone,
        lookup = lookup,
    )

    @Test
    fun pendingFollowsTheAlertAndTheOrderState() {
        fun explain(alert: String) =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf(
                    "type" to "staff_alert",
                    "alert" to alert,
                ),
            )

        assertTrue(explain("por_cobrar").isPending(found("USAGI", "por_cobrar")))
        assertFalse(explain("por_cobrar").isPending(found("USAGI", "listo")))
        assertTrue(explain("nueva_comanda").isPending(found("USAGI", "preparando")))
        assertTrue(explain("listo_mesero").isPending(found("USAGI", "listo")))
        assertFalse(explain("listo_mesero").isPending(found("USAGI", "entregado")))
        assertTrue(explain("pedido_domicilio").isPending(found("USAGI", "en_camino")))
        assertFalse(explain("pedido_domicilio").isPending(found("USAGI", "cancelado")))
        // Sin el pedido leído no se sabe, y un aviso a una persona nunca es pendiente.
        assertFalse(explain("por_cobrar").isPending(null))
        assertFalse(
            NoticeExplanation
                .explain(VenueContext.STANDARD, mapOf("type" to "order_state"))
                .isPending(found("USAGI", "por_cobrar")),
        )
    }

    @Test
    fun pendingGoFirstThenNewest() {
        val old = notice("a", at = 1, lookup = found("USAGI", "por_cobrar"))
        val newest = notice("b", at = 3, lookup = found("USAGI", "entregado"))
        val middle = notice("c", at = 2)
        assertEquals(listOf("a", "b", "c"), NoticePanel.ordered(listOf(middle, newest, old)).map { it.id })
    }

    @Test
    fun businessComesFromTheOrderOrFromTheModes() {
        val phone =
            NoticePhoneContext(
                account = "kao",
                openMode = "Mesero en USAGI",
                memberships = listOf(NoticeMembership("m1", OperationalRole.CASHIER, "est-michi", "Michi Market")),
            )
        assertEquals("USAGI", NoticePanel.business(notice("a", 1, lookup = found("USAGI", "listo"))))
        assertEquals("Michi Market", NoticePanel.business(notice("b", 1, establishmentId = "est-michi", phone = phone)))
        assertNull(NoticePanel.business(notice("c", 1)))
    }

    @Test
    fun businessAndCustomerFromThePushWhenTheServerSendsThem() {
        val fromPush =
            notice("a", 1).let {
                it.copy(payload = it.payload + mapOf("establecimientoNombre" to "USAGI", "clienteNombre" to "Ana"))
            }
        assertEquals("USAGI", NoticePanel.business(fromPush))
        assertEquals("Ana", NoticePanel.customer(fromPush))
        assertNull(NoticePanel.customer(notice("b", 1)))
    }

    @Test
    fun chipsCountByBusinessAndFilter() {
        val notices =
            listOf(
                notice("a", 1, lookup = found("USAGI", "listo")),
                notice("b", 2, lookup = found("USAGI", "listo")),
                notice("c", 3, lookup = found("Michi Market", "listo")),
                notice("d", 4),
            )
        assertEquals(listOf("USAGI" to 2, "Michi Market" to 1), NoticePanel.chips(notices))
        assertEquals(listOf("c"), NoticePanel.filter(notices, "Michi Market").map { it.id })
        assertEquals(4, NoticePanel.filter(notices, null).size)
    }

    @Test
    fun datesAreInSpanish() {
        val today = LocalDate.of(2026, 10, 31)

        fun at(
            day: Int,
            month: Int = 10,
            year: Int = 2026,
        ) = ZonedDateTime.of(year, month, day, 14, 20, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals("Hoy, 14:20", NoticePanel.date(at(31), zone, today))
        assertEquals("Ayer, 14:20", NoticePanel.date(at(30), zone, today))
        assertEquals("28 de octubre, 14:20", NoticePanel.date(at(28), zone, today))
        assertEquals("2 de diciembre de 2025, 14:20", NoticePanel.date(at(2, 12, 2025), zone, today))
    }

    @Test
    fun rowDatesAreShort() {
        val today = LocalDate.of(2026, 11, 3)

        fun at(
            day: Int,
            month: Int = 11,
            hour: Int = 9,
        ) = ZonedDateTime.of(2026, month, day, hour, 40, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals("9:40", NoticePanel.shortDate(at(3), zone, today))
        assertEquals("Ayer", NoticePanel.shortDate(at(2), zone, today))
        assertEquals("Sáb 31", NoticePanel.shortDate(at(31, 10), zone, today))
        assertEquals("Mié 28", NoticePanel.shortDate(at(28, 10), zone, today))
        assertEquals("27 oct", NoticePanel.shortDate(at(27, 10), zone, today))
    }

    @Test
    fun candidatesKeepEveryCashierMode() {
        val modes =
            (1..8).map {
                AuthorizedMode(OperationalRole.CASHIER, "est-$it", "Negocio $it", membershipId = "m$it")
            }
        val candidates = NoticeOrderLookup.candidates(listOf(OperationalRole.CASHIER), null, modes)
        assertEquals(8, candidates.size)
    }
}
