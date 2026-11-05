package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #18: Caja entra a Mesas desde su pantalla y el orbe abre el Manual de Caja, no el de Mesero. */
class TablesBoardManualTest {
    @Test
    fun `el mesero ve sus guias`() {
        val guides = tablesBoardAssistantGuides(OperationalRole.WAITER)
        assertTrue(guides.isNotEmpty())
        assertTrue(guides.all { it.role == OperationalRole.WAITER })
    }

    @Test
    fun `caja ve solo guias de caja, sin practica y sin ids repetidos`() {
        val guides = tablesBoardAssistantGuides(OperationalRole.CASHIER)
        assertTrue(guides.all { it.role == OperationalRole.CASHIER })
        assertTrue(guides.none { it.available })
        assertTrue(guides.any { it.id == "abrir-caja" })
        assertTrue(guides.any { it.id == "caja-atender-llamada" })
        assertEquals(guides.size, guides.map { it.id }.toSet().size)
    }
}
