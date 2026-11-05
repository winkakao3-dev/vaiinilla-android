package com.vaiinilla.app.ui.operational

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CashSessionTest {
    @Test
    fun `the cash session reads as one state`() {
        assertEquals(CashSession.Unknown, OperationalUiState().cashSession)
        assertEquals(CashSession.CheckFailed, OperationalUiState(cashSessionCheckFailed = true).cashSession)
        assertEquals(CashSession.Closed, OperationalUiState(cashSessionOpen = false).cashSession)
        assertEquals(CashSession.Open, OperationalUiState(cashSessionOpen = true).cashSession)
        assertEquals(
            CashSession.ConfirmingClose,
            OperationalUiState(cashSessionOpen = true, closeCash = CloseCashUi()).cashSession,
        )
        assertEquals(
            CashSession.Closing,
            OperationalUiState(cashSessionOpen = true, closeCash = CloseCashUi(closing = true)).cashSession,
        )
        assertEquals(
            CashSession.CloseBlockedByAccounts,
            OperationalUiState(cashSessionOpen = true, closeCash = CloseCashUi(blockedByAccounts = true)).cashSession,
        )
    }

    @Test
    fun `impossible combinations resolve to what the server said`() {
        // Una hoja de cierre con la caja cerrada es solo una caja cerrada.
        assertEquals(
            CashSession.Closed,
            OperationalUiState(cashSessionOpen = false, closeCash = CloseCashUi()).cashSession,
        )
        // Un error de lectura viejo no tapa una caja que ya se sabe abierta.
        assertEquals(
            CashSession.Open,
            OperationalUiState(cashSessionOpen = true, cashSessionCheckFailed = true).cashSession,
        )
        assertTrue(CashSession.Closing.isOpen)
        assertFalse(CashSession.CheckFailed.isOpen)
    }
}
