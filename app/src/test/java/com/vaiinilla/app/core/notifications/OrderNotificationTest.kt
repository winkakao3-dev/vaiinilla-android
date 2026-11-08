package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class OrderNotificationTest {
    private val venue = VenueContext.STANDARD

    private fun jwt(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("{}".toByteArray(Charsets.UTF_8))
        val payload = encoder.encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        return "$header.$payload.signature"
    }

    private fun contextPayload(
        uid: String = "uid-1",
        role: String = "caja",
        establishment: String = "est-1",
        membership: String = "mem-1",
        exp: Int = 1,
    ): String =
        """{"uid":"$uid","rol":"$role","establecimiento_id":"$establishment","membresia_id":"$membership","exp":$exp}"""

    @Test
    fun `legacy notification intents without target default to client`() {
        assertEquals(OrderNotificationTarget.CLIENT, OrderNotificationTarget.fromWireValue(null))
        assertEquals(OrderNotificationTarget.CLIENT, OrderNotificationTarget.fromWireValue("bogus"))
        assertEquals(OrderNotificationTarget.CLIENT, OrderNotificationTarget.fromWireValue("client"))
        assertEquals(OrderNotificationTarget.STAFF, OrderNotificationTarget.fromWireValue("staff"))
        assertEquals(OrderNotificationTarget.WAITER_CALL, OrderNotificationTarget.fromWireValue("waiter_call"))
    }

    @Test
    fun `request codes differ per order and per target`() {
        val clientA =
            OrderAdvanceNotifier.notificationRequestCode("order-1", OrderNotificationTarget.CLIENT)
        val clientB =
            OrderAdvanceNotifier.notificationRequestCode("order-2", OrderNotificationTarget.CLIENT)
        val staffA =
            OrderAdvanceNotifier.notificationRequestCode("order-1", OrderNotificationTarget.STAFF)
        assertNotEquals(clientA, clientB)
        assertNotEquals(clientA, staffA)
        assertEquals(
            clientA,
            OrderAdvanceNotifier.notificationRequestCode("order-1", OrderNotificationTarget.CLIENT),
        )
    }

    @Test
    fun `registration marker is stable across token refresh for the same context`() {
        val first = jwt(contextPayload(exp = 1))
        val refreshed = jwt(contextPayload(exp = 2))
        assertNotEquals(first, refreshed)
        assertEquals(
            deviceRegistrationMarker(first, "fcm-1"),
            deviceRegistrationMarker(refreshed, "fcm-1"),
        )
    }

    @Test
    fun `registration marker differs across account role establishment membership and device`() {
        val base = jwt(contextPayload())
        val baseMarker = deviceRegistrationMarker(base, "fcm-1")
        assertNotEquals(
            baseMarker,
            deviceRegistrationMarker(jwt(contextPayload(uid = "uid-2")), "fcm-1"),
        )
        assertNotEquals(
            baseMarker,
            deviceRegistrationMarker(jwt(contextPayload(role = "cliente")), "fcm-1"),
        )
        assertNotEquals(
            baseMarker,
            deviceRegistrationMarker(jwt(contextPayload(establishment = "est-2")), "fcm-1"),
        )
        assertNotEquals(
            baseMarker,
            deviceRegistrationMarker(jwt(contextPayload(membership = "mem-2")), "fcm-1"),
        )
        assertNotEquals(baseMarker, deviceRegistrationMarker(base, "fcm-2"))
    }

    @Test
    fun `table call notification body follows the reason`() {
        assertEquals("Pide cubiertos o servilletas", OrderAdvanceNotifier.tableCallText("utensilios"))
        assertEquals("Algo está mal con su pedido", OrderAdvanceNotifier.tableCallText("problema"))
        assertEquals("Necesita atención", OrderAdvanceNotifier.tableCallText("atencion"))
        assertEquals("Necesita atención", OrderAdvanceNotifier.tableCallText(null))
        // K02: pedir la cuenta es un motivo propio, no «Necesita atención».
        assertEquals("Pide la cuenta", OrderAdvanceNotifier.tableCallText("cuenta"))
        assertEquals("Necesita atención", OrderAdvanceNotifier.tableCallText("otro"))
    }

    @Test
    fun `turn ending notification says how long is left or asks to collect`() {
        assertEquals("Cancha 1: turno por terminar", OrderAdvanceNotifier.turnEndingTitle("Cancha 1"))
        assertEquals("Un turno está por terminar", OrderAdvanceNotifier.turnEndingTitle(" "))
        assertEquals(
            "Termina en 4 min. Ve preparando la cuenta.",
            OrderAdvanceNotifier.turnEndingText(4),
        )
        assertEquals(
            "Ya terminó. Cobra la cuenta y libera el espacio.",
            OrderAdvanceNotifier.turnEndingText(0),
        )
    }

    @Test
    fun `ready push names the place, the bar or home delivery`() {
        assertEquals(
            "Tu pedido está listo. En un momento te lo llevan a tu cancha.",
            OrderAdvanceNotifier.messageFor(venue, OrderState.READY, inSpace = true, spaceType = "cancha"),
        )
        // «Ya está en tu mesa» solo cuando el mesero lo entrega.
        assertEquals(
            "Ya está en tu mesa. Buen provecho.",
            OrderAdvanceNotifier.messageFor(venue, OrderState.DELIVERED, inSpace = true, spaceType = "mesa"),
        )
        assertEquals(
            "Pedido entregado. Buen provecho.",
            OrderAdvanceNotifier.messageFor(
                venue,
                OrderState.DELIVERED,
                inSpace = true,
                spaceType = "mesa",
                pickUpAtBar = true,
            ),
        )
        assertEquals("Pedido entregado. Buen provecho.", OrderAdvanceNotifier.messageFor(venue, OrderState.DELIVERED))
        assertEquals(
            OrderAdvanceNotifier.PICK_UP_AT_BAR_TEXT,
            OrderAdvanceNotifier.messageFor(
                venue,
                OrderState.READY,
                inSpace = true,
                spaceType = "cancha",
                pickUpAtBar = true,
            ),
        )
        assertEquals(
            "Tu pedido está listo para salir.",
            OrderAdvanceNotifier.messageFor(venue, OrderState.READY, homeDelivery = true),
        )
        assertEquals(
            "Tu pedido está listo. Recógelo en la barra.",
            OrderAdvanceNotifier.messageFor(venue, OrderState.READY),
        )
    }

    @Test
    fun `an order at a table is not announced as paid`() {
        // K01: un pedido a la cuenta llega cobrado sin pagarse; en un lugar no se dice «Pago confirmado».
        assertEquals(
            "Cocina recibió tu comanda.",
            OrderAdvanceNotifier.messageFor(venue, OrderState.PAID, inSpace = true),
        )
        assertEquals(
            "Pago confirmado. Cocina recibió tu comanda.",
            OrderAdvanceNotifier.messageFor(venue, OrderState.PAID),
        )
    }

    @Test
    fun `rental push talks about the schedule, not a delivered order`() {
        assertEquals(
            "Tu horario de Pista 1 está confirmado.",
            OrderAdvanceNotifier.rentalMessageFor(OrderState.DELIVERED, "Pista 1"),
        )
        assertEquals("Tu horario está confirmado.", OrderAdvanceNotifier.rentalMessageFor(OrderState.DELIVERED, " "))
        assertEquals(
            "Se canceló tu renta de Pista 1.",
            OrderAdvanceNotifier.rentalMessageFor(OrderState.CANCELED, "Pista 1"),
        )
        assertNull(OrderAdvanceNotifier.rentalMessageFor(OrderState.PAID, "Pista 1"))
    }

    @Test
    fun `waiter ready notice speaks of the customer, never of a student`() {
        val (title, body) = OrderAdvanceNotifier.staffAlertText(venue, folio = 12, alert = "listo_mesero")
        assertEquals("Pedido #12 listo", title)
        assertEquals("Llévaselo al cliente a su lugar.", body)
        assertEquals(
            "Llévaselo a Ana a su lugar.",
            OrderAdvanceNotifier
                .staffAlertText(
                    venue,
                    folio = 12,
                    alert = "listo_mesero",
                    customerName = " Ana ",
                ).second,
        )
        assertEquals(
            "Llévaselo al cliente a su lugar.",
            OrderAdvanceNotifier.staffAlertText(venue, folio = 12, alert = "listo_mesero", customerName = "  ").second,
        )
    }
}
