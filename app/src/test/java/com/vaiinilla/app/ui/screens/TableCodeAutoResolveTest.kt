package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableCodeAutoResolveTest {
    @Test
    fun `completing the fourth digit validates once`() {
        assertTrue(shouldAutoResolveTableCode(previous = "123", next = "1234", resolving = false))
    }

    @Test
    fun `no validation before the code is complete or while one is running`() {
        assertFalse(shouldAutoResolveTableCode(previous = "12", next = "123", resolving = false))
        assertFalse(shouldAutoResolveTableCode(previous = "123", next = "1234", resolving = true))
    }

    @Test
    fun `deleting back to four digits or typing past four does not validate again`() {
        assertFalse(shouldAutoResolveTableCode(previous = "12345", next = "1234", resolving = false))
        assertFalse(shouldAutoResolveTableCode(previous = "1234", next = "12345", resolving = false))
    }
}
