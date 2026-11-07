package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** L01: con letra grande «Medianoche» no cabía en una muestra de cuatro: van de dos en dos. */
class ThemeTilesLargeFontTest {
    @Test
    fun `con letra normal van las cuatro en un renglon`() {
        assertEquals(VaiinillaThemeMode.entries.size, themeTilesPerRow(1f))
    }

    @Test
    fun `con letra 1_3 van de dos en dos`() {
        assertEquals(2, themeTilesPerRow(1.3f))
    }
}
