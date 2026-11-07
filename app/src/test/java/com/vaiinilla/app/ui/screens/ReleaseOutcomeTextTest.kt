package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** #19: al liberar se dice qué pasa con lo pagado. */
class ReleaseOutcomeTextTest {
    @Test
    fun `con algo cobrado y tiempo de sobra dice que no se devuelve y que el tiempo se pierde`() {
        assertEquals(
            "Lo que ya se cobró se queda en la caja del turno: liberar no lo devuelve. " +
                "El tiempo que le quedaba se pierde. La cuenta se cierra y el siguiente cliente empieza una nueva.",
            releaseOutcomeText(somethingPaid = true, timeLeft = true),
        )
    }

    @Test
    fun `sin nada cobrado ni tiempo solo dice que la cuenta se cierra`() {
        assertEquals(
            "No hay nada cobrado en esta cuenta. La cuenta se cierra y el siguiente cliente empieza una nueva.",
            releaseOutcomeText(somethingPaid = false, timeLeft = false),
        )
    }
}
