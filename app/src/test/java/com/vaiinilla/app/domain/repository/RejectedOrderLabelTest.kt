package com.vaiinilla.app.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/** Quién canceló un pedido de la mesa (R28): el mesero no reclama a Cocina lo que canceló el cliente. */
class RejectedOrderLabelTest {
    private fun rejected(role: String?) = RejectedOrder(id = "p1", folio = 7, reason = null, byRole = role)

    @Test
    fun `cada rol se anuncia con su nombre`() {
        assertEquals("Caja rechazó", rejected("cajero").let { "${it.byLabel} ${it.verb}" })
        assertEquals("Cocina rechazó", rejected("cocina").let { "${it.byLabel} ${it.verb}" })
        assertEquals("Administración rechazó", rejected("admin").let { "${it.byLabel} ${it.verb}" })
        assertEquals("El cliente canceló", rejected("cliente").let { "${it.byLabel} ${it.verb}" })
        assertEquals("El sistema canceló", rejected(null).let { "${it.byLabel} ${it.verb}" })
    }
}
