package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.ui.operational.unpaidAccountKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class CashierUnpaidAccountsTest {
    @Test
    fun `el cambio sale de lo que falta, no del total`() {
        assertEquals("20.00", unpaidChange("100", "80.00")?.setScale(2)?.toPlainString())
        assertEquals("0.00", unpaidChange("80", "80.00")?.setScale(2)?.toPlainString())
        assertNull(unpaidChange("50", "80.00"))
        assertNull(unpaidChange("", "80.00"))
    }

    @Test
    fun `N08 con propina el efectivo tiene que cubrir lo que falta mas la propina`() {
        assertEquals("12.00", unpaidChange("100", "80.00", BigDecimal("8.00"))?.setScale(2)?.toPlainString())
        assertNull(unpaidChange("85", "80.00", BigDecimal("8.00")))
    }

    @Test
    fun `el turno abierto y la deuda de turnos cerrados del mismo espacio son cuentas distintas`() {
        val turn = AdminUnpaidAccount(4, "Pista 4", 1, "100.00", 0, abandoned = false, sessionId = "s1")
        val closed = turn.copy(abandoned = true, sessionId = null)
        assertNotEquals(unpaidAccountKey(turn), unpaidAccountKey(closed))
    }
}
