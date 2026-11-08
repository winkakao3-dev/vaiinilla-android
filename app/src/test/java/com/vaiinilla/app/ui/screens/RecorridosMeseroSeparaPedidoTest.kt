package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.PreparationStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El mesero separa lo que lleva ya de lo que hace Cocina (N10): las bebidas de Caja salen aparte y la comida
 * espera. Corre en la JVM; no cuenta como recorrido en teléfono.
 */
class RecorridosMeseroSeparaPedidoTest {
    private val stations =
        mapOf(
            "café" to PreparationStation.CASHIER,
            "coca-cola" to PreparationStation.NONE,
            "hot dog" to PreparationStation.KITCHEN,
        )

    @Test
    fun n10_lasBebidasDeCajaSalenAparteYLaComidaEsperaACocina() {
        val split = splitByStation("1× Café, 1× Hot dog", stations)
        assertEquals(listOf("Café"), split.counter.map { it.name })
        assertEquals(listOf("Hot dog"), split.kitchen.map { it.name })
    }

    @Test
    fun n10_loQueNoEstaYaHechoSeCuentaComoDeCajaPeroNoPasaPorCocina() {
        val split = splitByStation("2× Coca-Cola", stations)
        assertEquals(listOf("Coca-Cola"), split.counter.map { it.name })
        assertEquals(2, split.counter.single().quantity)
        assertTrue(split.kitchen.isEmpty())
    }

    @Test
    fun n10_loDesconocidoCuentaComoDeCocinaParaNuncaDecirLlevaloYaDeMas() {
        val split = splitByStation("1× Postre del día", stations)
        assertTrue(split.counter.isEmpty())
        assertEquals(listOf("Postre del día"), split.kitchen.map { it.name })
    }

    @Test
    fun n10_sinCantidadEscritaVaUnoYUnaListaVaciaNoParteNada() {
        assertEquals(1, splitByStation("Café", stations).counter.single().quantity)
        val empty = splitByStation("", stations)
        assertTrue(empty.counter.isEmpty() && empty.kitchen.isEmpty())
    }

    @Test
    fun n10_sinCatalogoTodoEsDeCocina() {
        val split = splitByStation("1× Café, 1× Hot dog", productStations(null))
        assertTrue(split.counter.isEmpty())
        assertEquals(2, split.kitchen.size)
    }
}
