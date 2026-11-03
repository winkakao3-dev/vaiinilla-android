package com.vaiinilla.app.ui.screenshot

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.model.SharedTableParticipant
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.ui.parentmode.childReloadReach
import com.vaiinilla.app.ui.parentmode.childCardTitle
import com.vaiinilla.app.ui.parentmode.childTodayLine
import com.vaiinilla.app.ui.screens.pastOrderStateLabel
import com.vaiinilla.app.ui.screens.pastOrderSubtitle
import com.vaiinilla.app.ui.screens.pastOrderTitle
import com.vaiinilla.app.ui.screens.tableSummaryLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** Los textos nuevos de L10 (ANTERIORES y la mesa compartida) y E09 (cuánto le alcanza la recarga). */
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
    fun `la tarjeta del hijo dice su grupo y cuánto le queda hoy`() {
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
        assertEquals("Sofía · 3°B", childCardTitle(child))
        assertEquals("Hoy le quedan \$18", childTodayLine(child))
        assertEquals(null, childTodayLine(child.copy(controls = null)))
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
    @Test
    fun `en ANTERIORES el renglón dice folio y negocio, y la mesa no se pasa a minúsculas`() {
        val takeAway = ScreenshotFixtures.sampleOrder(state = OrderState.DELIVERED)
        assertEquals("#${takeAway.summary.folio} · Pastelería Vaini", pastOrderTitle(takeAway, "Pastelería Vaini"))
        assertTrue(pastOrderSubtitle(takeAway).contains("para llevar"))
        val atTable = ScreenshotFixtures.sampleOrder(state = OrderState.DELIVERED, destination = OrderDestination.IN_SPACE, spaceId = 4)
        assertTrue(pastOrderSubtitle(atTable).contains("Mesa 4"))
    }

    @Test
    fun `la mesa compartida dice cuánto te falta pagar de tu parte`() {
        fun amounts(
            total: String,
            pending: String,
        ) = SharedTableAmounts(BigDecimal(total), BigDecimal(total) - BigDecimal(pending), BigDecimal(pending))
        val table =
            SharedTable(
                spaceId = 4,
                spaceName = "Mesa 4",
                spaceType = "mesa",
                myAlias = "Ana",
                accountOpen = true,
                participants = listOf(SharedTableParticipant("Ana", isMe = true), SharedTableParticipant("Luis", isMe = false)),
                groups = emptyList(),
                totals = amounts("300", "137"),
                myShare = amounts("137", "137"),
            )
        assertEquals("Tu parte por pagar · \$137", tableSummaryLine(table))
        assertEquals("Tu parte ya está pagada", tableSummaryLine(table.copy(myShare = amounts("137", "0"))))
        assertEquals("2 personas · sin pedidos aún", tableSummaryLine(table.copy(myShare = amounts("0", "0"))))
    }
}