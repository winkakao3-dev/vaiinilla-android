package com.vaiinilla.app.data.hotel

/**
 * Qué repositorio de hotel usa la app. Mientras el backend de hoteles no esté desplegado en el servidor al que
 * apunta la APK, la app usa [FakeHotelRepository]. Cuando B1 y B2 estén en el backend de pruebas, se cambia
 * `USE_FAKE` a `false` y todas las pantallas de hotel pasan a hablar con el servidor sin tocar nada más.
 */
object HotelBackend {
    const val USE_FAKE = true
}