package com.vaiinilla.app.data.contract

import org.junit.Assert.assertEquals
import org.junit.Test

class AllergenTextTest {
    @Test
    fun postgresArrayTextBecomesReadable() {
        assertEquals("", cleanAllergenText("{}"))
        assertEquals("huevo, lactosa", cleanAllergenText("{huevo,lactosa}"))
        assertEquals("Nueces", cleanAllergenText("Nueces"))
        assertEquals("", cleanAllergenText(null))
    }
}
