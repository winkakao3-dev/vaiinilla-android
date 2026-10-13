package com.vaiinilla.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SeaGlassColorTest {
    @Test
    fun oklchMatchesKnownColors() {
        val white = oklch(1.0, 0.0, 0.0)
        assertEquals(1.0, white.red, 0.002)
        assertEquals(1.0, white.blue, 0.002)
        // sRGB rojo puro es oklch(0.628 0.2577 29.23).
        val red = oklch(0.628, 0.2577, 29.23)
        assertEquals(1.0, red.red, 0.01)
        assertEquals(0.0, red.green, 0.01)
        assertEquals(0.0, red.blue, 0.01)
    }

    @Test
    fun outOfGamutKeepsHueByLoweringChroma() {
        val lime = oklch(0.95, 0.4, 128.0)
        listOf(lime.red, lime.green, lime.blue).forEach { assertTrue(it in 0.0..1.0) }
        assertTrue(lime.green > lime.blue)
    }

    @Test
    fun inkIsReadableOnEveryToneDayAndNight() {
        val tones =
            listOf(
                SeaGlassTone.lima,
                SeaGlassTone.matcha,
                SeaGlassTone.menta,
                SeaGlassTone.vainilla,
                SeaGlassTone.horchata,
                SeaGlassTone.mango,
                SeaGlassTone.cafe,
                SeaGlassTone.jamaica,
                SeaGlassTone.fresa,
                SeaGlassTone.cielo,
                SeaGlassTone.lavanda,
                SeaGlassTone.mora,
            )
        for (tone in tones) {
            for (night in listOf(false, true)) {
                val shades = seaGlassShades(tone, night)
                assertTrue(
                    "tono ${tone.hue} noche $night",
                    shades.ink.contrast(shades.mid) >= 4.5,
                )
                assertTrue(
                    "elegido ${tone.hue} noche $night",
                    shades.ink.contrast(seaGlassShades(tone.picked, night).mid) >= 4.5,
                )
            }
        }
    }

    @Test
    fun nightNeutralsAreBlackishAndLimaKeepsItsDayGreen() {
        val vainilla = seaGlassShades(SeaGlassTone.vainilla, night = true).mid
        assertEquals("sin tinte azulado", vainilla.red, vainilla.blue, 0.02)
        assertTrue(vainilla.luminance < 0.06)
        assertEquals(
            seaGlassShades(SeaGlassTone.lima, night = false).mid,
            seaGlassShades(SeaGlassTone.lima, night = true).mid,
        )
    }

    @Test
    fun bottomIsLighterThanTheMiddle() {
        val shades = seaGlassShades(SeaGlassTone.lavanda, night = false)
        assertTrue(shades.bottom.luminance > shades.mid.luminance)
        assertTrue(shades.top.luminance > shades.mid.luminance)
    }

    @Test
    fun edgeShiftsTowardTheNearestYellowOrCyan() {
        assertEquals(8.0, SeaGlassRecipe.edgeHueShift(318.0), 1.0)
        assertEquals(-33.0, SeaGlassRecipe.edgeHueShift(235.0), 1.0)
        assertEquals(39.0, SeaGlassRecipe.edgeHueShift(55.0), 1.0)
        assertTrue(abs(SeaGlassRecipe.edgeHueShift(200.0)) < 0.001)
    }
}
