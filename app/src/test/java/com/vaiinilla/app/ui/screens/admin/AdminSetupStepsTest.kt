package com.vaiinilla.app.ui.screens.admin

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
            AdminSetupStep("Entrega a domicilio", "Opcional", done = false, optional = true),
            AdminSetupStep("Pagos con tarjeta", "Opcional", done = false, optional = true),
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
}
