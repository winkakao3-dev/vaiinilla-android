package com.vaiinilla.app.ui.screenshot

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.screens.pastOrderStateLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guardia de la clase «inversión semántica de UI» (Z01, P08, C05): el valor guardado es correcto pero la
 * pantalla enseña otro letrero. Recorre TODAS las combinaciones estado × forma de pago, así un estado nuevo
 * sin letrero, o dos estados distintos con el mismo texto, truenan aquí y no en el teléfono de un cliente.
 */
class StateToTextGuardTest {
    private fun label(
        state: OrderState,
        method: PaymentMethod = PaymentMethod.CASH,
    ) = pastOrderStateLabel(ScreenshotFixtures.sampleOrder(state = state, paymentMethod = method))

    @Test
    fun `todo estado con toda forma de pago tiene letrero visible`() {
        for (state in OrderState.entries) {
            for (method in PaymentMethod.entries) {
                assertTrue("$state/$method sin letrero", label(state, method).isNotBlank())
            }
        }
    }

    @Test
    fun `los estados finales no comparten letrero`() {
        val finales = listOf(OrderState.DELIVERED, OrderState.CANCELED, OrderState.NOT_PICKED_UP, OrderState.EXPIRED)
        val textos = finales.map { label(it) }
        assertEquals("letreros repetidos: $textos", textos.size, textos.toSet().size)
    }

    @Test
    fun `entregado nunca se lee igual que un estado activo`() {
        val entregado = label(OrderState.DELIVERED)
        for (state in listOf(OrderState.PAID, OrderState.PREPARING, OrderState.READY, OrderState.ON_THE_WAY)) {
            assertNotEquals("$state se lee como entregado", entregado, label(state))
        }
    }

    @Test
    fun `ningun letrero menciona un destino que el estado no tiene`() {
        // Cancelado en efectivo no regresa dinero a ningún lado: no puede decir «a tu saldo» ni «a tu tarjeta».
        val efectivo = label(OrderState.CANCELED, PaymentMethod.CASH)
        assertTrue(efectivo, !efectivo.contains("saldo") && !efectivo.contains("tarjeta"))
    }
}
