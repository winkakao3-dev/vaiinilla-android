package com.vaiinilla.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El aviso de un hijo: lo que trae el push y a dónde lleva. */
class ChildDeepLinkTest {
    private val school = "5b0d0000-0000-4000-8000-0000000000e1"

    @Test
    fun `los tres avisos del hijo traen a quien abrir y de qué escuela`() {
        val order =
            mapOf(
                "type" to "order_state",
                "orderId" to "order-9",
                "folio" to "53",
                "state" to "listo",
                "dependienteUsuarioId" to "child-1",
                "establecimientoId" to school,
            )
        assertEquals(ChildDeepLink("child-1", school), ChildDeepLink.fromPush(order))
        val lowBalance =
            mapOf(
                "type" to "saldo_bajo",
                "dependienteUsuarioId" to "child-1",
                "establecimientoId" to school,
                "saldo" to "12.00",
            )
        assertEquals(ChildDeepLink("child-1", school), ChildDeepLink.fromPush(lowBalance))
        // Un servidor anterior no manda la escuela: se queda en la actual.
        val reload = mapOf("type" to "recarga_acreditada", "dependienteUsuarioId" to "child-1", "saldo" to "90.00")
        assertEquals(ChildDeepLink("child-1", null), ChildDeepLink.fromPush(reload))
    }

    @Test
    fun `un pedido propio o un id raro no es aviso de hijo`() {
        assertNull(ChildDeepLink.fromPush(mapOf("type" to "order_state", "orderId" to "order-9")))
        assertNull(ChildDeepLink.fromPush(mapOf("dependienteUsuarioId" to "../../x")))
        assertEquals(
            ChildDeepLink("child-1", null),
            ChildDeepLink.fromPush(mapOf("dependienteUsuarioId" to "child-1", "establecimientoId" to "a b")),
        )
    }

    @Test
    fun `en su escuela abre Cartera y desde otra primero cambia`() {
        val link = ChildDeepLink("child-1", school)
        assertEquals(ChildNoticeStep.OpenWallet, ChildNoticeStep.of(link, school))
        assertEquals(ChildNoticeStep.SwitchSchool(school), ChildNoticeStep.of(link, "otra"))
        assertEquals(ChildNoticeStep.SwitchSchool(school), ChildNoticeStep.of(link, null))
        assertEquals(ChildNoticeStep.OpenWallet, ChildNoticeStep.of(ChildDeepLink("child-1", null), "otra"))
    }
}
