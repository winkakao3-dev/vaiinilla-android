package com.vaiinilla.app.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLEncoder

/** Las mismas pruebas que `MapsLinkTests.swift` de iOS (CASA-02, CASA-05). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MapsLinkTest {
    @Test
    fun `encuentra el enlace que comparte Google Maps`() {
        val link = MapsLink.find("Pastelería Lupita\nhttps://maps.app.goo.gl/AbCdEf123")
        assertEquals("https://maps.app.goo.gl/AbCdEf123", link)
        assertTrue(MapsLink.isShort(link!!))
    }

    @Test
    fun `ignora enlaces que no son de mapas`() {
        assertNull(MapsLink.find("Mi menú: https://instagram.com/lupita"))
        assertNull(MapsLink.find("Calle Ocampo 123, Centro"))
        assertNull(MapsLink.find("https://www.google.com/search?q=pasteles"))
    }

    @Test
    fun `prefiere el pin al centro del mapa`() {
        val point =
            MapsLink.coordinate(
                "https://www.google.com/maps/place/Pasteler%C3%ADa/@28.6400000,-106.0800000,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d28.6353!4d-106.0889",
            )!!
        assertEquals(28.6353, point.latitude, 0.00001)
        assertEquals(-106.0889, point.longitude, 0.00001)
    }

    @Test
    fun `lee las coordenadas de la consulta`() {
        assertEquals(28.6353, MapsLink.coordinate("https://maps.google.com/?q=28.6353,-106.0889")!!.latitude, 0.00001)
        assertEquals(-106.0889, MapsLink.coordinate("https://maps.apple.com/?ll=28.6353,-106.0889&q=Pin")!!.longitude, 0.00001)
        assertEquals(28.6353, MapsLink.coordinate("https://www.google.com/maps/search/28.6353,+-106.0889")!!.latitude, 0.00001)
    }

    @Test
    fun `sin pin usa el centro del mapa`() {
        val point = MapsLink.coordinate("https://www.google.com/maps/@28.64,-106.08,15z")!!
        assertEquals(28.64, point.latitude, 0.00001)
        assertEquals(-106.08, point.longitude, 0.00001)
    }

    @Test
    fun `lee el lugar cuando no hay coordenadas`() {
        val query = "https://maps.google.com/?q=Calle+Ocampo+123,+Centro,+Chihuahua"
        assertNull(MapsLink.coordinate(query))
        assertEquals("Calle Ocampo 123, Centro, Chihuahua", MapsLink.placeName(query))
        assertEquals("Pastelería Lupita", MapsLink.placeName("https://www.google.com/maps/place/Pasteler%C3%ADa+Lupita"))
    }

    @Test
    fun `sigue el aviso de cookies`() {
        val inner = "https://www.google.com/maps/place/x/data=!3d28.6353!4d-106.0889"
        val consent = "https://consent.google.com/ml?continue=" + URLEncoder.encode(inner, "UTF-8")
        assertEquals(28.6353, MapsLink.coordinate(consent)!!.latitude, 0.00001)
    }

    @Test
    fun `rechaza coordenadas imposibles`() {
        assertNull(MapsLink.coordinate("https://maps.google.com/?q=128.6,-306.1"))
    }
}
