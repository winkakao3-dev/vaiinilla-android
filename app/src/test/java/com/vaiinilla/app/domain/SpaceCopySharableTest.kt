package com.vaiinilla.app.domain

import com.vaiinilla.app.domain.model.SpaceCopy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceCopySharableTest {
    @Test
    fun `mesa y barra son compartibles, sin tipo tambien`() {
        assertTrue(SpaceCopy.isSharable(null))
        assertTrue(SpaceCopy.isSharable("mesa"))
        assertTrue(SpaceCopy.isSharable("barra"))
    }

    @Test
    fun `cancha, carril y asiento van directo al menu`() {
        assertFalse(SpaceCopy.isSharable("cancha"))
        assertFalse(SpaceCopy.isSharable("drive_thru"))
        assertFalse(SpaceCopy.isSharable("asiento"))
    }
}
