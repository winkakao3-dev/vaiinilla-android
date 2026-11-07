package com.vaiinilla.app.ui.operational

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ciclo de vida de «Marcar lista» en Cocina: espera, deshacer, salir y varias a la vez (B04, N02). */
class PendingReadyQueueTest {
    private val window = PendingReadyQueue.UNDO_WINDOW_MS

    @Test
    fun `una marca no sale antes de acabar su espera y sale una sola vez despues`() {
        val queue = PendingReadyQueue()
        queue.add("a", version = 3, nowMs = 1_000)

        assertTrue(queue.takeDue(1_000 + window - 1).isEmpty())
        assertEquals(listOf(PendingReady("a", 3, 1_000 + window)), queue.takeDue(1_000 + window))
        assertTrue(queue.takeDue(1_000 + window * 2).isEmpty())
    }

    @Test
    fun `deshacer quita la marca y nada sale`() {
        val queue = PendingReadyQueue()
        queue.add("a", 1, 0)

        assertTrue(queue.undo("a"))
        assertFalse(queue.undo("a"))
        assertTrue(queue.takeDue(window * 5).isEmpty())
        assertTrue(queue.snapshot().isEmpty())
    }

    @Test
    fun `salir antes de la espera manda todas, no solo la primera, y en orden`() {
        val queue = PendingReadyQueue()
        queue.add("a", 1, 0)
        queue.add("b", 2, 2_000)
        queue.add("c", 4, 4_000)

        assertEquals(listOf("a", "b", "c"), queue.drain().map { it.orderId })
        assertTrue(queue.drain().isEmpty())
        assertTrue(queue.takeDue(window * 5).isEmpty())
    }

    @Test
    fun `varias marcas con espera cumplida salen juntas`() {
        val queue = PendingReadyQueue()
        queue.add("a", 1, 0)
        queue.add("b", 2, 500)

        assertEquals(listOf("a", "b"), queue.takeDue(500 + window).map { it.orderId })
    }

    @Test
    fun `marcar otra vez reinicia la espera de esa comanda`() {
        val queue = PendingReadyQueue()
        queue.add("a", 1, 0)
        queue.add("a", 2, 5_000)

        assertTrue(queue.takeDue(window).isEmpty())
        assertEquals(2, queue.takeDue(5_000 + window).single().version)
    }

    @Test
    fun `la foto para la pantalla trae lo que espera`() {
        val queue = PendingReadyQueue()
        queue.add("a", 1, 100)

        assertEquals(setOf("a"), queue.snapshot().keys)
        assertEquals(100 + window, queue.snapshot().getValue("a").deadlineMs)
    }
}
