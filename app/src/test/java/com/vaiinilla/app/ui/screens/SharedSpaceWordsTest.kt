package com.vaiinilla.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** C06: en Barra 1 la invitación de Pedidos habla de la barra, no de una mesa compartida. */
class SharedSpaceWordsTest {
    @Test
    fun `a bar is invited as a shared bar`() {
        val words = SharedSpaceWords.of("barra")
        assertEquals("Barra compartida", words.title)
        assertEquals("la barra", words.thePlace)
    }

    @Test
    fun `a table and an unknown type keep the table copy`() {
        assertEquals(SharedSpaceWords("Mesa compartida", "la mesa"), SharedSpaceWords.of("mesa"))
        assertEquals(SharedSpaceWords("Mesa compartida", "la mesa"), SharedSpaceWords.of(null))
    }

    @Test
    fun `a seat uses the masculine article`() {
        assertEquals(SharedSpaceWords("Asiento compartido", "el asiento"), SharedSpaceWords.of("asiento"))
    }
}
