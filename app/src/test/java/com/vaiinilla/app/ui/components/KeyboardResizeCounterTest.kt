package com.vaiinilla.app.ui.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardResizeCounterTest {
    private var mode = PAN
    private val set: (Int) -> Unit = { mode = it }

    @After
    fun reset() = KeyboardResizeCounter.resetForTest()

    @Test
    fun `una pantalla encoge y al salir regresa el modo de antes`() {
        val token = KeyboardResizeCounter.acquire(mode, set, RESIZE)
        assertEquals(RESIZE, mode)
        KeyboardResizeCounter.release(token, set)
        assertEquals(PAN, mode)
    }

    @Test
    fun `al pasar del asistente al carrito la ventana sigue encogiendo`() {
        val assistant = KeyboardResizeCounter.acquire(mode, set, RESIZE)
        // El carrito entra antes de que el asistente se vaya.
        val cart = KeyboardResizeCounter.acquire(mode, set, RESIZE)
        KeyboardResizeCounter.release(assistant, set)
        assertEquals(RESIZE, mode)
        KeyboardResizeCounter.release(cart, set)
        assertEquals(PAN, mode)
    }

    private companion object {
        const val PAN = 32
        const val RESIZE = 16
    }
}
