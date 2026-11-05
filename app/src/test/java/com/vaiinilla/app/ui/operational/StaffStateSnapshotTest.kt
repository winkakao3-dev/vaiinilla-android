package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** En vivo, USAGI: volver a Por cobrar no regresa a «Revisando la caja» ni quita «Cerrar caja». */
class StaffStateSnapshotTest {
    private val known =
        OperationalUiState(
            role = OperationalRole.CASHIER,
            cashSessionOpen = true,
            cashOpeningAmount = "500.00",
            expectedCash = "820.00",
            lastRefreshOk = true,
        )

    private fun snapshot(takenAt: Long = 1_000L) = StaffStateSnapshot(OperationalRole.CASHIER, "usagi", takenAt, known)

    @Test
    fun `sirve para el mismo rol y negocio poco despues`() {
        assertTrue(snapshot().matches(OperationalRole.CASHIER, "usagi", 1_000L + 60_000L))
    }

    @Test
    fun `no sirve para otro rol, otro negocio o mucho despues`() {
        assertFalse(snapshot().matches(OperationalRole.KITCHEN, "usagi", 2_000L))
        assertFalse(snapshot().matches(OperationalRole.CASHIER, "otro", 2_000L))
        val late = 1_000L + StaffStateSnapshot.STAFF_SNAPSHOT_TTL_MS + 1
        assertFalse(snapshot().matches(OperationalRole.CASHIER, "usagi", late))
    }

    @Test
    fun `el estado recien puesto conserva la caja abierta y la ultima carga`() {
        val reset = OperationalUiState(role = OperationalRole.CASHIER)
        val kept = reset.keepKnownStaffState(known, restoreOrders = true)
        assertEquals(true, kept.cashSessionOpen)
        assertEquals("500.00", kept.cashOpeningAmount)
        assertEquals("820.00", kept.expectedCash)
        assertEquals(true, kept.lastRefreshOk)
    }

    @Test
    fun `con el mismo rol no toca la fila ni el resultado de la carga`() {
        val reset = OperationalUiState(role = OperationalRole.CASHIER)
        val kept = reset.keepKnownStaffState(known, restoreOrders = false)
        assertEquals(true, kept.cashSessionOpen)
        assertNull(kept.lastRefreshOk)
    }

    @Test
    fun `lo nuevo del servidor gana sobre lo conocido`() {
        val fresh = OperationalUiState(role = OperationalRole.CASHIER, cashSessionOpen = false)
        assertEquals(false, fresh.keepKnownStaffState(known, restoreOrders = true).cashSessionOpen)
    }
}
