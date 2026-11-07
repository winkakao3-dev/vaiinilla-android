package com.vaiinilla.app.ui.assistant

import com.vaiinilla.app.domain.model.VenueKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPlaceNoteTest {
    // El mismo patrón con el que el servidor tacha teléfonos antes de mandar la plática al modelo.
    private val phone = Regex("""\+?\d[\d\s().-]{7,}\d""")

    @Test
    fun `nothing to say means no extra message`() {
        assertNull(AssistantPlaceNote.build(AssistantPlaceFacts()))
    }

    @Test
    fun `a hotel room says reception, room charge and kitchen hours`() {
        val note =
            AssistantPlaceNote.build(
                AssistantPlaceFacts(
                    kind = VenueKind.HOTEL,
                    placeName = "Habitación 204",
                    placeIsRoom = true,
                    chargeToRoom = true,
                    kitchenFrom = "07:00",
                    kitchenTo = "23:00",
                ),
            )!!
        assertTrue(note.contains("Habitación 204"))
        assertTrue(note.contains("recepción"))
        assertTrue(note.contains("se carga a mi cuenta del hotel"))
        assertTrue(note.contains("de 07:00 a 23:00"))
        assertFalse(phone.containsMatchIn(note))
    }

    @Test
    fun `a hotel restaurant table is not a room`() {
        val note =
            AssistantPlaceNote.build(AssistantPlaceFacts(kind = VenueKind.HOTEL, placeName = "Mesa 3", payAtEnd = true))!!
        assertTrue(note.contains("restaurante de un hotel"))
        assertFalse(note.contains("recepción"))
        assertTrue(note.contains("pagar al final"))
    }

    @Test
    fun `allergies, limit and cart go in and the note never passes the server limit`() {
        val note =
            AssistantPlaceNote.build(
                AssistantPlaceFacts(
                    isSchool = true,
                    allergens = listOf("Gluten", "Huevo"),
                    hasGuardianBlocks = true,
                    availableToday = "$40",
                    cart = List(40) { "1 × Producto con un nombre bastante largo $it" },
                ),
            )!!
        assertTrue(note.contains("alergia a: gluten, huevo"))
        assertTrue(note.contains("$40"))
        assertTrue(note.length <= AssistantPlaceNote.MAX_LETTERS)
        assertTrue(note.endsWith("Contesta mi siguiente mensaje con esto en cuenta."))
    }

    @Test
    fun `a part that does not fit is skipped and the allergies still go`() {
        val note =
            AssistantPlaceNote.build(
                AssistantPlaceFacts(placeName = "Mesa ".repeat(200), allergens = listOf("Cacahuate")),
            )!!
        assertTrue(note.length <= AssistantPlaceNote.MAX_LETTERS)
        assertFalse(note.contains("Mesa Mesa"))
        assertTrue(note.contains("cacahuate"))
    }
}