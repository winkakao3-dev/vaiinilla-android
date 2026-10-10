package com.vaiinilla.app.domain.location

import com.vaiinilla.app.domain.model.GeoCoordinates

interface DeviceLocationProvider {
    /**
     * Ubicación aproximada actual, o `null` si el dispositivo no tiene ninguna
     * (servicios de ubicación apagados y sin última posición conocida).
     * Lanza `SecurityException` si falta el permiso de ubicación.
     */
    suspend fun currentLocation(): GeoCoordinates?
}
