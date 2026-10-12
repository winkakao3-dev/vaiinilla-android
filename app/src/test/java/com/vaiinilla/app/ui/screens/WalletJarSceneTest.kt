package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletJarSceneTest {
    @Test
    fun emptyJarWithoutBalance() {
        assertEquals(0f, jarFillLevel(null))
        assertEquals(0f, jarFillLevel("0.00"))
        assertEquals(0f, jarFillLevel("abc"))
    }

    @Test
    fun jarFillsWithBalanceAndTopsOut() {
        val little = jarFillLevel("20.00")
        val more = jarFillLevel("500.00")
        assertTrue(little > 0.12f)
        assertTrue(more > little)
        assertEquals(1f, jarFillLevel("100000"))
        assertTrue(jarFillLevel("1,500.00") > more)
    }
}
