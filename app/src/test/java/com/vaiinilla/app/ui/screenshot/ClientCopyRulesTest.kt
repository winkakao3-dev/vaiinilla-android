package com.vaiinilla.app.ui.screenshot

import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.ui.parentmode.childReloadReach
import com.vaiinilla.app.ui.parentmode.childRowLine
import com.vaiinilla.app.ui.screens.pastOrderStateLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** Los textos nuevos de L10 (cancelado en ANTERIORES) y E09 (cuánto le alcanza la recarga). */
class ClientCopyRulesTest {
    @Test
    fun `un cancelado pagado con saldo dice que regresó a tu saldo`() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.CANCELED, paymentMethod = PaymentMethod.BALANCE)
        assertEquals(true, pastOrderStateLabel(order).endsWith(" · a tu saldo"))
    }

    @Test
    fun `un entregado no cambia su etiqueta`() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.DELIVERED, paymentMethod = PaymentMethod.BALANCE)
        assertEquals(false, pastOrderStateLabel(order).contains("·"))
    }

    @Test
    fun `la fila del hijo dice cuánto le queda hoy`() {
        val child =
            Dependent(
                userId = "s",
                name = "Sofía Arreola",
                group = "3°B",
                photoUrl = null,
                studentId = null,
                hasOwnAccount = false,
                balance = BigDecimal("142"),
                controls = DependentControls(dailyLimit = BigDecimal("60"), availableToday = BigDecimal("18")),
            )
        assertEquals(true, childRowLine(child).endsWith("Hoy le quedan \$18"))
        assertEquals(false, childRowLine(child.copy(controls = null)).contains("Hoy"))
    }

    @Test
    fun `la recarga dice para cuántos días le alcanza con su límite`() {
        assertEquals(
            "Queda en \$242: le alcanza para 4 días.",
            childReloadReach(BigDecimal("42"), BigDecimal("200"), BigDecimal("60")),
        )
        assertEquals("Queda en \$242.", childReloadReach(BigDecimal("42"), BigDecimal("200"), null))
        assertNull(childReloadReach(BigDecimal("42"), null, BigDecimal("60")))
    }
}