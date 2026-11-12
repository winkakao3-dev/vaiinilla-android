package com.vaiinilla.app.data.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class InventorySuppliesTest {
    private fun supply(
        id: Long,
        value: Double,
        active: Boolean,
    ) = InventorySupply(id, "Insumo $id", null, InventoryUnit.KILO, 10.0, 2.0, 30.0, value, false, active, 0)

    @Test
    fun `the in-use value leaves out the supplies taken out of use`() {
        val listed =
            InventorySupplies(
                supplies = listOf(supply(1, 300.0, active = true), supply(2, 750.0, active = false)),
                lowCount = 0,
                totalValue = 1050.0,
            )

        assertEquals(300.0, listed.inUseValue, 0.001)
    }
}