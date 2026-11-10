package com.vaiinilla.app.data.operational

import com.vaiinilla.app.data.cashcut.RemoteCashCutRepository
import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Los repositorios reales de la caja y de los cortes contra lo que el servidor de pruebas contestó de verdad. */
class RemoteCashSessionRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    private fun money(amount: String?) = BigDecimal(amount)

    @Test
    fun `el cajon cuadra con fondo, venta, entrada y salida, y al cerrar queda el corte del turno y del dia`() {
        val cash = floor.cash()
        cash.activeSession().getOrThrow()?.let { old ->
            cash
                .closeSession(
                    old.id,
                    cash
                        .drawerReport(old.id)
                        .getOrThrow()
                        .summary.expected,
                    floor.key(),
                ).getOrThrow()
        }
        cash.openSession("500.00", floor.key()).getOrThrow()
        floor.open()
        val session = cash.activeSession().getOrThrow()!!
        assertEquals(money("500.00"), money(session.initialAmount))
        assertEquals(session.id, cash.activeSessionId().getOrThrow())
        assertTrue(cash.hasActiveSession().getOrThrow())

        val order = floor.order(floor.product(), 2)
        assertEquals(OrderState.PENDING_PAYMENT, order.summary.state)
        val sale = money(order.summary.total)
        val collected =
            floor
                .orders(
                    "cajero",
                ).collectCash(order.summary.id, "100.00", order.summary.version, floor.key(), null)
                .getOrThrow()
        assertEquals(OrderState.PAID, collected.order.summary.state)
        assertEquals(money("100.00") - sale, money(collected.change))

        cash
            .registerDrawerMovement(
                session.id,
                CashDrawerMovementType.IN,
                "200.00",
                "Cambio del banco",
                floor.key(),
            ).getOrThrow()
        cash
            .registerDrawerMovement(
                session.id,
                CashDrawerMovementType.OUT,
                "50.00",
                "Compra de hielo",
                floor.key(),
            ).getOrThrow()
        val drawer = cash.drawerReport(session.id).getOrThrow()
        assertEquals(money("500.00"), money(drawer.summary.opening))
        assertEquals(sale, money(drawer.summary.cashSales))
        assertEquals(money("200.00"), money(drawer.summary.cashIn))
        assertEquals(money("50.00"), money(drawer.summary.cashOut))
        val expected = money("650.00") + sale
        assertEquals(expected, money(drawer.summary.expected))
        val ice = drawer.movements.single { it.reason == "Compra de hielo" }
        assertTrue(ice.outgoing)
        assertEquals(money("50.00"), money(ice.amount))
        assertFalse(drawer.movements.single { it.reason == "Cambio del banco" }.outgoing)

        val cuts = RemoteCashCutRepository(floor.client("cajero"))
        val running = cuts.shiftCut(session.id).getOrThrow()
        assertFalse(running.final)
        assertEquals(expected, money(running.shift.cash.expected))
        assertEquals(sale, money(running.breakdown.summary.sales))
        assertEquals(1, running.breakdown.summary.orders)

        cash.closeSession(session.id, (expected - money("10.00")).toPlainString(), floor.key()).getOrThrow()
        assertNull(cash.activeSession().getOrThrow())
        assertEquals(expected - money("10.00"), money(cash.lastClose?.counted))
        val closed = cuts.shiftCut(session.id).getOrThrow()
        assertTrue(closed.final)
        assertEquals(money("-10.00"), money(closed.shift.cash.difference))

        val admin = RemoteCashCutRepository(floor.client("admin"))
        val day = admin.dayCut(null).getOrThrow()
        assertTrue(day.shifts.any { it.id == session.id })
        val today = LocalDate.parse(day.date)
        val report = admin.accounting(today, today).getOrThrow()
        assertEquals(day.breakdown.summary.sales, report.breakdown.summary.sales)
        assertTrue(admin.accountingCsv(today, today).getOrThrow().isNotBlank())

        val start = admin.dayStart().getOrThrow()
        assertEquals("05:00", admin.setDayStart("05:00").getOrThrow())
        assertEquals("05:00", admin.dayStart().getOrThrow())
        admin.setDayStart(start ?: "00:00").getOrThrow()
        floor.open()
    }
}
