package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.ApiFailure
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cambiar de negocio con el mismo rol no deja campos del negocio anterior a la vista del nuevo. */
class FreshForNewRoleTest {
    private val busy =
        OperationalUiState(
            role = OperationalRole.CASHIER,
            cashSessionOpen = true,
            suggestedCashFloat = "500.00",
            expectedCash = "820.00",
            cashOpeningAmount = "500.00",
            cashOpenedAt = "2026-11-07T01:00:00Z",
            cashSessionCheckFailed = true,
            heartbeatOnline = true,
            queue = QueueSync.Failed(ApiFailure.NoConnection),
            lastSyncedAt = "2026-11-07T02:00:00Z",
            kitchenPreparesUnpaid = false,
            unsyncedOfflineCash = 2,
            collectedToday = CollectedToday(total = "1200.00", payments = emptyList()),
            storeImageNotice = "Logo subido",
            actionNotice = "Comanda #12 lista",
            closeCash = CloseCashUi(closing = true),
            cashChangeNotice = "Cambio: 30.00",
            errorMessage = "No se pudo cobrar.",
        )

    @Test
    fun `another business starts clean, keeping only the device presence signal`() {
        val fresh = busy.freshForNewRole(establishmentChanged = true)

        assertEquals(OperationalUiState(heartbeatOnline = true), fresh)
        assertNull(fresh.expectedCash)
        assertNull(fresh.collectedToday)
        assertTrue(fresh.kitchenPreparesUnpaid)
        assertEquals(0, fresh.unsyncedOfflineCash)
        assertEquals(QueueSync.Pending, fresh.queue)
        assertEquals(CashSession.Unknown, fresh.cashSession)
    }

    @Test
    fun `the same business keeps its last cash change notice`() {
        val fresh = busy.freshForNewRole(establishmentChanged = false)

        assertEquals("Cambio: 30.00", fresh.cashChangeNotice)
        assertNull(fresh.actionNotice)
        assertNull(fresh.closeCash)
        assertFalse(fresh.cashSessionCheckFailed)
    }
}
