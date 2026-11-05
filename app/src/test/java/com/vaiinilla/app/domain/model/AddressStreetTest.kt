package com.vaiinilla.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressStreetTest {
    @Test
    fun aPostalCodeOnlyAddressAsksForTheStreet() {
        val pin = DeliveryAddress("06700, Roma Norte, Ciudad de México", phone = "6141234567")
        assertEquals("", pin.street)
        assertEquals("Escribe la calle y el número de tu casa.", pin.problem)

        val area = AddressStreet.area(pin.address)
        val typed = pin.copy(address = AddressStreet.join("Av. Álvaro Obregón 12", area))
        assertEquals("Av. Álvaro Obregón 12, 06700, Roma Norte, Ciudad de México", typed.address)
        assertEquals("Av. Álvaro Obregón 12", typed.street)
        assertNull(typed.problem)
    }

    @Test
    fun aStreetWithoutNumberIsNotEnoughAndIsNotRepeated() {
        val pin = DeliveryAddress("Calle Roble, Centro, Chihuahua", phone = "6141234567")
        assertEquals("", pin.street)
        val area = AddressStreet.area(pin.address)
        assertEquals("Calle Roble 5, Centro, Chihuahua", AddressStreet.join("Calle Roble 5", area))
        assertEquals("Roble s/n", DeliveryAddress("Roble s/n, Centro", phone = "6141234567").street)
    }

    @Test
    fun theMapFallbackKeepsNoCoordinatesInTheText() {
        val pin = DeliveryAddress("${AddressStreet.LOCATION_PREFIX}28.63530, -106.08890", phone = "6141234567")
        assertEquals("", pin.street)
        assertEquals("Aldama 1408", AddressStreet.join("Aldama 1408", AddressStreet.area(pin.address)))
    }

    @Test
    fun aFullAddressIsUnchanged() {
        val full = DeliveryAddress("Aldama 1408, Centro", phone = "6140000000")
        assertEquals("Aldama 1408", full.street)
        assertNull(full.problem)
    }
}
