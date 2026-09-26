package com.vaiinilla.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Base64

class OrderNotificationTest {
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
    }
}
