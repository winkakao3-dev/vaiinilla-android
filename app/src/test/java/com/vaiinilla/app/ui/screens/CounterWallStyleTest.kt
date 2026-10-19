package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterWallStyleTest {
    private val size = FloorLogo.SIZE

    /** Centro de un color y la orilla con [border] (pixeles ARGB). */
    private fun grid(border: (Int) -> Int): FloorLogo {
        val pixels =
            IntArray(size * size) { index ->
                val x = index % size
                val y = index / size
                val onEdge = x == 0 || y == 0 || x == size - 1 || y == size - 1
                if (onEdge) border(index) else 0xFF336699.toInt()
            }
        return FloorLogo.of("prueba", pixels)
    }

    @Test
    fun `un logo con orilla lisa o transparente sigue como logo`() {
        val white = grid { 0xFFFFFFFF.toInt() }
        assertEquals(0.0, white.edgeSpread, 0.0)
        assertEquals(CounterWallStyle.Logo, CounterWallStyle.decide(white, 1200, 1200))
        val clear = grid { 0x00000000 }
        assertEquals(CounterWallStyle.Logo, CounterWallStyle.decide(clear, 300, 300))
        // Un JPEG con fondo blanco trae ruido en la orilla y sigue siendo logo.
        val jpeg = grid { if (it % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFF8F6F9.toInt() }
        assertEquals(CounterWallStyle.Logo, CounterWallStyle.decide(jpeg, 800, 600))
    }

    @Test
    fun `una foto llena la pared y una chica lleva fondo desenfocado`() {
        val colors = intArrayOf(0xFF101010.toInt(), 0xFFE0D0B0.toInt(), 0xFF508030.toInt(), 0xFFA02020.toInt())
        val busy = grid { colors[it % 4] }
        assertTrue(busy.edgeSpread > CounterWallStyle.LOGO_EDGE_SPREAD)
        assertEquals(CounterWallStyle.Photo, CounterWallStyle.decide(busy, 1600, 1200))
        assertEquals(CounterWallStyle.SmallPhoto, CounterWallStyle.decide(busy, 1600, 480))
        assertEquals(CounterWallStyle.SmallPhoto, CounterWallStyle.decide(busy, 400, 400))
    }

    @Test
    fun `el color de la orilla es la orilla sobre crema`() {
        assertEquals(0xFFFF8000.toInt(), grid { 0xFFFF8000.toInt() }.edgeColor)
        // Orilla transparente: la pared queda crema.
        assertEquals(0xFFF5EEDE.toInt(), grid { 0x00000000 }.edgeColor)
    }
}
