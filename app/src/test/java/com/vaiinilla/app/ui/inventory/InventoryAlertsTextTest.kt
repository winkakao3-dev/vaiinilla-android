package com.vaiinilla.app.ui.inventory

import com.vaiinilla.app.data.inventory.InventoryAlertSupply
import com.vaiinilla.app.data.inventory.InventoryAlerts
import com.vaiinilla.app.data.inventory.InventorySoldOut
import com.vaiinilla.app.data.inventory.InventoryUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryAlertsTextTest {
    private fun supply(
        name: String,
        stock: Double,
        daily: Double,
        phone: String? = null,
    ) = InventoryAlertSupply(
        1,
        name,
        InventoryUnit.ML,
        stock,
        2000.0,
        daily,
        null,
        null,
        phone?.let {
            "Abarrotes Rivera"
        },
        phone,
    )

    @Test
    fun `lo agotado va primero y nombra el insumo que falta`() {
        val alerts =
            InventoryAlerts(
                7,
                tomorrow = listOf(supply("Clamato", 640.0, 2400.0, "614 123 4567"), supply("Corona", 22.0, 40.0)),
                soldOut = listOf(InventorySoldOut(31, "Michelada", listOf("Clamato"))),
                belowMinimum = emptyList(),
            )
        assertEquals("Se acabó: Clamato", alertsHeadline(alerts))
        assertEquals("Michelada ya salió del menú. Para mañana falta Clamato y Corona.", alertsDetail(alerts))
        assertEquals("614 123 4567", alerts.firstSupplier?.supplierPhone)
    }

    @Test
    fun `un solo insumo dice cuánto hay y cuánto se usa`() {
        val alerts = InventoryAlerts(7, listOf(supply("Clamato", 640.0, 2400.0)), emptyList(), emptyList())
        assertEquals("Clamato no alcanza para mañana", alertsHeadline(alerts))
        assertEquals("Tienes 640 ml; mañana se usan unos 2,400 ml.", alertsDetail(alerts))
        assertNull(alerts.firstSupplier)
        assertEquals("Clamato, Corona y 2 más", namesLabel(listOf("Clamato", "Corona", "Limón", "Sal")))
        assertEquals("1.5 kg", inventoryAmount(1.5, InventoryUnit.KILO))
    }
}
