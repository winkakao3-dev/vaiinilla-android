package com.vaiinilla.app

import com.vaiinilla.app.ui.profile.displayInitials
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayInitialsTest {
    @Test
    fun `blank becomes question mark`() {
        assertEquals("?", displayInitials("  "))
    }

    @Test
    fun `single word takes two letters`() {
        assertEquals("DA", displayInitials("David"))
    }

    @Test
    fun `two words take first and last initial`() {
        assertEquals("WK", displayInitials("Win Kakao"))
    }
}
