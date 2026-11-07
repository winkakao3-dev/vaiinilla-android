package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Test

/** C06: en Barra 1 la invitación de Pedidos habla de la barra, no de una mesa compartida. */
class SharedSpaceWordsTest {
    private val venue = VenueContext.STANDARD

    @Test
    fun `a bar is invited as a shared bar`() {
        val words = SharedSpaceWords.of(venue, "barra")
        assertEquals("Barra compartida", words.title)
        assertEquals("la barra", words.thePlace)
    }

    @Test
    fun `a table and an unknown type keep the table copy`() {
        assertEquals(SharedSpaceWords("Mesa compartida", "la mesa"), SharedSpaceWords.of(venue, "mesa"))
        assertEquals(SharedSpaceWords("Mesa compartida", "la mesa"), SharedSpaceWords.of(venue, null))
    }

    @Test
    fun `a seat uses the masculine article`() {
        assertEquals(SharedSpaceWords("Asiento compartido", "el asiento"), SharedSpaceWords.of(venue, "asiento"))
    }
}
