package com.vaiinilla.app.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CremeStatRowLayoutTest {
    @Test
    fun `four stats on a narrow phone with larger text are stacked`() {
        // OnePlus CPH2493: 354 dp de ancho con la letra al 115 % partía «Producto s».
        assertTrue(cremeStatsStack(screenWidthDp = 354, fontScale = 1.15f, count = 4))
    }

    @Test
    fun `four stats fit in columns on a common phone`() {
        assertFalse(cremeStatsStack(screenWidthDp = 411, fontScale = 1f, count = 4))
        assertFalse(cremeStatsStack(screenWidthDp = 360, fontScale = 1f, count = 4))
    }

    @Test
    fun `three stats still fit on the narrow phone`() {
        assertFalse(cremeStatsStack(screenWidthDp = 354, fontScale = 1.15f, count = 3))
    }

    @Test
    fun `very large system text always stacks`() {
        assertTrue(cremeStatsStack(screenWidthDp = 600, fontScale = 1.5f, count = 2))
    }
}
