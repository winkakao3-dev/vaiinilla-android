package com.vaiinilla.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pistas con precio y productos que no son renta (U01): en un boliche las canchas se llaman pistas, las pistas se
 * rentan por momento y «Renta de zapatos» o «Juego por persona» son productos que no avisan que parecen una renta.
 * Corre en la JVM; no cuenta como recorrido en teléfono.
 */
class RecorridosBolichePistasTest {
    private val bowling = VenueContext.of("boliche")
    private val standard = VenueContext.of("restaurante")

    @Test
    fun u01_enUnBolicheLasCanchasSeLlamanPistasYSeRentanPorMomento() {
        assertEquals(VenueKind.BOWLING, bowling.kind)
        assertEquals("pista", bowling.vocabulary.court)
        assertEquals("Pistas", bowling.vocabulary.courtsTitle)
        assertTrue(bowling.capabilities.rentsByTheMoment)
        assertTrue(bowling.capabilities.asksPlayerCount)
    }

    @Test
    fun u01_enUnBolicheElMenuNoMarcaZapatosNiJuegoPorPersonaComoRenta() {
        assertFalse(bowling.capabilities.flagsRentalWordsInMenu)
        assertTrue(
            bowling.vocabulary.menuRentalAdvice.contains(
                "Juegos por persona y renta de zapatos sí van aquí como producto",
            ),
        )
    }

    @Test
    fun u01_enOtroNegocioRentarCanchasNoSeCreaComoProducto() {
        assertTrue(standard.capabilities.flagsRentalWordsInMenu)
        assertEquals("cancha", standard.vocabulary.court)
        assertTrue(standard.vocabulary.menuRentalAdvice.contains("No crees la renta como producto"))
        assertFalse(standard.capabilities.rentsByTheMoment)
    }

    @Test
    fun u01_unTipoDesconocidoCaeEnNegocioNormal() {
        assertEquals(VenueKind.STANDARD, VenueContext.of("pasteleria").kind)
        assertEquals(VenueKind.STANDARD, VenueContext.of(null).kind)
    }
}
