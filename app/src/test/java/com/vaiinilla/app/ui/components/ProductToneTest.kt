package com.vaiinilla.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductToneTest {
    @Test
    fun theProductWinsOverAWhiteBackground() {
        val white = 0xFFFFFFFF.toInt()
        val red = 0xFFD03020.toInt()
        val pixels = IntArray(100) { if (it < 20) red else white }
        assertEquals(red, dominantTone(pixels))
    }

    @Test
    fun aGreyPhotoKeepsItsGrey() {
        val grey = 0xFF808080.toInt()
        assertEquals(grey, dominantTone(IntArray(16) { grey }))
    }

    @Test
    fun noPixelsNoTone() {
        assertNull(dominantTone(IntArray(0)))
    }
}
