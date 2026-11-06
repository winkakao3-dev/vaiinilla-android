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

    @Test
    fun `leading emoji is skipped instead of split`() {
        assertEquals("SR", displayInitials("\uD83E\uDD84 Sofi Ramírez"))
        assertEquals("SO", displayInitials("\uD83E\uDD84Sofi"))
        assertEquals("?", displayInitials("\uD83E\uDD84"))
    }
}
