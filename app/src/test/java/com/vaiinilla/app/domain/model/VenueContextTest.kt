package com.vaiinilla.app.domain.model

import com.vaiinilla.app.core.venue.VenueContextStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VenueContextTest {
    @Test
    fun `un boliche renta pistas, pregunta cuántos juegan y dibuja la pista`() {
        val venue = VenueContext.of("boliche")

        assertTrue(venue.isBowling)
        assertEquals("pista", venue.vocabulary.court)
        assertEquals("Pistas", venue.vocabulary.courtsTitle)
        assertTrue(venue.capabilities.asksPlayerCount)
        assertTrue(venue.capabilities.drawsLanes)
        assertTrue(venue.capabilities.rentsByTheMoment)
        assertFalse(venue.capabilities.flagsRentalWordsInMenu)
        assertEquals("Quedan 5 min de pista.", venue.lastMinutesNotice(5))
    }

    @Test
    fun `restaurante, escuela, club y un tipo desconocido conservan el texto de siempre`() {
        listOf("cafeteria", "restaurante", "escuela", "club", "pasteleria", "hotel", "", null).forEach { type ->
            val venue = VenueContext.of(type)

            assertEquals("tipo $type", VenueContext.STANDARD, venue)
            assertEquals("cancha", venue.vocabulary.court)
            assertEquals("Canchas", venue.vocabulary.courtsTitle)
            assertFalse(venue.capabilities.asksPlayerCount)
            assertFalse(venue.capabilities.drawsLanes)
            assertTrue(venue.capabilities.flagsRentalWordsInMenu)
            assertEquals("Quedan 5 min: los reflectores ya bajaron.", venue.lastMinutesNotice(5))
        }
    }

    @Test
    fun `el vocabulario del espacio sigue al negocio`() {
        val bowling = VenueContext.of("boliche")
        val padel = VenueContext.of("club")

        assertEquals("tu pista", SpaceCopy.yourPlace(bowling, "cancha"))
        assertEquals("En la pista", SpaceCopy.destinationTitle(bowling, "cancha"))
        assertEquals("Pista", SpaceCopy.kindLabel(bowling, "cancha"))
        assertEquals("Selecciona tu pista", SpaceCopy.pickPrompt(bowling, "cancha"))
        assertEquals("tu cancha", SpaceCopy.yourPlace(padel, "cancha"))
        assertEquals("Mesa", SpaceCopy.kindLabel(bowling, "mesa"))
    }

    @Test
    fun `el almacén publica el negocio y no emite si el tipo no cambia`() {
        val store = VenueContextStore()
        assertSame(VenueContext.STANDARD, store.current)

        store.update("boliche")
        assertTrue(store.context.value.isBowling)

        val bowling = store.current
        store.update("boliche")
        assertSame(bowling, store.current)

        store.update("escuela")
        assertFalse(store.current.isBowling)
    }
}
