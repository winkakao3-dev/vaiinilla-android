package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.PreparationStation
import org.junit.Assert.assertEquals
import org.junit.Test

class WaiterStationSplitTest {
    private val stations =
        mapOf(
            "coca-cola de vidrio" to PreparationStation.CASHIER,
            "agua de jamaica" to PreparationStation.CASHIER,
            "crepa de cajeta" to PreparationStation.KITCHEN,
        )

    @Test
    fun `lo de caja se lleva ya y la comida espera a cocina`() {
        val split = splitByStation("2× Coca-Cola de vidrio, 1× Agua de jamaica, 2× Crepa de cajeta", stations)
        assertEquals(listOf(SummaryLine(2, "Coca-Cola de vidrio"), SummaryLine(1, "Agua de jamaica")), split.counter)
        assertEquals(listOf(SummaryLine(2, "Crepa de cajeta")), split.kitchen)
    }

    @Test
    fun `lo que no esta en el catalogo cuenta como de cocina`() {
        val split = splitByStation("1× Agua de jamaica, 1× Platillo nuevo", stations)
        assertEquals(listOf("Agua de jamaica"), split.counter.map { it.name })
        assertEquals(listOf("Platillo nuevo"), split.kitchen.map { it.name })
        assertEquals(emptyList<SummaryLine>(), splitByStation("1× Agua de jamaica", emptyMap()).counter)
    }
}
