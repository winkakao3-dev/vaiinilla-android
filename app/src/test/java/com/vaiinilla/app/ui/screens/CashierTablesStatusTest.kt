package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashierTablesStatusTest {
    @Test
    fun `with tables there is nothing to explain`() {
        assertNull(cashierTablesStatus(hasTables = true, loading = false, errorMessage = null, retryError = null))
        assertNull(
            cashierTablesStatus(hasTables = true, loading = true, errorMessage = "Sin conexión.", retryError = null),
        )
    }

    @Test
    fun `without tables it says they are loading until the board answers`() {
        assertEquals(
            CashierTablesStatus.Loading,
            cashierTablesStatus(hasTables = false, loading = true, errorMessage = null, retryError = null),
        )
    }

    @Test
    fun `a failed board shows the error instead of looking like a business without tables`() {
        assertEquals(
            CashierTablesStatus.Failed("Sin conexión.", retrying = false),
            cashierTablesStatus(hasTables = false, loading = false, errorMessage = "Sin conexión.", retryError = null),
        )
    }

    @Test
    fun `while retrying the card keeps the previous error and says it is loading`() {
        assertEquals(
            CashierTablesStatus.Failed("Sin conexión.", retrying = true),
            cashierTablesStatus(hasTables = false, loading = true, errorMessage = null, retryError = "Sin conexión."),
        )
    }

    @Test
    fun `a loaded board without tables says the business has none`() {
        assertEquals(
            CashierTablesStatus.Empty,
            cashierTablesStatus(hasTables = false, loading = false, errorMessage = null, retryError = null),
        )
    }
}
