package com.vaiinilla.app.ui.order

import org.junit.Assert.assertEquals
import org.junit.Test

class CartTrayPolicyTest {
    @Test
    fun drinksPastriesAndPlates() {
        assertEquals(TrayItemKind.DRINK, trayItemKind("Café americano"))
        assertEquals(TrayItemKind.DRINK, trayItemKind("Chocolate frío"))
        assertEquals(TrayItemKind.DRINK, trayItemKind("Té verde"))
        assertEquals(TrayItemKind.PASTRY, trayItemKind("Galleta de avena"))
        assertEquals(TrayItemKind.PLATE, trayItemKind("Burrito norteño"))
        assertEquals(TrayItemKind.PLATE, trayItemKind("Tenders de pollo"))
        assertEquals(TrayItemKind.PASTRY, trayItemKind("Galleta de chocolate"))
        assertEquals(TrayItemKind.PLATE, trayItemKind("Filete de res"))
    }
}
