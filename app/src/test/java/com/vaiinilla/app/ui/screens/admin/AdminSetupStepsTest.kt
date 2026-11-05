package com.vaiinilla.app.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminSetupStepsTest {
    private val required =
        listOf(
            AdminSetupStep("Logo y foto del negocio", null, done = true),
            AdminSetupStep("Productos en el menú", "48", done = true),
            AdminSetupStep("Cocina invitada", null, done = true),
            AdminSetupStep("Caja abierta", "Fondo $500", done = true),
        )
    private val optionalPending =
        listOf(
            AdminSetupStep("Entrega a domicilio", "Opcional", done = false, optional = true, key = "domicilio"),
            AdminSetupStep("Pagos con tarjeta", "Opcional", done = false, optional = true, key = "tarjeta"),
        )

    @Test
    fun `un restaurante sin domicilio ni Stripe ya esta listo y la tarjeta se oculta`() {
        val setup = required + optionalPending
        assertTrue(setup.storeReady())
        assertFalse(showsStoreSetup(setup))
    }

    @Test
    fun `mientras falte algo obligatorio la tarjeta sigue`() {
        val setup = required.map { if (it.title == "Caja abierta") it.copy(done = false) else it } + optionalPending
        assertFalse(setup.storeReady())
        assertTrue(showsStoreSetup(setup))
    }

    @Test
    fun `sin pasos no hay tarjeta`() {
        assertFalse(showsStoreSetup(emptyList()))
    }

    @Test
    fun `No quiero quita el paso opcional pendiente y deja los demas`() {
        val setup = required + optionalPending
        val left = setup.withoutDeclined(setOf("tarjeta"))
        assertEquals(required + optionalPending.first(), left)
    }

    @Test
    fun `No quiero no quita un paso obligatorio ni uno ya hecho`() {
        val pendingRequired = AdminSetupStep("Caja abierta", null, done = false, key = "caja")
        val doneOptional = optionalPending.last().copy(done = true)
        val setup = listOf(pendingRequired, doneOptional)
        assertEquals(setup, setup.withoutDeclined(setOf("caja", "tarjeta")))
    }
}
